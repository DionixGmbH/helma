# Container Deployment

Running Helma in a container is the **recommended** production deployment. The
model is deliberately simple: **one trusted application per container image**,
with isolation between tenants delegated to the container runtime and
orchestrator (Kubernetes, Nomad, ECS, plain Docker/Podman) rather than to
anything inside the JVM.

> **Trust model.** Helma runs application JavaScript with the full privileges of
> the JVM — there is no in-process sandbox (the legacy `SecurityManager` was
> removed; see [Architecture](../concepts/architecture.md)). Treat everything in
> one container as a single trust domain and rely on the runtime for isolation.

## Build flow

The image is built from the Gradle distribution tarball, so the build is two
steps: produce the dist, then build the image.

```bash
# 1. Produce build/distributions/helma-<version>.tgz
./gradlew distTar

# 2. Build the image (repo root contains the Dockerfile)
docker build -t helma:latest .
```

The provided `Dockerfile` uses a multi-stage build on Alpine: it unpacks the
distribution into `/opt/helma` and runs it on `openjdk25-jre`, with
`WORKDIR /opt/helma` and `CMD ["bin/helma"]`.

### Bundling a JDBC driver

Database drivers are loaded from `lib/ext`. The Dockerfile can fetch MariaDB or
PostgreSQL drivers at build time via build args:

```bash
docker build --build-arg WITH_MARIADB=true  -t helma:latest .
docker build --build-arg WITH_POSTGRES=true -t helma:latest .
```

For any other database, add the driver JAR to `lib/ext` in your own image layer.

## Running

```bash
docker run --rm -p 8080:8080 helma:latest
```

Helma serves HTTP on port 8080 by default. In production you front the container
with a reverse proxy / ingress for TLS — see [Reverse Proxy](reverse-proxy.md).

## Configuration

Bake immutable defaults into the image; supply environment- and
deployment-specific values at run time. The three files that vary per
deployment are `server.properties`, `apps.properties`, and `db.properties`.

### Mounting config and secrets

```bash
docker run --rm -p 8080:8080 \
  -v "$PWD/server.properties:/opt/helma/server.properties:ro" \
  -v "$PWD/apps.properties:/opt/helma/apps.properties:ro" \
  -v "$PWD/db.properties:/opt/helma/db.properties:ro" \
  helma:latest
```

- **Never bake `db.properties` with real credentials into the image.** Mount it,
  or inject credentials as a secret (Kubernetes `Secret`, Docker secret) and
  reference them.
- The embedded object database lives under `db/`. Mount it on a **persistent
  volume** if any prototype relies on it, otherwise data is lost when the
  container is replaced. Relationally-mapped prototypes persist to your external
  SQL database instead.

### JVM options

Pass JVM flags via `JAVA_OPTS` (honoured by `bin/helma`):

```bash
docker run --rm -p 8080:8080 \
  -e JAVA_OPTS="-Xmx4g -Duser.timezone=UTC" \
  helma:latest
```

Prefer setting a fixed heap (`-Xmx`) sized to the container's memory limit.

## Single-app layout

Because a container is one trust domain, run **one app per image**. Put your
application under `apps/<appname>/` and list exactly that app (plus, optionally,
nothing else) in `apps.properties`:

```properties
# apps.properties
myapp
myapp.mountpoint = /
```

Do **not** ship the bundled `manage` app in a public-facing image — it has no
default credentials and no HTTP-level IP allowlist. If you need it, run it in a
separate, internal-only deployment behind the proxy. See
[Authentication](../framework/authentication.md).

## Health checks

Define a lightweight action your orchestrator can poll, and wire it to a
container/readiness probe:

```javascript
// Root/health.js
function health_action() {
    res.contentType = "text/plain";
    res.write("ok");
}
```

```yaml
# docker-compose / k8s readiness probe (sketch)
healthcheck:
  test: ["CMD", "wget", "-qO-", "http://localhost:8080/health"]
  interval: 30s
  timeout: 3s
  retries: 3
```

## Graceful shutdown

Helma installs a shutdown hook that stops apps and flushes the embedded DB on
`SIGTERM` (see [Standalone Server](standalone.md#shutdown-hook)). Container
runtimes send `SIGTERM` on stop, so this works out of the box — just give the
container enough termination grace (e.g. Kubernetes `terminationGracePeriodSeconds`)
to drain in-flight requests.

## See Also

- [Reverse Proxy](reverse-proxy.md) — TLS termination and `X-Forwarded-For`
- [Performance Tuning](performance.md) — heap, threads, caching
- [Reference: server.properties](../reference/server-properties.md)
- [Reference: apps.properties](../reference/apps-properties.md)
