# HopObject Constructors

Every HopObject prototype has a JavaScript constructor function. `new Post()` creates a new (initially transient) Post. This page explains how to customise constructor behaviour.

## Default Behaviour

```javascript
// Just creating
var p = new Post();
// All properties undefined
// No ID yet (assigned on transaction commit)

// Set properties
p.title = "Hello";
p.body = "World";

// Persist as a child
root.posts.add(p);
```

The default constructor:

1. Allocates a fresh transient Node
2. Sets its prototype to the named one
3. Returns a HopObject wrapping the Node

No properties are pre-set. No DB row is inserted yet.

## Custom Constructor

Define a `constructor` function on the prototype:

```javascript
// Post/main.js
Post.prototype.constructor = function(args) {
    // 'this' is the new HopObject
    this.created = new Date();
    this.published = false;
    if (args && args.title) {
        this.title = args.title;
    }
};

// Usage
var p = new Post({ title: "Hello" });
print(p.title);              // "Hello"
print(p.created);            // current Date
```

The constructor function runs every time `new Post(args)` is invoked. The `args` parameter (the first argument to the constructor call) is passed in.

## Implementation Note

The constructor is set via `HopObjectCtor.java`. When you create a HopObject via `new`, Helma:

1. Looks up the prototype's `constructor` property on the JS side.
2. If defined, invokes it with `this` = the new transient Node, plus any arguments.
3. Returns the new HopObject.

This is JavaScript inheritance — your constructor can call into parent prototype constructors:

```javascript
Article.prototype.constructor = function(args) {
    // Call Post's constructor first
    Post.prototype.constructor.call(this, args);
    // Then add Article-specific setup
    this.category = "article";
};
```

## Static Factory Methods

For cleaner APIs, expose factory methods on the prototype constructor:

```javascript
Post.fromMarkdown = function(text) {
    var p = new Post();
    p.body = convertMarkdown(text);
    p.title = extractTitle(text);
    p.created = new Date();
    return p;
};

// Usage
var p = Post.fromMarkdown(req.postParams.text);
root.posts.add(p);
```

Static methods don't require a prototype directory entry — define them inline on the constructor object.

## Instance Methods

Methods defined on the prototype are inherited by all instances:

```javascript
// Post/methods.js
Post.prototype.toJSON = function() {
    return {
        id: this._id,
        title: this.title,
        body: this.body,
        author: this.author ? this.author.name : null,
        created: this.created.toISOString()
    };
};

Post.prototype.wordCount = function() {
    return this.body.split(/\s+/).length;
};
```

```javascript
var p = root.posts.get(0);
print(p.wordCount());                    // 42
res.write(JSON.stringify(p.toJSON()));
```

You don't need to use `Post.prototype.X = ...` — you can just define `function wordCount()` in `Post/methods.js`. Functions in `Post/*.js` are added to the prototype automatically at compilation.

## Constructor vs Initialisation Hook

Two phases:

| Hook | When | Purpose |
|---|---|---|
| `constructor(args)` | On `new` for a transient Node | Set defaults from args |
| `onInit()` | When a Node is reloaded from the DB | Compute derived fields after DB load |

```javascript
Post.prototype.constructor = function(args) {
    this.created = new Date();
    this.viewCount = 0;
};

Post.prototype.onInit = function() {
    // Computed once after DB load
    this.preview = this.body.substring(0, 100);
};
```

`onInit` is called via `INodeStateListener` when a Node transitions from "not loaded" to "loaded". `preview` is then available without re-computing.

## Persist Hook

`onPersist()` runs immediately before the Node is written to the DB:

```javascript
Post.prototype.onPersist = function() {
    this.modified = new Date();
    if (!this.slug) {
        this.slug = generateSlug(this.title);
    }
};
```

Use this for computed columns that should be set just before commit.

## Constructor Property

A subtle quirk: when you set `Post.prototype.constructor = function(){...}`, you're redefining the prototype's `constructor` property. This is normal JS. But because Helma's `HopObject.defineProperty()` does special handling for `constructor` (`src/main/java/helma/scripting/rhino/HopObject.java:144`), the new constructor function actually works.

Don't try to use `Object.defineProperty(Post.prototype, "constructor", {...})` — use the simple assignment.

## Type Checking

```javascript
var p = root.posts.get(0);

// JS-style instanceof
p instanceof Post;          // true

// Prototype-name check
p._prototype;               // "Post"

// Helma's isInstanceOf — walks the prototype chain
p._isInstanceOf("Post");    // true
p._isInstanceOf("Article"); // true if Article extends Post

// Direct prototype name comparison
String(p._prototype) === "Post";
```

## Persistence State

A new HopObject is initially transient:

```javascript
var p = new Post();
p._state;          // "TRANSIENT"
p.persist();        // forces immediate INSERT
p._state;           // "CLEAN" (persisted, in cache)

p.title = "New";
p._state;          // "MODIFIED"
res.commit();
p._state;           // "CLEAN"
```

The `_state` property exposes the underlying `INodeState` constant:

- `TRANSIENT` — never persisted
- `NEW` — being persisted in current transaction
- `CLEAN` — persisted, no dirty modifications
- `MODIFIED` — modified, awaiting commit
- `DELETED` — pending deletion
- `INVALID` — referenced but missing in DB

## See Also

- [Concepts: Prototypes & Inheritance](../concepts/prototypes.md)
- [Concepts: Object Model](../concepts/object-model.md)
- [Reference: HopObject](../reference/hopobject.md) — complete method list
