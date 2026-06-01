# Debugging

Helma provides three debugging facilities: in-response debug output, a request tracer, and a full visual debugger via Rhino's built-in tool.

## In-Response Debug Output

The simplest tool. `res.debug(msg)` accumulates messages that can be appended to the rendered response:

```javascript
function main_action() {
    res.debug("user is " + (session.user ? session.user.name : "anonymous"));
    res.debug("cache hit = " + (cache.has(key) ? "yes" : "no"));
    renderSkin("main");
}
```

In a skin:

```html
<!DOCTYPE html>
<html>
<body>
  <!-- content -->
  <!-- ... -->

  <% if response.debug %>
    <hr>
    <h2>Debug</h2>
    <pre><% response.debug %></pre>
  <% end if %>
</body>
</html>
```

`response.debug` is the accumulated buffer of `res.debug(...)` calls during this request.

`app.properties::debug = true` is **not** required for `res.debug` — but it is for `app.debug()`.

## The Tracer

When enabled, the Tracer logs every function call made during a request, indented to show the call stack:

```properties
# app.properties
tracer = true
```

Output (appended to the response via `res.debug` automatically):

```
function Root.main_action
  function Root.posts_macro
    function Post.title_macro
    function Post.render
  function Post.title_macro
  function Post.render
```

This is Rhino's `Debugger` callback wired to write to the response buffer (`src/main/java/helma/scripting/rhino/debug/Tracer.java`).

Use the tracer to:

- Discover surprising recursion
- Find slow code paths (combine with the Profiler for timing)
- Trace through unfamiliar code

**Always disable in production.** The tracer overhead is significant.

## The Profiler

```properties
# app.properties
profile = true
```

The Profiler tracks per-function timing and writes a summary to the app's event log at the end of each request:

```
[helma.myapp.event] Profiler data for GET:/posts/123:
   12.3% (8 calls,  120 ms) Root.posts_macro
    9.4% (1 call,    92 ms) Post.render
    8.1% (12 calls,  79 ms) helma.util.HtmlEncoder.encode
    ...
```

Each line: percentage of total request time, call count, total time, function name.

The Profiler is **per-request** — each request gets its own profile. There's no cross-request aggregation. For aggregated profiling, use a JVM-level tool (async-profiler, JFR).

See [Profiling](profiling.md) for more.

## Rhino Source-Level Debugger

For step-through debugging, Rhino has a Swing-based debugger UI that Helma can launch:

```properties
# app.properties
rhino.debug = true
```

On the next request, the debugger window opens. You can:

- Set breakpoints in your `.js` files
- Step through actions line by line
- Inspect variables and the call stack
- Evaluate expressions in the current frame

The debugger uses `helma.scripting.rhino.debug.HelmaDebugger` which wraps Rhino's `org.mozilla.javascript.tools.debugger.Dim`.

Source files visible in the debugger are the ones already compiled — i.e. anything in your prototype directories. CommonJS modules show up once loaded.

### Headless Servers

Headless server, no display? The debugger needs a graphical environment. Run Helma on a workstation for debugging, or use VNC/X-forwarding to a remote server.

### Production Caveat

**Never enable `rhino.debug = true` in production.** It opens a UI on the JVM's display, halts request handling for breakpoints, and is generally not safe for shared servers.

## Logging-Based Debugging

For situations where the tracer/profiler/debugger aren't applicable:

```javascript
app.log("Reached point X with value " + value);
app.logError("Unexpected state", new Error());
```

`app.log()` writes to the app's event log (`log/helma.<appname>.event.log`). `app.logError()` includes a stack trace.

For a quick debug dump:

```javascript
function debug(obj, label) {
    app.log("DEBUG " + (label || "") + ":\n" + JSON.stringify(obj, null, 2));
}
```

## Inspecting HopObjects

```javascript
print(post._id);
print(post._prototype);
print(post._parent);
print(post._state);          // "CLEAN", "MODIFIED", "TRANSIENT", ...
print(post._children);       // size of children collection

// All properties (skips DB-mapping internals)
for (var k in post) {
    print(k + " = " + post[k]);
}
```

For the underlying Node:

```javascript
var node = post.__unwrap__();          // Helma's INode unwrap
print(node.getName());
print(node.getElementName());
print(node.getInternalName());
```

## Inspecting the Application

```javascript
// What's loaded?
for each (var proto in app.getPrototypes()) {
    print(proto.getName());
}

// What sessions are active?
print(app.countSessions());
for each (var u in app.getActiveUsers()) {
    print(u.name);
}

// What's in the modules map?
for each (var k in app.modules.keySet().toArray()) {
    print(k);
}

// What's app.data hold?
print(app.data._children);
```

## Capturing JavaScript Stack Traces

```javascript
try {
    riskyCall();
} catch (e) {
    print(e.message);
    print(e.scriptStack);     // JS stack
    print(e.javaStack);        // Java stack
}
```

Inside the framework, `res.scriptStack` and `res.javaStack` are pre-populated when the error handler runs.

## Debugging Memory Usage

```javascript
var rt = java.lang.Runtime.getRuntime();
print("Used: " + (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024 + " MB");
print("Max:  " + rt.maxMemory() / 1024 / 1024 + " MB");
```

To force a GC (rarely useful):

```javascript
java.lang.System.gc();
```

## Common Debugging Patterns

### "Why does this property return null?"

Suspect lazy loading or cache eviction. Inspect:

```javascript
print(post._state);
print(post._parent);
print(post._id);
```

If `_state` is `INVALID`, the Node has been deleted from the DB but a stale reference remains.

### "Why is this skin not rendering?"

Check skin resolution:

```javascript
function diagnose() {
    var skin = app.getSkin("Post", "main", res.skinpath);
    print("Skin found: " + (skin !== null));
    print("Skin path: " + JSON.stringify(res.skinpath));
}
```

### "Why is my action not being called?"

```javascript
function onRequest() {
    res.debug("path: " + req.path);
    res.debug("action: " + req.action);
    res.debug("this: " + (this._prototype || "?"));
    res.debug("currentElement: " + path[path.length - 1]);
}
```

### "Why is the request slow?"

Enable the Profiler. Look at the top entries.

## See Also

- [Profiling](profiling.md)
- [Logging](../framework/logging.md)
- [`HelmaDebugger.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/scripting/rhino/debug/HelmaDebugger.java)
