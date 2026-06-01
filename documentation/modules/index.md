# Modules

Helma ships with four bundles of JavaScript modules:

| Bundle | Purpose | Path |
|---|---|---|
| **`modules/core`** | Extensions to native JS prototypes — String, Array, Date, etc. | Per-bundle via `app.addRepository("modules/core/all.js")` |
| **`modules/helma`** | Helma's own toolkit — File, Mail, Http, Image, etc. | Per-file via `app.addRepository()` |
| **`modules/jala`** | Jala Project — extra utilities (forms, captcha, RSS, i18n) | Per-file via `app.addRepository()` |
| **`modules/tools`** | Built-in dev tools — auth, shell, sqlshell, inspector, markup | As repository: `apps.properties::<app>.repository.N = modules/tools` |

| Page | Subject |
|---|---|
| [Core](core/index.md) | Built-in prototype extensions |
| [Helma](helma/index.md) | The `helma.*` modules |
| [Jala](jala.md) | The Jala toolkit |
| [Tools](tools.md) | Bundled dev tools |

## How Loading Works

Helma's bundled JS modules are **not** CommonJS modules — they don't `module.exports` anything. Instead, each file installs its API onto a global namespace object like `helma.File`, `helma.Mail`, `jala.Form`, etc. To make a module's namespace visible in your app:

```javascript
// In Global/main.js or any code that runs at startup
app.addRepository("modules/helma/File.js");
app.addRepository("modules/helma/Mail.js");
app.addRepository("modules/jala/code/Form.js");
```

After `addRepository`, the module's globals are reachable everywhere in the app:

```javascript
// In any action, macro, or function
var f = new helma.File("/tmp/foo.txt");
var m = new helma.Mail();
var form = new jala.Form("signup", {});
```

To load multiple files at once, use the bundle's `all.js` aggregator file (provided for that purpose), or list the individual `.js` files:

```javascript
// Load everything in modules/helma via the all.js helper:
app.addRepository("modules/helma/all.js");

// Or per file:
app.addRepository("modules/helma/File.js");
app.addRepository("modules/helma/Mail.js");
```

A directory passed to `addRepository` is treated as a code repository for prototype directories — it does not load top-level `.js` files inside that directory. To load top-level scripts, point at the file (or use `all.js`).

## `modules/core` — Opt-in

The `core` modules extend native JS prototypes (`String`, `Array`, `Date`, `Number`, `Object`) and add some globals. They are bundled in the distribution but are **not** auto-loaded. Enable them with:

```javascript
// Global/main.js
app.addRepository("modules/core/all.js");
```

The aggregator `all.js` loads every file in `modules/core/`. Pointing `apps.properties::<app>.repository.N = modules/core` adds the directory as a `FileRepository` for prototype-directory lookup, but it does **not** execute the top-level `.js` files — use the `all.js` form for that.

```javascript
"hello world".format("%s, %s", "a", "b");      // String.prototype.format
[1,2,3].contains(2);                            // Array.prototype.contains
new Date().format("yyyy-MM-dd");                // Date.prototype.format
```

## CommonJS `require()` — For Your Own Modules

`require("./local-utils")` works for **your own modules** that follow the CommonJS pattern (set `module.exports`). The bundled `modules/helma/*` and `modules/jala/*` files don't follow that convention — load them via `addRepository`.

See [CommonJS Modules](../scripting/commonjs-require.md) for the `require()` algorithm and conventions for your own modules.
