# Global Functions

This page lists every free function in the global scope of a Helma application — i.e. functions you can call without any qualifier.

The functions come from three sources:

1. **`GlobalObject.java`** — registered by the Helma framework. Always available.
2. **`modules/core/*.js`** — bundled core extensions. Available **only after** explicitly loading them with `app.addRepository("modules/core/all.js")`.
3. **The application** — globals defined in `Global/*.js` and any CommonJS modules pulled in via `require`.

This page covers (1) and the key parts of (2). See [modules/core](../modules/core/index.md) for the full core API and how to enable it.

## Framework-Provided Globals

These come from `helma.scripting.rhino.GlobalObject` and are defined for every application.

### `renderSkin(skinName, paramObj)`

Render a "global" skin — one not bound to any HopObject. The skin is looked up against the `Global` prototype's skin directory.

```javascript
renderSkin("navigation");
renderSkin("page", { title: "Hello", body: "Content" });
```

The skin's `this` will be `null`. Use `<% param.title %>` inside the skin to access the parameter.

### `renderSkinAsString(skinName, paramObj)`

Same as `renderSkin` but returns the rendered output as a string instead of writing to the response.

```javascript
var html = renderSkinAsString("notification", { msg: "Hi" });
sendEmail(user, "Welcome", html);
```

### `createSkin(source)`

Parse a string into a `Skin` object.

```javascript
var dynamic = "<h1><% this.name %></h1>";
var skin = createSkin(dynamic);
skin.render(post);
```

Useful for user-supplied templates, but **review for injection**. Use the sandbox constructor for untrusted input:

```javascript
var sandbox = new java.util.HashSet();
sandbox.add("name");
sandbox.add("body");
var skin = new Packages.helma.framework.core.Skin(userSource, app.get__app__(), sandbox);
```

### `getProperty(name, defaultValue)`

Read a property from `app.properties`.

```javascript
var smtpHost = getProperty("smtp", "localhost");
var maxThreads = getProperty("maxThreads", "12");
```

Equivalent to `app.getProperty(name, defaultValue)`.

### `authenticate(user, pwd)`

Check credentials against the server-wide or app-local `passwd` file.

```javascript
if (authenticate("admin", req.password)) {
    // grant access
}
```

The `passwd` file accepts entries hashed with the framework's PBKDF2 format (`hashPassword()`, recommended) as well as legacy Unix `crypt(3)` and MD5-hex entries. All are checked with a constant-time comparison; no configuration switch is needed.

### `hashPassword(plain)`

Hash a plaintext password into a self-describing, salted **PBKDF2-HMAC-SHA256** string suitable for storage. Use this instead of unsalted digests like `String.md5()` for user credentials.

```javascript
user.password_hash = hashPassword(req.postParams.pwd);
// → "$pbkdf2-sha256$210000$<salt>$<hash>"
```

### `verifyPassword(plain, stored)`

Constant-time check of a plaintext password against a hash produced by `hashPassword()`. Also transparently verifies legacy Unix `crypt` and MD5-hex hashes, so existing credentials can be migrated on next login.

```javascript
if (verifyPassword(req.postParams.pwd, user.password_hash)) {
    session.login(user);
}
```

### `getDBConnection(name)`

Get a `DatabaseObject` for the named DbSource (from `db.properties`).

```javascript
var db = getDBConnection("main");
var rs = db.executeRetrieval("SELECT count(*) FROM posts", []);
```

### `getURL(url, condition, timeout)`

Fetch a URL via HTTP. Returns a `MimePart` with the body, content type, and headers.

```javascript
var page = getURL("https://example.com/api/data");
res.write(page.content);

// With conditional GET — pass a Date for If-Modified-Since
var cached = app.data.cached;
var page = getURL("https://example.com/data", cached.lastModified, 5000);
if (page) {
    // 200 OK — new content
    cached.body = page.content;
    cached.lastModified = page.lastModified;
}
// page is null on 304 Not Modified or error

// With ETag-based conditional GET — pass a string
var page = getURL("https://example.com/data", cached.etag, 5000);
```

Parameters:

- `url` — the URL to fetch
- `condition` — optional. Either a `Date` (for `If-Modified-Since`) or a string (for `If-None-Match`).
- `timeout` — optional. Milliseconds for connect and read timeouts.

`User-Agent` is set from `app.properties::httpUserAgent`.

### `getXmlDocument(src)`

Parse XML to a DOM document. `src` can be a URL string, a `java.io.InputStream`, a `java.io.Reader`, or a string of XML.

```javascript
var doc = getXmlDocument("https://example.com/feed.xml");
var entries = doc.getElementsByTagName("entry");
for (var i = 0; i < entries.length; i++) {
    res.write(entries.item(i).getTextContent());
}
```

### `getHtmlDocument(src)`

Parse HTML to a DOM document (uses TagSoup for tolerant parsing).

```javascript
var doc = getHtmlDocument("https://example.com");
var title = doc.getElementsByTagName("title").item(0).getTextContent();
```

### `format(obj)`

