# Global Object

The **global object** is the top-level JavaScript scope of an application. It contains:

- The free functions registered by `GlobalObject.java`
- The `app`, `Xml` and other framework-provided globals
- Compiled prototype constructors
- CommonJS modules (`require`-ed)
- Bundled extensions from `modules/core/*.js`

This page is the API reference for `GlobalObject.java`. For the per-thread additions (`req`, `res`, `session`, `path`), see their dedicated reference pages.

## Skin Functions

### `renderSkin(skinName, paramObj)`

Render a global skin (one not associated with a specific HopObject).

### `renderSkinAsString(skinName, paramObj)` (String)

Render a global skin and return the output.

### `createSkin(source)` (Skin)

Parse a string into a Skin object.

## Properties

### `getProperty(name, defaultValue)` (String)

Read a setting from `app.properties`.

## HTTP

### `getURL(url, condition, timeout)` (MimePart)

Fetch a URL. `condition` can be a `Date` (for `If-Modified-Since`) or a string (for `If-None-Match`).

Returns a `MimePart` with `.content`, `.contentType`, `.lastModified`, `.eTag`. Returns `null` on error or 304.

## Authentication

### `authenticate(user, pwd)` (boolean)

Check `(user, pwd)` against the server's or app's `passwd` file.

## DOM Parsing

### `getXmlDocument(src)` (Document)

Parse XML from a URL, string, InputStream, or Reader. Returns a `org.w3c.dom.Document`.

### `getHtmlDocument(src)` (Document)

Parse HTML using TagSoup. Returns a Document.

## Database

### `getDBConnection(name)` (DatabaseObject)

Get a `DatabaseObject` wrapping a `DbSource` connection.

## Formatting

### `format(obj)` (String)

HTML-escape the object's string representation.

### `formatParagraphs(obj)` (String)

HTML-escape + paragraph-conversion (newlines → `<br>`).

## Object Utilities

### `seal(obj1, obj2, ...)`

Make objects immutable. Subsequent property assignments fail in strict mode.

### `serialize(obj, file)`

Persist a JS object to a file using `ScriptableOutputStream`.

### `deserialize(file)` (Object)

Read a previously serialized object.

### `dontEnum(...names)` (on `Object.prototype`)

Mark properties as non-enumerable.

## Java Interop

### `wrapJavaMap(map)` (Object)

Wrap a `java.util.Map` so it behaves like a JS object.

### `unwrapJavaMap(wrapper)` (Object)

Convert a wrapped map back to a raw Java object.

### `toJava(obj)` (Object)

Wrap a JS value as the underlying Java wrapper for explicit interop.

## Output (Server Console)

### `write(str)` / `writeln(str)`

Write to `System.out`. For user-visible output use `res.write()` instead.

## Library Definition

### `defineLibraryScope(name)` (deprecated)

Define an empty namespace in the global scope. Replaced by CommonJS modules.

### `definePrototype(name, descriptor)` (HopObject)

Create or update a prototype at runtime. The descriptor is a JS object with the same keys as a `type.properties` file.

```javascript
definePrototype("Tag", {
    _db: "main",
    _table: "tags",
    _id: "tag_id",
    name: "name_column",
    posts: {
        collection: "Post",
        local: "tag_id",
        foreign: "tag_id"
    }
});
```

## Module System

### `require(path)` (Object)

Load a CommonJS module. See [CommonJS Modules](../scripting/commonjs-require.md).

### `module` (Object)

The current module — `{ id, exports, filename, parent }`.

### `exports` (Object)

Shortcut for `module.exports`.

### `__dirname` (String) / `__filename` (String)

The directory and full path of the current module.

## Always Present

These are always defined in the global scope by `RhinoCore` (independent of any opt-in modules):

| Symbol | Source |
|---|---|
| `app` | `ApplicationBean` |
| `Xml` | `XmlObject` for XML processing |
| `global` | The global object itself |
| `print`, `quit`, `version` | Rhino built-ins |
| `Packages`, `java`, `com`, `org`, `net`, `edu` | Java package roots |
| `importPackage`, `importClass` | Rhino package imports |
| All `Object`, `Array`, `String`, `Number`, `Date`, `RegExp`, `Math`, `JSON` | Standard JS |
| `Map`, `Set`, `WeakMap`, `WeakSet` | Standard JS |
| `Promise`, `Symbol`, `Iterator` | Standard JS |
| `parseInt`, `parseFloat`, `isNaN`, `isFinite` | Standard JS |
| `encodeURI`, `encodeURIComponent`, `decodeURI`, `decodeURIComponent` | Standard JS |

## Modules-Core Additions

The bundled `modules/core/*.js` files are **not** auto-loaded. After an app explicitly enables them via `app.addRepository("modules/core/all.js")`, they add:

- `String.prototype.encode`, `.encodeForm`, `.encodeXml`, `.encodeUrl`, `.stripTags`, `.contains`, `.format`
- `Array.prototype.contains`, `.intersection`, `.union`, `.first`, `.last`
- `Date.prototype.format` (with `SimpleDateFormat` pattern)
- `Number.prototype.format`
- `Object.clone`, `.dontEnum`
- Many skin filters

See [modules/core](../modules/core/index.md) for the full list.

## Per-Application Globals

`Global/*.js` files in your application directory contribute additional globals — functions and variables defined at the top level of `Global/main.js` are available everywhere.

```javascript
// Global/main.js
function fullURL(path) {
    return getProperty("baseUrl", "http://localhost:8080") + path;
}

// In any action or skin:
res.write(fullURL("/about"));
```

## See Also

- [Scripting: Rhino Engine](../scripting/rhino-engine.md)
- [Scripting: Global Functions](../scripting/global-functions.md)
- [Scripting: CommonJS Modules](../scripting/commonjs-require.md)
- [`GlobalObject.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/scripting/rhino/GlobalObject.java) — source
