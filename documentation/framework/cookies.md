# Cookies

## Reading cookies

```javascript
// Map of cookie name → jakarta.servlet.http.Cookie
req.cookies

req.cookies["sessionid"]            // Cookie object, or undefined
req.cookies["sessionid"].getValue() // string value
req.cookies["sessionid"].getPath()
req.cookies["sessionid"].getDomain()

// Shortcut
var lang = req.cookies.lang && req.cookies.lang.getValue();
```

## Setting cookies

```javascript
res.setCookie(name, value);
res.setCookie(name, value, days);
res.setCookie(name, value, days, path);
res.setCookie(name, value, days, path, domain);
```

Parameters:

- `name` — cookie name. Must be a valid HTTP cookie name (no spaces or control characters).
- `value` — cookie value. Special characters are URL-encoded automatically when needed.
- `days` — lifetime in days. `-1` (default) → session cookie (deleted on browser close). `0` → delete cookie. Otherwise expiry = now + N days.
- `path` — URL path scope. Defaults to the request path, but in practice you typically want `/`.
- `domain` — domain scope. Defaults to the request host. Use to share cookies across subdomains.

!!! note "HttpOnly and Secure"
    `ResponseTrans.setCookie()` does **not** accept per-cookie HttpOnly or Secure flags. Those attributes are emitted globally for every response cookie based on app properties:

    - `cookies.httpOnly` — defaults to `true`; emits `HttpOnly` on all cookies unless explicitly set to `false`.
    - `cookies.secure` — defaults to `false`; emit `Secure` by setting to `true`.

    The session cookie is emitted by `AbstractServletClient` and follows the same properties. Set these in `app.properties` rather than passing args to `setCookie`.

```javascript
// 30-day persistent cookie, path "/"
// HttpOnly/Secure are governed by app.properties::cookies.httpOnly and cookies.secure
res.setCookie("lang", "en", 30, "/");

// Session-only cookie, defaults
res.setCookie("flash", "Saved!");

// Cross-subdomain
res.setCookie("sso", token, 7, "/", ".example.com");
```

## Deleting cookies

```javascript
res.unsetCookie(name);                  // shorthand for setCookie(name, "", 0, ...)
```

This sends `Set-Cookie: name=; Max-Age=0` to the browser, which evicts the cookie.

## The session cookie

The session cookie is special. Its name is configurable per-application:

```properties
# apps.properties
myapp.sessionCookieName = MySession
```

Default: `HopSession`.

Helma sets the session cookie automatically when:

- A new anonymous session is created
- The session is logged in/out (to renew the cookie)
- `session.touch()` is called and the session was previously not registered

The session cookie:

- Has `path = /<app-mountpoint>` so it's scoped to the app
- Is **HttpOnly** by default
- Is **not** Secure by default — for HTTPS deployments, set `cookies.secure = true` in `app.properties`

## SameSite

Helma 25.x emits cookies using the underlying `jakarta.servlet.http.Cookie` class. SameSite handling is delegated to Jetty's default cookie spec. To force `SameSite=Strict`:

```javascript
res.setHeader("Set-Cookie", "name=value; SameSite=Strict; Path=/; HttpOnly");
```

(Bypassing the `setCookie` helper and writing the header directly.)

Or configure Jetty's session-cookie-config via the XML config — see [Jetty Configuration](../deployment/jetty.md).

## Cookies and Cache

A response that sets cookies is **not** cacheable by intermediaries. Helma automatically disables caching when `res.cookies` is non-empty unless you explicitly set `res.cache = true`.

## Encoding and special characters

`res.setCookie` does not URL-encode the value automatically. To be safe with non-ASCII values:

```javascript
res.setCookie("greeting", encodeURIComponent("héllo"));

// And on read:
var v = decodeURIComponent(req.cookies.greeting.getValue());
```

## Browser quirks

- Domain attribute: leading `.` is optional in modern browsers but historically required for cross-subdomain.
- Max-Age vs Expires: Helma emits `Max-Age` (a positive integer count of seconds). Some old browsers needed `Expires` — Helma omits `Expires` but Jetty's cookie serialization may add both.
- Path scoping is strict: a cookie set at `/myapp/` is not sent for `/other/`. To set a cookie visible app-wide, use `path = "/"`.
