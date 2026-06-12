# Reverse Proxy

The standard production setup is **Helma behind a reverse proxy** that handles TLS, compression, caching, and static-file serving. This page shows nginx, Apache httpd, and Caddy configurations.

## Why a Reverse Proxy?

- **TLS termination** — the reverse proxy handles certificates and HTTPS
- **HTTP/2 and HTTP/3** — Helma's Jetty supports HTTP/1.1; the proxy adds modern protocols
- **Gzip/Brotli compression** — better at the proxy layer
- **Caching** — proxies cache static and conditional GETs efficiently
- **Privileged port binding** — proxy binds 443; Helma binds 8080 unprivileged
- **Multiple apps on one host** — different paths to different Helma installs
- **Rate limiting and DoS protection**

## nginx

```nginx
upstream helma {
    server 127.0.0.1:8080;
    keepalive 16;
}

server {
    listen 80;
    server_name www.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name www.example.com;

    ssl_certificate     /etc/letsencrypt/live/www.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/www.example.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    # Static files served directly
    location /static/ {
        alias /var/www/helma-static/;
        expires 1d;
        add_header Cache-Control "public, immutable";
    }

    # Proxy everything else to Helma
    location / {
        proxy_pass http://helma;
        proxy_http_version 1.1;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host  $host;
        proxy_set_header Connection        "";
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
        client_max_body_size 10m;

        # Optional: enable response buffering for slower clients
        proxy_buffering on;
    }

    # Gzip for compressible content
    gzip on;
    gzip_types text/plain text/css text/javascript application/javascript application/json text/xml application/xml;
    gzip_min_length 1024;
}
```

In Helma's `app.properties`:

```properties
baseuri = https://www.example.com/
```

So `href()` generates proper HTTPS URLs.

## Apache httpd

```apache
<VirtualHost *:80>
    ServerName www.example.com
    Redirect permanent / https://www.example.com/
</VirtualHost>

<VirtualHost *:443>
    ServerName www.example.com

    SSLEngine on
    SSLCertificateFile      /etc/letsencrypt/live/www.example.com/cert.pem
    SSLCertificateKeyFile   /etc/letsencrypt/live/www.example.com/privkey.pem
    SSLCertificateChainFile /etc/letsencrypt/live/www.example.com/fullchain.pem

    # Static
    Alias /static /var/www/helma-static
    <Directory /var/www/helma-static>
        Require all granted
        ExpiresActive on
        ExpiresDefault "access plus 1 day"
    </Directory>

    # Proxy to Helma
    ProxyRequests Off
    ProxyPreserveHost On

    ProxyPass        /static !
    ProxyPass        / http://127.0.0.1:8080/
    ProxyPassReverse / http://127.0.0.1:8080/

    RequestHeader set X-Forwarded-Proto "https"

    # Gzip
    SetOutputFilter DEFLATE
    SetEnvIfNoCase Request_URI \.(?:gif|jpe?g|png|webp|woff2?)$ no-gzip dont-vary
</VirtualHost>
```

## Caddy

Caddy 2 has the simplest config — automatic HTTPS via Let's Encrypt:

```caddyfile
www.example.com {
    handle /static/* {
        root * /var/www/helma-static
        file_server
    }

    reverse_proxy 127.0.0.1:8080 {
        header_up X-Forwarded-Proto {scheme}
        header_up X-Forwarded-Host {host}
    }

    encode gzip zstd
}
```

## Helma Configuration for Reverse Proxy

In `app.properties`:

```properties
# Generate URLs with the proxy's hostname/scheme
baseuri = https://www.example.com/

# Secure session cookie (only over HTTPS)
cookies.secure = true
```

In `apps.properties`:

```properties
# Set X-Forwarded-* trust (informational; Helma uses HttpServletRequest header lookups)
myapp.cookieDomain = .example.com
```

Reading the real client IP in your action:

```javascript
var ip = req.getHeader("X-Forwarded-For") || req.getHeader("X-Real-IP") || req.data.http_remotehost;
```

## WebSockets

WebSocket upgrade requests go through the same proxy as regular HTTP, but need additional headers and a longer idle timeout.

### nginx

Add a dedicated `location` block for your WebSocket endpoint(s) **before** the catch-all `location /`:

```nginx
# WebSocket endpoint(s) — adjust the path to match your app
location /ws/ {
    proxy_pass http://helma;
    proxy_http_version 1.1;
    proxy_set_header Upgrade    $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host       $host;
    proxy_set_header X-Real-IP  $remote_addr;
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # Must be longer than Helma's websocketIdleTimeout (default 300 s)
    proxy_read_timeout 360s;
    proxy_send_timeout 360s;
}
```

Key differences from the HTTP block:
- `proxy_set_header Upgrade $http_upgrade` and `Connection "upgrade"` are required for the WebSocket handshake.
- `proxy_read_timeout` must exceed Helma's `websocketIdleTimeout` (default 300 s); otherwise nginx drops the connection first.
- Do **not** set `proxy_buffering on` for WebSocket locations.

### Caddy

Caddy proxies WebSockets automatically — no extra headers needed:

```caddyfile
www.example.com {
    reverse_proxy /ws/* 127.0.0.1:8080 {
        header_up X-Forwarded-Proto {scheme}
        header_up X-Forwarded-Host  {host}
        # flush_interval -1 disables buffering for streaming/WS responses
        flush_interval -1
    }

    # … rest of your config
}
```

### Timeout alignment

Helma's WebSocket idle timeout defaults to 300 seconds and is configurable per app via [`<app>.websocketIdleTimeout`](../reference/apps-properties.md#appwebsocketidletimeout) in `apps.properties`. The proxy read timeout must be set **above** that value — a 20 % margin is a good rule of thumb:

| `websocketIdleTimeout` | Recommended proxy read timeout |
|------------------------|-------------------------------|
| 300 s (default)        | 360 s                         |
| 600 s                  | 720 s                         |
| disabled (`-1`)        | 0 / unlimited                 |

## Connection Pooling

For nginx with `upstream` keepalive (shown above), Helma's Jetty accepts persistent connections by default — no extra config needed.

For Apache, use `mod_proxy_http` (HTTP/1.1) which keeps connections alive automatically.

## Static File Serving

Two options:

1. **Reverse proxy serves static** — fastest, but each static dir must be mounted explicitly in nginx/Apache
2. **Helma serves static via `static`** — easier but slower

For high-traffic, option 1 wins. Helma's `<app>.static` is fine for low-traffic sites or when assets are app-managed.

## TLS Termination

TLS terminates at the proxy. Helma sees HTTP. Tell it to mark cookies Secure:

```properties
# app.properties
cookies.secure = true       # all emitted cookies get the Secure attribute
cookies.httpOnly = true     # default; emits HttpOnly on all cookies
```

`res.setCookie()` does not accept per-cookie HttpOnly/Secure arguments — these attributes are emitted globally based on the two `app.properties` settings above.

## See Also

- [Standalone Server](standalone.md)
- [Logging Setup](logging.md)
- [`AbstractServletClient.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/servlet/AbstractServletClient.java) — header handling
