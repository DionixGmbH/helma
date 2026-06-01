# Rhino Engine

Helma uses Mozilla Rhino version 1.9.0 (declared in `build.gradle`). The integration lives under `helma.scripting.rhino`.

## Anatomy

```
RequestEvaluator (1 per thread)
  └─ RhinoEngine (1 per RequestEvaluator)
       └─ RhinoCore (1 per Application — shared across evaluators)
            └─ shared Rhino global scope (compiled prototypes, modules)
       └─ per-thread GlobalObject (per-request globals)
            └─ inherits from shared scope
```

- **`RhinoCore`** owns the application-wide state: the compiled prototype constructors, the loaded CommonJS modules, the prototype-to-TypeInfo map. One per Application.
- **`RhinoEngine`** is per-RequestEvaluator. It enters a fresh Rhino `Context` on `enterContext()` and creates a per-thread `GlobalObject` that inherits from the shared scope.
- **`GlobalObject`** holds the per-request globals (`req`, `res`, `session`, `path`, `root`).

## Scope Resolution

When you write `foo` in an action:

1. Rhino looks in the **per-thread global**. `req`, `res`, etc. found here.
2. Rhino walks the prototype chain to the **shared global**. `app`, `Math`, `String`, your loaded CommonJS modules found here.
3. If still not found, returns `undefined`.

This is why setting a global from an action (`window.foo = 1` style) is risky — it writes into the *shared* scope and other requests see it. Prefer `var` inside functions.

## Hot Reload

`RhinoEngine.enterContext()` calls `RhinoCore.updatePrototypes()`. This iterates the `TypeManager`'s prototype list and asks each `Prototype` if its files have changed. If yes:

1. The prototype's `.js` files are re-read.
2. Compiled into a fresh JS `Script` via `Context.compileString()`.
3. The resulting functions are bound to the prototype's `Object` representation in the shared scope.
4. Existing HopObject instances continue to point at the new prototype.

This is what gives Helma its instant feedback loop — you save a `.js`, reload the page, change is live.

The reload is per-prototype — only changed prototypes are recompiled. Unchanged ones keep their compiled code.

## Compilation Modes

By default, Helma uses Rhino's **interpreted mode** to support hot reload at scope level. To use compiled mode (faster but harder to reload):

```properties
# app.properties
optLevel = 9         # -1 (interpreted) through 9 (max compile)
```

Lower values trade startup time for runtime speed. `-1` (interpreted) is the default and is what you usually want during development. Production deployments can experiment with higher levels.

## Language Features

Rhino 1.9.0 supports a substantial subset of modern JavaScript:

| Feature | Supported |
|---|---|
| ES5 (strict mode, JSON, Array methods) | Yes |
| `let`/`const` | Yes |
| Arrow functions | Yes |
| Template literals | Yes |
| Destructuring | Yes |
| Default and rest parameters | Yes |
| Classes | Yes |
| Iterators / `for...of` | Yes |
| Generators | Yes |
| `Promise` | Partial (no microtask queue beyond Rhino's) |
| `async`/`await` | Partial (no event loop) |
| Modules (ES6) | No — use CommonJS `require` |
| `for each` (legacy non-standard) | Yes — useful for HopObject iteration |
| E4X (XML literals) | Yes (legacy) |
| `Map`, `Set` | Yes |

The `for each` syntax (`for each (var x in arr) { ... }`) is non-standard JS but supported by Rhino, and idiomatic in Helma applications. It iterates *values*, not keys.

## Strict Mode

`"use strict"` works inside functions. There's no app-wide strict mode — opt in per file or per function.

```javascript
function strictAction() {
    "use strict";
    // ...
}
```

## ECMAScript Globals

All standard JS globals are available:

- `Object`, `Array`, `String`, `Number`, `Boolean`, `Date`, `RegExp`, `Math`, `JSON`
- `parseInt`, `parseFloat`, `isNaN`, `isFinite`
- `encodeURI`, `encodeURIComponent`, `decodeURI`, `decodeURIComponent`
- `Map`, `Set`, `WeakMap`, `WeakSet`
- `Promise`, `Symbol`, `Iterator`, `Generator`

Plus the Rhino-specific:

- `Packages` — top-level Java package namespace (`Packages.java.util.Date`)
- `java` — alias for `Packages.java`
- `com`, `org`, `net`, `edu` — pre-populated package roots
- `importPackage(java.util)` — bulk-imports a package into the scope (avoid in shared modules)
- `importClass(java.util.ArrayList)` — single-class import
- `print(...)` — shortcut for `System.out.println`
- `quit()` — Rhino's exit (don't use in Helma)
- `load(file)` — load and evaluate a JS file
- `runCommand(cmd, ...)` — run an external command
- `version(N)` — set Rhino language version (rarely needed)

The Helma-specific globals (`req`, `res`, `session`, `app`, `path`, `root`) overlay these.

## Native Modules

The `modules/core/*.js` bundle is included in the distribution but is **not** auto-loaded. Apps that want these extensions must add the repository via `app.addRepository("modules/core/all.js")` in `Global/main.js`, or via an entry in `apps.properties`:

- `modules/core/Array.js` — extends `Array.prototype` (chunk, contains, ...)
- `modules/core/Date.js` — extends `Date.prototype` (format, addMonths, ...)
- `modules/core/String.js` — extends `String.prototype` (encode, contains, stripTags, ...)
- `modules/core/Number.js` — extends `Number.prototype`
- `modules/core/Object.js` — adds `Object.dontEnum`, `Object.clone`, etc.
- `modules/core/JSON.js` — JSON parse/stringify (mostly redundant with native)
- `modules/core/Filters.js` — global skin filter functions
- `modules/core/Global.js` — extra global helpers
- `modules/core/HopObject.js` — extends `HopObject.prototype`

These extend the standard JS prototypes. See [modules/core](../modules/core/index.md) for the full API.

## Java Class Loader

The classes you can construct via `new java.X` or `Packages.X` are loaded by the app's `AppClassLoader`:

1. Bootstrap classes (JDK) — always available
2. `lib/` — server-wide Helma JARs
3. `lib/ext/` — server-wide third-party JARs
4. `apps/<appname>/lib/` — per-app JARs
5. Any extra path declared via `extensions =` in `server.properties`

The `AppClassLoader` is rooted in `RhinoCore`. Adding a new JAR to `apps/<appname>/lib/` requires restarting the app (or `app.clearCache()` if just adding a new prototype's classes).

## Per-Request Cleanup

After `RhinoEngine.exitContext()`:

- The per-thread `GlobalObject` is dropped.
- The Rhino `Context` is exited (returned to the per-thread context cache).
- The thread reference in the engine is cleared.

The shared scope persists across requests — that's what makes hot reload meaningful.

## See Also

- [Concepts: Scripting Environment](../concepts/scripting-environment.md) — high-level model
- [Global Functions](global-functions.md) — what's exposed in the global scope
- [Java Interoperability](java-interop.md) — calling Java from JavaScript
- [Rhino Documentation](https://github.com/mozilla/rhino/blob/master/docs/README.md) — upstream Rhino docs
