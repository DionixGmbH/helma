# Filters

`modules/core/Filters.js` adds global skin filters — usable in skin macros via the `|` operator.

```html
<% this.title | uppercase %>
<% this.body | truncate length="200" %>
<% this.description | stripTags %>
```

## Available Filters

### Text Transformation

- **`lowercase_filter(input)`** — convert to lowercase
- **`uppercase_filter(input)`** — convert to uppercase
- **`capitalize_filter(input)`** — capitalize first letter
- **`titleize_filter(input)`** — capitalize each word
- **`truncate_filter(input, param, limit, clipping)`** — truncate to `limit` characters, append `clipping` if truncated
- **`trim_filter(input)`** — remove leading/trailing whitespace

### Encoding

- **`stripTags_filter(input)`** — remove HTML/XML tags
- **`escapeXml_filter(input)`** — XML-escape special chars (`< > & ' "`)
- **`escapeHtml_filter(input)`** — HTML-escape
- **`escapeUrl_filter(input, param, charset)`** — URL-encode using the given charset (defaults to UTF-8)
- **`escapeJavaScript_filter(input)`** — escape for JavaScript string literals
- **`linebreakToHtml_filter(input)`** — replace `\n` with `<br>`

### String

- **`replace_filter(input, param, oldString, newString)`** — replace all occurrences of `oldString` with `newString`
- **`substring_filter(input, param, from, to)`** — substring extraction

### Date

- **`dateFormat_filter(input, param, format)`** — format a Date using SimpleDateFormat pattern

## Usage

```html
<!-- Lowercase a title -->
<% this.title | lowercase %>

<!-- Truncate with custom clipping -->
<% this.body | truncate length="100" clipping="..." %>

<!-- Strip tags then escape -->
<% this.html | stripTags | escapeHtml %>

<!-- Format a date -->
<% this.created | dateFormat format="yyyy-MM-dd" %>

<!-- URL-encode for a query string -->
<% this.searchQuery | escapeUrl %>
```

## Defining Custom Filters

```javascript
// Global/filters.js
function shorten_filter(input, param, length) {
    var n = parseInt(length || "30", 10);
    if (input.length <= n) return input;
    return input.substring(0, n - 1) + "…";
}
```

```html
<% this.title | shorten length="50" %>
```

Filters defined on a prototype (`Post/filters.js`) are scoped to that prototype's skins. Filters in `Global/filters.js` are available everywhere.

## See Also

- [Macro Syntax](../../framework/macro-syntax.md)
- [`modules/core/Filters.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/core/Filters.js)
