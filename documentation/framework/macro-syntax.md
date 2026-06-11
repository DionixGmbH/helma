# Macro Syntax

This page is the complete grammar reference for Helma's skin macros.

## Grammar

```ebnf
skin         ::= (text | macro)*
macro        ::= "<%" content "%>"
content      ::= comment | subskin | extends | regular_macro
comment      ::= "//" anything
subskin      ::= "#" identifier
extends      ::= ".extends" SP string
regular_macro::= [filter_lhs] macroname (SP parameter)* ["|" macroname (SP parameter)*]
macroname    ::= identifier ("." identifier)*
parameter    ::= named_param | positional_param
named_param  ::= identifier "=" value
positional_param ::= value
value        ::= string | identifier | nested_macro
nested_macro ::= macro
string       ::= '"' chars '"' | "'" chars "'"
```

## Open and Close Tags

```html
<% ... %>
```

Backslash-escape the opening `<` to write a literal `<%`:

```html
\<% not a macro %>
```

## Macro Names

Forms:

- `name` → call `name_macro()` on the current `this`, or look up `name` property
- `handler.name` → call on a specific handler
- `handler.sub.name` → walk into nested object: `handler.sub.name_macro()` or `handler.sub.name` property

The valid identifiers in `[a-zA-Z][a-zA-Z0-9_-]*` — note that **dashes are allowed in macro names** at the JavaScript side they are translated to underscores. So `<% search-form %>` calls `search_form_macro()`.

## Handlers

Special handler names recognised by the parser:

| Handler | Resolves to |
|---|---|
| `this` | The object passed to `renderSkin(name, this, param)` |
| `response` | The `ResponseTrans` (i.e. `res`) |
| `request` | The `RequestTrans` (i.e. `req`) |
| `session` | The current `Session` |
| `param` | The second arg passed to `renderSkin` |

Any other handler name is looked up in `res.handlers` (the macro handler map). The framework auto-registers:

- Every path object under its prototype's `name` and `lowerCaseName`
- Any handler you explicitly add via `res.handlers.foo = ...` or `onRequest()`

## Named Parameters

```html
<% this.title default="Untitled" encoding="html" %>
```

Parameters are passed as a `Map` to the `_macro` function:

```javascript
function title_macro(param) {
    // param.default = "Untitled"
    // param.encoding = "html"
}
```

Note: `encoding` and `default` are *standard parameters* and are also intercepted by the framework — see below.

## Positional Parameters

```html
<% this.repeat "hello" 3 %>
```

Pass after the macro name, without a `key=`:

```javascript
function repeat_macro(param, first, second) {
    // param is the named-param map (empty in this case)
    // first = "hello"
    // second = "3" (always a string unless nested macro)
}
```

Mixed:

```html
<% this.repeat sep="-" "hello" 3 %>
```

```javascript
function repeat_macro(param, first, second) {
    // param.sep = "-"
    // first = "hello", second = "3"
}
```

## Standard Parameters

The framework intercepts these named parameters and applies them around the macro's output:

### `prefix`

Prepend this string **if the macro outputs anything** (non-empty after default handling).

```html
<% this.tags prefix="Tagged with: " %>
```

### `suffix`

Append this string if the macro outputs anything.

```html
<% this.tags suffix=". " %>
```

### `default`

Substituted when the macro outputs nothing or `undefined`/`null`:

```html
<% this.title default="Untitled" %>
```

### `encoding`

Re-encode the rendered output.

#### `encoding=` (legacy — active when `skinDefaultEncoding` is set, or explicitly on a macro)

| Value | What it does |
|---|---|
| `none` (or omit) | No transformation |
| `escape` (alias: `xml`) | Strict 5-char HTML escape: `<>&"'` |
| `attr` (alias: `form`) | 4-char escape: `<>&"` — double-quoted HTML attributes |
| `all` | 4-char escape + `\n` → `<br>` |
| `url` | Percent-encoding — URL query-string values only |
| `format` (alias: `html`) | Legacy format encoder — passes HTML tags through; **not XSS-safe** |

#### `context=` (new — active when `skinDefaultEncoding` is not set in `app.properties`)

| Value | What it does |
|---|---|
| `none` | No transformation |
| `html` | Strict 5-char HTML escape: `<>&"'` |
| `attr` | 4-char escape: `<>&"` — double-quoted HTML attributes |
| `lines` | 5-char escape + `\n` → `<br>` |
| `url` | Percent-encoding — URL query-string values |
| `json` | Type-aware JSON literal: strings quoted, numbers/booleans/null verbatim |
| `js` | Type-aware JS literal: superset of `json` with U+2028, U+2029, and `</` safety |

