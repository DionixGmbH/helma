# Session Bean (`session`)

`session` is the per-user session — a `SessionBean` wrapping the underlying `helma.framework.core.Session`. Defined in `src/main/java/helma/framework/core/SessionBean.java`.

## Identity

### `session.cookie` / `session._id` (String, read-only)

The session ID — the value of the session cookie.

## Authentication

### `session.login(username, password)` (boolean)

Look up the User HopObject by `username`, read its stored `password` property, and compare to the supplied password using **plaintext string equality** (no hashing). On match, attach the user to the session. Returns `true` on success.

Helma does **not** invoke any `onLogin` hook — there is no such call site in the framework.

!!! warning "Plaintext comparison"
    The built-in flow stores and compares passwords as plaintext. For real users, use a hashed-password scheme — see [Authentication](../framework/authentication.md#custom-authentication).

```javascript
if (session.login(req.postParams.user, req.postParams.pwd)) {
    res.redirect("/");
}
```

### `session.login(userNode)`

Attach a User HopObject directly to the session without credential checking. For custom auth flows.

```javascript
var u = lookupUserByEmail(req.postParams.email);
if (u && u.verifyPassword(req.postParams.pwd)) {
    session.login(u);
}
```

### `session.logout()`

Detach the user from the session. Invokes `User.onLogout(sessionId)` if defined.

## User

### `session.user` (INode, read-only)

The current logged-in User HopObject, or `null` if anonymous.

## State

### `session.data` (INode, read-only)

A transient cache node for session-scoped storage. Properties persist for the lifetime of the session (and across restarts if `persistentSessions = true`).

```javascript
session.data.cart = session.data.cart || [];
session.data.cart.push({ sku: ..., qty: ... });

session.data.lastVisited = req.path;
```

### `session.message` (String, read-write)

A message that survives one redirect. Useful for flash notifications.

```javascript
session.message = "Saved!";
res.redirect("/profile");

// In the next request:
res.write(session.message);     // "Saved!"
// session.message is now null
```

## Timing

### `session.onSince` (Date, read-only)

When this session was created.

### `session.lastActive` (Date, read-only)

When this session was last touched.

### `session.lastModified` (Date, read-only)

When this session was last modified (login/logout, data change).

### `session.touch()`

Update `lastActive` to now. Used to keep the session alive when the user isn't making explicit requests.

## File Uploads

### `session.getUploadStatus(uploadId)` (UploadStatus)

Get an `UploadStatus` object for an in-flight upload. The client passes `?uploadId=...` and polls a separate endpoint to track progress.

## Example: Login flow

```javascript
function login_action_post() {
    if (session.login(req.postParams.user, req.postParams.pwd)) {
        session.touch();
        session.data.welcome = "Welcome back, " + session.user.name;
        res.redirect("/");
    } else {
        res.message = "Invalid credentials";
        renderSkin("login");
    }
}

function logout_action() {
    if (session.user) {
        app.log("User " + session.user.name + " logged out");
    }
    session.logout();
    res.unsetCookie("HopSession");
    res.redirect("/");
}

function profile_action() {
    if (!session.user) {
        res.redirect("/login");
    }
    res.data.user = session.user;
    res.data.welcome = session.data.welcome;
    renderSkin("profile");
}
```

## Example: Shopping cart

```javascript
function addToCart_action_post() {
    var cart = session.data.cart || (session.data.cart = []);
    cart.push({
        sku: req.postParams.sku,
        qty: parseInt(req.postParams.qty || "1", 10)
    });
    res.message = "Added to cart";
    res.redirect(req.getHeader("Referer") || "/");
}

function cart_action() {
    res.data.cart = session.data.cart || [];
    renderSkin("cart");
}

function checkout_action_post() {
    if (!session.user) {
        res.redirect("/login");
    }
    var cart = session.data.cart || [];
    var order = new Order();
    order.user = session.user;
    order.items = cart;
    root.orders.add(order);

    delete session.data.cart;
    res.redirect(order.href());
}
```

## See Also

- [Framework: Authentication](../framework/authentication.md)
- [Concepts: Sessions & Users](../concepts/sessions-and-users.md)
- [`SessionBean.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/framework/core/SessionBean.java) — source
