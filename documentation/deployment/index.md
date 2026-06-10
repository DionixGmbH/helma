# Deployment

This section covers how to deploy Helma to production. The **recommended** model
is **one trusted application per container**, with isolation between tenants
handled by the container runtime and orchestrator rather than by the JVM (Helma
has no in-process sandbox). Running directly on a host under a process
supervisor is still fully supported as an alternative.

| Page | Topic |
|---|---|
| [Container](container.md) | **Recommended** — one app per image, run under Docker/Podman/Kubernetes. |
| [Standalone Server](standalone.md) | Bare-metal alternative — Helma + embedded Jetty in one JVM. |
| [Jetty Configuration](jetty.md) | Configuring the embedded Jetty via XML or programmatically. |
| [Reverse Proxy](reverse-proxy.md) | Behind nginx, Apache, or Caddy. |
| [Logging Setup](logging.md) | Log rotation, slf4j, monitoring. |
| [Performance Tuning](performance.md) | JVM args, threading, caching. |

## Default Deployment

The simplest production setup is a container:

1. `./gradlew distTar` to produce the distribution tarball
2. `docker build -t helma:latest .`
3. Place your single application under `apps/<appname>/` and list it in `apps.properties`
4. Mount `server.properties` / `apps.properties` / `db.properties` and run the image
5. Front with a reverse proxy / ingress for TLS termination

See [Container](container.md) for the full walkthrough. To run on bare metal
instead, see [Standalone Server](standalone.md).

## Production Checklist

- [ ] App built into a container image (`./gradlew distTar` + `docker build`)
- [ ] One trusted app per image; `manage` app **not** shipped in public-facing images
- [ ] `server.properties` configured (ports, log dir)
- [ ] `apps.properties` lists exactly your app(s) with absolute appdir/dbdir
- [ ] `db.properties` injected as a secret/mount — **not** baked into the image
- [ ] Embedded `db/` on a persistent volume if used
- [ ] Reverse proxy / ingress in front with HTTPS, rewriting `X-Forwarded-For`
- [ ] Log rotation / log shipping configured
- [ ] Memory tuned (`-Xmx` matched to the container memory limit)
- [ ] Termination grace period long enough to drain in-flight requests
- [ ] Backup strategy for `db/` and SQL databases
- [ ] Health-check endpoint defined

## Process Supervision (bare metal)

When running directly on a host instead of in a container, run Helma under a
process supervisor that restarts on crash. Example systemd unit:

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
