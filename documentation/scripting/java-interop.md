# Java Interoperability

Because Rhino runs on the JVM, the entire Java platform is reachable from your JavaScript code. This page covers idiomatic patterns and the gotchas.

## Accessing Java Classes

```javascript
// All packages accessible under Packages.*
var ArrayList = Packages.java.util.ArrayList;

// java, com, org, net, edu are pre-aliased
var ArrayList = java.util.ArrayList;
var Builder   = com.example.MyBuilder;

// Construct
var list = new java.util.ArrayList();
list.add("a");
list.add("b");

// Static methods
var now = java.lang.System.currentTimeMillis();

// Static fields
var MAX = java.lang.Integer.MAX_VALUE;
```

## Bean-Style Access

For Java objects with getter/setter pairs:

```javascript
var file = new java.io.File("/tmp/foo");

// Bean property — calls getName()
file.name;

// Same as
file.getName();

// Setters too
file.lastModified = Date.now();
// → calls file.setLastModified(Date.now())
```

The rule: `obj.foo` first looks for `getFoo()`, then `isFoo()`, then falls back to a field access.

## Conversion Rules

Going JS → Java:

| JS value | Becomes Java |
|---|---|
| `null` | `null` |
| `undefined` | `null` (or omitted for varargs) |
| `true`/`false` | `boolean` or `java.lang.Boolean` |
| number | `int`, `long`, `double`, `float` — based on target signature |
| string | `String` |
| `Date` | `java.util.Date` |
| Array | `Object[]` |
| Object | `java.util.Map` (`NativeObject` wraps as map) |
| HopObject | `helma.objectmodel.INode` |

Going Java → JS:

| Java type | Becomes JS |
|---|---|
| `null` | `null` |
| primitives | JS number / boolean |
| `String` | JS string |
| `java.util.Date` | wrapped — `getTime()` works |
| `java.util.Map` | wrapped — `map.get(k)` works, NOT `map.k` |
| `java.util.List`, arrays | wrapped — `list.get(0)`, `arr[0]` work |
| Other objects | wrapped — methods callable |

## Common Patterns

### Creating a `java.util.HashMap`

```javascript
var m = new java.util.HashMap();
m.put("key", "value");
print(m.get("key"));         // "value"
```

To use it as a JS-friendly object, wrap with `wrapJavaMap`:

```javascript
var m = new java.util.HashMap();
var jm = wrapJavaMap(m);
jm.key = "value";
print(jm.key);               // "value"
```

### Working with `java.io.File`

```javascript
var f = new java.io.File("/var/log/myapp.log");
if (f.exists() && f.canRead()) {
    var reader = new java.io.BufferedReader(new java.io.FileReader(f));
    try {
        var line;
        while ((line = reader.readLine()) !== null) {
            res.write(line);
        }
    } finally {
        reader.close();
    }
}
```

Better: use [`helma.File`](../modules/helma/file.md):

```javascript
app.addRepository("modules/helma/File.js");
var f = new File("/var/log/myapp.log");
res.write(f.read());
```

### Multi-threading

```javascript
var lock = new java.util.concurrent.locks.ReentrantLock();
lock.lock();
try {
    // critical section
} finally {
    lock.unlock();
}
```

You can spawn threads:

```javascript
var thread = new java.lang.Thread(new java.lang.Runnable({
    run: function() {
        // runs in a separate thread, no req/res available
        someBackgroundWork();
    }
}));
thread.start();
```

But this is dangerous — the spawned thread has no transactor, no scripting context, and may break things. Prefer `app.invokeAsync()` for background work.

### Cryptography

```javascript
var md = java.security.MessageDigest.getInstance("SHA-256");
var digest = md.digest(new java.lang.String("hello").getBytes("UTF-8"));
var hex = "";
for (var i = 0; i < digest.length; i++) {
    hex += ((digest[i] + 256) & 0xFF).toString(16).padStart(2, "0");
}
print(hex);
```

### Regular expressions (when Java is faster)

JS's regex is fine, but Java's `java.util.regex` can be useful for complex patterns:

```javascript
var pattern = java.util.regex.Pattern.compile("^[a-z]+$", java.util.regex.Pattern.CASE_INSENSITIVE);
var matcher = pattern.matcher("Hello");
print(matcher.matches());     // true
```

## Implementing Java Interfaces

Wrap a JS object that conforms to the interface:

```javascript
var runnable = new java.lang.Runnable({
    run: function() {
        print("Running!");
    }
});

new java.lang.Thread(runnable).start();
```

