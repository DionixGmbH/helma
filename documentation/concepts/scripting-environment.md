# Scripting Environment

Helma uses [Mozilla Rhino](https://github.com/mozilla/rhino) — currently version 1.9.0 — as its JavaScript engine. This page explains how the scripting environment is set up, what's available globally, and how Java interop works.

## Scope Hierarchy

```
┌──────────────────────────────────────┐
│ Per-thread Global (GlobalObject)     │   ← `req`, `res`, `session`, `path`, etc.
│ - belongs to one RequestEvaluator    │
├──────────────────────────────────────┤
│ Shared Global (RhinoCore.global)     │   ← `app`, prototypes, CommonJS modules
│ - one per Application                │
├──────────────────────────────────────┤
│ Native JS prototypes (Object, Array) │   ← extended by modules/core/*.js
└──────────────────────────────────────┘   if the app loaded modules/core/all.js
```

### Shared scope

The shared scope holds:

- The application's compiled **prototype constructors** (`Post`, `Comment`, `User`, ...) — created by `RhinoCore.initPrototype()`
- All **CommonJS modules** ever loaded by this application (cached after first load)
- The `app` global — an `ApplicationBean` wrapper around the Java `Application`
- The `Xml` global — an `XmlObject` for XML processing
- The bundled JS module functions from `modules/core/*.js` if loaded via `app.addRepository("modules/core/all.js")` — these extend `String.prototype`, `Array.prototype`, etc.
- The Jala global functions if Jala is loaded

### Per-thread scope

The per-thread scope is recreated on each `RhinoEngine.enterContext()`. It inherits from the shared scope and adds the per-request globals:

- `req` — `RequestBean` wrapping the `RequestTrans`
- `res` — `ResponseBean` wrapping the `ResponseTrans`
- `session` — `SessionBean` wrapping the `Session`
- `path` — `PathWrapper` around the `RequestPath` (the array of HopObjects visited during path resolution)
- `root` — the application root object

Globals set inside an action (`var foo = 1` at module level, or `global.foo = 1`) write to the *shared* scope, not the per-thread scope, *except* during prototype compilation where they are intercepted. This means accidental globals from your actions can leak across requests — use `var` inside functions only.

## Built-in Globals

These are always available from any JavaScript code in an application:

### `req` — the request

```javascript
req.method                  // "GET", "POST", ...
req.path                    // "/users/alice"
req.uri                     // "/myapp/users/alice"
req.action                  // "main" (action name, without _action suffix)
req.params                  // combined query + post params
req.queryParams             // query params only
req.postParams              // post params only
req.cookies                 // request cookies
req.data                    // combined map (http_host, http_browser, etc.)
req.isGet()                 // method check
req.isPost()
req.isXmlHttpRequest()      // X-Requested-With: XMLHttpRequest
req.getHeader("X-Foo")
req.servletRequest          // raw HttpServletRequest (for low-level access)
```

Full reference: [Request Bean](../reference/req-bean.md).

### `res` — the response

```javascript
res.write("text")
res.writeln("with newline")
res.writeBinary(bytes)
res.encode(obj)             // write HTML-escaped string
res.encodeXml(obj)
res.encodeForm(obj)
res.format(obj)             // same as encode(), legacy

res.contentType = "application/json"
res.charset = "UTF-8"
res.status = 201
res.cache = false           // disable client-side caching
res.lastModified = new Date()
res.etag = "abc123"
res.dependsOn("foo")        // contribute to auto-ETag
res.digest()                // emit 304 if all dependsOn unchanged

res.setCookie(name, value, days, path, domain)
// HttpOnly/Secure come from app.properties::cookies.httpOnly and cookies.secure
res.unsetCookie(name)

res.redirect("/elsewhere")  // 302 redirect (throws)
res.forward("/internal")    // internal forward (throws)
res.stop()                  // immediately terminate (throws)
res.abort()                 // rollback transaction + terminate
res.reset()                 // clear all output
res.resetBuffer()           // clear body only

res.data                    // free map for skin access via <% response.foo %>
res.handlers                // map of macro handlers
res.meta                    // free map for arbitrary meta data
res.message = "Saved!"      // persisted across redirects

res.push() / res.pop()      // capture buffered output as string
res.commit()                // commit current transaction, start new one
res.rollback()              // rollback + start new
```

Full reference: [Response Bean](../reference/res-bean.md).

### `session` — the current session

```javascript
session.cookie              // session id
session.user                // current user HopObject, or null
session.data                // transient cache node — session-scoped storage
session.message             // string preserved across one redirect

session.login("alice", "pw")
session.login(userNode)
session.logout()
session.touch()             // bump last-active timestamp
session.onSince             // Date created
session.lastActive          // Date last touched
```

Full reference: [Session Bean](../reference/session-bean.md).

### `app` — the application

```javascript
app.name
app.getProperty("key")
app.data                    // transient per-app cache node
app.modules                 // free map for modules to register state
app.invoke(thisObj, "fn", args, timeout)
app.invokeAsync(thisObj, "fn", args)
app.addRepository("...")
app.addCronJob("nightly", "*", "*", "*", "*", "2", "0")
app.getDbSource("main")
app.log("info message")
app.debug("debug, only when debug=true in app.properties")
app.getPrototypes()
app.getActiveUsers()
app.countSessions()
app.countActiveThreads
app.countFreeThreads
```

Full reference: [Application Bean](../reference/app-bean.md).

### `path` — the request path

```javascript
path[0]                     // root object
path[1]                     // first child traversed
path.length                 // number of objects in path
path.contains(node)
```

`path` is a wrapper around the array of HopObjects walked during path resolution. Useful for breadcrumbs.

### Global Functions

A handful of free functions are always available (defined in `GlobalObject.java`):

| Function | Description |
|---|---|
| `renderSkin(skinName, params)` | Render a global skin (no `this` object) |
| `renderSkinAsString(skinName, params)` | Same, return as string |
| `createSkin(source)` | Compile a `Skin` from a string |
| `getProperty(key, default)` | Read `app.properties` |
| `authenticate(user, pwd)` | Verify against `passwd` file |
| `getDBConnection(name)` | Get a `DatabaseObject` for a db.properties source |
| `getURL(url, condition, timeout)` | HTTP GET, returns `MimePart` |
| `getXmlDocument(src)` / `getHtmlDocument(src)` | Parse XML/HTML to DOM |
| `format(obj)` | HTML-encode |
| `formatParagraphs(obj)` | HTML-encode + replace newlines with `<p>` |
| `serialize(obj, file)` / `deserialize(file)` | Persist a JS object to a file |
| `seal(obj)` | Make an object immutable |
| `wrapJavaMap(map)` / `unwrapJavaMap(wrapped)` | Convert `java.util.Map` ↔ JS object |
| `toJava(obj)` | Wrap a JS value as the underlying `java.lang.*` for explicit Java interop |
| `definePrototype(name, descriptor)` | Define a prototype at runtime |
| `write(str)` / `writeln(str)` | `System.out.print` (no `res`-binding — used in scripts) |
| `require(path)` | CommonJS module loader |

Also defined on `Object.prototype`:

- `obj.dontEnum("foo", "bar")` — mark properties as non-enumerable

Full list of bundled global functions: [Global Functions](../scripting/global-functions.md) and [modules/core/Global.js](../modules/core/global.md).

## Java Interop

Rhino exposes the entire JVM. From JavaScript:

```javascript
// Construct a Java object
var sb = new java.lang.StringBuffer("hi");
sb.append(", world!");
print(sb.toString());

// Static methods
var now = java.lang.System.currentTimeMillis();

// Use the app's classloader (sees apps/<app>/lib/*.jar)
var x = new com.example.MyClass();
```

The `Packages.java.*` and `Packages.com.*` etc. namespaces are aliases. Bare `java.*` works because Rhino imports the standard `java` packages.

`importPackage()` and `importClass()` are available too:

```javascript
importPackage(java.util);
var list = new ArrayList();
list.add("a");
list.add("b");
```

But these have side effects on the scope so are best used only in CommonJS modules.

### `JavaObject` wrapper

When Java returns an object, Rhino wraps it in a `NativeJavaObject` so its methods are callable from JS:

```javascript
var file = new java.io.File("/tmp/foo");
file.getName();            // ← method call passes through
file.absolutePath;         // ← bean property: calls getAbsolutePath()
```

Bean-property reflection is automatic: `obj.foo` tries `obj.getFoo()` and `obj.isFoo()` before reporting undefined.

### `helma.scripting.rhino.JavaObject`

Helma's own `JavaObject` wraps select Java objects when they have a custom prototype mapping registered in `class.properties`. This is how `helma.File` Java objects can have JavaScript methods added.

## Script Compilation

When a prototype is added or its files change, Helma:

1. Concatenates every `.js` file in the prototype directory (sorted by `ResourceComparator`)
2. Compiles into a single Rhino `Script`
3. Creates a `JsObject` with that script's compiled functions
4. Sets it as the JavaScript prototype of all instances of that HopObject prototype

This is done lazily — the first request after a file change triggers recompilation in `RhinoCore.updatePrototypes()`.

## Module Loading

The `require("foo")` function:

1. Resolves the `foo` path against the app directory (`app.getAppDir()`) and optionally `app.properties::commonjs.dir` — registered repositories are **not** searched by `require()`
2. Loads the resource as a CommonJS module — sets up `module`, `exports`, `__dirname`, `__filename`
3. Caches the loaded module by absolute path

See [CommonJS Modules](../scripting/commonjs-require.md).

## Encoding and Charsets

- `app.properties::charset` (default `UTF-8`) controls the default response charset
- `app.properties::skinCharset` (default = response charset) controls how `.skin` files are decoded from disk
- `req.encoding` is autodetected from the `Content-Type` header on POST or defaults to UTF-8

## Debug & Trace

In `app.properties`:

```properties
# Debug logging in app.log
debug = true

# Per-request Rhino tracing — prints every function call to res.debug()
tracer = true

# Per-request profiling — prints function-level timing to log on exitContext()
profile = true

# Enable the source-level debugger UI (Rhino's bundled Swing debugger)
rhino.debug = true
```

See [Debugging](../scripting/debugging.md) for details.

## Extensions

Java code can inject globals into the scripting environment by implementing `helma.extensions.HelmaExtension`. See [Writing Java Extensions](../extensions/writing-extensions.md).
