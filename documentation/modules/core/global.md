# Global

`modules/core/Global.js` adds global functions and skin macros.

## Macros (`*_macro`)

These are skin macros usable as `<% name %>` in any skin.

### `<% property name="key" default="..." %>`

Render an `app.properties` value.

```html
<title><% property name="siteTitle" default="My Site" %></title>
```

### `<% write text="Hello" %>`

Literally write the text. Useful in macro chains.

### `<% now %>`

Write the current Date object.

### `<% skin name="header" %>`

Render a global skin by name.

## Encoding Functions

### `encode(text, encodeNewLine)` → String

HTML-encode text. When `encodeNewLine` is true, also replaces newlines with `<br>`.

```javascript
encode("<script>");          // "&lt;script&gt;"
encode("a\nb", true);        // "a<br>b"
```

### `encodeXml(text)` → String

XML-encode text (escapes `< > & ' "`).

### `encodeForm(text)` → String

Encode for use as a form field value (preserves whitespace, escapes quotes and angle brackets).

### `stripTags(markup)` → String

Remove HTML/XML tags from text.

```javascript
stripTags("<p>Hello <b>world</b></p>");   // "Hello world"
```

## See Also

- [Reference: Global Object](../../reference/global-object.md) — all global functions from Java/Rhino
- [`modules/core/Global.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/core/Global.js)
