# HTTP Status & Errors

This page lists every HTTP status code emitted by the Helma framework itself, plus the conditions that produce them.

## Status Codes Set Automatically

### 200 OK (default)

The default response status. Returned for any successful action.

### 302 Found (redirect)

Emitted by `res.redirect(url)`. The framework sets `Location: <url>` and status 302.

```javascript
res.redirect("/elsewhere");
```

### 304 Not Modified

Emitted when:

- `res.lastModified` is set AND `If-Modified-Since` matches
- `res.etag` is set AND `If-None-Match` matches
- `res.digest()` is called AND the digest matches

```javascript
function show_action() {
    res.lastModified = this.modified;
    if (res.notModified) return;        // framework emits 304
    renderSkin("main");
}
```

### 401 Unauthorized

Emitted when `res.realm` is set AND `res.status = 401`. The framework adds `WWW-Authenticate: Basic realm="<realm>"`.

```javascript
res.realm = "Admin Area";
res.status = 401;
res.write("Please log in");
```

### 404 Not Found

Emitted automatically when:

- The URL path can't be resolved (an intermediate object is missing)
- `NotFoundException` is thrown
- The terminal action doesn't exist

The framework dispatches to the configured `notfound` action (default `notfound_action`) on the root object.

### 500 Internal Server Error

Emitted automatically when:

- An uncaught exception bubbles to the framework
- All `ConcurrencyException` retries have been exhausted
- A request times out

The framework dispatches to the configured `error` action on the root object. If the error action also fails, a minimal text body is sent: `Application too busy, please try again later`.

## Status Codes You Set Yourself

```javascript
res.status = 201;        // Created
res.status = 202;        // Accepted
res.status = 204;        // No Content
res.status = 400;        // Bad Request
res.status = 403;        // Forbidden
res.status = 405;        // Method Not Allowed
res.status = 409;        // Conflict
res.status = 410;        // Gone
res.status = 422;        // Unprocessable Entity
res.status = 429;        // Too Many Requests
res.status = 503;        // Service Unavailable
```

Set before any redirect or write. Once a redirect is set, status is forced to 302.

## Error Object Conventions

When the framework dispatches to the `error` action, `res.error` is populated:

```javascript
function error_action() {
    var e = res.error;                     // Throwable
    var scriptStack = res.scriptStack;     // JS stack trace
    var javaStack = res.javaStack;         // Java stack trace

    res.status = 500;
    res.write("Error: " + (e ? e.message : "unknown"));
    res.write("<pre>" + scriptStack + "</pre>");
}
```

For 404 errors, `res.error` is null — the framework sets `res.status = 404` and dispatches to `notfound_action`.

## Specific Framework Exceptions

| Exception | Source | Effect |
|---|---|---|
| `NotFoundException` | Path not found | 404 + dispatch to `notfound_action` |
| `RedirectException` | `res.redirect()`, `res.forward()`, `res.stop()` | Normal completion, redirect or no-op |
| `AbortException` | `res.abort()` | Transaction rolled back, response unchanged |
| `ConcurrencyException` | Optimistic locking conflict | Retry up to 8 times, then 500 |
| `TimeoutException` | Request exceeded `requestTimeout` | 500 + "Request timed out" |
| `ApplicationStoppedException` | App stopped mid-request | 503 + "Application stopped" |
| `IllegalStateException` | Various invariant violations | 500 |

## Conditional Status Logic

```javascript
function update_action_post() {
    // Validation
    if (!req.postParams.title) {
        res.status = 400;
        res.message = "Title required";
        renderSkin("edit");
        return;
    }

    // Authorisation
    if (!this.canEdit(session.user)) {
        res.status = 403;
        renderSkin("forbidden");
        return;
    }

    // Conflict detection (e.g. optimistic UI)
    if (req.postParams.version && req.postParams.version !== this.version) {
        res.status = 409;
        res.message = "Conflict — please reload";
        renderSkin("edit");
        return;
    }

    // Success
    this.title = req.postParams.title;
    this.version++;
    res.status = 200;
    res.redirect(this.href());
}
```

## Header-Sensitive Statuses

The `Date` header is included by Helma unless you suppress it via Jetty config. `Last-Modified` and `ETag` are emitted only when you set them. `Vary` is not emitted automatically — set it manually if you customise responses by request headers:

```javascript
res.setHeader("Vary", "Accept-Language, Cookie");
```

## See Also

- [Framework: Error Handling](../framework/error-handling.md)
- [Framework: Caching](../framework/caching.md) — conditional GET
- [Reference: `res` bean](res-bean.md) — full response API
