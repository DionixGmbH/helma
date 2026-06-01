# Sessions & Users

Helma has built-in session support and a simple username/password login flow tied to a HopObject called the **user**.

## What is a Session?

A `Session` (`src/main/java/helma/framework/core/Session.java`) is the server-side state associated with one HTTP client. It exists for the lifetime of one browser session — bounded by:

- Inactivity timeout (`sessionTimeout` in `app.properties`, default 30 minutes)
- Explicit `session.logout()` from JavaScript
- Cookie eviction on the client side

Sessions are identified by a cookie. The default cookie name is `HopSession`; override with `sessionCookieName` in `apps.properties` per-application.

```javascript
session.cookie             // the session id
session.user               // the logged-in user HopObject, or null
session.data               // a transient INode for session-scoped storage
session.message            // string that survives one redirect
session.onSince            // Date the session was created
session.lastActive         // Date the session was last touched
```

## Lazy Registration

Anonymous sessions are **not** stored in the `SessionManager` until they actually modify state. So an anonymous browse session that never logs in or touches `session.data` consumes no memory beyond the transient cache node held during the request.

The moment something modifies the session (`session.login()`, `session.data.foo = ...`, or assigning to `session.message`), `session.commit()` registers it in the session manager.

## Logging In

Two ways:

### 1. Built-in user mapping (plaintext)

Define a `User` prototype with a `password` property. **Helma stores and compares passwords as plaintext** — see the auth chapter for the production-ready alternative.

```javascript
function login_action() {
    if (req.isPost()) {
        if (session.login(req.postParams.user, req.postParams.pwd)) {
            res.redirect("/dashboard");
        } else {
            res.message = "Invalid credentials";
        }
    }
    renderSkin("login");
}
```

`session.login(user, pwd)` looks up the User HopObject by name, reads the stored `password` property, and compares it directly to the submitted password (no hashing). On success it attaches the user to the session.

!!! warning
    The built-in flow is fine for prototypes and toy apps. For real users use [bcrypt-based custom auth](../framework/authentication.md#custom-authentication).

### 2. Custom auth — attach a user manually

For modern auth (bcrypt, OAuth, OIDC, MFA), do your own credential check then call the variant that takes a HopObject directly:

```javascript
function login_action() {
    var u = root.users.getByEmail(req.postParams.email);
    if (u && bcrypt.verify(req.postParams.pwd, u.password_hash)) {
        session.login(u);              // attach user to session
        res.redirect("/dashboard");
    }
    ...
}
```

`session.login(node)` skips credential checking and just associates the user with the session.

## Logging Out

```javascript
function logout_action() {
    session.logout();
    res.redirect("/");
}
```

`session.logout()`:

1. Invokes `onLogout(sessionId)` on the User HopObject if defined (this is the **only** lifecycle hook the framework dispatches for logins/logouts)
2. Detaches the user from the session
3. Marks session modified

The cookie itself is **not** removed. The session keeps existing but is anonymous again. To remove the cookie, call `res.unsetCookie("HopSession")`.

## Lifecycle Hooks

The framework's only user-related lifecycle hook is `onLogout`. There is **no built-in `onLogin` hook** — the source code does not call any function named `onLogin` when `session.login()` runs. If you want login auditing, do it explicitly:

```javascript
// In your login flow
function login_action_post() {
    if (session.login(req.postParams.user, req.postParams.pwd)) {
        session.user.lastLogin = new Date();
        app.log("User " + session.user.name + " logged in");
        res.redirect("/");
    }
}
```

### `onLogout(sessionId)`

Defined on the **User** prototype. Called when:

- `session.logout()` is invoked from JavaScript with a request evaluator active, OR
- A session expires due to `sessionTimeout` (the cleanup thread also invokes `onLogout` for expired logged-in sessions)

Both code paths are in `src/main/java/helma/framework/core/Session.java` and `SessionManager.java`. Exceptions thrown by `onLogout` are caught and logged.

## Auto-Logout on Timeout

The application worker thread runs `SessionManager.cleanupSessions()` every 60 seconds:

```text
for each session:
    if now - session.lastTouched > sessionTimeout * 60_000:
        if session has user:
            invoke User.onLogout(sessionId)
        discard session
```

The session is removed from the in-memory map and (effectively) the user is logged out.

## Active Users

`app.getActiveUsers()` returns the array of User HopObjects with at least one active session. Useful for "who's online" widgets:

```javascript
function online_macro() {
    for each (var u in app.getActiveUsers()) {
        res.write(u.name + " ");
    }
}
```

`app.getSessionsForUser(userNode)` returns the array of sessions for a particular user.

## Persisting Sessions Across Restart

Sessions live in memory. If Helma restarts, all sessions are gone — unless you enable persistence:

```properties
# app.properties
persistentSessions = true     # write sessions to db/<app>/sessions on shutdown
```

On startup, `SessionManager.loadSessionData()` reads the serialized sessions and restores them. Note that the session's transient `cache` node is also serialized — everything you put in `session.data` must be `Serializable`. (Helma objects, HopObjects, primitive types, and most JS objects via `ScriptableOutputStream` are OK.)

## Authorisation

Helma has **no built-in authorisation dispatch hook**. Specifically, there is no `getPermission` function consulted automatically by the framework before actions or macros — that pattern was documented historically but is not implemented in this codebase.

Implement authorisation in `onRequest` or at the top of each action:

```javascript
// Post/main.js
function onRequest() {
    if (req.action === "edit" || req.action === "delete") {
        if (!session.user || session.user !== this.author) {
            res.status = 403;
            renderSkin("forbidden");
            res.stop();
        }
    }
}
```

## Session Data Storage

`session.data` is a transient HopObject. Properties on it are stored in memory and lost on logout/timeout (unless `persistentSessions = true`).

Use it for:

- Shopping carts (write to DB only on checkout)
- Multi-step wizard state
- "Recently viewed" lists
- Per-user CSRF tokens

```javascript
function cart_action() {
    var cart = session.data.cart || (session.data.cart = []);
    if (req.isPost()) {
        cart.push({ sku: req.postParams.sku, qty: req.postParams.qty });
    }
    renderSkin("cart", { cart: cart });
}
```

## CSRF Considerations

Helma does not bundle CSRF protection. Common pattern shown in [Authentication](../framework/authentication.md#csrf-protection). The Jala library bundles a more featureful `jala.Form` that handles CSRF automatically.
