# helma.Aspects

A simple aspect-oriented programming utility for wrapping function calls with before/after/around advice.

```javascript
app.addRepository("modules/helma/Aspects.js");
```

## Constructor

```javascript
var aspects = new helma.Aspects();
```

A pre-instantiated singleton is available as `helma.aspects`.

## Methods

### `addBefore(obj, fname, before)`

Run `before(args)` before each call to `obj[fname]`.

### `addAfter(obj, fname, after)`

Run `after(returnValue)` after each call.

### `addAround(obj, fname, around)`

Wrap each call. `around(proceed, args)` must call `proceed(args)` to invoke the original.

## Example

```javascript
app.addRepository("modules/helma/Aspects.js");
var aspects = helma.aspects;

aspects.addBefore(somePrototype.prototype, "save", function(args) {
    app.log("About to save: " + this);
});

aspects.addAfter(somePrototype.prototype, "save", function(retval) {
    app.log("Saved: " + this);
    return retval;
});

aspects.addAround(somePrototype.prototype, "fetch", function(proceed, args) {
    var start = Date.now();
    var result = proceed(args);
    app.log("fetch took " + (Date.now() - start) + "ms");
    return result;
});
```

## See Also

- [`modules/helma/Aspects.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Aspects.js)
