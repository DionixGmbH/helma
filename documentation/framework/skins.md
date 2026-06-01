# Skins & Macros

A **skin** is Helma's HTML template. Skins are plain text files with `<% macro %>` tags that interpolate dynamic content. Macros call back into JavaScript to render data.

## What a Skin Looks Like

`Post/main.skin`:

```html
<article class="post">
  <h2><% this.title %></h2>
  <p><% this.body encoding="html" %></p>
  <footer>
    By <% this.author.name %> · <% this.created format="yyyy-MM-dd" %>
    <% this.tags prefix="<ul class='tags'><li>" suffix="</li></ul>" %>
  </footer>
</article>
```

## Rendering a Skin

From an action:

```javascript
function main_action() {
    renderSkin("main");                        // render Post/main.skin
}
```

From a skin or another macro:

```javascript
function block_macro() {
    this.renderSkin("block");                  // render a different skin
}
```

Or render to a string:

```javascript
var html = this.renderSkinAsString("main");
res.contentType = "application/json";
res.write(JSON.stringify({ html: html }));
```

The `renderSkin` lookup order is:

1. The active `res.skinpath` array (if set)
2. The prototype's own directory
3. Parent prototypes in the inheritance chain
4. The `Global` prototype

See `SkinManager.getSkin()` at `src/main/java/helma/framework/core/SkinManager.java:45`.

## Macro Anatomy

The macro syntax is `<% handler.name attr1="value1" attr2="value2" %>`:

```text
<%       this.title       encoding="html"       default="(no title)"    %>
   ↑     ↑     ↑          ↑                     ↑
   open  handler  name    standard param        standard param
```

Components:

- **Handler**: optional. One of `this`, `response`, `request`, `session`, `param`, or a registered handler. Defaults to a prototype-registered handler or `global`.
- **Name**: the property or function to call. Resolved as `<handler>.<name>_macro()` (a function) or `<handler>.<name>` (a property).
- **Parameters**: key=value pairs. Named or positional.

## How a Macro Is Resolved

For `<% this.title %>`:

1. `this` is the current macro context. In a skin rendered for a HopObject, `this` is that HopObject.
2. Look for `title_macro()` on the prototype. If found, invoke it. The return value or anything it writes to the buffer becomes the macro output.
3. Otherwise, look for the property `title` on the HopObject. Use its value.
4. If neither exists and `onUnhandledMacro` is defined, invoke it with the name.
5. If still nothing and `failmode="verbose"`, throw an error. Otherwise emit nothing.

For `<% somehandler.foo %>`:

1. Look up `somehandler` in `res.handlers` (registered macro handlers).
2. The registered handlers include:
    - All path objects by their prototype names (e.g. `<% post.title %>` works during a request to `/posts/123` where `post` is the Post HopObject in the path)
    - Anything you register in `onRequest` via `res.handlers.foo = ...`
3. Then look for `foo_macro()` or property `foo`.

## Built-in Handlers

Inside a skin you can always reference:

| Handler | Refers to |
|---|---|
| `this` | The object passed to `renderSkin()` |
| `response` | `res` |
| `request` | `req` |
| `session` | The current session |
| `param` | The parameter object passed as second arg to `renderSkin()` |

```html
<!-- session/user handler -->
Hello, <% session.user.name default="anonymous" %>!

<!-- response message handler -->
<% response.message prefix="<div class='flash'>" suffix="</div>" %>

<!-- the request action -->
You are on the <% request.action %> action.

<!-- skin parameters -->
function main_action() {
    renderSkin("main", { theme: "dark" });
}
<!-- inside main.skin: -->
<body class="<% param.theme default="light" %>">
```

## Subskins

A skin file can contain multiple **subskins** delimited by `<% #name %>` markers:

`Post/main.skin`:

```html
<article>
  <% #header %>
  <h2><% this.title %></h2>
  <p><% this.body %></p>
  <% #footer %>
  By <% this.author.name %>
</article>
```

Render a specific subskin with `#`:

```javascript
this.renderSkin("main#header");
this.renderSkin("main#footer");
```

Rendering `main` without `#` renders only the content **before** the first `<% #name %>` marker — i.e. only the prologue `<article>` line.

## Inheritance: `.extends`

A skin can extend another:

`special.skin`:

```html
<% .extends "main" %>
<% #header %>
<h1>Special: <% this.title %></h1>
```

Rendering `special` is equivalent to rendering `main`, but with `header` overridden. Subskins not redefined fall through to the parent.

