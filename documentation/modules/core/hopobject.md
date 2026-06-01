# HopObject Extensions

`modules/core/HopObject.js` adds methods and macros to `HopObject.prototype`. These are available on every persistent or transient HopObject in any Helma app.

## Iteration

### `forEach(callback)`

Iterate child nodes. `callback` is called with the child index, and `this` is the child node.

```javascript
post.comments.forEach(function(i) {
    print(i + ": " + this.text);
});
```

## Skin Macros

These are HopObject-aware macros — usable as `<% this.id %>`, `<% this.href %>`, etc.

### `<% this.id %>`

Writes the HopObject's internal ID.

### `<% this.href action="X" %>`

Writes the URL to this object, optionally for a specific action.

```html
<a href="<% this.href %>">View</a>
<a href="<% this.href action="edit" %>">Edit</a>
```

### `<% this.skin name="N" as="source" %>`

Render a skin, optionally retrieving its source instead.

```html
<%-- Render --%>
<% this.skin name="teaser" %>

<%-- Retrieve source --%>
<pre><% this.skin name="teaser" as="source" %></pre>
```

### `<% this.switch name="published" on="✓" off="✗" %>`

Render `on` text if `this[name]` is truthy, otherwise `off`.

```html
Status: <% this.switch name="active" on="Active" off="Inactive" %>
```

### `<% this.loop %>`

Iterate over a collection rendering a skin per item.

```html
<% this.loop collection="comments" skin="commentItem" limit="20" order="created desc" %>
```

Parameters:

- `collection` — name of the collection to iterate (defaults to children)
- `skin` — skin to render per item
- `limit` — max items per page
- `order` — SQL-ish order (`field` or `field desc`)
- `prefix` / `suffix` — wrapping content
- Pagination via `req.data.page`

### `<% this.size name="comments" verbose="true" none="no comments" one="one comment" many="%n comments" %>`

Write the size of a collection.

```html
<% this.size name="comments" %>
<%-- → "42" --%>

<% this.size name="comments" verbose="true" none="No replies yet" one="1 reply" many="%n replies" %>
<%-- → "42 replies" --%>
```

`%n` is replaced with the actual count.

## See Also

- [Reference: HopObject](../../reference/hopobject.md) — full HopObject API
- [Concepts: Object Model](../../concepts/object-model.md)
- [`modules/core/HopObject.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/core/HopObject.js)
