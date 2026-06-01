# Deployment

This section covers how to deploy Helma to production.

| Page | Topic |
|---|---|
| [Standalone Server](standalone.md) | Default mode — Helma + embedded Jetty in one JVM. |
| [Jetty Configuration](jetty.md) | Configuring the embedded Jetty via XML or programmatically. |
| [Servlet Container](servlet-container.md) | Running Helma inside Tomcat, Jetty, etc. |
| [Reverse Proxy](reverse-proxy.md) | Behind nginx, Apache, or Caddy. |
| [Logging Setup](logging.md) | Log rotation, slf4j, monitoring. |
| [Performance Tuning](performance.md) | JVM args, threading, caching. |

## Default Deployment

The simplest production setup:

1. Build Helma from source or download a release
2. Edit `server.properties` and `apps.properties`
3. Place application code under `apps/<appname>/`
4. Run `./bin/helma`
5. Front with nginx or Apache for TLS termination

For most use cases, this is all you need.

## Production Checklist

- [ ] Java 25 installed and on PATH
- [ ] `server.properties` configured (ports, log dir)
- [ ] `apps.properties` lists your apps with absolute appdir/dbdir
- [ ] `db.properties` configured with real DB credentials (not committed to git)
- [ ] `manage` app disabled (bare line removed from `apps.properties`) or IP-restricted at the reverse proxy
- [ ] Reverse proxy in front with HTTPS
- [ ] Log rotation configured
- [ ] Memory tuned (`-Xmx`)
- [ ] Process supervisor configured (systemd, runit, supervisord)
- [ ] Backup strategy for `db/` and SQL databases
- [ ] Health-check endpoint defined

## Process Supervision

Helma should run under a process supervisor that restarts on crash. Example systemd unit:

```ini
[Unit]
Description=Helma server
After=network.target

[Service]
Type=simple
User=helma
WorkingDirectory=/opt/helma
ExecStart=/opt/helma/bin/helma
Restart=on-failure
RestartSec=5
SuccessExitStatus=0 143
TimeoutStopSec=60
Environment="JAVA_HOME=/usr/lib/jvm/java-25-openjdk"
Environment="JAVA_OPTS=-Xmx4g -Duser.timezone=UTC"

[Install]
WantedBy=multi-user.target
```

## See Also

- [Reference: server.properties](../reference/server-properties.md)
- [Reference: apps.properties](../reference/apps-properties.md)
- [Reference: CLI](../reference/cli.md)
