# Caching

Helma has three layers of caching: **client-side** (HTTP), **server-side skin** (compiled templates), and **server-side object** (the NodeManager cache).

## HTTP Caching

### Client-side cache control

```javascript
res.cache = false;       // emit Cache-Control: no-cache, no-store, must-revalidate
                          // and Pragma: no-cache
```

Or:

```javascript
res.cache = true;        // default — let the framework set ETag/Last-Modified
```

### Last-Modified / If-Modified-Since

```javascript
function main_action() {
    res.lastModified = this.modified;
    renderSkin("main");
}
```

Helma emits the `Last-Modified` header. On the next request, the browser sends `If-Modified-Since`. Helma compares the date — if not modified, returns 304 Not Modified.

Multiple calls to `res.setLastModified` keep the latest value.

### ETag / If-None-Match

```javascript
function main_action() {
    res.etag = '"' + this.id + ":" + this.modified.getTime() + '"';
    renderSkin("main");
}
```

Helma emits `ETag` and compares the client's `If-None-Match`.

### `res.dependsOn()` and `res.digest()`

For an auto-computed ETag based on multiple inputs:

```javascript
function main_action() {
    res.dependsOn(this.modified.getTime());
    res.dependsOn(this.title);
    res.dependsOn(session.user ? session.user.id : "anon");
    res.digest();                         // computes ETag, emits 304 if matched
    renderSkin("main");
}
```

`res.dependsOn(thing)` accumulates a hash. `res.digest()`:

1. Computes the MD5 of all `dependsOn` values
2. Sets that as the ETag
3. Compares to `If-None-Match`
4. If matched, sets `res.notModified = true` and the framework will emit a 304

Use `dependsOn` when "is this page stale?" depends on many things — user permissions, the underlying post, the current locale, etc.

## Skin Caching

Compiled skins are cached:

- **By `Resource`** — `getContent()` caches the parsed skin until file mtime changes
- **Per-response** — `ResponseTrans.skincache` caches skins resolved this request

The cache is automatic; you don't manage it. To force a global re-read:

```javascript
app.clearCache();      // clears both NodeManager and skin caches
```

…or via the management UI's "Clear caches" button.

## Object Cache

The `NodeManager` LRU cache holds recently-accessed Nodes. Cache size:

```properties
# app.properties
cacheNodes = 1000      # default — number of Nodes to keep in memory
```

The cache uses a `helma.util.CacheMap` (open-addressed LRU). On a miss, the Node is loaded from the DB; on cache full, the least-recently-used Node is evicted.

Cache statistics:

```javascript
app.cacheusage     // number of nodes currently cached
```

Force a clear:

```javascript
app.clearCache();      // empties NodeManager cache
```

## `app.data` and Custom Caching

For computed values that should outlive a single request:

```javascript
function topPosts_macro() {
    var cached = app.data.topPosts;
    if (!cached || (Date.now() - cached.at) > 60_000) {
        cached = {
            at: Date.now(),
            posts: root.posts.list({ order: "rating desc", maxSize: 10 })
        };
        app.data.topPosts = cached;
    }
    for each (var post in cached.posts) {
        post.renderSkin("teaser");
    }
}
```

`app.data` is shared across all evaluator threads. Reads are thread-safe; read-modify-write is not — use `app.invoke()` for atomicity if multiple requests can write concurrently.

## Conditional GET Pattern

A complete pattern for a heavy page:

```javascript
function show_action() {
    // 1. Cheap headers first
    res.lastModified = this.modified;
    res.etag = '"' + this.id + ":" + this.modified.getTime() + '"';

    // 2. Bail if client has fresh copy
    if (res.notModified) return;

    // 3. Expensive rendering only on miss
    renderSkin("show");
}
```

`res.notModified` is set by the framework if `If-Modified-Since >= res.lastModified` or `If-None-Match` matches `res.etag`. When set, the body is dropped and 304 is emitted.

## Reverse Proxy Caching

When behind a reverse proxy (varnish, nginx) you can leverage HTTP headers:

```javascript
function publicPage_action() {
    res.setHeader("Cache-Control", "public, max-age=300");
    res.setHeader("Vary", "Accept-Language");
    renderSkin("page");
}
```

For private pages:

```javascript
res.setHeader("Cache-Control", "private, no-cache");
```

## When Caching Is Disabled

Helma forces `Cache-Control: no-cache` and clears `Last-Modified`/`ETag` when:

- `res.cache = false`
- The response sets cookies
- The user is logged in *and* the response varies by user (the framework can't tell — you must opt out manually for logged-in users)

## Cache Headers Defaults

| Condition | Headers emitted |
|---|---|
| `res.cache = true` (default) | None automatic — you set `Last-Modified`/`ETag` |
| `res.cache = false` | `Cache-Control: no-cache, no-store, must-revalidate`, `Pragma: no-cache`, `Expires: 0` |
| Cookies set in response | `Cache-Control: no-cache, no-store, must-revalidate` (override) |
| `If-Modified-Since` matched | `304 Not Modified`, no body |
| `If-None-Match` matched | `304 Not Modified`, no body |

## Skin Caching with `cachenode`

A common pattern: cache the rendered output of a slow skin per-user, per-object:

```javascript
function expensiveSection_macro() {
    var key = "section-" + this.id + "-" + (session.user ? session.user.id : "anon");
    if (!this.cache[key] || isStale(this.cache[key])) {
        var captured = renderSkinAsString("expensive");
        this.cache[key] = { html: captured, at: Date.now() };
    }
    res.write(this.cache[key].html);
}
```

`this.cache` is a per-HopObject transient node — entries persist as long as the HopObject is in the LRU cache.

## See Also

- [Object Model](../concepts/object-model.md) — how the NodeManager cache works
- [Reverse Proxy](../deployment/reverse-proxy.md) — `Vary` and `Cache-Control` for proxy-friendly responses