## The `param` Handler

Inside a macro function:

```javascript
function tags_macro(param) {
    var sep = param.separator || ", ";
    res.write(this.tags.join(sep));
}
```

The first argument to a `_macro` function is the **named parameter map** from the macro call. So:

```html
<% this.tags separator=" | " %>
```

…calls `tags_macro({ separator: " | " })`.

Positional parameters follow:

```html
<% this.greet "Hello" "World" %>
```

…calls `greet_macro({}, "Hello", "World")`.

## Standard Macro Parameters

Helma reserves four parameter names with special meaning. Available on every macro:

| Parameter | Effect |
|---|---|
| `prefix="..."` | Prepend this string if the macro outputs anything |
| `suffix="..."` | Append this string if the macro outputs anything |
| `default="..."` | Use this value if the macro outputs nothing |
| `encoding="..."` | Re-encode the macro output. Values: `html`, `xml`, `form`, `url`, `all` |
| `failmode="..."` | Behaviour on unhandled macro: `silent` or `verbose` |

```html
<% this.title default="Untitled" prefix="<h1>" suffix="</h1>" %>
<% this.search-query encoding="url" %>
<% this.untrusted encoding="html" %>
```

## Encoding Values

The `encoding` parameter applies a transformation:

| Value | Encoder |
|---|---|
| `html` | `HtmlEncoder.encode()` — HTML escape `<`, `>`, `&`, `"` |
| `xml` | XML escape — same as HTML plus `'` |
| `form` | Encode for `<textarea>` / `<input>` value attributes |
| `url` | `URLEncoder.encode()` — URL-safe |
| `all` | HTML escape + replace newlines with `<br>` |

Default encoding is **none** — output is written verbatim. **Always encode untrusted input** to prevent XSS.

## Filters: The `|` Operator

Pipe a macro through one or more filter functions:

```html
<% this.body | trim | nl2br %>
```

This calls:

1. `this.body_macro()` → returns the body string
2. `trim_filter(body)` → returns trimmed string
3. `nl2br_filter(trimmed)` → returns string with newlines as `<br>`

Filters are functions named `<name>_filter` defined on a prototype, on `this`, or globally. Each takes the previous return value as its first argument.

## Comment Macros

```html
<%// this is a comment, ignored at render time %>
```

A macro starting with `//` is a comment.

## Global Macros

A macro `<% somefunction %>` without a handler tries to resolve `somefunction_macro` in this order:

1. As a property/macro of any prototype in `res.handlers` (path objects)
2. On the `Global` prototype
3. On the `global` scope (free function)

Global macros are useful for site-wide widgets:

```javascript
// Global/macros.js
function navigation_macro() {
    renderSkin("Global", "navigation");
}
```

```html
<!-- in any skin -->
<% navigation %>
```

## Macros That Take Skins as Arguments

A macro can render a skin inside its body:

```javascript
function tags_macro(param) {
    var sub = createSkin(param.itemSkin || "<li><% this.name %></li>");
    for each (var tag in this.tags.list()) {
        sub.render(tag);
    }
}
```

## Nested Macros

A macro parameter can itself be a macro:

```html
<% this.greet name=<% session.user.name %> %>
```

The inner macro is evaluated first; its result becomes the value of `name`.

## Skin Caching

Compiled skins are cached:

- On disk in the Resource layer — file mtime invalidates the cache
- In memory per-request via `ResponseTrans.skincache`

The cache is cleared on `app.clearCache()` from the management UI.

## Skin Sandboxing

`new Skin(source, app, sandbox)` creates a skin with a whitelist of allowed macros. Calling a macro outside the sandbox throws `MacroException: Macro not allowed in sandbox: ...`. Useful for user-generated templates.

```javascript
var sandbox = new java.util.HashSet();
sandbox.add("title");
sandbox.add("body");
var skin = new Packages.helma.framework.core.Skin(userTemplate, app, sandbox);
skin.render(reval, post, null);
```

## Best Practices

- Always set `encoding="html"` on user-supplied data.
- Use macros for view logic only. Heavy computation belongs in plain functions on the prototype.
- Prefer subskins over many small files when one logical view splits naturally.
- Use `res.skinpath` to support themes — point at different skin directories per user/tenant.

## See Also

- [Macro Syntax](macro-syntax.md) — the complete grammar
- [`createSkin()` and `Skin` API](../reference/global-object.md)
- [Examples in modules/helma/Skin.js](../modules/helma/skin.md)
