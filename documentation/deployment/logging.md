# Logging Setup

For framework-level logging concepts see [Logging](../framework/logging.md). This page focuses on production setup.

## Log Files

| File | Source | Purpose |
|---|---|---|
| `log/helma.event.log` | Server | Startup, shutdown, extension loads |
| `log/helma.<app>.event.log` | Per-app event log | `app.log()`, `app.logError()`, exceptions |
| `log/helma.<app>.access.log` | Per-app access log | One line per HTTP request |
| `log/helma.access.log` | Server-wide access log | All apps combined |

Files rotate daily at midnight and the previous day is gzipped. See `helma.util.Logging` for the implementation.

## Log Rotation

Daily rotation is automatic. To clean up old files:

```bash
# systemd timer or cron
find /opt/helma/log -name "*.log.gz" -mtime +90 -delete
```

Or use `logrotate`:

```
# /etc/logrotate.d/helma
/opt/helma/log/*.log.gz {
    weekly
    rotate 12
    compress
    missingok
    notifempty
    nodelaycompress
}
```

## Logging to stdout (Container Friendly)

For Kubernetes/Docker, log to stdout so the container runtime captures logs:

```properties
# server.properties
logdir = console
```

All logs (event, access) write to stdout. The pod's stdout is then picked up by the container log driver.

## SLF4J / Logback

To use slf4j + logback for structured logging:

1. Add to `lib/ext/`:
    - `slf4j-api-2.x.jar`
    - `logback-classic-1.x.jar`
    - `logback-core-1.x.jar`
    - `jcl-over-slf4j-2.x.jar` (replaces commons-logging)
2. Remove (or never add) `helma.util.Logging` from the classpath default — actually it's bundled. Configure commons-logging to use SLF4J instead:
    ```properties
    # server.properties
    org.apache.commons.logging.Log = org.apache.commons.logging.impl.SLF4JLog
    ```
3. Add a `logback.xml` to the classpath:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%date %level [%logger{16}] %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/helma/helma.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>/var/log/helma/helma-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%date %level [%logger{16}] %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Log levels per category -->
    <logger name="helma" level="INFO"/>
    <logger name="helma.myapp" level="DEBUG"/>
    <logger name="helma.framework.core" level="INFO"/>
    <logger name="org.eclipse.jetty" level="WARN"/>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

## JSON Logging

For Elasticsearch / Loki / Splunk ingestion, use logback's JSON encoder:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
```

Add `logstash-logback-encoder` to `lib/ext/`.

## Access Log Format

Helma's default access log format:

```text
<timestamp> <remote-ip> <method> <path> <protocol> <status> <bytes> "<user-agent>"
```

Example:

```text
2026-06-01 12:34:56 +0200 192.0.2.1 GET /blog/posts/hello HTTP/1.1 200 5432 "Mozilla/5.0..."
```

To change the format, write a `RequestListener` extension. The framework doesn't expose format customisation from properties.

## Disabling Access Log

```properties
# app.properties
logAccess = false
```

For the server-wide access log:

```properties
# server.properties
logAccess = false
```

(Disable per-app then individually.)

## Slow Request Tracking

Helma doesn't log slow requests by default. Roll your own in `onResponse`:

```javascript
// Root/main.js
function onResponse() {
    if (req.runtime > 1000) {
        app.getLogger("slow").warn(
            req.method + " " + req.path + " took " + req.runtime + "ms"
        );
    }
}
```

Output goes to `log/slow.log` (with the default `helma.util.Logging` backend).

## Centralised Logging

For multi-server deployments, ship logs to a central aggregator:

- **Filebeat → Elasticsearch** — tail log files, ship to ES
- **Fluentd → S3/Loki** — same
- **Promtail → Loki** — same, simpler config
- **journald → systemd-journal-upload** — if logging via stdout under systemd

JSON logging at source makes parsing trivial.

## See Also

- [Framework: Logging](../framework/logging.md) — application-side logging
- [`Logging.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/util/Logging.java) — Helma's built-in logger
- [SLF4J docs](https://www.slf4j.org/)
- [Logback docs](https://logback.qos.ch/)
