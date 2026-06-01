# Request & Response

Every action in Helma has implicit access to two globals: `req` (the request) and `res` (the response). This page covers the essentials. For complete API reference see the [`req` bean](../reference/req-bean.md) and [`res` bean](../reference/res-bean.md).

## The Request: `req`

```javascript
// What method?
req.method                 // "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE"
req.isGet()                // boolean
req.isPost()
req.isXmlHttpRequest()     // true if X-Requested-With: XMLHttpRequest

// What URL?
req.uri                    // "/myapp/users/alice"
req.path                   // "users/alice"  (mountpoint stripped)
req.action                 // "main"  (base action name, no _action suffix)

// Parameters
req.params                 // combined query + post (a Map)
req.queryParams            // ?foo=bar
req.postParams             // form/multipart POST body
req.cookies                // request cookies (Map of name → Cookie)
req.data                   // free map; auto-populated with http_host etc.

// Headers
req.getHeader("X-Foo")
req.getHeaders("Accept")     // String[]
req.getIntHeader("Content-Length")
req.getDateHeader("If-Modified-Since")

// HTTP basic auth (rarely used; prefer session-based auth)
req.username
req.password

// Time
req.runtime                  // ms since request start

// Raw access (escape hatch)
req.servletRequest           // the underlying jakarta.servlet.http.HttpServletRequest
```

### Reading parameters

```javascript
// Single value
var name = req.params.name;

// Multi-value (HTML <select multiple>)
var tags = req.params.tags;   // string or string[] depending on number of values

// File upload — uploaded files appear as MimePart objects
var photo = req.postParams.photo;
if (photo) {
    var bytes = photo.getContent();
    var filename = photo.getName();
    var contentType = photo.getContentType();
    // save somewhere
}
```

For nested form values like `<input name="user[name]">`, Helma parses `user[name]` into nested objects automatically. `req.params.user.name` gives `"alice"`.

### `req.data`

`req.data` is a free-form map shared between handlers and skins. Helma auto-populates:

