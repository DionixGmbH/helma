# Performance Tuning

Practical guidance for tuning Helma in production. Most defaults are fine; this page covers the knobs worth turning.

## JVM Settings

### Heap

Set `-Xmx` to a value appropriate to your workload. Helma's per-request memory is modest, but the NodeManager cache and compiled prototype scripts add up:

```bash
JAVA_OPTS="-Xms2g -Xmx4g" ./bin/helma
```

| App scale | Suggested `-Xmx` |
|---|---|
| Dev / tiny | 256m - 1g |
| Small production (< 1M req/day) | 1g - 2g |
| Medium (1M - 100M req/day) | 4g - 8g |
| Large | 16g+ |

Set `-Xms = -Xmx` to avoid heap-resizing pauses.

### GC

For Java 25, the **G1 GC** is the default and works well for most Helma workloads. For latency-sensitive apps, try **ZGC** (low pause times):

```bash
JAVA_OPTS="-Xmx8g -XX:+UseZGC"
```

For batch/throughput-oriented apps, the **Parallel GC**:

```bash
JAVA_OPTS="-Xmx8g -XX:+UseParallelGC"
```

Monitor GC with `-verbose:gc -Xlog:gc*` or a JMX-aware tool (VisualVM, JConsole, Async Profiler).

### Other JVM Args

```bash
JAVA_OPTS="
    -Xms2g -Xmx4g
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -Duser.timezone=UTC
    -Dfile.encoding=UTF-8
    -Djava.security.egd=file:/dev/urandom
    -Dorg.eclipse.jetty.LEVEL=WARN
"
```

`-Djava.security.egd=file:/dev/urandom` avoids stalls on first random-number generation (uses non-blocking urandom).

## Helma Threading

In `app.properties`:

```properties
# Max concurrent requests per app
maxThreads = 24

# Max time before a request is killed
requestTimeout = 30
```

Sizing `maxThreads`:

- For CPU-bound work: ~2× CPU count
- For I/O-bound work: 4× - 16× CPU count
- For a mix: start at 4× CPU count, tune from there

Higher `maxThreads` means more memory (each thread has its own scripting engine). Don't set it absurdly high.

## NodeManager Cache

```properties
# app.properties
cacheNodes = 10000      # default 1000 — increase for large datasets
```

Each cached Node is small but adds up. For an app with 50k active HopObjects, set `cacheNodes = 50000` or higher.

Monitor cache effectiveness:

```javascript
function cache_stats_action() {
    res.write("Cache size: " + app.cacheusage + "\n");
}
```

## Compilation Mode

```properties
# Production
optLevel = 9            # max Rhino compilation

# Development
optLevel = -1           # interpreted, faster reload
```

The trade-off: `-1` (interpreted) restarts faster after edits but each function call is slower at runtime. `9` (compiled) takes longer to start but runs faster. For production, set `9`.

## Update Interval

```properties
# Production: poll for code changes less often
updateInterval = 60000      # 1 minute

# Development: poll on every request (default)
updateInterval = 1000
```

Setting `updateInterval = -1` disables hot reload entirely — code changes require an app restart.

## Database Connection Pool

Helma's DbSource pool is unbounded. For most apps this is fine — each request acquires one connection per touched DbSource.

If your database has connection limits (e.g. PostgreSQL `max_connections`), put a connection pooler (PgBouncer, ProxySQL) between Helma and the DB.

## Profiling

For dev/staging:

```properties
profile = true
```

Run a representative request; the per-function timings appear in the log. Hunt for hotspots.

For production-quality profiling: Async Profiler.

```bash
# Attach to running Helma JVM
async-profiler -d 60 -o flamegraph.html <pid>
```

## Disable Tracer in Production

```properties
# Always:
tracer = false
profile = false
rhino.debug = false
debug = false
```

## HTTP Caching

Use `res.lastModified` / `res.etag` aggressively:

```javascript
function show_action() {
    res.lastModified = this.modified;
    if (res.notModified) return;
    renderSkin("main");
}
```

A 304 response is much faster than re-rendering — measure this. For a busy page, 304s should be > 50% of hits in steady state.

## Static File Serving

Don't serve static files through Helma. Use the reverse proxy:

```nginx
location /static/ {
    alias /var/www/static/;
    expires 1d;
    add_header Cache-Control "public, immutable";
}
```

Helma's `<app>.static` is convenient but slower than nginx's `sendfile`.

## Avoid N+1 Queries

Set `.loadmode = aggressive` for references heavily used during list rendering:

```properties
# Post/type.properties
author.object  = User
author.local   = author_id
author.foreign = user_id
author.loadmode = aggressive
```

Now `for each (var post in posts) { post.author.name }` doesn't issue an extra SELECT per post.

## Don't Iterate Large Collections in Memory

```javascript
// BAD — loads all posts
for each (var p in root.posts.list()) {
    process(p);
}

// GOOD — paginate
for (var offset = 0; ; offset += 100) {
    var batch = root.posts.list({ offset: offset, maxSize: 100 });
    if (batch.length === 0) break;
    for each (var p in batch) process(p);
}
```

For very large iterations, commit between batches:

```javascript
for (var offset = 0; ; offset += 1000) {
    var batch = root.events.list({ offset: offset, maxSize: 1000 });
    if (batch.length === 0) break;
    for each (var e in batch) process(e);
    res.commit();    // release locks, batch the txn
}
```

## Avoid Synchronous Blocking in Actions

If an action calls a slow external service, it ties up a RequestEvaluator thread.

```javascript
// BAD
function notify_action() {
    sendSlowEmail(user);
    res.write("OK");
}

// GOOD
function notify_action() {
    app.invokeAsync(null, "sendSlowEmail", [user]);
    res.write("OK");
}
```

The async invocation runs on a separate evaluator; the HTTP request returns quickly.

## Skin Caching Tactics

For expensive renderings of rarely-changing content, cache the result on the HopObject's cache node:

```javascript
function show_macro() {
    if (!this.cache.html || this.cache.htmlAt < this.modified) {
        this.cache.html = renderSkinAsString("show");
        this.cache.htmlAt = Date.now();
    }
    res.write(this.cache.html);
}
```

The cache node is in memory, evicted with the HopObject.

## Monitoring

Track these key metrics:

| Metric | Source | Healthy |
|---|---|---|
| Request rate | `app.requestCount` over time | growing steadily |
| Error rate | `app.errorCount` / `app.requestCount` | < 1% |
| 99th percentile latency | derive from access log | depends on app |
| Active sessions | `app.countSessions()` | depends on traffic |
| Free threads | `app.freeThreads` | > 0 most of the time |
| Cache size | `app.cacheusage` | < `cacheNodes` |
| Heap usage | JMX | < 80% of `-Xmx` after GC |

Expose via a `/metrics` endpoint for Prometheus, log periodically, or use a JMX exporter.

## See Also

- [Standalone Server](standalone.md)
- [Logging Setup](logging.md)
- [Concepts: Architecture](../concepts/architecture.md) — what's in memory
- [Database: Object-Relational Mapping](../database/orm.md) — query optimisations