This works for any interface with one or a few methods. For interfaces with many methods, use `java.lang.reflect.Proxy`:

```javascript
var Proxy = java.lang.reflect.Proxy;
var ClassLoader = java.lang.ClassLoader.getSystemClassLoader();
var interfaces = [java.util.Comparator];
var handler = {
    invoke: function(proxy, method, args) {
        if (method.getName() === "compare") {
            return args[0].length - args[1].length;
        }
    }
};
var comparator = Proxy.newProxyInstance(ClassLoader, interfaces, handler);
```

(Verbose. Prefer single-method interfaces where you can.)

## Extending Java Classes

```javascript
var MyMap = java.util.HashMap.extend({
    put: function(k, v) {
        print("Putting " + k + " = " + v);
        return java.util.HashMap.prototype.put.call(this, k, v);
    }
});

var m = new MyMap();
m.put("a", 1);    // logs and stores
```

The `.extend({...})` mechanism is Rhino-specific and not always reliable. Prefer composition over inheritance when possible.

## Calling Methods on `null`

```javascript
var f = null;
f.length;        // throws "Cannot call property length of null"
```

Same as JS. Be aware: when a Java method returns `null`, accessing a property on it throws.

## Type Coercion Pitfalls

### Numbers

```javascript
var i = 1.5;
java.util.Arrays.asList(i);    // converts to Double, not Integer
```

Sometimes you need to force a specific type:

```javascript
var i = parseInt(1.5);                                    // → 1 (JS Number)
var ji = new java.lang.Integer(1);                        // → java.lang.Integer
var jl = java.lang.Long.valueOf(java.lang.String("100")); // → java.lang.Long
```

### Booleans

```javascript
"true" == true;          // false in JS
java.lang.Boolean.TRUE;   // java.lang.Boolean.TRUE — not the JS true!
```

Always compare with `===`:

```javascript
if (result === true) { ... }
```

### Strings

JS strings are converted to `java.lang.String` automatically. But if you call `String.charAt()` you get a JS char (a 1-character string); on `java.lang.String.charAt()` you get a Java `char` (a 16-bit integer).

```javascript
var js = "hello";
var jv = new java.lang.String("hello");

js.charAt(0);            // "h" — a string
jv.charAt(0);            // 104 — a char (an int)
```

Use `toJava("hello").charAt(0)` to force the Java semantics.

## Method Overload Resolution

When Java has overloaded methods, Rhino picks based on argument types:

```javascript
var sb = new java.lang.StringBuilder();
sb.append("string");     // calls append(String)
sb.append(42);           // calls append(int)
sb.append(true);         // calls append(boolean)
```

Rhino's overload resolution sometimes picks wrong — usually for numeric ambiguity (int vs long vs Integer vs Long). Force explicit types when needed:

```javascript
sb.append(new java.lang.Long(42));  // unambiguous
```

## Exceptions

Java exceptions propagate as JS exceptions:

```javascript
try {
    var conn = java.sql.DriverManager.getConnection("invalid-url");
} catch (e) {
    print(e.getClass().getName());      // class name
    print(e.message);                    // JS error message
    print(e.javaException);              // the underlying Java Throwable
    print(e.javaException.getMessage()); // Java exception's message
}
```

`e.javaException` is set when the thrown exception originated in Java code.

## Reflection

```javascript
var clazz = java.lang.Class.forName("java.lang.String");
var methods = clazz.getMethods();
for (var i = 0; i < methods.length; i++) {
    res.write(methods[i].getName() + "\n");
}
```

## Garbage Collection

The Java GC handles both Java and Rhino objects together. You generally don't worry about it, but:

- A long-running cron job that creates many objects can trigger heavy GC. Use `-Xmx` to give enough heap.
- Strong references from JS hold onto Java objects. Set to `null` when done with large objects in long-running scripts.
- The Rhino `Context` doesn't have a separate per-context heap — everything is on the JVM heap.

## Performance

Java method calls from JS go through Rhino's reflective dispatch — they're slower than pure-JS method calls but still fast enough for most workloads. Performance-critical code should:

- Avoid the JS-Java boundary in tight loops
- Prefer pure-JS or pure-Java implementations
- Use `toJava()` to force native operations on strings/numbers

## See Also

- [Reference: Global Object](../reference/global-object.md)
- [Rhino's JavaAdapter docs](https://github.com/mozilla/rhino/blob/master/docs/scripting.md#javaadapter)
- [Concepts: Scripting Environment](../concepts/scripting-environment.md)
