# Profiling

When you need to know *why* an action is slow.

## Enabling the Profiler

```properties
# app.properties
profile = true
```

On the next request, Helma's `Profiler` attaches as the Rhino debugger callback. Every function call gets timed. At the end of the request, results are logged to the app event log:

```
2026-06-01 12:34:56 INFO [helma.myapp.event] Profiler data for GET:/posts/123:
   12.3% (8 calls,  120 ms) Root.posts_macro
    9.4% (1 call,    92 ms) Post.render
    8.1% (12 calls,  79 ms) helma.util.HtmlEncoder.encode
    7.0% (1 call,    68 ms) Post.computeStats
    6.5% (1 call,    63 ms) <native_op>
    ...
```

Each line shows:

- **Percentage** of total request time
- **Call count** — how many times this function ran
- **Total time** in ms — wall-clock, summed across all invocations
- **Function name** — qualified `Prototype.method` form

Functions taking < 1% of total time are aggregated and omitted from the report.

## Reading the Output

Top of the list = where time was spent. Look for:

- **High percentage, low call count** → one slow function. Probably a DB query or a complex computation.
- **High percentage, high call count** → many cheap calls. Maybe an N+1 query.
- **`<native_op>`** → time in Rhino's built-in operations, or in synchronous Java calls.

A typical fast page has the entries spread across many functions, each <5%. A typical slow page has one function dominating, often 30-50%.

## Identifying N+1 Queries

If you see something like:

```
35.0% (100 calls, 350 ms) Post.author_macro
```

…where `100` is the number of posts on the page, you have an N+1: each post is loading its author lazily. Fix with aggressive loading:

```properties
# Post/type.properties
author.aggressiveLoading = true
```

Or batch-load:

```javascript
function posts_macro() {
    var posts = this.list();
    // Pre-fetch authors so they're cached
    var authorIds = posts.map(p => p.author_id);
    var authors = root.users.list({ filter: "user_id IN (" + authorIds.join(",") + ")" });
    var byId = {};
    for each (var a in authors) byId[a.user_id] = a;

    for each (var p in posts) {
        p.cache.author = byId[p.author_id];
        p.renderSkin("teaser");
    }
}
```

## Profiling a Specific Function

To profile *just one* function without running the full profiler:

```javascript
function timed(label, fn) {
    var start = java.lang.System.currentTimeMillis();
    try {
        return fn();
    } finally {
        app.log("TIMING " + label + ": " + (java.lang.System.currentTimeMillis() - start) + "ms");
    }
}

function main_action() {
    var posts = timed("load-posts", () => root.posts.list());
    var rendered = timed("render", () => renderSkinAsString("main", { posts: posts }));
    res.write(rendered);
}
```

Or wrap the whole action:

```javascript
function main_action() {
    var start = java.lang.System.nanoTime();
    try {
        // ...
    } finally {
        var ms = (java.lang.System.nanoTime() - start) / 1_000_000;
        app.log("Action took " + ms + " ms");
    }
}
```

## Comparing Profiles

To compare optimised vs unoptimised:

1. Enable profiler.
2. Hit the page once.
3. Note the percentages.
4. Make your change.
5. Hit the page again.
6. Diff.

The Profiler aggregates across the lifetime of a request — successive requests don't accumulate.

## Profiler vs Application Performance Monitoring

For production-grade observability:

- **Helma's Profiler** — per-request, dev-only. Too expensive for production.
- **Async Profiler** (`async-profiler`) — JVM-wide, low-overhead, sampling. Suitable for production.
- **Java Flight Recorder** — built into the JVM, low-overhead. Excellent for production.
- **HTTP-level APM** (New Relic, DataDog, etc.) — request-level latency, no JS function detail.

For Helma-specific JS profiling, you're stuck with Helma's built-in or building your own via `app.log()` instrumentation.

## What Gets Profiled

Profiling tracks JS function calls and the bridge calls into Java. It does **not** include:

- Pure Java function execution (only the entry from JS)
- Internal Helma framework operations (path resolution, skin lookup)
- Database I/O (you'll see `DatabaseObject.executeRetrieval` as a single entry)

For DB-level profiling, enable JDBC logging at the driver level (e.g. `loglevel=2` in PostgreSQL connection URL, or use a JDBC proxy like p6spy).

## Disabling

```properties
# app.properties
profile = false      # default
```

You can also disable per-request via `req.actionHandler` magic but it's easier to just toggle the global flag.

## See Also

- [Debugging](debugging.md) — Tracer and visual debugger
- [Logging](../framework/logging.md) — where profiler output goes
- [`Profiler.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/scripting/rhino/debug/Profiler.java) — implementation
