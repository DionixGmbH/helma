# Response Bean (`res`)

`res` is the response transmitter — a `ResponseBean` wrapping the underlying `helma.framework.ResponseTrans`. Defined in `src/main/java/helma/framework/ResponseBean.java`.

This page lists every method and property.

## Output

### `res.write(str, ...)` 

Append one or more strings to the response buffer.

```javascript
res.write("hello");
res.write("a", "b", "c");          // varargs
res.write(12345);                  // numbers are toString-ed
```

### `res.writeln(str, ...)` 

Same as `write` plus a newline.

### `res.writeBinary(bytes)`

Write raw bytes — replaces the buffer with a binary payload.

```javascript
res.contentType = "image/png";
res.writeBinary(generatePng());
```

### `res.encode(obj)` / `res.format(obj)`

HTML-escape the object's `toString()` and write.

### `res.encodeXml(obj)`

XML-escape and write (includes `'` escape).

### `res.encodeForm(obj)`

Form/textarea-safe escape and write.

## Status & Headers

### `res.status` (int)

HTTP status code. Default 200.

### `res.contentType` (String)

Content-Type header. Default `text/html`.

### `res.charset` (String)

Character set for the response. Defaults to the app's charset (UTF-8).

### `res.realm` (String)

HTTP authentication realm. Setting this and `status = 401` triggers a `WWW-Authenticate` header.

### `res.setHeader(name, value)`

Set (replace) a header.

### `res.addHeader(name, value)`

Add (append) a header value.

### `res.setDateHeader(name, date)`

Set a header containing an HTTP-format date.

### `res.addDateHeader(name, date)`

Add a header containing an HTTP-format date.

## Caching

### `res.cache` (boolean)

Default `true`. Set `false` to disable client-side caching.

### `res.lastModified` (Date)

The `Last-Modified` header. The framework also uses this for conditional GET — if the request's `If-Modified-Since` matches, the framework emits 304.

### `res.etag` (String)

The `ETag` header. Used for conditional GET against `If-None-Match`.

### `res.dependsOn(thing)`

Contribute to the auto-ETag.

### `res.digest()`

Compute an ETag from all `dependsOn` values and emit 304 if matching.

### `res.notModified` (boolean, read-only)

True if the framework has determined the response is unchanged and a 304 should be emitted.

## Cookies

### `res.setCookie(name, value)`
### `res.setCookie(name, value, days)`
### `res.setCookie(name, value, days, path)`
### `res.setCookie(name, value, days, path, domain)`

Set a cookie. `days = -1` makes it a session cookie. `days = 0` deletes the cookie.

HttpOnly and Secure are **not** per-cookie parameters — they come from `app.properties::cookies.httpOnly` (default `true`) and `cookies.secure` (default `false`). See [Cookies](../framework/cookies.md).

### `res.unsetCookie(name)`

Delete a cookie.

## Redirects

### `res.redirect(url)`

Emit a 302 redirect (throws `RedirectException`).

### `res.forward(url)`

Internal forward (throws `RedirectException`).

### `res.stop()`

Bail immediately, no redirect (throws `RedirectException`).

### `res.abort()`

Roll back the transaction and bail (throws `AbortException`).

### `res.reset()`

Clear all output and state.

### `res.resetBuffer()`

Clear the response body only — keep headers, status, etc.

## Buffer Manipulation

### `res.push()` / `res.pushBuffer()`

Start a fresh output buffer; subsequent writes go there.

### `res.pop()` / `res.popString()` (String)

Return the captured string and restore the previous buffer.

### `res.pushBuffer(buffer)` (StringBuffer)

Push a specific `StringBuffer`.

### `res.popBuffer()` (StringBuffer)

Pop the current buffer without converting to string.

### `res.getBuffer()` (String)

Get the current buffer content as string (non-destructive).

## Data Maps

### `res.data` (Map)

Free map. Accessible from skins as `<% response.foo %>`.

### `res.handlers` (Map)

Map of macro handler names to objects. Used to register custom handlers for `<% handler.X %>` in skins.

### `res.meta` (Map)

Free map for response metadata. Not exposed in skins by default.

### `res.message` (String)

A user-visible message. Preserved across one redirect — perfect for "saved successfully" notifications.

## Error Info

### `res.error` (Throwable, read-only)

The current error, when the framework dispatches to the `error` action.

### `res.scriptStack` (String, read-only)

JS stack trace of the error.

### `res.javaStack` (String, read-only)

Java stack trace of the error.

## Skin Path

### `res.skinpath` (Object[])

Array of directories or HopObjects to search for skins. Set to override the default skin lookup.

## Transactions

### `res.commit()`

Commit the current DB transaction; start a new one.

### `res.rollback()`

Rollback the current DB transaction; start a new one.

### `res.abort()`

Throw `AbortException` — rollback and bail out of the request.

## Servlet Access

### `res.servletResponse` (HttpServletResponse, read-only)

The underlying `jakarta.servlet.http.HttpServletResponse`.

## Debug

### `res.debug(msg, ...)`

Append to the response's debug buffer. Render in skins via `<% response.debug %>`.

## Example

```javascript
function save_action_post() {
    if (!req.postParams.title) {
        res.status = 400;
        res.message = "Title is required";
        renderSkin("edit");
        return;
    }
    this.title = req.postParams.title;
    this.body = req.postParams.body;
    res.message = "Saved!";
    res.redirect(this.href());
}

function api_action() {
    res.contentType = "application/json";
    res.charset = "UTF-8";
    res.cache = false;
    res.write(JSON.stringify(this.toJSON()));
}

function image_action() {
    res.contentType = "image/jpeg";
    res.setHeader("Content-Disposition", 'inline; filename="' + this.name + '"');
    res.writeBinary(this.imageData);
}
```

## See Also

- [Framework: Request & Response](../framework/request-response.md)
- [Framework: Caching](../framework/caching.md)
- [Framework: Cookies](../framework/cookies.md)
- [`ResponseBean.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/framework/ResponseBean.java) — source