HTML-escape an object's string representation. Equivalent to `res.encode(obj)` but returns the string instead of writing it.

```javascript
var safe = format("<script>");   // → "&lt;script&gt;"
```

### `formatParagraphs(obj)`

HTML-escape and replace newlines with `<p>` paragraph separators.

```javascript
var html = formatParagraphs("Line 1\nLine 2\n\nLine 3");
// → "Line 1<br />Line 2<p>Line 3"
```

### `seal(obj)`

Make a JavaScript object immutable. Subsequent assignments fail in strict mode and silently in non-strict.

```javascript
var config = { host: "localhost", port: 8080 };
seal(config);
config.host = "evil";    // silently ignored or throws
```

### `serialize(obj, file)` and `deserialize(file)`

Write/read a JavaScript object to disk using Rhino's `ScriptableOutputStream`.

```javascript
serialize({ users: [...], settings: {...} }, "/tmp/state.ser");
var state = deserialize("/tmp/state.ser");
```

Useful for checkpointing computed state across restarts.

### `defineLibraryScope(name)` (deprecated)

Define an empty namespace in the global scope. Marked deprecated; use CommonJS modules instead.

### `wrapJavaMap(map)` / `unwrapJavaMap(wrapper)`

Wrap a `java.util.Map` so it behaves as a JS object (`map.foo`, `map.foo = "bar"`); unwrap returns it to a native Java object.

```javascript
var props = new java.util.Properties();
var jsProps = wrapJavaMap(props);
jsProps.foo = "bar";
print(jsProps.foo);          // "bar"
print(props.getProperty("foo"));  // "bar"
```

### `toJava(obj)`

Convert a JS value into its underlying Java wrapper, exposing the Java methods.

```javascript
var s = toJava("hello");
print(s.charAt(0));         // 'h' — calls java.lang.String.charAt
```

Useful when you want explicit Java semantics (locale-aware comparison, etc.).

### `definePrototype(name, descriptor)`

Define a HopObject prototype at runtime (rather than via a directory and `type.properties`).

```javascript
definePrototype("Tag", {
    _db: "main",
    _table: "tags",
    _id: "tag_id",
    name: "name_column"
});
```

Equivalent to creating `Tag/type.properties` with the same content. The prototype is registered immediately.

### `write(str)` and `writeln(str)`

`System.out.print(str)` and `println(str)` — prints to the **server's stdout**, not the response. Use these only for ad-hoc debugging that should appear in the server log, not user-visible output.

For user-visible output, use `res.write()` / `res.writeln()`.

### `dontEnum(...names)` (on `Object.prototype`)

Mark properties as non-enumerable so they don't appear in `for...in` loops.

```javascript
var obj = { a: 1, b: 2, c: 3 };
obj.dontEnum("b");
for (var k in obj) print(k);    // → "a", "c"
```

This is set on `Object.prototype` itself, so every object has it.

## CommonJS Loader

### `require(path)`

Load a module. See [CommonJS Modules](commonjs-require.md).

```javascript
app.addRepository("modules/helma/File.js");
app.addRepository("modules/helma/Mail.js");
```

### `module.exports`

The module's export object. See CommonJS Modules.

### `__dirname`, `__filename`

Inside a loaded module, the directory and full path of the module's file.

## Bundled Core Extensions

These come from `modules/core/*.js` and extend native prototypes.

### Globals from `modules/core/Global.js`

- `print(...args)` — print to stdout (Rhino-built-in, available everywhere)
- `defineGlobal(name, value)` — set a global variable, equivalent to `global[name] = value`

### Date functions

`modules/core/Date.js` adds methods to `Date.prototype`. See [modules/core/Date](../modules/core/date.md).

### Array functions

`modules/core/Array.js` adds methods like `Array.prototype.contains()` and aliases. See [modules/core/Array](../modules/core/array.md).

### String functions

`modules/core/String.js` adds methods like:

- `String.prototype.format(...)` — printf-style formatting
- `String.prototype.contains(substr)` — same as `indexOf >= 0`
- `String.prototype.stripTags()` — remove HTML tags
- `String.prototype.encode()` — HTML-escape
- `String.prototype.encodeForm()` — escape for form fields
- `String.prototype.encodeXml()` — XML-escape
- `String.prototype.encodeUrl()` — URL-encode

See [modules/core/String](../modules/core/string.md).

### Filters from `modules/core/Filters.js`

Global skin filters — used as `<% var | filtername %>`. Examples:

- `escape_filter` — HTML-escape
- `format_filter` — pretty-print
- `lowercase_filter` / `uppercase_filter`
- `nl2br_filter` — newlines → `<br>`
- `truncate_filter` — clip to length

See [modules/core/Filters](../modules/core/filters.md).

## See Also

- [Reference: Application Bean](../reference/app-bean.md) — `app.X` methods
- [Reference: Request Bean](../reference/req-bean.md) — `req.X` methods
- [Reference: Response Bean](../reference/res-bean.md) — `res.X` methods
- [Reference: Session Bean](../reference/session-bean.md) — `session.X` methods
- [modules/core](../modules/core/index.md) — full bundled API
