# Standalone Server

The default and recommended way to deploy Helma — as a single JVM running both the Helma framework and an embedded Jetty HTTP server.

## Architecture

```
┌─────────────────────────────────────┐
│ JVM                                  │
│  ┌────────────────────────────────┐ │
│  │ helma.main.Server (singleton)  │ │
│  │  ├─ JettyServer                │ │
│  │  ├─ ApplicationManager         │ │
│  │  │   ├─ Application "blog"     │ │
│  │  │   ├─ Application "manage"   │ │
│  │  │   └─ ...                    │ │
│  │  ├─ Worker thread              │ │
│  │  └─ Shutdown hook              │ │
│  └────────────────────────────────┘ │
└─────────────────────────────────────┘
        ↑
        │ HTTP
        ↓
     Clients
```

## Starting

```bash
./bin/helma
```

Or with custom config:

```bash
./bin/helma -h /opt/helma -f /etc/helma/server.properties -w 8080
```

See the [CLI reference](../reference/cli.md).

## Shutdown Hook

Helma registers `HelmaShutdownHook` with the JVM. On `SIGTERM` (`kill <pid>`) or `Ctrl+C`:

1. Each application is stopped:
    - Worker thread is interrupted (cron jobs cancelled)
    - Sessions persisted if `persistentSessions = true`
    - Embedded DB flushed
    - JDBC connections closed
2. Jetty stops accepting new connections, drains in-flight requests (within idleTimeout)
3. JVM exits

Forced kill (`SIGKILL`) bypasses the hook — in-flight changes to the embedded DB may be lost.

## Process Lifecycle Under systemd

```ini
# /etc/systemd/system/helma.service
[Unit]
Description=Helma server
After=network.target postgresql.service

[Service]
Type=simple
User=helma
Group=helma
WorkingDirectory=/opt/helma
ExecStart=/opt/helma/bin/helma
Restart=on-failure
RestartSec=10
TimeoutStopSec=60
KillSignal=SIGTERM
SuccessExitStatus=0 143

Environment="JAVA_HOME=/usr/lib/jvm/java-25-openjdk"
Environment="JAVA_OPTS=-Xmx4g -Xms2g -Duser.timezone=UTC -Dfile.encoding=UTF-8"

# Security hardening
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/helma/log /opt/helma/db

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now helma
sudo systemctl status helma
journalctl -u helma -f
```

## Running on a Privileged Port

For port 80/443, you need elevated privileges. Three options:

1. **Reverse proxy** — recommended. Helma listens on 8080 or similar; nginx/Apache binds 80/443.
2. **`setcap`** — give the `java` binary `cap_net_bind_service`:
    ```bash
    sudo setcap 'cap_net_bind_service=+ep' /usr/lib/jvm/java-25-openjdk/bin/java
    ```
3. **`AmbientCapabilities=CAP_NET_BIND_SERVICE`** in systemd unit.

Helma's `JettyServer.openListeners()` is called before `start()` to allow privileged port binding under jsvc, but for systemd setcap is the standard approach.

## Filesystem Layout

```
/opt/helma/
├── bin/                  helma startup script
├── lib/                  Helma core + dependencies
├── lib/ext/              additional drivers (JDBC, etc.)
├── apps/                 application code
│   ├── blog/
│   ├── manage/
│   └── ...
├── db/                   embedded XML database (per app)
│   ├── blog/
│   └── ...
├── log/                  rotating log files
├── modules/              bundled JS modules
├── server.properties
├── apps.properties
├── db.properties
└── launcher.jar
```

Filesystem permissions:

- Helma user owns `apps/`, `db/`, `log/`, and the property files
- Others have no access to `db.properties` (contains passwords)

```bash
chmod 600 /opt/helma/db.properties
chown helma:helma /opt/helma -R
```

## Resource Limits

For high-traffic deployments:

```ini
# /etc/security/limits.d/helma.conf
helma soft nofile 65536
helma hard nofile 65536
```

Increase `LimitNOFILE` in the systemd unit if needed.

## Health Check

Add a tiny action that responds to load-balancer health checks:

```javascript
// Root/health.js
function health_action() {
    res.contentType = "text/plain";

    // Basic check
    if (!app.data.startupComplete) {
        res.status = 503;
        res.write("starting");
        return;
    }

    // Optional DB ping
    try {
        var db = app.getDbSource("main").getConnection();
        var stmt = db.createStatement();
        stmt.execute("SELECT 1");
        stmt.close();
    } catch (e) {
        res.status = 503;
        res.write("db unavailable: " + e);
        return;
    }

    res.write("ok");
}
```

Hit `https://example.com/health` to check.

## Monitoring

Helma exposes statistics via `app`:

```javascript
function metrics_action() {
    res.contentType = "text/plain";
    var lines = [
        "helma_requests_total " + app.requestCount,
        "helma_errors_total " + app.errorCount,
        "helma_active_sessions " + app.countSessions(),
        "helma_cache_size " + app.cacheusage,
        "helma_threads_active " + app.activeThreads,
        "helma_threads_free " + app.freeThreads,
        "helma_uptime_seconds " + Math.floor((Date.now() - app.upSince) / 1000)
    ];
    res.write(lines.join("\n"));
}
```

Scrape with Prometheus or similar.

## See Also

- [Reverse Proxy](reverse-proxy.md)
- [Jetty Configuration](jetty.md)
- [Logging Setup](logging.md)
- [Performance Tuning](performance.md)
