# Logging

Helma uses [Apache Commons Logging](https://commons.apache.org/proper/commons-logging/) as its logging facade. By default it ships with its own log implementation (`helma.util.Logging`) but can be redirected to slf4j/logback or log4j via the classpath.

## Default Layout

Two logs per application plus one server-wide log:

| Log file | What it contains |
|---|---|
| `log/helma.event.log` | Server-wide events: startup, shutdown, extension loads |
| `log/helma.<appname>.event.log` | Per-app `app.logEvent()`, `app.logError()`, action exceptions |
| `log/helma.<appname>.access.log` | One line per HTTP request (path, method, status, time) |
| `log/helma.access.log` | Server-wide access log (often a copy or alias) |

Logs rotate at midnight and are gzipped.

## Configuration

### Log directory

```properties
# server.properties
logdir = log               # default — relative to helma.home
```

Set to `console` to log to stdout instead of files (useful in containers).

### Log levels

By default, all Helma logs are at INFO level. Override globally via the `logger` setting:

```properties
# server.properties
logger = helma.util.Logging
```

Or via the JVM system property `-Dorg.eclipse.jetty.LEVEL=WARN` to silence Jetty's chatter.

### Per-app event log name

```properties
# app.properties
eventLog = helma.myapp.event     # default; can be overridden if you want to share logs
accessLog = helma.myapp.access
```

### Access log format

The access log line format is fixed:

```text
2026-06-01 12:34:56 +0200 192.168.1.100 GET /myapp/users/alice HTTP/1.1 200 1234 "Mozilla/5.0..."
```

Columns: timestamp, remote IP, method, path, protocol, status, bytes sent, user agent.

To disable the access log per app:

```properties
# app.properties
logAccess = false
```

To customise the access log line, write a `RequestListener` Java extension. There is no scripting-level hook.

## Logging from JavaScript

```javascript
// Info message
app.log("Order " + order.id + " placed");

// Same with explicit log category
app.log("orderlog", "Order " + order.id + " placed");

// Debug — only writes if app.properties::debug = true
app.debug("Variable X is " + x);

// Get the underlying commons-logging Log
var log = app.getLogger();         // helma.<appname>.event
log.warn("Heads up");
log.error("Something broke", exception);

// Custom log category
var orderLog = app.getLogger("orders");
orderLog.info("Audit entry");
```

`getLogger(name)` returns a commons-logging `Log` for the given category. The category name becomes the log file name if using the default `helma.util.Logging` backend.

## Logging Java Exceptions

```javascript
try {
    riskyCall();
} catch (e) {
    app.logError("Risky call failed", e);
}
```

`logError(message, throwable)` writes the message, the exception class, and the stack trace.

`e.javaException` exposes the underlying Java throwable for JS exceptions — useful if you need to inspect it.

## Debug Logging

`app.properties::debug = true` unlocks a few extras:

- `app.debug(msg)` actually writes (otherwise no-op)
- `res.debug(msg)` appends a debug message to the bottom of the rendered response
- Stack traces in error responses include the full JS stack (otherwise truncated)
- The skin macro `<% response.debug %>` writes all `res.debug(...)` accumulated during the request

```javascript
// In an action
res.debug("Computed " + n + " posts");
res.debug("Cache hit: " + (cached ? "yes" : "no"));
```

Then in a skin:

```html
<!-- emit collected debug below the page -->
<% if response.debug %>
<div class="debug"><pre><% response.debug %></pre></div>
<% end if %>
```

## Custom Loggers

To use slf4j/logback instead of Helma's built-in:

1. Add `slf4j-api.jar`, `jcl-over-slf4j.jar`, `logback-classic.jar`, `logback-core.jar` to `lib/ext/`.
2. Remove the `helma.util.Logging` from the classpath (or override the setting):
    ```properties
    # server.properties
    logger = org.apache.commons.logging.impl.SLF4JLogFactory
    ```
3. Provide a `logback.xml` on the classpath. Standard logback config applies.

Or use log4j 2: include `log4j-jcl.jar` and Helma's commons-logging will automatically delegate.

## Log Rotation and Cleanup

Helma's built-in logger rotates daily at midnight and gzips the previous day's file. Old files are kept indefinitely — clean up with logrotate or cron:

```bash
find log/ -name "*.log.gz" -mtime +30 -delete
```

Or configure logback's `TimeBasedRollingPolicy` with `maxHistory` for automatic deletion.

## Logs from Cron Jobs

Cron jobs log to the per-app event log just like regular actions. The transaction name is `cron:<functionName>`:

```text
2026-06-01 03:00:01 INFO  [helma.myapp.event] cron:nightlyCleanup starting
2026-06-01 03:01:23 INFO  [helma.myapp.event] cron:nightlyCleanup completed
```

## Profiler Output

When `app.properties::profile = true`:

```text
[helma.myapp.event] Profiler data for GET:/posts/123:
   12% (8 calls) Root.posts_macro
    9% (1 call)  Post.render
    8% (12 calls) helma.util.HtmlEncoder.encode
   ...
```

See [Profiling](../scripting/profiling.md).

## Tracer Output

When `app.properties::tracer = true`, each request emits a trace of function calls inline at the bottom of the rendered page (via `res.debug`):

```text
function Root.main_action
  ↪ function Root.posts_macro
    ↪ function Post.title_macro
    ↪ function Post.render
  ↪ function Post.title_macro
```

See [Debugging](../scripting/debugging.md).

## Best Practices

- Use `app.logError(msg, exc)` rather than `app.log(msg)` for exceptions — the framework knows how to render the stack.
- Use categorised logs (`getLogger("orders")`) for high-volume audit-style logging that you don't want mixed with the general event log.
- Keep `debug = false` in production. The skin engine's debug branches add overhead.
- Don't log PII (passwords, tokens, raw bodies) — log file persistence is forever.
- For metrics (request counts, latencies), use a metrics extension or pipe access logs to your log aggregator.
