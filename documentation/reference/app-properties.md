# app.properties

`app.properties` is the per-application configuration. Lives inside the app directory (e.g. `apps/blog/app.properties`). Overlays server-wide settings from `server.properties`.

## Threading

### `maxThreads`

Maximum number of RequestEvaluator threads (concurrent requests). Default 50 (hardcoded in `Application.java`).

```properties
maxThreads = 24
```

### `requestTimeout`

Seconds before a stuck request is killed. Default 60.

```properties
requestTimeout = 30
```

## Sessions

### `sessionTimeout`

Minutes before an idle session expires. Default 30.

```properties
sessionTimeout = 60
```

### `persistentSessions`

If `true`, sessions are serialised to `db/<app>/sessions` on shutdown and restored on startup. Default `false`.

```properties
persistentSessions = true
```

### `sessionCookieName`

Override the default session cookie name (`HopSession`).

```properties
sessionCookieName = MySession
```

### `cookies.secure`

If `true`, the session cookie has the `Secure` attribute set (HTTPS only). Default `false`.

```properties
cookies.secure = true
```

## WebSockets

### `socketManagerImpl`

Class name of the WebSocket registry / fan-out manager. Default
`helma.framework.core.SocketManager`, which keeps connections and channel
subscriptions in this JVM only. Override with a clustered implementation to make
`app.publish()` fan out across instances (e.g. Helma-Swarm). See
[WebSockets](../framework/websockets.md#clustering).

```properties
socketManagerImpl = helma.swarm.SwarmSocketManager
```

## Database

### `cacheNodes`

NodeManager cache size — number of Nodes kept in memory. Default 1000.

```properties
cacheNodes = 5000
```

### `caching`

If `false`, disables the NodeManager cache entirely. Useful for debugging cache-related issues.

```properties
caching = true
```

## Skin Rendering

### `skinCharset`

Character set for reading skin files from disk. Defaults to the app charset.

```properties
skinCharset = UTF-8
```

### `failmode`

Default failmode for unhandled macros. `silent` or `verbose`. Default `silent`.

```properties
failmode = verbose      # show unhandled macros in output — dev only
```

### `skinDefaultEncoding`

Default output encoding for macro **return values** when the macro has no explicit
`encoding=`. Values: `escape` (alias `xml`), `attr` (alias `form`), `all`, `url`,
`format` (alias `html`), `none`. Unset by default.

When this property is set (including to `none`), the new `context=` parameter is
globally disabled and treated as an ordinary named parameter.

**Do not set to `html`**: the `html`/`format` mode passes `<script>` tags through
unchanged and provides no XSS protection. Setting it logs a warning.

To pin legacy behaviour and disable `context=` globally:

```properties
skinDefaultEncoding = none
```

To use XSS-safe escaping via the legacy track:

```properties
skinDefaultEncoding = escape
```

See [Skins](../framework/skins.md#default-encoding-and-context-skindefaultencoding).

### `skinDefaultContext`

Default `context=` value applied to macro return values when the macro has no
explicit `encoding=` or `context=`, and `skinDefaultEncoding` is **not** set.
Values: `html`, `attr`, `lines`, `url`, `json`, `js`, `none`. Default: `none`.

Recommended for new apps:

```properties
skinDefaultContext = html
```

Has no effect when `skinDefaultEncoding` is present.

See [Skins](../framework/skins.md#default-encoding-and-context-skindefaultencoding).

## Routing

### `notfound`

Action name to dispatch to on path-not-found. Default `notfound`.

```properties
notfound = my404Action
```

### `error`

Action name to dispatch to on uncaught exception. Default `error`.

```properties
error = myErrorAction
```

## Scripting

### `scriptingEngine`

Class name of the scripting engine. Default `helma.scripting.rhino.RhinoEngine`. You normally never change this.

### `optLevel`

Rhino optimisation level. `-1` (interpreted) through `9` (max compile). Default `0`.

```properties
optLevel = 9            # production — compiled
optLevel = -1           # development — interpreted
```

### `tracer`

If `true`, every JS function call is logged to the response's debug buffer.

```properties
tracer = true
```

### `profile`

If `true`, each request is profiled and timings logged at end of request.

```properties
profile = true
```

### `rhino.debug`

If `true`, opens Rhino's source-level debugger UI on next request.

```properties
rhino.debug = true
```

### `debug`

If `true`, enables verbose debug logging (`app.debug` writes; full stack traces).

```properties
debug = true
```

## Update Intervals

### `updateInterval`

Milliseconds between prototype-directory mtime checks. Default 1000 (1 second). Set higher in production.

```properties
updateInterval = 10000       # 10 seconds, production
```

## Logging

### `accessLog`

Log category name for access logging. Default `helma.<appname>.access`.

```properties
accessLog = my-blog-access
```

### `eventLog`

Log category name for event logging. Default `helma.<appname>.event`.

### `logAccess`

If `false`, disable the access log entirely.

```properties
logAccess = false
```

## HTTP

### `httpUserAgent`

User-Agent header for outgoing HTTP requests (via `getURL()`).

```properties
httpUserAgent = MyBlog/1.0
```

### `baseURI`

Override the auto-computed base URI for `href()` generation.

```properties
baseURI = https://www.example.com/
```

## Authentication

Helma's built-in `session.login(user, pwd)` uses **plaintext password comparison**. There is no `passwordEncoding` property — see [Authentication](../framework/authentication.md) for the production-ready bcrypt pattern.

The separate `authenticate(user, pwd)` function (for the `passwd` file) auto-detects Unix `crypt` and MD5 hashes; no setting controls this.

## Cron Jobs

### `cron`

If `false`, disable cron job processing for this app.

```properties
cron = false
```

## Mail

### `smtp`

Override `server.properties::smtp` for this app.

```properties
smtp = mail.example.com
smtpPort = 587
```

## Globals

### `globalMacroPath`

Comma-separated namespace list for global macro resolution.

```properties
globalMacroPath = MyMacros, Utils
```

## CommonJS Modules

### `commonjs.dir`

A single directory added to the CommonJS `require()` search path, in addition to the application directory. Initialized by `RhinoCore` when constructing the per-app CommonJS roots.

```properties
commonjs.dir = modules/commonjs
```

Helma has **no** multi-path `nodepath` setting — only the app directory and (optionally) this one extra directory are searched.

## Defaults Inherited from server.properties

Anything not set in `app.properties` falls back to `server.properties`. This is automatic — `app.properties` is a `ResourceProperties` overlay.

## Example

```properties
# app.properties for production blog

# Performance
maxThreads = 24
requestTimeout = 30
sessionTimeout = 60
cacheNodes = 10000
optLevel = 9
updateInterval = 30000

# Storage
persistentSessions = true

# Security
cookies.secure = true
sessionCookieName = BlogSession

# UI
charset = UTF-8
failmode = silent
notfound = notfound
error = error

# Mail
smtp = smtp.example.com
smtpPort = 587

# Logging
debug = false
tracer = false
profile = false

# Routing
baseURI = https://blog.example.com/
```

## See Also

- [Reference: server.properties](server-properties.md)
- [Reference: apps.properties](apps-properties.md)
- [Getting Started: Running Helma](../getting-started/running.md)
- [Concepts: Architecture](../concepts/architecture.md)
