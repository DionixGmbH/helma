# Object

`modules/core/Object.js` adds methods to `Object.prototype` for cloning, reduction, and dumping.

## Instance Methods

### `clone(target, recursive)` → Object

Copy properties of `this` to `target` (or a new object). If `recursive` is true, nested objects are cloned too (deep copy).

```javascript
var orig = { a: 1, b: { c: 2 } };
var shallow = orig.clone();               // { a: 1, b: <shared reference> }
var deep = orig.clone(null, true);        // { a: 1, b: { c: 2 } } — new b
```

### `reduce(recursive)` → Object

Reduce an object to its plain-data representation. Skips functions, prototype chain. If `recursive`, processes nested objects.

```javascript
var obj = {
    name: "Alice",
    say: function() { return "hi"; },
    nested: { foo: 1 }
};
obj.reduce(true);     // { name: "Alice", nested: { foo: 1 } } — no functions
```

Useful before `JSON.stringify` on objects that may contain functions.

### `dump(recursive)` → String

Pretty-print an object as a debug string. With `recursive`, nests deeper.

```javascript
print({ a: 1, b: [2, 3] }.dump());
```

## Notes

These methods are added via `Object.prototype.X = ...`. Helma uses the `dontEnum()` helper to mark them as non-enumerable so they don't appear in `for (var k in obj)`.

## See Also

- [`modules/core/Object.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/core/Object.js)
