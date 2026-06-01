# Running Helma

## Starting

### From source

```bash
./gradlew run
```

### From an installation

=== "macOS / Linux"

    ```bash
    ./bin/helma
    ```

=== "Windows"

    ```powershell
    bin\helma.bat
    ```

### Manually with `java`

```bash
java -jar launcher.jar -h "$(pwd)" -w 8080
```

See the [CLI reference](../reference/cli.md) for every command-line flag.

## Stopping

- `Ctrl+C` in the foreground process.
- Send `SIGTERM` (`kill <pid>`) — the shutdown hook in [`helma.main.HelmaShutdownHook`](../deployment/standalone.md) stops every app and persists the embedded DB.
- Use the **manage** application UI at [http://localhost:8080/manage/](http://localhost:8080/manage/).

## The Management Application

Helma bundles a web UI for managing the server, called `manage`. It is enabled by default in `apps.properties`:

```properties
manage
```

Browse to `http://localhost:8080/manage/`. From there you can:

- Start, stop, and restart individual applications
- View per-application stats (request count, active sessions, free threads)
- Inspect the embedded XML database
- Clear caches
- Trigger garbage collection

The first time you access `/manage` Helma writes a randomly-generated admin password to the server log and to `manage/db/`. Future access requires HTTP basic auth.

To disable the `manage` app, remove its line from `apps.properties`.

## Hot Reload

Helma watches every file in every prototype directory. When a file changes, the next request triggers `TypeManager.checkPrototypes()` which:

1. Re-reads `type.properties` and updates the [`DbMapping`](../database/type-properties.md).
2. Marks the prototype's `lastCodeUpdate` timestamp.
3. The next request to enter `RhinoCore.updatePrototypes()` recompiles the changed `.js` files into the scripting engine.
4. Skin templates are read on demand and cached until the file mtime changes.

For production, you can lower the watcher frequency or disable it by setting `updateInterval` in `app.properties`. See [app.properties](../reference/app-properties.md#updateinterval).

## Logs

Default logs go to `log/`:

- `helma.event.log` — server-wide events (startup, shutdown, errors)
- `helma.<appname>.event.log` — per-app `app.logEvent()`, `app.logError()`, and exceptions from request evaluators
- `helma.<appname>.access.log` — HTTP access log (one line per request)
- `helma.access.log` — server-wide HTTP access log

Logs rotate at midnight and are gzipped. Configure via the `logdir` server property and the `logCanonical` / `logTimeFormat` app properties — see [Logging](../framework/logging.md).

## Multiple Applications

`apps.properties` is the registry. Each line declares an application:

```properties
# minimal
blog

# with overrides
shop.appdir = /var/www/shop-code
shop.dbdir  = /var/www/shop-db
shop.repository.0 = /var/www/shop-code
shop.repository.1 = ${user.home}/shared-modules

# To disable an app, comment out or remove its bare line.
# intranet
```

| Setting | Effect |
|---|---|
| `<app>.appdir` | Override the default `apps/<app>/` location |
| `<app>.dbdir` | Override the default `db/<app>/` location |
| `<app>.repository.N` | Add an extra code repository at index N |
| `<app>.repository.N.implementation` | Repository class override |
| `<app>.mountpoint` | URL prefix; defaults to `/<app>` |
| `<app>.static` | Static-files directory served publicly |
| `<app>.staticHome` | Default file (defaults to `index.html, index.htm`) |
| `<app>.staticMountpoint` | URL prefix for static (defaults to `<mountpoint>/static`) |
| `<app>.protectedStatic` | Second static dir reachable via `res.forward()`; you must check auth yourself |
| `<app>.cookieDomain` | Cookie domain — overrides per-request origin |
| `<app>.sessionCookieName` | Cookie name for the session id (defaults to `HopSession`) |
| `<app>.uploadLimit` | Max upload size in KB (default 1024) |
| `<app>.charset` | Default response charset (default UTF-8) |
| `<app>.caseInsensitive` | If `true`, HopObject property access is case-insensitive |

See [apps.properties reference](../reference/apps-properties.md) for the complete list.

## Memory and Threads

By default Helma starts a fixed pool of [RequestEvaluator](../concepts/architecture.md) threads — one per concurrent request. Tune with:

```properties
# app.properties
maxThreads = 12
requestTimeout = 60      # seconds before a stuck request is killed
sessionTimeout = 30      # minutes before idle sessions expire
```

The default `maxThreads` is 50 (hardcoded in `Application.java`).

## Production Checklist

- Run behind a reverse proxy (nginx, Apache) for TLS termination — see [Reverse Proxy](../deployment/reverse-proxy.md).
- Set `caching = true` in `app.properties` to enable skin and prototype caches.
- Configure `db.properties` to point at a real database for any prototype you actually want to persist relationally.
- Remove or comment out the bare `manage` line in `apps.properties` to disable the management app, or put it behind a reverse proxy with IP-based access rules (Helma has no built-in HTTP IP allowlist — only `paranoid = true` + `allowXmlRpc` for XML-RPC).
- Configure log rotation (Helma does this on its own, but set `logSweepInterval` for the cleanup window).
- Persist sessions across restarts with `persistentSessions = true`.

See [Deployment](../deployment/index.md) for full guidance.
