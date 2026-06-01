# server.properties

`server.properties` configures the Helma server itself — ports, logging, extensions, server-wide defaults. Lives at the project/installation root.

## Locations

Helma looks for `server.properties` in this order:

1. The file specified by `-f <path>` on the command line
2. `<helma.home>/server.properties`
3. `server.properties` in the current directory

`helma.home` defaults to the directory containing `server.properties`, or the directory specified by `-h <path>` / `helma.home` system property.

## Network

### `webPort`

The HTTP listener port. Format: `host:port` or just `port`.

```properties
webPort = 8080
webPort = 127.0.0.1:8080
webPort = 0.0.0.0:80
```

Override with `-w <port>` on the command line.

### `xmlrpcPort`

Optional XML-RPC port. Helma also accepts XML-RPC over the regular HTTP port (POST with `Content-Type: text/xml`).

```properties
xmlrpcPort = 8081
```

Override with `-x <port>`.

### `paranoid`

If `true`, restrict **XML-RPC** access to a whitelist of IPs (configured via `allowXmlRpc`). Has no effect on regular HTTP. Helma does **not** have a built-in HTTP-level IP allowlist — use a reverse proxy for that.

```properties
paranoid = true
allowXmlRpc = 127.0.0.1, 192.168.1.0/24
```

### `allowXmlRpc`

Comma-separated list of IP addresses or CIDR ranges allowed to make XML-RPC calls (only effective when `paranoid = true`).

```properties
allowXmlRpc = 127.0.0.1, 10.0.0.0/8
```

## Paths

### `hophome` / `helma.home`

The Helma home directory. Falls back to `-h <path>`, then the `helma.home` system property, then the parent dir of `server.properties`.

```properties
hophome = /var/lib/helma
```

### `appsHome`

Override the default `apps/` directory.

```properties
appsHome = /var/lib/helma/apps
```

### `dbHome`

Override the default `db/` directory.

```properties
dbHome = /var/lib/helma/db
```

### `logdir`

Where log files are written. Set to `console` to log to stdout.

```properties
logdir = /var/log/helma
logdir = console
```

### `staticHome` / `static` (per-app — see apps.properties)

## Extensions

### `extensions`

Comma-separated list of `HelmaExtension` Java class names to load on server startup.

```properties
extensions = com.example.MyExtension, helma.extensions.demo.DemoExtension
```

Extensions can register additional globals, prototypes, and event listeners. See [Writing Java Extensions](../extensions/writing-extensions.md).

## Mail

### `smtp`

SMTP host for outgoing mail. The `helma.Mail` module reads this when not given an explicit host.

```properties
smtp = mail.example.com
```

### `smtpPort`

Optional SMTP port. Default 25.

```properties
smtpPort = 587
```

## Logging

### `logger`

The log implementation class. Default `helma.util.Logging`.

```properties
logger = helma.util.Logging
# or for slf4j:
logger = org.apache.commons.logging.impl.SLF4JLogFactory
```

### `logCanonical`

If `true`, use canonical hostnames in access logs. Default `false`.

### `logTimeFormat`

Date format for log entries. Default `yyyy-MM-dd HH:mm:ss zzz`.

```properties
logTimeFormat = yyyy-MM-dd HH:mm:ss.SSS
```

### `logSweepInterval`

Milliseconds between log directory sweeps for rotation. Default 60_000 (1 minute).

## Application Manager

The interval between scans of `apps.properties` is **hardcoded to 3 seconds** in `Server.run()` (`Thread.sleep(3000L)`). There is no configurable property for this; pick up of added/removed apps happens within a few seconds.

## Embedded Web Server

The embedded Jetty server starts when `webPort` is set (or an XML config is provided via the `-c` CLI flag). There is no `webServer` boolean; omit `webPort` to skip starting Jetty.

## Sample server.properties

```properties
# Network
webPort = 8080
xmlrpcPort = 8081

# Paths
hophome = .
logdir = log

# Mail
smtp = localhost
smtpPort = 587

# Security
paranoid = false
allowXmlRpc = 127.0.0.1

# Extensions
extensions = 

# Logging
logTimeFormat = yyyy-MM-dd HH:mm:ss.SSS
```

## See Also

- [Reference: apps.properties](apps-properties.md)
- [Reference: app.properties](app-properties.md)
- [Reference: CLI](cli.md)
- [Deployment: Standalone](../deployment/standalone.md)
- [`Server.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/main/Server.java) — source
