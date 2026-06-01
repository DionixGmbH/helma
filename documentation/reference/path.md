# Path (`path`)

`path` is a wrapper around the **RequestPath** — the ordered list of HopObjects walked while resolving the URL. Defined in `src/main/java/helma/scripting/rhino/PathWrapper.java`.

## What's in the Path

For a request to `/blog/posts/hello-world`:

```
path[0]  → root (Root HopObject)
path[1]  → root.blog (or whatever it traversed to)
path[2]  → posts collection
path[3]  → hello-world Post
path.length === 4
```

The first element is **always** the application root. The last element is the **current element** — the HopObject the action is invoked on.

## Indexed Access

```javascript
path[0]          // root
path[1]          // first traversed child
path[path.length - 1]   // current element (same as `this` in an action)
```

You can also access by negative index (Rhino feature):

```javascript
path[-1]         // last element (current)
```

## Length

```javascript
path.length      // number of HopObjects in the path
```

## Contains Check

```javascript
path.contains(node)    // true if node is anywhere in the path
```

## Iteration

```javascript
for (var i = 0; i < path.length; i++) {
    res.write(path[i]._prototype + " / ");
}
// → "Root / Collection / Post / "
```

Or with `for each`:

```javascript
for each (var node in path) {
    res.write(node._prototype);
}
```

## Prototype-Name Aliases

In addition to numeric indexing, the path objects are registered in `res.handlers` by their prototype names — accessible from skins:

```html
<!-- inside any skin during a request to /users/alice/photos/holiday -->
<% user.name %>          → alice's name
<% photos.size %>        → number of photos
<% holiday.title %>      → holiday's title
```

These aliases come from `Application.registerPathHandlers()` which adds each path object to `res.handlers` under its prototype's name and lowercase name.

The `Prototype.registerParents()` method additionally registers each object under its parent prototype names — so a `BlogPost extends Post` is reachable as both `<% post.* %>` and `<% blogpost.* %>`.

## Use Cases

### Breadcrumbs

```javascript
function breadcrumb_macro() {
    var parts = [];
    for (var i = 0; i < path.length; i++) {
        var node = path[i];
        var name = node.name || node._id;
        parts.push('<a href="' + node.href() + '">' + name + '</a>');
    }
    res.write(parts.join(" › "));
}
```

```html
<!-- in a skin -->
<nav class="breadcrumbs"><% breadcrumb %></nav>
```

### Context-Aware Macros

```javascript
function navigation_macro() {
    // Show admin links only if we have a Blog in the path and user is admin
    if (path.contains(currentBlog()) && session.user.isAdminOf(currentBlog())) {
        res.write('<a href="' + currentBlog().href("admin") + '">Admin</a>');
    }
}

function currentBlog() {
    for (var i = path.length - 1; i >= 0; i--) {
        if (path[i]._prototype === "Blog") return path[i];
    }
    return null;
}
```

## Ancestors via `_parent`

The path's hierarchy may differ from the HopObject's `_parent` chain. For URL `/users/alice/photos/holiday`:

- Path: `[Root, users, alice, photos, holiday]`
- `holiday._parent`: `photos` (from `_parent = photos` in type.properties or implicit)

The path follows the URL traversal; `_parent` follows the DB relation. They usually coincide but can diverge when you have multiple parent candidates.

## Custom Path Objects

When `getChildElement(name)` returns an object that isn't normally a child, the object still becomes part of the path:

```javascript
// Root/main.js
function getChildElement(name) {
    if (name === "search") {
        var searchHandler = new SearchHandler();    // transient HopObject
        searchHandler.query = req.params.q;
        return searchHandler;
    }
    return null;
}
```

After traversing `/search`, `path[1]` is the SearchHandler. It appears in the path even though it doesn't exist in the DB.

## Mountpoints

If a URL traverses through a mountpoint (`prop.mountpoint = Other`), the mounted object becomes part of the path. The path always reflects the URL traversal, not the storage hierarchy.

## See Also

- [Concepts: Request Lifecycle](../concepts/request-lifecycle.md) — path resolution
- [Framework: URL Routing](../framework/url-routing.md)
- [`PathWrapper.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/scripting/rhino/PathWrapper.java) — source
