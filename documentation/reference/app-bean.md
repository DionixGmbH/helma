# Application Bean (`app`)

`app` is the global reference to the current application — an `ApplicationBean` wrapping the underlying `helma.framework.core.Application`. Defined in `src/main/java/helma/framework/core/ApplicationBean.java`.

This page lists every method and property.

## Identity & Lifecycle

### `app.name` (String, read-only)

The application name as configured in `apps.properties`.

### `app.dir` / `app.appDir` (String, read-only)

The absolute application directory (defaults to `apps/<name>`).

### `app.serverDir` (String, read-only)

The absolute server home directory.

### `app.upSince` (Date, read-only)

When the application was started.

### `app.charset` (String, read-only)

The default response charset (from `app.properties::charset`, default `UTF-8`).

## Statistics

### `app.requestCount` (long, read-only)

Number of requests served since startup.

### `app.errorCount` (long, read-only)

Number of errors logged.

### `app.cacheusage` (int, read-only)

Number of Nodes currently in the NodeManager cache.

### `app.freeThreads` / `app.activeThreads` / `app.maxThreads` (int)

Evaluator thread pool stats. `maxThreads` is settable.

### `app.countSessions()` (int)

Number of currently active sessions.

## Data Maps

### `app.data` (HopObject, read-only)

A transient INode shared across all evaluators in this app. Use to store in-memory state, caches, counters.

```javascript
app.data.hits = (app.data.hits || 0) + 1;
app.data.startupTime = app.data.startupTime || new Date();
```

### `app.modules` (Map, read-only)

A free-form map for modules to register state. Used by Jala and other libraries to namespace their globals.

```javascript
app.modules.myPlugin = {
    config: { ... },
    instance: ...
};
```

### `app.properties` (Map, read-only)

Read-only view of `app.properties` settings.

### `app.dbProperties` (Map, read-only)

Read-only view of `db.properties` settings.

### `app.appsProperties` (Map, read-only)

Read-only view of `apps.properties` for this app.

## Logging

### `app.log(msg)` / `app.log(category, msg)`

Log an INFO message to the app's event log (`helma.<appname>.event`). With two args, the first is the log category name.

### `app.debug(msg)` / `app.debug(category, msg)`

Log a DEBUG message — only writes if `app.properties::debug = true`.

### `app.logError(msg, throwable)`

Log an ERROR message with optional throwable stack trace.

### `app.getLogger()` (Log)

Get the underlying commons-logging Log for the app's event log.

### `app.getLogger(name)` (Log)

Get a Log for a custom category. The category name typically becomes the log file name with `helma.util.Logging`.

## Invocation

### `app.invoke(thisObject, function, args)` (Object)

### `app.invoke(thisObject, function, args, timeoutMillis)` (Object)

Synchronously invoke a function on a fresh evaluator with its own transaction. Returns the function's return value.

```javascript
var result = app.invoke(null, "Root.computeStats", [], 5000);
```

Throws `TimeoutException` if the function doesn't return within `timeoutMillis` (default 30000).

### `app.invokeAsync(thisObject, function, args)` (FutureResult)

### `app.invokeAsync(thisObject, function, args, timeoutMillis)` (FutureResult)

Same but async. Returns immediately with a `FutureResult`.

```javascript
var f = app.invokeAsync(null, "Root.heavyJob", []);
// ... other work ...
f.waitForResult();   // block until done
print(f.result);
```

FutureResult API:

- `f.running` — true while running
- `f.result` — return value after completion
- `f.exception` — exception if any
- `f.waitForResult()` — block forever
- `f.waitForResult(ms)` — block up to `ms`

See [Internal Invocation](../framework/internal-invocation.md).

## Sessions

### `app.getSession(sessionId)` (SessionBean)

Get a session by ID. Returns `null` if not found.

### `app.createSession(sessionId)` (SessionBean)

Get-or-create a session for the given ID.

### `app.getSessions()` (SessionBean[])

Get all active sessions.

### `app.getActiveUsers()` (INode[])

Get all User HopObjects with at least one active session.

### `app.getRegisteredUsers()` (INode[])

