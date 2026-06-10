# Request Bean (`req`)

`req` is the request transmitter — a `RequestBean` wrapping the underlying `helma.framework.RequestTrans`. Defined in `src/main/java/helma/framework/RequestBean.java`.

This page lists every method and property.

## Method & Type

### `req.method` (String, read-only)

The HTTP method: `"GET"`, `"POST"`, `"PUT"`, `"DELETE"`, `"HEAD"`, `"OPTIONS"`, `"TRACE"`, or one of the Helma pseudo-methods (`"INTERNAL"`, `"EXTERNAL"`).

### `req.isGet()` / `req.isPost()` (boolean)

Convenience method checks.

### `req.isXmlHttpRequest()` (boolean)

True if the request contains the `X-Requested-With: XMLHttpRequest` header.

## URL

### `req.path` (String, read-only)

The path-info — the URL with the application mountpoint stripped. E.g. `/users/alice` for a request to `/myapp/users/alice`.

### `req.uri` (String, read-only)

The full request URI including the mountpoint.

### `req.action` (String, read-only)

The base action name (without `_action` suffix). E.g. for `/users/alice/edit`, `req.action` is `"edit"`.

### `req.actionHandler` (Object, read-write)

The function object to invoke. Set from `onRequest()` to override the URL-resolved action.

```javascript
function onRequest() {
    if (!session.user) {
        req.actionHandler = login_action;
    }
}
```

## Parameters

### `req.params` (Map, read-only)

Combined map of query and post parameters.

### `req.queryParams` (Map, read-only)

Just the query string parameters.

### `req.postParams` (Map, read-only)

Just the POST body parameters. For `multipart/form-data`, uploaded files appear here as `MimePart` objects.

### `req.cookies` (Map, read-only)

Map of cookie name → `jakarta.servlet.http.Cookie`.

```javascript
var lang = req.cookies.lang && req.cookies.lang.getValue();
```

### `req.data` (Map, read-only)

Free map. Pre-populated with:

| Key | Source |
|---|---|
| `http_host` | `Host` header |
| `http_referer` | `Referer` header |
| `http_remotehost` | Remote IP address |
| `http_browser` | `User-Agent` header |
| `http_language` | `Accept-Language` header |
| `authorization` | `Authorization` header |
| `body` | Raw POST body (for non-form content types) |

## Headers

### `req.getHeader(name)` (String)

Get a single header value. Returns `null` if missing.

### `req.getHeaders(name)` (String[])

Get all values of a multi-valued header.

### `req.getIntHeader(name)` (int)

Get a header parsed as an integer. Returns `-1` on missing or non-numeric.

### `req.getDateHeader(name)` (long)

Get a header parsed as a date, returned as Unix milliseconds. Returns `-1` on missing or invalid.

## Authentication

### `req.username` (String, read-only)

The username from HTTP Basic auth, or `null`.

### `req.password` (String, read-only)

The password from HTTP Basic auth, or `null`.

## Timing

### `req.runtime` (long, read-only)

Milliseconds since this request started processing.

## Servlet Access

### `req.servletRequest` (HttpServletRequest, read-only)

The underlying `jakarta.servlet.http.HttpServletRequest`. Use for low-level operations not exposed by the bean.

```javascript
var locale = req.servletRequest.getLocale();
var session = req.servletRequest.getSession(false);   // servlet session, not Helma's
```

## Generic Getter

### `req.get(name)` (Object)

Get any value by name from the `req.data` map. Equivalent to `req.data[name]`.

## Example

```javascript
function main_action() {
    if (!req.isGet()) {
        res.status = 405;
        return;
    }

    res.data.searchQuery = req.params.q || "";
    res.data.page = parseInt(req.params.page || "0", 10);

    res.write("Method: " + req.method + "\n");
    res.write("Path: " + req.path + "\n");
    res.write("Action: " + req.action + "\n");
    res.write("AJAX: " + req.isXmlHttpRequest() + "\n");
    res.write("User-Agent: " + req.data.http_browser + "\n");
    res.write("Runtime: " + req.runtime + "ms\n");
}
```

## See Also

- [Framework: Request & Response](../framework/request-response.md)
- [Framework: AJAX Action Resolution](../framework/ajax-actions.md)
- [`RequestBean.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/framework/RequestBean.java) — source