### `failmode`

Controls behaviour when the macro is unhandled:

- `silent` (default) — emit nothing
- `verbose` — emit `[Unhandled macro: foo]`

```html
<% this.maybe-missing failmode="verbose" %>
```

You can globally set `failmode = verbose` in `app.properties` to force verbose mode during development.

## Filter Chain (`|`)

```html
<% this.body | trim | nl2br | strip-tags %>
```

The `|` separator passes the macro's output to filter functions. Each filter is invoked as `<name>_filter(value, paramMap)` on `this` (or globally).

Filters can come from anywhere — `this`, a registered handler, or the global scope. Filters defined on `this` are tried first.

Example filter definition:

```javascript
// Post/filters.js
function truncate_filter(value, param) {
    var max = param.length || 100;
    if (value.length <= max) return value;
    return value.substring(0, max) + "…";
}
```

```html
<% this.body | truncate length="200" %>
```

## Subskins

```html
<% #subskinname %>
```

Marks the start of a subskin. Content from this marker until the next `<% #... %>` is its body.

```html
default content (the unnamed/main subskin if any)
<% #header %>
content of the header subskin
<% #footer %>
content of the footer subskin
```

Calling `renderSkin("main")` renders the unnamed/main subskin (the content before `<% #header %>`). To render a specific subskin: `renderSkin("main#header")`.

If a skin contains *only* subskins and nothing before the first `<% #... %>`, calling `renderSkin("main")` produces no output.

## Skin Inheritance

```html
<% .extends "basename" %>
```

The first macro in a skin can be `.extends` to declare a parent skin. The current skin's subskins override the parent's; subskins not redefined fall through.

Example:

`Post/admin.skin`:

```html
<% .extends "main" %>
<% #header %>
<h1>Admin View: <% this.title %></h1>
```

`renderSkin("admin")` renders `main.skin`'s structure but with `header` replaced.

## Comments

```html
<%// a comment, never rendered %>
```

Anything inside `<%// ... %>` is dropped at render time.

## Escaping

To produce literal `<% ... %>` in output:

```html
\<% literal %\>
```

The backslash before `<` (and ideally `>`) escapes the macro parser. Inside parameter values, `\\` escapes a backslash and `\"` escapes a double quote.

## Empty Macros

```html
<% %>
```

Empty macros are parsed but render nothing. Useful for noting positions without semantic effect.

## Lenient Parsing

When a macro tag's terminator is missing, Helma's parser switches to **lenient mode** which accepts quote-mismatched values. This is a recovery mechanism — your skin will still render but a warning will be logged:

```
Unterminated Macro Tag: <% something
```

Fix the source; don't rely on lenient mode.

## Nested Macros in Parameters

A macro parameter can be a nested macro:

```html
<% this.greet name=<% session.user.name %> %>
```

The inner macro is evaluated and its **raw return value object** (not a rendered string) is passed to the outer macro as the parameter. `context=` / `encoding=` on the inner macro is ignored unless `prefix=` or `suffix=` is also present — only then is the inner rendered to a string with its own encoding applied. Otherwise, encoding is applied to the outer macro's output.

For `context="json"` / `context="js"` this means the outer's type-aware serialization sees the underlying Number / Boolean / null, so type fidelity is preserved across nesting:

```html
<%// `count_macro` returns 42 (Number) %>
<% data context="json" count=<% this.count %> %>   <!-- emits  42  not  "42"  -->
```

This works at any depth — but readability quickly suffers. Prefer named parameters and helper macros.

## Whitespace and Newlines

Helma preserves all whitespace between macros. After a `<% #subskin %>` marker, the *next* `\r\n`, `\n` or `\r` is consumed (so the marker doesn't leave a blank line).

## Performance Notes

- Macros are parsed once at skin load (and cached as `Macro[]` on the `Skin` object).
- Each macro invocation looks up the handler at runtime — Helma doesn't do static dispatch.
- A macro that writes directly to `res` (via `res.write` inside `_macro()`) is faster than one returning a string, because the string doesn't need to be allocated and copied.

## See Also

- [Skins & Macros](skins.md) — the conceptual introduction
- [Skin reference in `helma.Skin`](../modules/helma/skin.md) — high-level utilities for working with skin objects from JavaScript
