# Servlet Container

Helma can run inside a separate servlet container (Tomcat, Jetty standalone, WildFly) instead of starting its own Jetty. Two modes:

1. **`EmbeddedServletClient`** — same JVM, direct call to `Application`. (Used by the bundled Jetty too.)
2. **`StandaloneServletClient`** — servlet talks to a separate Helma JVM via Java RMI.

Most users don't need either — the default standalone setup is simpler and recommended. This page is for the rare case where you must integrate Helma into an existing servlet container.

## EmbeddedServletClient

Same JVM, Helma `Application` instantiated directly inside the servlet.

### web.xml

```xml
<web-app>
    <servlet>
        <servlet-name>blog</servlet-name>
        <servlet-class>helma.servlet.EmbeddedServletClient</servlet-class>
        <init-param>
            <param-name>application</param-name>
            <param-value>blog</param-value>
        </init-param>
        <init-param>
            <param-name>uploadLimit</param-name>
            <param-value>2048</param-value>
        </init-param>
        <init-param>
            <param-name>cookieDomain</param-name>
            <param-value>.example.com</param-value>
        </init-param>
        <init-param>
            <param-name>sessionCookieName</param-name>
            <param-value>BlogSession</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>blog</servlet-name>
        <url-pattern>/*</url-pattern>
    </servlet-mapping>
</web-app>
```

### Init Parameters

| Parameter | Purpose |
|---|---|
| `application` | Name of the app from `apps.properties` |
| `uploadLimit` | Max upload size in KB |
| `totalUploadLimit` | Max total upload per request in KB |
| `uploadSoftfail` | If true, upload errors become a request flag |
| `cookieDomain` | Session cookie domain |
| `sessionCookieName` | Session cookie name |
| `protectedSessionCookie` | Pin session to client IP subnet |
| `debug` | Enable debug output |
| `caching` | Enable response caching |

The Helma server itself must still be initialized — typically by a `ServletContextListener` that calls `helma.main.Server.loadServer(args)` on startup. This is non-trivial; for new projects use standalone Helma + reverse proxy.

## StandaloneServletClient

Servlet running in container A communicates with Helma running in JVM B via Java RMI.

### Architecture

```
┌───────────────────┐         ┌───────────────────┐
│ Tomcat            │         │ Helma JVM         │
│  StandaloneServlet│ <─RMI─> │ Application "blog"│
└───────────────────┘         └───────────────────┘
```

### web.xml

```xml
<servlet>
    <servlet-name>blog</servlet-name>
    <servlet-class>helma.servlet.StandaloneServletClient</servlet-class>
    <init-param>
        <param-name>hopdir</param-name>
        <param-value>/opt/helma</param-value>
    </init-param>
    <init-param>
        <param-name>appdir</param-name>
        <param-value>/var/www/blog</param-value>
    </init-param>
    <init-param>
        <param-name>dbdir</param-name>
        <param-value>/var/www/blog-db</param-value>
    </init-param>
    <init-param>
        <param-name>application</param-name>
        <param-value>blog</param-value>
    </init-param>
    <init-param>
        <param-name>repository.0</param-name>
        <param-value>/var/www/blog/code</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>
```

### Trade-offs

| Pros | Cons |
|---|---|
| Helma can be restarted without restarting the container | Extra RMI hop per request |
| Multiple containers can share one Helma | More complex deployment |
| Run Helma as a different user/process | RMI port + firewall config |

In practice, this mode has fallen out of use. The standalone-with-reverse-proxy pattern is simpler and faster.

## When to Use Servlet Container Mode

- Your shop runs WildFly or Tomcat as standard and you can't deviate
- You need to share a JVM with another servlet-based app
- You want to use the container's session and SSL handling

Otherwise: **prefer standalone**. Helma's embedded Jetty is fast and well-tested.

## See Also

- [Standalone Server](standalone.md) — the recommended deployment
- [Reverse Proxy](reverse-proxy.md)
- [`AbstractServletClient.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/servlet/AbstractServletClient.java)
- [`EmbeddedServletClient.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/servlet/EmbeddedServletClient.java)
- [`StandaloneServletClient.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/servlet/StandaloneServletClient.java)
