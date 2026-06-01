# Jetty Configuration

Helma embeds Jetty 12 (`jetty-ee9` for Servlet 5 compatibility). Two configuration modes:

1. **Programmatic** — simple HTTP listener configured from CLI args / `server.properties`
2. **XML configuration** — full Jetty XML config file via `-c <path>`

## Programmatic (Default)

When `webPort` is set in `server.properties` and no `-c` flag is given, Helma starts a single HTTP connector:

- No `Server` header
- No `Date` header
- Idle timeout 30 seconds
- Acceptor priority 0
- Accept queue 0 (system default)
- Bound to the configured host:port

Implementation: `JettyServer.java:63-83`. Tweaks require editing source or switching to XML config.

## XML Configuration

For TLS, multiple connectors, request handlers, contexts, etc., use Jetty's XML config:

```bash
./bin/helma -c /etc/helma/jetty.xml
```

Or in `server.properties`:

```properties
# Note: this is set via CLI, not server.properties
# Use -c <path>
```

A minimal `jetty.xml`:

```xml
<?xml version="1.0"?>
<!DOCTYPE Configure PUBLIC "-//Mort Bay Consulting//DTD Configure//EN"
                            "https://www.eclipse.org/jetty/configure_10_0.dtd">
<Configure id="Server" class="org.eclipse.jetty.server.Server">

    <Set name="dumpAfterStart">false</Set>
    <Set name="stopAtShutdown">true</Set>
    <Set name="stopTimeout">10000</Set>

    <Call name="addConnector">
        <Arg>
            <New class="org.eclipse.jetty.server.ServerConnector">
                <Arg><Ref refid="Server"/></Arg>
                <Set name="host">0.0.0.0</Set>
                <Set name="port">8080</Set>
                <Set name="idleTimeout">30000</Set>
            </New>
        </Arg>
    </Call>
</Configure>
```

## HTTPS

Add a `ServerConnector` with `SslContextFactory`:

```xml
<New id="sslContextFactory" class="org.eclipse.jetty.util.ssl.SslContextFactory$Server">
    <Set name="KeyStorePath">/etc/helma/keystore.jks</Set>
    <Set name="KeyStorePassword">password</Set>
    <Set name="KeyManagerPassword">password</Set>
</New>

<Call name="addConnector">
    <Arg>
        <New class="org.eclipse.jetty.server.ServerConnector">
            <Arg><Ref refid="Server"/></Arg>
            <Arg type="int">1</Arg>
            <Arg type="int">-1</Arg>
            <Arg>
                <Array type="org.eclipse.jetty.server.ConnectionFactory">
                    <Item>
                        <New class="org.eclipse.jetty.server.SslConnectionFactory">
                            <Arg><Ref refid="sslContextFactory"/></Arg>
                            <Arg>http/1.1</Arg>
                        </New>
                    </Item>
                    <Item>
                        <New class="org.eclipse.jetty.server.HttpConnectionFactory"/>
                    </Item>
                </Array>
            </Arg>
            <Set name="host">0.0.0.0</Set>
            <Set name="port">8443</Set>
        </New>
    </Arg>
</Call>
```

In practice, TLS termination is usually handled at the reverse proxy. See [Reverse Proxy](reverse-proxy.md).

## Increase Idle Timeout

For long-polling endpoints:

```xml
<Set name="idleTimeout">300000</Set>     <!-- 5 minutes -->
```

## Bind to a Specific Address

```xml
<Set name="host">192.168.1.10</Set>
<Set name="port">8080</Set>
```

## Configure Thread Pool

```xml
<Set name="ThreadPool">
    <New class="org.eclipse.jetty.util.thread.QueuedThreadPool">
        <Set name="minThreads">10</Set>
        <Set name="maxThreads">200</Set>
        <Set name="idleTimeout">60000</Set>
    </New>
</Set>
```

This is Jetty's HTTP-accepting thread pool — separate from Helma's RequestEvaluator pool. Both should be sized to fit your workload.

## Multiple Apps with Different Mountpoints

By default, each Helma app gets its own context path (`/<appname>`). To customise URL routing globally, add a `HandlerCollection` in the XML config. This is rarely needed — `apps.properties::<app>.mountpoint` usually suffices.

## Logging Jetty Output

By default Helma sets `-Dorg.eclipse.jetty.LEVEL=WARN` via the `bin/helma` script to silence Jetty's INFO logging. To debug Jetty issues:

```bash
JAVA_OPTS="-Dorg.eclipse.jetty.LEVEL=DEBUG" ./bin/helma
```

## See Also

- [Jetty 12 documentation](https://eclipse.dev/jetty/documentation/jetty-12/operations-guide/index.html)
- [Reverse Proxy](reverse-proxy.md)
- [`JettyServer.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/main/JettyServer.java) — implementation
