# helma.Skin

Skin wrapper with rendering helpers.

```javascript
app.addRepository("modules/helma/Skin.js");
```

## Constructor

```javascript
var skin = new helma.Skin(source, encFlag);
```

Parameters:

- `source` — skin source text
- `encFlag` — optional encoding flag (boolean)

## Methods

### `render(param)`

Render the skin to the current response, with parameters.

```javascript
skin.render({ title: "Hello" });
```

### `renderAsString(param)` → String

Render and return as string.

### `containsMacro(name, handler)` → boolean

Test whether the skin contains a specific macro.

```javascript
if (skin.containsMacro("title", "this")) {
    // this skin uses <% this.title %>
}
```

### `toString()` / `valueOf()`

The skin's source text.

## Usage

```javascript
app.addRepository("modules/helma/Skin.js");

var src = "<h1><% this.name %></h1>";
var skin = new Skin(src);

skin.render(user);
// → <h1>Alice</h1>

var html = skin.renderAsString(user);
```

## Compared to Built-in Skins

This wrapper is useful when:

- Loading skin sources from a non-file location (e.g. a DB)
- Building skins dynamically
- Parsing user-provided templates

For static skins stored as `.skin` files, use the built-in [`renderSkin()`](../../scripting/global-functions.md) or `HopObject.renderSkin()` directly.

## See Also

- [Framework: Skins & Macros](../../framework/skins.md)
- [Framework: Macro Syntax](../../framework/macro-syntax.md)
- [`createSkin()`](../../scripting/global-functions.md) — equivalent global function
- [`modules/helma/Skin.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Skin.js)
