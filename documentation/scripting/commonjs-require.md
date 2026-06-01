# CommonJS Modules

Helma supports the [CommonJS](http://wiki.commonjs.org/wiki/Modules/1.1.1) module pattern via `require()` for your own modules. (The bundled `modules/helma/*` and `modules/jala/*` files use a different convention — they install themselves on a global namespace and are loaded via `app.addRepository(...)`. See [Modules overview](../modules/index.md).)

## Loading a Module

```javascript
app.addRepository("modules/helma/File.js");
app.addRepository("modules/helma/Mail.js");
var something = require("./local-module");
```

`require()`:

1. Resolves the module path (see resolution rules below)
2. Loads the file once and caches the loaded `exports`
3. Returns the `module.exports` of that module

## Defining a Module

`mylib.js`:

```javascript
// Module-level code runs once
var counter = 0;

// Set on module.exports
module.exports = {
    increment: function() {
        return ++counter;
    },
    getValue: function() {
        return counter;
    }
};
```

Or use direct exports:

```javascript
// Same as above
exports.increment = function() { ... };
exports.getValue = function() { ... };
```

## Module Path Resolution

Helma's CommonJS `require()` is initialised in `RhinoCore` with two roots:

1. The application directory (`app.getAppDir()`)
2. Optionally a directory configured via `app.properties::commonjs.dir`

For relative paths (`require("./foo")` or `require("../bar")`), resolution is relative to the current module's directory. For bare paths (`require("foo")`), Helma searches the two roots above.

The Helma-specific `NodeModulesProvider` (Node-style fallback) tries:

- The exact file at the resolved URI
- `<name>.js` and `<name>.json` (JSON files are loaded as parsed JSON)
- `<name>/index.js`
- `<name>/package.json` — when present, its `main` field is honoured to point at the entry file (this is the standard Node behaviour)

`package.json` is honoured by Helma when present in a module directory. Bundled `modules/helma/*`, `modules/jala/*` etc are **not** in the require-search path — they don't use the CommonJS convention. Use `app.addRepository()` for those (see [Modules](../modules/index.md)).

## The Module Object

Inside a module, three globals are special:

- **`module`** — `{ id, exports, filename, parent }`
- **`exports`** — initially `module.exports` (an empty object)
- **`require`** — the loader function

Plus two helpers Helma sets on the module scope:

- **`__dirname`** — directory containing this module's file
- **`__filename`** — full path of this module's file

```javascript
console.log(__filename);     // "/path/to/myapp/mylib.js"
console.log(__dirname);      // "/path/to/myapp"
console.log(module.id);      // "mylib"
console.log(module.parent);  // the module that required this one (if any)
```

## Caching

A module is loaded **once per application**. On `require("foo")`, Helma checks an internal cache; if present, returns the cached `exports`. If not, loads the file, runs its module-level code, and caches.

This means **module-level state persists across requests**:

```javascript
// counter.js
var count = 0;
exports.inc = function() { return ++count; };
exports.get = function() { return count; };

// In an action:
var c = require("./counter");
c.inc();    // 1
c.inc();    // 2 — across requests
```

To force a reload, clear the cache via `app.clearCache()` (or restart the app). There's no per-module cache invalidation.

## Compiled vs Interpreted

Modules are compiled via Rhino's `Context.compileString()`. The compiled function is stored in the cache; module-level code only runs once.

## Hot Reload

CommonJS modules **are not hot-reloaded** like prototype directories. Edit a module, you must restart the app (or call `app.clearCache()`) for changes to be visible.

This is a deliberate trade-off — module loading is once-per-app for performance.

## Cyclic Imports

CommonJS handles cycles by giving the cycle-completing import a *partial* `exports`:

```javascript
// a.js
exports.greet = function() {
    var b = require("./b");
    return "hello " + b.name();
};

// b.js
exports.name = function() {
    var a = require("./a");
    return "world (cyclic, a.greet = " + (a.greet || "undefined") + ")";
};
```

If `b.js` is loaded as part of `a.js`'s initialisation, `b.js` sees `a.exports = {}` (empty so far). After `a.js` finishes initialising, `a.exports.greet` is defined and subsequent calls see it.

Avoid cycles when possible; they're a common source of subtle bugs.

## JSON Modules

Helma can `require` a `.json` file:

```javascript
var config = require("./config.json");
// loads and JSON.parses
```

The module is registered with `JSONModuleSource`. Caching works the same way.

## Loading from a Second CommonJS Root

The default CommonJS root is `app.getAppDir()`. To add one more search root, set `commonjs.dir` in `app.properties`:

```properties
# app.properties
commonjs.dir = modules/commonjs
```

This is the only additional path Helma supports for `require()` — there is no `nodepath` setting and registered repositories are **not** added to the require-search path.

The `NodeModulesProvider` (`src/main/java/helma/scripting/rhino/NodeModulesProvider.java`) honours the Node convention `package.json::main` to choose the entry script of a module directory.

## ZIP Repositories and `require()`

A `.zip` registered via `app.addRepository(...)` becomes a code repository for the prototype-resolution system. It is **not** added as a CommonJS search root. Code inside the zip that you want to load via `require()` must live under the app's CommonJS root (`app.getAppDir()`) or under `commonjs.dir`. To distribute a CommonJS module bundle, unpack it into one of those directories or symlink it from there.

## Exposing Your Modules Globally

`require()` returns a local reference. To make a module's exports globally available:

```javascript
// Global/main.js
global.MyLib = require("./MyLib");
```

Now `MyLib` is reachable from any action without `require`-ing again. Use sparingly — pollutes the shared scope.

## Common Use Cases

### Application-private utilities

```javascript
// utils.js (in app root)
exports.formatPrice = function(cents) {
    return "$" + (cents / 100).toFixed(2);
};
exports.formatDate = function(d) {
    return d.toISOString().slice(0, 10);
};

// In Post/main.js
var utils = require("./utils");
function main_action() {
    res.write(utils.formatPrice(this.price));
}
```

### Helma's built-in modules

```javascript
app.addRepository("modules/helma/Mail.js");
app.addRepository("modules/helma/File.js");
app.addRepository("modules/helma/Http.js");
```

See [modules/helma](../modules/helma/index.md).

### Third-party libraries

Drop the library into your app directory or the directory pointed to by `commonjs.dir`, then:

```javascript
var lib = require("somelib");
```

If the library is a directory containing `package.json`, `NodeModulesProvider` will read its `main` field to find the entry file.

### Tests

```javascript
// tests/test-utils.js
var assert = require("./assert");
var utils = require("../utils");

assert.equal(utils.formatPrice(123), "$1.23");
```

(No bundled test runner — write a simple one or use jala.Test.)

## Comparison to Node.js

| Feature | Node.js | Helma |
|---|---|---|
| `require("x")` from `node_modules` | Yes (auto-walks ancestors) | Searches `app.getAppDir()` and `commonjs.dir` only |
| `package.json` `main` field | Yes | Yes (via `NodeModulesProvider`) |
| `package.json` `exports` field | Yes | No |
| `import` (ES6 modules) | Yes | No — use require |
| Native modules | Yes (.node) | No |
| Hot reload | No (manual) | No (manual) |
| Cached `require` | Yes | Yes |
| Cyclic imports | Partial exports | Partial exports |

Helma's CommonJS is closer to the original spec than Node's, but lacks Node's ecosystem.

## See Also

- [Reference: `require()`](../reference/global-object.md)
- [modules/](../modules/index.md) — the bundled module library
- [CommonJS spec](http://wiki.commonjs.org/wiki/Modules/1.1.1)
