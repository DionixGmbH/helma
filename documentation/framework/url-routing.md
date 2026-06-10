# URL Routing & Path Resolution

Helma doesn't have a router. URL routing is just **object traversal**: each URL segment asks the current object for a child by that name. This page explains the algorithm in detail and shows how to customise it.

## The Algorithm

Given a request to `/myapp/users/alice/photos/holiday`:

1. The servlet strips the application mountpoint (`/myapp`). What remains: `users/alice/photos/holiday`.
2. The path is tokenised on `/`. Empty tokens are skipped. Maximum 50 tokens.
3. The evaluator sets `currentElement = root` (the application root HopObject).
4. For each token, in order:
    - **If this is the last token and the URL does not end with `/`**, try interpreting it as an action name on the current object. If a matching action function exists, this token is the action and we're done resolving.
    - Otherwise, treat the token as a child name and replace `currentElement` with `currentElement.getChildElement(token)`.
    - If the child doesn't exist: throw `NotFoundException`.
5. After processing all tokens:
    - If no action was assigned in step 4 (e.g. URL ended with `/`), use the default action name (`main`).
    - Look up the action function on the final `currentElement`. If absent: throw `NotFoundException`.

The result: `currentElement` = the target object, `action` = the function name to invoke.

See `RequestEvaluator.run()` at `src/main/java/helma/framework/core/RequestEvaluator.java:265` for the implementation.

## What Counts as a "Child"

`getChildElement(name)` is resolved in priority order:

1. **`getChildElement(name)`** function defined on the prototype — fully custom child lookup
2. The DB-backed `_children` collection if defined in `type.properties`
3. A sub-property of the HopObject with matching access-name column
4. A sub-node by element name (the embedded DB matches on name)

If you want URL segments to map to virtual objects (not real children), implement `getChildElement` on the prototype:

```javascript
// Root/main.js
function getChildElement(name) {
    if (name === "static") {
        return staticContentHandler;        // a global object
    }
    if (name === "search") {
        return new SearchHandler();         // transient HopObject
    }
    return null;                            // fall through to default
}
```

Returning `null` makes Helma try its standard child lookup. Returning a HopObject (or `INode`-like) makes that the next path object.

## What Counts as an "Action"

An action is found when:

- The current object's prototype defines `<token>_action`, OR
- A method-specific variant (`<token>_action_<method>` / `_ajax` / `_ajax_<method>`) matches the current request

See [Actions](actions.md) for the precise lookup order.

## Trailing Slash Semantics

The trailing slash is meaningful:

| URL | Behaviour |
|---|---|
| `/blog/post/123` | The last segment `123` is tried as an action first, then as a child. If no `123_action`, then `currentElement = post.getChildElement("123")` and action = `main`. |
| `/blog/post/123/` | All segments are treated as children. `currentElement = post.getChildElement("123")`. Action = `main`. |

Practical: if you have an action named `123` (unusual but possible), `/post/123` invokes it; `/post/123/` walks past it to look for a child.

## Examples

### Static blog

URL `/posts/hello-world`:

```
root → posts (Root.getChildElement("posts"))
posts → "hello-world" (a Post HopObject by access name)
action: main_action on hello-world
```

`Root/type.properties`:

```properties
posts.collection = Post
```

`Post/type.properties`:

```properties
_db = main
_table = posts
slug = slug_column
```

`Root/Post.collection.accessname = slug` is implicit when you say `posts.accessname = slug` in Root.

### User profile

URL `/users/alice/edit`:

```
root → users (a collection of User HopObjects)
users → alice (User HopObject)
alice / edit → "edit" matches edit_action on User
```

### Custom virtual child

URL `/api/v1/posts`:

```javascript
// Root/main.js
function getChildElement(name) {
    if (name === "api") return new ApiV1Handler();
    return null;
}

// ApiV1Handler is a regular HopObject prototype
// Its getChildElement might route "v1" → another handler, etc.
```

### Catch-all

URL `/anything/whatsoever`:

```javascript
// Root/main.js
function getChildElement(name) {
    // every segment matches — useful for blog-style permalinks
    return root.posts.getByPermalink(name);
}
```

## The `path` Object

While resolving the URL, Helma builds a `RequestPath` — an ordered list of the objects visited. After resolution, it's exposed as the global `path`:

```javascript
// In an action or skin
path[0]                    // root
path[1]                    // first child traversed
path[path.length - 1]      // the current element

// path also acts as a map of prototype-name → object
path.user                  // the first object of prototype User in the path
path.post                  // the first object of prototype Post
```

This is how `<% user.name %>` in a Root skin works during a request to `/users/alice` — the macro `user` resolves to the path's `User` object.

## Application Mountpoints

By default, application `myapp` is mounted at `/myapp/`. URLs at the root mount the `welcome` app. Override per-app:

```properties
# apps.properties
blog.mountpoint = /
shop.mountpoint = /store
```

A mountpoint of `/` makes the app the **root** application — requests like `/posts/123` go directly to the blog. Only one app can be mounted at `/`.

## The Welcome App

`apps.properties` ships with `welcome` enabled. The welcome app's Root prototype is hardcoded to serve a landing page that links to other registered apps. If you mount your own app at `/`, remove or comment out the bare `welcome` line in `apps.properties`. There is no `<app>.enabled` flag.

## Static File Serving

For each application, you can declare a directory of static files:

```properties
# apps.properties
myapp.static = /var/www/myapp-static
myapp.staticHome = index.html        # default file
myapp.staticMountpoint = /static     # URL prefix (defaults to /static)
```

Requests under `/myapp/static/foo.png` are served directly from `/var/www/myapp-static/foo.png` without invoking the scripting engine.

`myapp.protectedStatic` registers a second static-file directory served from a different URL via `res.forward(...)` indirection. Helma does **not** automatically check `session.user` for that directory — the name is misleading. You must gate access in `onRequest` or in the action that calls `res.forward()` to the protected path.

## Path Validation

To prevent infinite path loops, Helma rejects requests with more than 50 path segments (`Path too long` runtime exception). To raise this limit, fork the framework — it's hardcoded.

## URL Generation: `href()`

To generate URLs back, use `node.href()`:

```javascript
post.href()                  // /myapp/posts/<post-id>
post.href("edit")            // /myapp/posts/<post-id>/edit
post.href("edit", "id=42")   // /myapp/posts/<post-id>/edit?id=42
```

`href()` walks the parent chain up to the root, prepending each step's element name, and prefixes with the application's `baseURI`.

The `baseURI` is set from `app.properties::baseURI` (or computed from the mountpoint).

## See Also

- [Actions](actions.md) — what makes a function an action
- [AJAX Action Resolution](ajax-actions.md) — AJAX-aware action lookup
- [Object Model](../concepts/object-model.md) — what HopObjects are and how children work
