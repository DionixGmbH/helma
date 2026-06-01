# Prototypes & Inheritance

A **prototype** in Helma is the central organising unit. It is at once:

- A **directory** of `.js`, `.skin`, and `.properties` files
- A **JavaScript prototype** in the Rhino scripting engine
- A **DbMapping** (relation to a DB table, if any)
- A **type discriminator** for URL path resolution

Every persistent object (HopObject) has exactly one prototype.

## Built-in Prototypes

Three prototypes are always present, even if their directories are missing:

| Prototype | Role |
|---|---|
| `Root` | Required. The application root object. The URL `/` maps to it. |
| `User` | Optional. The prototype of registered users (`app.registerUser(...)`). Required for the built-in authentication flow. |
| `Global` | Optional. Holds globally-available macros, functions, and the fall-back resolution for unhandled macros. |
| `HopObject` | Built-in. Base prototype of all user-defined prototypes. Cannot be defined as a directory. |

## File-Based Definition

A prototype is defined by a directory inside an application repository:

```
apps/myapp/
├── Root/            ← Root prototype
├── User/            ← User prototype
└── Post/            ← Post prototype
    ├── type.properties
    ├── actions.js
    ├── functions.js
    ├── main.skin
    └── edit.skin
```

The directory name is the prototype name (case sensitive on most filesystems). All `.js` files are concatenated into one JavaScript scope at compile time.

## Type Properties — `type.properties`

The DB mapping for the prototype:

```properties
_db = postgres
_table = posts
_id = post_id
_idgen = [uuid]
_extends = HopObject

title = title
body = content
created = created_at
author.object = User
author.local = author_id
author.foreign = user_id
comments.collection = Comment
comments.local = post_id
comments.foreign = post_id
comments.order = created_at desc
```

See [Type Properties Reference](../database/type-properties.md) for every option.

## Inheritance

Prototype inheritance is declared with `_extends`:

```properties
# Post/type.properties
_extends = Document
```

Inheritance applies to:

- **DB mapping** — child inherits `_table`, `_db`, columns, relations from parent
- **JavaScript prototype** — `Post.prototype` extends `Document.prototype`. A method defined on `Document` is callable on `Post` instances.
- **Skin resolution** — when looking for a skin `main.skin`, Helma checks the prototype's own directory first, then walks up the prototype chain.
- **Macro handler registration** — a `Post` in the request path is registered both as `<% post.* %>` and `<% document.* %>`.

The implicit parent for any user prototype is `HopObject` (`Prototype.setParentPrototype()` at `src/main/java/helma/framework/core/Prototype.java:265`).

## Multi-Repository Composition

A single prototype directory can come from multiple repositories. Each repository contributes its own files, and they are merged. Use cases:

- A core application defines `User` with basic methods
- A plugin repository adds `User/admin.js` with admin-only methods
- The plugin doesn't have to fork the core app

`app.addRepository("path/to/plugin.zip")` adds a repository at runtime. All resources in the new repository immediately become visible to the prototype.

## Resource Types

Inside a prototype directory:

| File | Purpose | Reload |
|---|---|---|
| `*.js` | JavaScript functions | Hot reload — recompile on file mtime change |
| `*.skin` | Skin template | Re-read on file mtime change, parsed lazily |
| `type.properties` | DB mapping | Reread; `DbMapping.update()` rebuilds the relation map |
| `<name>.properties` | Skin meta-properties (rare) | Same as `type.properties` |
| `*.hac` | Helma Action file (legacy) — body is an action function | Reload |
| `*.hsp` | Helma Server Page (legacy) — converted to JS at load | Reload |

`*.hac` and `*.hsp` are kept for backward compatibility. `HacHspConverter` at `src/main/java/helma/scripting/rhino/HacHspConverter.java` handles the conversion.

## JavaScript Conventions Inside a Prototype

Inside `Post/actions.js`:

```javascript
function main_action() {
    renderSkin("Post");
}

function edit_action() {
    if (!session.user) {
        res.redirect("/login");
    }
    if (req.isPost()) {
        this.title = req.postParams.title;
        this.body  = req.postParams.body;
        res.redirect(this.href());
    }
    renderSkin("edit");
}

function delete_action_post() {
    this.remove();
    res.redirect(this._parent.href());
}

function title_macro() {
    res.write(this.title.toUpperCase());
}

function published_filter(value) {
    return value ? "published" : "draft";
}

function onRequest() {
    if (!this.published && !session.user) {
        res.redirect("/login");
    }
}

function onPersist() {
    this.modified = new Date();
}

// Helma has no built-in `getPermission` dispatch; authorisation must be
// implemented inline in onRequest or at the top of each action.
```

Functions ending in:

| Suffix | Role |
|---|---|
| `_action` | URL-routable action |
| `_action_<method>` | Method-specific action (`_post`, `_get`, ...) |
| `_action_ajax` | Only for AJAX requests |
| `_action_ajax_<method>` | AJAX + method |
| `_action_xmlrpc` | XML-RPC handler |
| `_macro` | Skin macro |
| `_filter` | Skin filter (used after `\|`) |

Hook function names (without suffix):

| Name | When invoked |
|---|---|
| `onRequest` | Before the action runs, on `currentElement` |
| `onResponse` | After the action runs, on `currentElement` |
| `onInit` | When a HopObject is reconstituted from the DB |
| `onPersist` | Just before the object is written to the DB |
| `onCodeUpdate` | When this prototype's code has changed and recompiled |
| `onLogout` | On the User HopObject, when `session.logout()` runs or the session times out |
| `getChildElement(name)` | Custom URL-path child lookup. Override to map URL segments to virtual children. |
| `onUnhandledMacro(name)` | Called when a macro `<% this.foo %>` has no corresponding `foo_macro` or property |

Helma does **not** auto-invoke `onLogin` or `getPermission` — those are user conventions, not framework hooks. Authorisation must be implemented explicitly in `onRequest` or at the top of each action.

## Defining Prototypes at Runtime

`app.definePrototype(name, descriptor)` lets you create a prototype from JavaScript, useful for plugins:

```javascript
app.definePrototype("Comment", {
    _db: "main",
    _table: "comments",
    _id: "comment_id",
    text: "text_column",
    author: { object: "User", local: "author_id", foreign: "user_id" }
});
```

This is equivalent to creating `Comment/type.properties` with the same content but does not persist between restarts unless you call it in `Global.js` or similar.

## Naming and Case Sensitivity

By default, prototype names are *case-sensitive*. The `caseInsensitive` setting in `apps.properties`:

```properties
myapp.caseInsensitive = true
```

…makes all property lookups case-insensitive, which is useful with case-folding databases (most MySQL deployments). Prototype names themselves are always lower-cased internally for comparisons (`Prototype.lowerCaseName`).
