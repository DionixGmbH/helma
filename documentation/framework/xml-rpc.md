# XML-RPC

Helma includes a full XML-RPC client and server. You can expose JavaScript functions as XML-RPC methods, and you can call out to remote XML-RPC services.

## Exposing JavaScript Functions

### Step 1 — Whitelist allowed functions

By default, **no** function is callable via XML-RPC. List allowed `prototype.function` pairs in `app.properties`:

```properties
# app.properties
xmlrpcAccess = Root.echo, Root.calculate.add, User.getProfile
```

The format is `<Prototype>.<function>`. Use `*` to allow all functions of a prototype:

```properties
xmlrpcAccess = Root.*
```

### Step 2 — Define the function

```javascript
// Root/main.js
function echo(s) {
    return "Hello, " + s;
}

function calculate(a, b) {
    return a + b;
}
```

### Step 3 — Configure the XML-RPC port

```properties
# server.properties
xmlrpcPort = 8081
```

Or pass `-x 8081` on the command line. With no port set, Helma still accepts XML-RPC requests over the regular HTTP port (POST with `Content-Type: text/xml`).

### Step 4 — Configure the handler name

```properties
# app.properties
xmlrpcHandlerName = myapp        # defaults to the application name
```

XML-RPC clients address methods as `<handler>.<function>` — so `myapp.echo` or `myapp.calculate.add`.

### Step 5 — Test from the client

```python
import xmlrpc.client
c = xmlrpc.client.ServerProxy("http://localhost:8081/")
print(c.myapp.echo("World"))    # → "Hello, World"
print(c.myapp.calculate.add(1, 2))  # → 3
```

## Calling XML-RPC Services

### From JavaScript

```javascript
// Get a client for a remote endpoint
var rpc = new Packages.helma.scripting.rhino.extensions.XmlRpcObject(
    "http://example.com/RPC2",
    "examples"
);

// Call a remote method
var result = rpc.getStateName(41);    // returns "South Dakota"
```

`XmlRpcObject` (`src/main/java/helma/scripting/rhino/extensions/XmlRpcObject.java`) wraps Apache XML-RPC's `XmlRpcClient`. Constructor:

- `new XmlRpcObject(url)` — defaults handler name to empty
- `new XmlRpcObject(url, handler)` — explicit handler

Any method invocation on the object is forwarded as an XML-RPC call. Return values are converted to JavaScript:

- `String` → JS string
- `int` / `double` → JS number
- `boolean` → JS boolean
- `Date` → JS Date
- `Hashtable` → JS object
- `Vector` → JS array
- `byte[]` (base64) → JS string

## XML-RPC-Specific Action Suffix

Define an `_xmlrpc` variant of an action:

```javascript
function echo_action_xmlrpc(name) {
    return "Hello, " + name;
}
```

This is invoked when the URL is hit with `Content-Type: text/xml` (an XML-RPC request). For the rest of the dispatch logic see [AJAX Action Resolution](ajax-actions.md) — `_xmlrpc` has the highest priority.

## Type Conversion

When a JavaScript function is called via XML-RPC:

- Return value is converted to XML-RPC types
- JS object → struct
- JS array → array
- JS Date → dateTime.iso8601
- JS number → int (if integral) or double
- JS string → string
- JS boolean → boolean

Arguments arrive converted the other way around. For maximum interoperability, restrict your function signatures to these types.

## Authentication

XML-RPC requests support HTTP Basic auth via the `Authorization` header. From server side:

```javascript
function privateMethod_xmlrpc(arg) {
    if (!session.user || !session.user.isAdmin()) {
        throw new Error("Unauthorized");
    }
    return doWork(arg);
}
```

Throwing an exception bubbles up as an XML-RPC fault to the client.

From client side (JavaScript):

```javascript
var rpc = new Packages.helma.scripting.rhino.extensions.XmlRpcObject("http://example.com/RPC2");
rpc.setBasicAuthentication("user", "pwd");      // sets Authorization header
```

## Error Handling

XML-RPC errors become "fault responses" with a fault code and string. Helma wraps your JS exceptions into faults:

- Code 0, string = exception message

On the client side:

```javascript
try {
    rpc.someMethod();
} catch (e) {
    // e is wrapped XmlRpcException
    print(e.getFaultString());
    print(e.getFaultCode());
}
```

## Performance

The XML-RPC handler runs through the same RequestEvaluator pool as HTTP requests. Each XML-RPC call is a transaction. Throughput is similar to HTTP.

`app.xmlrpcCount` tracks the cumulative count of XML-RPC requests served.

## Choosing XML-RPC vs JSON

XML-RPC is the older protocol bundled with Helma. For new APIs you'll usually want JSON over HTTP:

```javascript
// JSON endpoint (no XML-RPC infrastructure needed)
function api_action_post() {
    var body = JSON.parse(req.data.body);
    var result = doSomething(body);
    res.contentType = "application/json";
    res.write(JSON.stringify(result));
}
```

XML-RPC is included for compatibility with older Helma applications and integrations.

## Disabling XML-RPC

```properties
# server.properties
xmlrpcPort = -1       # don't open a dedicated XML-RPC port

# app.properties — leave xmlrpcAccess blank (default)
# Without listed methods, no XML-RPC method is exposed
```

If both the dedicated XML-RPC port is disabled AND `xmlrpcAccess` is empty, no XML-RPC requests will be served. POSTs with `Content-Type: text/xml` to the HTTP port still arrive but fail dispatch.

## See Also

- [XmlRpc extension](../extensions/xmlrpc.md) — the Java-side wrapper API
- [`xmlrpc.properties`](../reference/app-properties.md#xml-rpc) — full settings list
- Apache XML-RPC: <https://ws.apache.org/xmlrpc/>
