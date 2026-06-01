# XmlRpc Extension

`Remote` is the global JS constructor for making XML-RPC calls to remote servers.

Implementation: `src/main/java/helma/scripting/rhino/extensions/XmlRpcObject.java` (using Apache XML-RPC).

For exposing Helma functions as XML-RPC servers, see [XML-RPC](../framework/xml-rpc.md).

## Constructor

```javascript
var rpc = new Remote("http://example.com/RPC2");
var rpc = new Remote("http://example.com/RPC2", "myapp");   // with handler name
```

## Method Calls

`Remote` instances are **callable** and support **property chaining**. Both work:

```javascript
// Property chaining: builds the method name
rpc.echo("hello");
// → calls method "echo" on the remote server

// With handler name in constructor:
var rpc = new Remote("http://example.com/RPC2", "myapp");
rpc.posts.list();
// → calls "myapp.posts.list" on the server

// Chaining without handler:
var rpc = new Remote("http://example.com/RPC2");
rpc.examples.getStateName(41);
// → calls "examples.getStateName" on the server
```

The chained property access (`rpc.examples.getStateName`) doesn't immediately call — it builds up the method name as `examples.getStateName`. Only when you invoke as a function does the XML-RPC call execute.

## Return Value

A successful call returns the unwrapped result. On error, an exception is thrown:

```javascript
try {
    var name = rpc.examples.getStateName(41);
    res.write(name);     // "South Dakota"
} catch (e) {
    app.logError("RPC failed", e);
}
```

## Type Conversion

| JS type | XML-RPC type |
|---|---|
| String | `string` |
| Number (integer) | `int` |
| Number (float) | `double` |
| Boolean | `boolean` |
| Date | `dateTime.iso8601` |
| Array | `array` |
| Object | `struct` |
| `byte[]` | `base64` |
| `null` | not supported — use empty string or false |

Inverse conversion happens for return values.

## Authentication

For HTTP Basic auth:

```javascript
var rpc = new Remote("http://example.com/RPC2");
rpc.setBasicAuthentication("user", "password");
var result = rpc.someMethod();
```

(Set via Java method; not exposed as `jsFunction_` but accessible via JS-Java interop.)

## Example: Pinging weblogs.com

```javascript
function notifyWeblog() {
    var rpc = new Remote("http://rpc.weblogs.com/RPC2");
    var result = rpc.weblogUpdates.ping(
        "My Blog",
        "https://www.example.com/blog"
    );
    app.log("Ping result: " + result);
}
```

## Example: Calling Another Helma App

```javascript
function fetchRemotePosts() {
    var rpc = new Remote("http://otherserver:8081/", "otherapp");
    var posts = rpc.api.recentPosts(10);     // returns an array of structs
    for each (var p in posts) {
        res.write("<li>" + p.title + "</li>");
    }
}
```

## Limitations

- XML-RPC is an old protocol. For new integrations, prefer JSON-over-HTTP via [`helma.Http`](../modules/helma/http.md).
- No streaming — full request and response are buffered.
- No async — call blocks until response or timeout.
- `null` is not representable.

## Registration

`Remote` is registered as a lazy global by `RhinoCore.java`.

## See Also

- [Framework: XML-RPC](../framework/xml-rpc.md) — exposing Helma functions as XML-RPC server
- [Apache XML-RPC docs](https://ws.apache.org/xmlrpc/)
- [`XmlRpcObject.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/scripting/rhino/extensions/XmlRpcObject.java)
