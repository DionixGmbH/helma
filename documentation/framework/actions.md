# Actions

An **action** is a JavaScript function on a prototype that responds to an HTTP request. The function name follows a convention that the framework parses to dispatch the request.

## Naming Convention

| Function name | Matches |
|---|---|
| `main_action` | Any GET, POST, or HEAD request when the URL ends with `/<this>`, or when no specific action segment is present |
| `foo_action` | Same, but for URLs ending with `/foo` |
| `foo_action_get` | GET-only |
| `foo_action_post` | POST-only |
| `foo_action_put` | PUT-only |
| `foo_action_delete` | DELETE-only |
| `foo_action_head` | HEAD-only |
| `foo_action_options` | OPTIONS-only |
| `foo_action_trace` | TRACE-only |
| `foo_action_ajax` | Any AJAX request (`X-Requested-With: XMLHttpRequest`) |
| `foo_action_ajax_get` | AJAX GET only |
| `foo_action_ajax_post` | AJAX POST only |
| `foo_action_xmlrpc` | XML-RPC request to method `foo` |

The base action name (`foo`) becomes `req.action` regardless of which variant matched.

## Action Resolution Algorithm

Helma resolves an action by walking the URL path, then determining which function to invoke on the final object. The algorithm — implemented in `RequestEvaluator.getAction()` — is:

```text
1. Walk URL segments. Each segment ChildElement-lookup on the current object.
2. The LAST segment, if URL does not end with /, is a candidate action name.
   - If a matching action function exists on the final object: that's the action.
   - Otherwise the segment is also treated as a child name.
3. If no candidate action found, try the implicit "main" action.
4. If still nothing: 404.
```

For action variant selection (once the action name is known):

```text
Given action name "foo" and HTTP method "POST":

If XML-RPC request:
    1. foo_action_xmlrpc
    (no fall-through)

Else if AJAX request:
    1. foo_action_ajax_<method>
    2. foo_action_ajax
    3. foo_action_<method>
    4. foo_action  (only if method is GET, POST, or HEAD)

Else:
    1. foo_action_<method>
    2. foo_action  (only if method is GET, POST, or HEAD)
```

The `foo_action` fallback is **not** tried for PUT, DELETE, etc. — for those, you must define a method-specific handler.

See [AJAX Action Resolution](ajax-actions.md) for the deep dive on AJAX vs non-AJAX dispatch.

## Examples

### Basic action

```javascript
// Root/main.js
function main_action() {
    renderSkin("Root");
}
```

URL: `GET /` → renders `Root/main.skin`.

### Method-specific action

```javascript
// Post/edit.js
function edit_action_get() {
    renderSkin("edit");
}

function edit_action_post() {
    this.title = req.postParams.title;
    this.body  = req.postParams.body;
    res.redirect(this.href());
}
```

URL: `GET /post-id/edit` → shows the form. `POST /post-id/edit` → saves and redirects.

### AJAX-specific action

```javascript
// Comment/main.js
function main_action() {
    renderSkin("Comment");          // full HTML page
}

function main_action_ajax() {
    res.contentType = "application/json";
    res.write(JSON.stringify({
        text: this.text,
        author: this.author.name,
        created: this.created.getTime()
    }));
}
```

Browser request: HTML page. `$.ajax({ url: "/comments/42" })`: JSON.

### RESTful resource

```javascript
// Post/api.js
function api_action_get() {
    res.contentType = "application/json";
    res.write(JSON.stringify(this.toJSON()));
}

function api_action_put() {
    var body = JSON.parse(req.data.body);
    this.title = body.title;
    this.body = body.body;
    res.contentType = "application/json";
    res.write(JSON.stringify({ ok: true }));
}

function api_action_delete() {
    this.remove();
    res.status = 204;
}
```

## Action Lifecycle

Per request, on the final object:

1. `onRequest()` — runs before the action. Use to check auth, setup state, override the action via `req.actionHandler`.
2. *action function* — your code.
3. `onResponse()` — runs after the action. Use to inject debug bars, finalise cookies.

`onRequest` and `onResponse` are optional. If they throw, the request bails to the error handler.

### Overriding the action from onRequest

```javascript
function onRequest() {
    if (!session.user && !["login","signup"].includes(req.action)) {
        req.actionHandler = login_action;     // run login instead
    }
}
```

The action resolved from the URL is overridden by `req.actionHandler` if set.

## Authorisation

Helma has **no built-in `getPermission()` dispatch hook**. The framework's action lookup does not consult any function named `getPermission`. Implement authorisation explicitly in `onRequest` or at the top of each action:

```javascript
function onRequest() {
    if (req.action === "delete" && session.user !== this.author) {
        res.status = 403;
        renderSkin("forbidden");
        res.stop();
    }
}
```

## Accessing Path Objects

Inside an action, `this` is the object on which the action was invoked. To reach other path objects:

```javascript
// URL: /users/alice/photos/holiday/show
// Path: root → users → alice → photos → holiday
// Action: show_action on `holiday`

function show_action() {
    var holiday = this;
    var photos  = this._parent;
    var alice   = this._parent._parent;
    var root    = path[0];

    // or via the path wrapper:
    var alice2  = path[2];
    var alice3  = path["users"] ? path["alice"] : null;  // not supported, use indices
}
```

`path` is the `RequestPath` — an array-like of HopObjects walked during resolution.

## Path-Index Aliases

Inside actions and skins, the request path objects are *also* registered as macro handlers by their prototype name:

```html
<!-- inside any skin during a request to /users/alice/photos/holiday -->
<% user.name %>          → alice's name
<% photos.size %>        → number of photos
<% holiday.title %>      → holiday's title
```

`Prototype.registerParents()` additionally registers each object under its parent prototype names so a `BlogPost extends Post` is reachable as `<% post.* %>` too.

## Returning Output

The convention: actions don't return values. Output is built by:

- `res.write(s)` / `res.writeln(s)` — append to the response buffer
- `res.writeBinary(bytes)` — replace the response with a binary payload
- `res.encode(s)` — HTML-escape and write
- `renderSkin(name, params)` — render a skin to the buffer
- `res.redirect(url)` — send a 302 redirect (terminates the action)
- `res.forward(url)` — internal forward (terminates the action)

For XML-RPC and `app.invoke()` invocations, the action's **return value** is the response.

## Action Functions in Different Files

All `.js` files in a prototype directory are concatenated and compiled together. There's no need to split actions and other functions; a common organisation is:

```
Post/
├── actions.js          ← *_action functions
├── macros.js           ← *_macro functions
├── functions.js        ← regular methods
└── lifecycle.js        ← onRequest, onResponse, onPersist, ...
```

…but it's purely convention. Helma treats all `.js` files identically.

## Legacy: `.hac` and `.hsp` Files

For backward compatibility:

- `.hac` files (Helma Action) — the file body is treated as the action function. Filename `foo.hac` → defines `foo_action`. See `HacHspConverter.java`.
- `.hsp` files (Helma Server Page) — like a JSP. The file body is converted to JS at load time. Use modern `.js` + `.skin` for new code.

Both extensions are still recognised by `TypeManager` and produce equivalent compiled output.
