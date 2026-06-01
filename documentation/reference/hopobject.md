# HopObject

`HopObject` is the base class of every persistent and transient object in a Helma application. It exposes the same API regardless of whether the underlying storage is the embedded XML database or a relational table.

This page lists every JS-facing method and property.

## Constructor

```javascript
var p = new Post();              // create a transient HopObject of prototype Post
var p = new Post(args);          // also calls Post.prototype.constructor(args) if defined
```

See [HopObject Constructors](../scripting/hopobject-constructors.md) for customisation.

## Internal Properties

These are special properties recognized by Helma. They all start with `_`:

### `_id` (String)

The primary key value. Read-only for the most part; can be set explicitly via direct field assignment (`node._id = "..."`) for data migration.

### `_name` (String)

The element name (URL segment). Defaults to `_id` unless `_name` column is configured.

### `_prototype` (String)

The prototype name (`"Post"`, `"User"`, etc.).

### `_parent` (HopObject)

The parent HopObject as per the `_parent` mapping or `parent.add(this)` lineage.

### `_state` (String)

The persistence state:

| Value | Meaning |
|---|---|
| `TRANSIENT` | Newly created, never persisted |
| `NEW` | Persistence pending in current transaction |
| `CLEAN` | Persisted, no dirty modifications |
| `MODIFIED` | Modified, awaiting commit |
| `DELETED` | Pending deletion |
| `INVALID` | Reference to a Node no longer in the DB |

### `_created` / `_lastModified` (Date)

Auto-managed timestamps. Set on insert and on update respectively.

### `_children` (collection)

For HopObjects with a `_children` mapping, the children collection.

## Collection-Like API

A HopObject's children form an addressable collection.

### `size()` / `length` (int)

Number of children.

```javascript
post.size();        // number of comments (if Comment is the child relation)
post.length;        // alias for size()
```

### `get(index)` / `get(name)`

Get a child by index (0-based) or by element name.

```javascript
post.get(0);                // first comment
post.get("comment-id");     // child by name (uses access name)
```

### `list()` / `list(options)` (Array)

Get all children as an array.

```javascript
post.list();                                          // all
post.list({ offset: 10, maxSize: 20 });               // paginated
post.list({ order: "created desc", maxSize: 10 });    // sorted
post.list({ filter: "is_approved = true" });          // filtered
```

Options:

- `order` — SQL `ORDER BY` clause
- `maxSize` — `LIMIT`
- `offset` — `OFFSET`
- `filter` — additional `WHERE`

### `add(node)` / `add(node, position)`

Add a child. `position` is optional — for ordered collections it sets the index.

```javascript
post.add(comment);
post.add(comment, 0);    // insert at beginning
```

### `remove(node)` / `remove()`

Remove a child (or this HopObject from its parent if called without argument).

```javascript
post.comments.remove(comment);
comment.remove();               // remove from parent
```

### `contains(node)` (boolean)

Test whether a node is a child.

```javascript
post.contains(comment);     // true if comment is in post.comments
```

### `indexOf(node)` (int)

Get the index of a child, or -1 if not a child.

## Lifecycle Methods

### `persist()`

Force immediate persistence of this HopObject within the current transaction. Useful when you need the auto-generated ID before the transaction commits.

```javascript
var p = new Post();
this.add(p);
p.persist();             // p._id is now available
res.redirect(p.href());
```

### `invalidate()`

Mark this Node as stale. The next access re-fetches from the DB.

```javascript
post.invalidate();
print(post.title);     // forces re-fetch
```

### `clearCache()`

Clear the per-HopObject transient cache (`this.cache`).

```javascript
this.cache.foo = "transient value";
this.clearCache();
print(this.cache.foo);   // undefined
```

## URL Generation

### `href()` / `href(action)` / `href(action, query)` (String)

Generate the URL for this HopObject.

```javascript
post.href();                    // /blog/posts/123
post.href("edit");              // /blog/posts/123/edit
post.href("edit", "from=list"); // /blog/posts/123/edit?from=list
```

The URL is built by walking the `_parent` chain back to the application root and joining element names with `/`. The result is prefixed with the application's mountpoint.

### `path()` (String)

Same as `href()` but without the mountpoint prefix.

## Skin Rendering

### `renderSkin(name, params)`

Render a skin associated with this prototype.

```javascript
post.renderSkin("main");
post.renderSkin("teaser", { highlight: true });
post.renderSkin("main#summary");    // subskin
```

### `renderSkinAsString(name, params)` (String)

Same but returns the rendered text.

### `getResource(name)` (Resource)

Get a resource (file) from this prototype's directory by name.

```javascript
var template = this.getResource("config.json");
var content = template.getContent();
```

### `getResources(name)` (Resource[])

Get all matching resources across the prototype chain.

## Cache

### `this.cache` (HopObject, transient)

A transient cache node attached to this HopObject. Useful for storing computed values without persistence.

```javascript
this.cache.preview = this.body.substring(0, 100);
this.cache.lastRendered = new Date();
```

## Permission Checking

Helma has **no built-in permission hook**. The framework's action dispatcher does not consult any `getPermission` function. Implement authorisation in `onRequest` or at the top of each action. See [Authentication](../framework/authentication.md).

## Lifecycle Hooks

These are functions you define on the prototype:

| Hook | When | Args | Purpose |
|---|---|---|---|
| `constructor(args)` | On `new` | `args` | Initialise transient state |
| `onInit()` | After DB load | none | Compute derived fields |
| `onRequest()` | Before action runs | none | Auth check, override action |
| `onResponse()` | After action runs | none | Post-process the response |
| `onPersist()` | Before DB write | none | Set computed columns |
| `onLogout(sessionId)` | On User logout / session timeout | sessionId | Audit |
| `onCodeUpdate()` | On hot reload | none | Re-init state after code change |
| `getChildElement(name)` | URL path step | child name | Return child HopObject |
| `onUnhandledMacro(name)` | Macro lookup miss | macro name | Return rendered value |

Helma does **not** ship with framework-invoked `onLogin` or `getPermission` hooks — those names are sometimes used as user-level conventions, but the framework does not call them automatically. See [Sessions & Users](../concepts/sessions-and-users.md#lifecycle-hooks).

## Iteration

```javascript
// for...in iterates property names
for (var key in post) {
    res.write(key + " = " + post[key] + "\n");
}

// for each (Rhino-specific) iterates property values
for each (var value in post) {
    res.write(value + "\n");
}

// for each on a collection iterates children
for each (var comment in post.comments) {
    res.write(comment.text);
}
```

## JSON Serialization

`HopObject` has no built-in `toJSON()`. Define your own:

```javascript
// Post/main.js
Post.prototype.toJSON = function() {
    return {
        id: this._id,
        title: this.title,
        body: this.body,
        author: this.author ? { id: this.author._id, name: this.author.name } : null,
        created: this.created.toISOString()
    };
};

// Use:
JSON.stringify(post);    // calls post.toJSON()
```

## Internal Access

### `_unwrap_()` (INode)

Get the underlying `INode` Java object. Escape hatch.

```javascript
var inode = post._unwrap_();
inode.getElementName();
inode.getString("title");
```

## See Also

- [Concepts: Prototypes & Inheritance](../concepts/prototypes.md)
- [Concepts: Object Model](../concepts/object-model.md)
- [HopObject Constructors](../scripting/hopobject-constructors.md)
- [modules/core/HopObject.js](../modules/core/hopobject.md) — bundled extensions
- [`HopObject.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/scripting/rhino/HopObject.java) — source
