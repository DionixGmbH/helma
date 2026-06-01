# Command-Line Interface

Helma starts via `bin/helma` (Linux/macOS) or `bin\helma.bat` (Windows). Both wrappers invoke `java -jar launcher.jar` with command-line arguments.

## Usage

```
helma [options]
```

## Options

### `-h <path>`

Set the Helma home directory.

```bash
./bin/helma -h /var/lib/helma
```

Equivalent to setting the JVM system property `-Dhelma.home=/var/lib/helma`. The home directory contains `server.properties`, `apps.properties`, and the `apps/`, `db/`, `log/` directories.

### `-f <path>`

Specify a `server.properties` file. If unset, Helma looks in `<helma.home>/server.properties`.

```bash
./bin/helma -f /etc/helma/server.properties
```

### `-w <host:port>` / `-w <port>`

Set the HTTP listener port (and optionally host).

```bash
./bin/helma -w 8080
./bin/helma -w 127.0.0.1:80
./bin/helma -w 0.0.0.0:8080
```

Overrides `webPort` in `server.properties`.

### `-x <host:port>` / `-x <port>`

Set the XML-RPC listener port.

```bash
./bin/helma -x 8081
```

Overrides `xmlrpcPort`.

### `-a <app1,app2,...>`

Comma-separated list of applications to start. Overrides `apps.properties` for this run.

```bash
./bin/helma -a blog,api
```

Useful for starting a subset of apps during testing.

### `-c <path>`

Path to a Jetty XML configuration file. When given, Helma uses Jetty's `XmlConfiguration` to set up the embedded server.

```bash
./bin/helma -c etc/jetty.xml
```

Use this to configure SSL, multiple connectors, request handling chains, etc.

### `-i <ignored>`

Reserved for the launcher (handled before Helma sees args).

## Examples

### Default startup

```bash
./bin/helma
```

Uses `server.properties` in the current directory or `helma.home`, listens on the port specified there.

### Production with custom config

```bash
./bin/helma \
    -h /var/lib/helma \
    -f /etc/helma/server.properties \
    -c /etc/helma/jetty.xml \
    -w 8080 -x 8081
```

### Run only one app

```bash
./bin/helma -a blog
```

Starts only `blog`, ignoring other entries in `apps.properties`.

### Debug mode with the Rhino debugger

Set `rhino.debug = true` in `app.properties`, then start normally:

```bash
./bin/helma
```

The debugger UI opens on the first request.

## JVM Properties

Helma reads several JVM system properties:

| Property | Default | Purpose |
|---|---|---|
| `helma.home` | (from `-h`) | Helma home directory |
| `user.timezone` | system default | Time zone for cron and dates |
| `file.encoding` | platform default | Default charset for file I/O |

Set via `-D` on the JVM:

```bash
java -Duser.timezone=UTC -Dfile.encoding=UTF-8 -jar launcher.jar -h .
```

The `bin/helma` script reads `JAVA_OPTS` env var:

```bash
JAVA_OPTS="-Xmx4g -Duser.timezone=UTC" ./bin/helma
```

## Gradle Tasks (Development)

When running from source:

| Task | Description |
|---|---|
| `./gradlew run` | Compile and run Helma |
| `./gradlew debug` | Run with JDWP debugging enabled on port 5005 |
| `./gradlew rhinoShell` | Open the interactive Rhino JS shell |
| `./gradlew commandLine -Pfunction=app.fn` | Run an app function and exit (see below) |
| `./gradlew installDist` | Build into `build/install/helma/` |
| `./gradlew update` | Sync `build/install/helma/` over the project dir |
| `./gradlew distTar` / `distZip` | Build a redistributable archive |
| `./gradlew xgettext` | Extract i18n strings from app code |
| `./gradlew po2js` | Convert PO files to JS |

### `commandLine`

Run a function on a Helma app and exit:

```bash
./gradlew commandLine -Pfunction=blog.dailyCleanup
```

This loads the application, invokes `Root.dailyCleanup()` in `blog`, prints the return value, and exits. Useful for cron jobs without keeping Helma running.

## Exit Codes

| Code | Meaning |
|---|---|
| 0 | Clean shutdown |
| 1 | Configuration error |
| 1+ | Various startup or runtime errors |

## Shutdown Hook

A JVM shutdown hook (`HelmaShutdownHook`) is registered on startup:

1. On `SIGTERM` or `Ctrl+C`, the hook fires
2. Each running application is stopped:
    - Sessions are persisted if `persistentSessions = true`
    - Embedded DB is flushed
    - JDBC connections closed
3. Jetty is stopped
4. JVM exits

Force-kill (`SIGKILL`) bypasses the hook — the embedded DB may have in-flight writes.

## See Also

- [Reference: server.properties](server-properties.md)
- [Reference: apps.properties](apps-properties.md)
- [Deployment: Standalone](../deployment/standalone.md)
