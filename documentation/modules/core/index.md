# Core Modules

The `modules/core/` directory contains JavaScript files that extend Helma's built-in JavaScript globals — adding methods to `String.prototype`, `Array.prototype`, `Date.prototype`, etc.

These modules are bundled in the Helma distribution but are **not** auto-loaded. Each app that wants them must add the aggregator file:

```javascript
// Global/main.js
app.addRepository("modules/core/all.js");
```

`all.js` is a single-file aggregator that loads every other file in `modules/core/`. After this call the methods are available everywhere in the app without `require`.

!!! note
    Adding the bare directory (`modules/core`) as a repository in `apps.properties` is **not** equivalent — directory repositories are only scanned for prototype subdirectories, not executed as top-level scripts. Always use `modules/core/all.js` (a file path) for the core bundle.

## Bundled Modules

| Module | Extends | Purpose |
|---|---|---|
| [Array](array.md) | `Array.prototype` and globals | Array utilities |
| [Date](date.md) | `Date.prototype` | Formatting, arithmetic |
| [Filters](filters.md) | global skin filters | Common macro filters |
| [Global](global.md) | global scope | Helper functions |
| [HopObject](hopobject.md) | `HopObject.prototype` | HopObject utilities |
| [JSON](json.md) | global | Legacy JSON helpers (mostly redundant with native) |
| [Number](number.md) | `Number.prototype` | Numeric formatting, parsing |
| [Object](object.md) | `Object.prototype`, global | Object helpers, cloning |
| [String](string.md) | `String.prototype` | Encoding, formatting, sanitisation |

## Loading Order

The `modules/core/all.js` aggregator loads each individual file in the order listed in `all.js` itself (open the file to confirm the sequence). The aggregator is what executes the bundle's scripts; the order is not determined by repository enumeration.

Method conflicts: when two modules define the same method on a prototype, the later-loaded one wins. This is rare; the bundled core modules are designed to avoid clashes.

## Customisation

You can override or extend any of these methods from your application's `Global/*.js`:

```javascript
// Global/main.js
String.prototype.format = function(...args) {
    // your own implementation
};
```

Your version is loaded after the core modules so it wins. But it affects **all** code in the app — be careful.

## Opting Out

Core modules are **opt-in per app**, not auto-loaded. Simply do not call `app.addRepository("modules/core/all.js")` and the core extensions will be absent from your app's scripting environment. If parts of your code (or a library you `require`) rely on `String.prototype.format` and friends, they will fail.