Get all User HopObjects (regardless of session state).

### `app.getSessionsForUser(userOrName)` (SessionBean[])

Get sessions for a specific user.

## WebSockets

See [WebSockets](../framework/websockets.md) for the full guide.

### `app.publish(channel, message)`

Send `message` to every open WebSocket connection subscribed to `channel`. A
string is sent verbatim; stringify structured data first (e.g.
`JSON.stringify(obj)`). Callable from anywhere — an action, a macro, a cron job.

### `app.getSockets()` / `app.getSockets(channel)` (SocketConnection[])

The open connections held by this node — all of them, or those subscribed to a
given channel.

### `app.countSockets()` (int)

Number of open WebSocket connections held by this node.

## User Management

### `app.registerUser(username, password)` (INode)

Create a new User HopObject with the given username and (hashed) password. Returns the new User node, or `null` if the username is already taken.

### `app.getUser(username)` (INode)

Get a User HopObject by name. Returns `null` if not found.

## Cron Jobs

### `app.addCronJob(functionName)`

### `app.addCronJob(functionName, year, month, day, weekday, hour, minute)`

Add a cron job. All parameters but the first are strings or `null` for `*`.

### `app.removeCronJob(functionName)`

Remove a cron job.

### `app.getCronJobs()` (Map)

Read-only map of registered cron jobs.

## Database

### `app.getDbSource(name)` (DbSource)

Get a `DbSource` from `db.properties`.

```javascript
var src = app.getDbSource("main");
var conn = src.getConnection();
```

## Repositories

### `app.getRepositories()` (Object[])

Get the list of repositories backing this application.

### `app.addRepository(path)`

Add a repository at runtime. `path` can be:

- A directory path → `FileRepository`
- A `.zip` file path → `ZipRepository`
- Any other file → `SingleFileRepository`
- An existing `Repository` object

If the path doesn't exist, Helma tries appending `.zip` and `.js`.

## Prototypes

### `app.getPrototypes()` (Prototype[])

Get all prototypes registered with this application.

### `app.getPrototype(name)` (Prototype)

Get a prototype by name.

### `definePrototype(name, descriptor)` — global function

Define a prototype at runtime. See [Global Functions](../scripting/global-functions.md).

## Skins

### `app.getSkin(protoName, skinName, skinpath)` (Skin)

Get a Skin object.

### `app.getSkinfiles()` (Map)

Map of `prototypeName → SkinMap`. Each SkinMap is `skinName → skinSource`.

### `app.getSkinfilesInPath(skinpath)` (Map)

Same but using the given skinpath array as override.

### `app.setGlobalMacroPath(path)` / `app.getGlobalMacroPath()`

Set or get the global macro lookup path — an array of namespaces searched when resolving a macro like `<% somefn %>`.

## Cache Management

### `app.clearCache()`

Clear the NodeManager cache and the per-app skin cache.

## Internals

### `app.classLoader` (ClassLoader, read-only)

The application's class loader. Use to load JARs from `apps/<app>/lib/` reflectively.

### `app.get__app__()` (Application, read-only)

The underlying `helma.framework.core.Application` Java object. Escape hatch for low-level operations.

## Example: Full App Status

```javascript
function status_action() {
    res.contentType = "application/json";
    res.write(JSON.stringify({
        name: app.name,
        upSince: app.upSince,
        requests: app.requestCount,
        errors: app.errorCount,
        cacheUsage: app.cacheusage,
        threads: {
            free: app.freeThreads,
            active: app.activeThreads,
            max: app.maxThreads
        },
        sessions: app.countSessions(),
        users: app.getActiveUsers().length,
        prototypes: app.getPrototypes().map(p => p.getName())
    }, null, 2));
}
```

## See Also

- [Concepts: Architecture](../concepts/architecture.md) — what `app` represents
- [Internal Invocation](../framework/internal-invocation.md) — `app.invoke` and `app.invokeAsync`
- [Cron Jobs](../framework/cron-jobs.md) — `app.addCronJob`
- [`ApplicationBean.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/framework/core/ApplicationBean.java) — source
