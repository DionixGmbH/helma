# apps.properties

`apps.properties` is the registry of applications. The `ApplicationManager` polls this file and starts/stops applications as their entries appear or disappear.

## Format

```properties
# Lines like just "blog" enable an app
blog

# Per-app settings: <appname>.<key> = <value>
blog.mountpoint = /
blog.appdir = /var/www/blog
blog.dbdir  = /var/www/blog-db
blog.repository.0 = /var/www/blog
blog.repository.1 = /var/www/blog-plugins
```

The first column is the **app name** — the prototype directory under `apps/` and the URL mountpoint.

## Per-App Settings

### Enabling / Disabling Apps

An app is enabled simply by listing its name as a bare key (no `.`) in `apps.properties`. To disable an app, **remove or comment out** the bare line. There is no `<app>.enabled` flag — `ApplicationManager.checkForChanges()` only inspects bare top-level keys as the set of apps to start.

```properties
# Enabled
blog

# Disabled (commented out)
# intranet
```

### `<app>.appdir`

Override the default `apps/<app>/` location for the **code**.

```properties
shop.appdir = /var/www/shop-code
```

### `<app>.dbdir`

Override the default `db/<app>/` location for the embedded XML database.

```properties
shop.dbdir = /var/www/shop-data
```

### `<app>.repository.N`

Add a repository at index N. Indices control search order — lower N is searched first.

```properties
shop.repository.0 = /var/www/shop-code
shop.repository.1 = /var/www/shared-modules
shop.repository.2 = /var/www/shop-plugins.zip
```

Without explicit repositories, the default is `apps/<app>/` as repository 0.

### `<app>.mountpoint`

URL prefix for this application. Default `/<app>`.

```properties
shop.mountpoint = /store
blog.mountpoint = /                    # mount at root — one app only
```

### `<app>.static`

Directory of static files to serve under `/<app>/static/`.

```properties
shop.static = /var/www/shop-assets
```

### `<app>.staticMountpoint`

URL prefix for static files. Defaults to the app's mountpoint followed by `/static` (e.g. `/blog/static`).

```properties
shop.staticMountpoint = /assets
```

### `<app>.staticHome`

Default file when the URL is the static mountpoint root (a directory). Defaults to `index.html, index.htm` (comma-separated list of candidate filenames).

```properties
shop.staticHome = home.html
```

### `<app>.protectedStatic`

A second static-files directory served via `res.forward(...)` indirection. Helma registers this directory as a Jetty resource base but **does not** automatically check authentication. You must gate access yourself before forwarding.

```properties
shop.protectedStatic = /var/www/shop-private
```

Usage from JavaScript:

```javascript
function private_action() {
    if (!session.user) { res.status = 403; return; }
    res.forward("/path/to/file");   // resolved against protectedStatic
}
```

### `<app>.cookieDomain`

Override the session cookie's `Domain` attribute. By default, no Domain is set (browser-default scope).

```properties
shop.cookieDomain = .example.com
```

### `<app>.sessionCookieName`

Override the session cookie name. Default `HopSession`.

```properties
shop.sessionCookieName = ShopSession
```

### `<app>.websocketIdleTimeout`

Jetty idle timeout for WebSocket connections, in seconds. A connection that has sent or received no frames in this period is closed by the server. Default `300` (5 minutes).

Set to `0` or a negative value to disable the timeout entirely — only do this when another layer (reverse proxy, load balancer) is responsible for cleaning up stale connections.

```properties
shop.websocketIdleTimeout = 600    # 10 minutes
shop.websocketIdleTimeout = -1     # no timeout
```

> **Note for reverse-proxy deployments**: your proxy's WebSocket read timeout must be longer than this value, otherwise the proxy will close the connection first. See [Reverse Proxy — WebSockets](../deployment/reverse-proxy.md#websockets).

### `<app>.uploadLimit`

Max upload size in KB. Default 1024.

```properties
shop.uploadLimit = 10240
```

### `<app>.charset`

Default response charset. Default `UTF-8`.

```properties
shop.charset = ISO-8859-1
```

### `<app>.caseInsensitive`

If `true`, HopObject property/method names are case-insensitive. Useful for legacy code or case-folding databases. Default `false`.

```properties
shop.caseInsensitive = true
```

### `<app>.classpath`

Comma-separated list of additional `.jar` files for this app's classloader.

```properties
shop.classpath = lib/jdbc-driver.jar, lib/custom.jar
```

### `<app>.welcomeFiles`

Comma-separated list of files to try when a request is for a directory. Default `index.html, index.htm`.

```properties
shop.welcomeFiles = home.html, index.html
```

## Example

```properties
# Default apps (enabled out of the box)
manage
welcome

# Main application — mounted at root
blog.mountpoint = /
blog.appdir = /var/www/blog
blog.dbdir = /var/www/blog/db
blog.static = /var/www/blog/public
blog.uploadLimit = 5120
blog.caseInsensitive = true

# Internal admin tool, IP-restricted via reverse proxy
admin.repository.0 = /var/www/admin
admin.repository.1 = /var/www/shared-modules
admin.dbdir = /var/www/admin/db

# Additional app served from the same server (e.g. an API).
# Note: apps in one server share a JVM and are NOT security-isolated from
# each other; treat them as a single trust domain (see deployment docs).
api.appdir = /var/www/api-code
api.repository.0 = /var/www/api-code
api.repository.1 = /var/www/shared-modules.zip
api.mountpoint = /api
```

## Reloading

The `ApplicationManager` polls `apps.properties` every 3 seconds (the interval is hardcoded in `Server.run()`). When you add a new app entry or change settings, Helma:

- Starts new apps
- Stops apps removed from the file
- Restarts apps whose settings changed

To force an immediate reload, use the management UI.

## See Also

- [Reference: app.properties](app-properties.md) — per-app runtime settings
- [Reference: server.properties](server-properties.md)
- [Getting Started: Running Helma](../getting-started/running.md)
- [Concepts: Repositories](../concepts/repositories.md)