- `http_host` — Host header
- `http_referer` — Referer header
- `http_remotehost` — remote IP
- `http_browser` — User-Agent
- `http_language` — Accept-Language
- `authorization` — Authorization header
- `body` — raw POST body (for `Content-Type` that isn't form-encoded)

Plus all query and form params (deprecated; use `req.params` instead).

### Overriding the action

In `onRequest()`:

```javascript
function onRequest() {
    if (!session.user && req.action !== "login") {
        req.actionHandler = login_action;     // a function reference
    }
}
```

`req.actionHandler` overrides the action selected from the URL. The base `req.action` string is unchanged.

## The Response: `res`

### Output

```javascript
res.write("plain text");                  // append to buffer, no escaping
res.write("a", "b", "c");                 // varargs OK
res.writeln("text");                      // write + newline
res.write(12345);                         // primitives get toString()
res.writeBinary(byteArray);               // binary payload; replaces buffer

res.encode("<script>");                   // HTML-escape and write: "&lt;script&gt;"
res.encodeXml("a & b");                   // XML-escape
res.encodeForm("Multi\nline");            // textarea-safe
res.format("text");                       // legacy alias for encode

renderSkin("name", { param: value });     // render a skin to the buffer
```

### Status, headers, charset

```javascript
res.status = 201;
res.contentType = "application/json";
res.charset = "UTF-8";

res.setHeader("X-Powered-By", "Helma");
res.addHeader("Set-Cookie", "...");
res.setDateHeader("Last-Modified", new Date());
res.addDateHeader("X-Sent-At", new Date());
```

### Cookies

```javascript
res.setCookie("name", "value");                                         // session cookie
res.setCookie("name", "value", 30);                                     // 30-day cookie
res.setCookie("name", "value", 30, "/path");                            // with path
res.setCookie("name", "value", 30, "/path", "example.com");             // with domain
// HttpOnly and Secure cannot be set per-cookie; they come from
// app.properties::cookies.httpOnly (default true) and cookies.secure (default false).

res.unsetCookie("name");                                                // delete cookie
```

See [Cookies](cookies.md) for the full API.

### Redirects

```javascript
res.redirect("/elsewhere");          // 302 redirect (also throws to bail)
res.forward("/internal");            // internal forward
res.stop();                          // bail immediately, no redirect
```

All three throw a `RedirectException` to bail out of the action. The transaction is still committed.

### Caching

```javascript
res.cache = false;                   // disable client-side caching for this response

res.lastModified = new Date();
res.etag = "abc123";

res.dependsOn("post-" + this.id);    // contribute to auto-ETag
res.digest();                        // emit 304 if all dependsOn unchanged
```

See [Caching](caching.md) for the full caching story.

### Response data

```javascript
res.data.foo = "bar";                // available in skins as <% response.foo %>
res.handlers.myThing = obj;          // register macro handler for <% myThing.* %>
res.meta.foo = "bar";                // meta data, not exposed in skins by default

res.message = "Saved!";              // persisted across one redirect — flash message
```

`res.message` survives a `res.redirect()` — useful for "saved successfully" notifications shown after PRG (Post-Redirect-Get).

### Buffer manipulation

```javascript
res.reset();                         // clear everything
res.resetBuffer();                   // clear body only

res.push();                          // start capturing output
res.write("captured");
var s = res.pop();                   // get captured string

res.pushBuffer(myBuffer);            // push a specific StringBuffer
res.popBuffer();                     // pop and return it
```

`push()/pop()` is what `renderSkinAsString()` uses internally.

### Transaction control

```javascript
res.commit();    // commit current DB transaction, start new one
res.rollback();  // rollback, start new
res.abort();     // throw AbortException → rollback + terminate request
```

### Error info

```javascript
res.error                  // current Throwable, or null
res.scriptStack            // JavaScript stack trace
res.javaStack              // Java stack trace
```

These are populated when the framework dispatches to the `error` action — the original error is captured for display.

## Conditional GET

For HTTP caching, set `res.lastModified` or `res.etag`. The framework compares against `If-Modified-Since` / `If-None-Match` from the request:

```javascript
function main_action() {
    res.lastModified = this.modified;
    res.etag = '"' + this.id + '-' + this.modified.getTime() + '"';
    if (res.notModified) return;        // emits 304 automatically
    renderSkin("main");
}
```

Or use `dependsOn()`:

```javascript
function main_action() {
    res.dependsOn(this.id);
    res.dependsOn(this.modified.getTime());
    res.dependsOn(session.user ? session.user.id : "anon");
    res.digest();                       // computes ETag and emits 304 if matching
    renderSkin("main");
}
```

`res.digest()` builds an ETag by hashing all `dependsOn` values and compares to the request's `If-None-Match`.

## Common Patterns

### POST-Redirect-Get

```javascript
function save_action() {
    if (req.isPost()) {
        this.title = req.postParams.title;
        res.message = "Saved!";
        res.redirect(this.href());           // 302
    }
    renderSkin("edit");
}
```

After redirect, the GET request retrieves the message and clears it.

### JSON API endpoint

```javascript
function api_action() {
    res.contentType = "application/json";
    res.charset = "UTF-8";
    res.write(JSON.stringify(this.toJSON()));
}
```

### File download

```javascript
function download_action() {
    res.contentType = "application/pdf";
    res.setHeader("Content-Disposition", 'attachment; filename="report.pdf"');
    res.writeBinary(this.generatePdf());
}
```

### Streaming a binary blob

```javascript
function image_action() {
    var image = new helma.Image(this.imageBytes);
    res.contentType = "image/jpeg";
    res.writeBinary(image.toByteArray("jpeg"));
}
```

### Internal forward

```javascript
function legacy_action() {
    res.forward("/new-url");        // identical to a 302 but kept internal
}
```

## See Also

- [Cookies](cookies.md)
- [File Uploads](file-uploads.md)
- [Caching](caching.md)
- [Reference: `req` bean](../reference/req-bean.md)
- [Reference: `res` bean](../reference/res-bean.md)
