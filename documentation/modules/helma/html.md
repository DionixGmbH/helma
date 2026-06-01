# helma.Html

A high-level HTML/XHTML construction helper.

```javascript
app.addRepository("modules/helma/Html.js");
```

## Constructor

```javascript
var html = new helma.Html();
```

## Tag Output

```javascript
html.openTag("div", { class: "post" });             // writes <div class="post">
html.closeTag("div");                                // writes </div>

html.tag("br");                                      // writes <br />
html.tag("img", { src: "/x.jpg", alt: "..." });     // <img src="/x.jpg" alt="..." />

html.element("h1", "Title", { class: "main" });     // <h1 class="main">Title</h1>
```

`*AsString` variants return the string instead of writing to `res`.

## Form Inputs

```javascript
html.form({ method: "POST", action: "/submit" });   // opens form
html.hidden({ name: "_token", value: csrf });
html.input({ name: "username", value: "" });
html.password({ name: "password" });
html.textArea({ name: "body", value: this.body });
html.checkBox({ name: "subscribe", value: "yes" });
html.radioButton({ name: "color", value: "red" });
html.dropDown({ name: "color" }, ["red", "green", "blue"], "red");
html.file({ name: "upload" });
html.submit({ value: "Save" });
html.button({ value: "Cancel" });
```

## Links

```javascript
html.link({ href: "/about" }, "About");             // <a href="/about">About</a>
html.openLink({ href: "/about" });                  // opens <a href="/about">
html.closeLink();                                    // </a>
```

## Tables

```javascript
html.table(
    ["Name", "Email"],                              // headers
    [
        ["Alice", "alice@example.com"],
        ["Bob",   "bob@example.com"]
    ],
    { class: "users" }                              // attrs
);

// Or with TableWriter for streaming
var tw = new html.TableWriter(3, { class: "data" });
tw.write("col1");
tw.write("col2");
tw.write("col3");
// auto-wraps to new row after 3 cells
tw.close();
```

## Other

```javascript
html.color("#ff0000");                              // color swatch
html.map("namedMap", [areas]);                       // image map
html.activateUrls(text);                            // convert URLs in text to links
```

## Static

### `helma.Html.renderMarkupPart(name, start, end, attr)` → String

Internal helper.

### `helma.Html.isSelected(value, selectedValue)` → boolean

Test for select/radio selection.

## See Also

- [`modules/helma/Html.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Html.js)
- [`jala.Form`](../jala.md#form) — for full form validation framework
