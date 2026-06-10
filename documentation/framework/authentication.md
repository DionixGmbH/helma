# Authentication

Helma ships with a simple session-based authentication mechanism tied to the `User` prototype. It performs **plaintext password comparison** out of the box — for any production system you should implement your own hashed-password flow.

## Built-in Authentication (Plaintext)

### Prerequisites

1. A `User` prototype with a `password` property (mapped or transient — see [Type Properties](../database/type-properties.md)).
2. The password property stores the password as supplied — Helma **does not hash it**.

```properties
# User/type.properties
_db = main
_table = users
_id = user_id
_name = username

password = pwd
email = email
```

### Registering a user

```javascript
function signup_action_post() {
    var u = app.registerUser(req.postParams.user, req.postParams.pwd);
    if (u) {
        session.login(u);
        res.redirect("/");
    } else {
        res.message = "Username taken";
    }
}
```

`app.registerUser(username, password)`:

- Stores `password` **verbatim as a string property on the User node** (see `Application.registerUser()` in `src/main/java/helma/framework/core/Application.java`)
- Creates a new User HopObject with `name = username`
- Returns the new User node, or `null` if the username is already taken

The username uniqueness check is by `getElementName()` — the `_name` column or the auto-name from `_idgen=[hop]`.

!!! warning "Plaintext storage"
    Helma's built-in `registerUser` and `session.login(user, pwd)` use **plaintext password comparison**. For any system handling real users, do not rely on this. See [Custom Authentication](#custom-authentication) below.

### Logging in

```javascript
function login_action_post() {
    if (session.login(req.postParams.user, req.postParams.pwd)) {
        res.redirect("/");
    } else {
        res.message = "Invalid credentials";
    }
}
```

`session.login(user, pwd)`:

1. Looks up `User` HopObject named `<user>`
2. Reads the stored `password` property
3. Compares `storedPassword.equals(suppliedPassword)` (plaintext string compare)
4. On match: attaches the user to the session
5. Returns `true` on success, `false` on failure

There is **no `onLogin` hook** invoked by the framework — Helma's source has no such call site. (`onLogout` does exist; see below.)

### Logging out

```javascript
function logout_action() {
    session.logout();
    res.redirect("/");
}
```

`session.logout()`:

1. If a request evaluator is active, invokes `User.onLogout(sessionId)` on the user node if defined (`Session.java` calls `reval.invokeDirectFunction(userNode, "onLogout", ...)`)
2. Detaches the user from the session
3. Marks session modified

The cookie itself is **not** removed. The session keeps existing but is anonymous again. To remove the cookie, call `res.unsetCookie("HopSession")`.

### The `passwd` File and `authenticate()`

Separate from the `session.login` flow, Helma supports a server-wide or app-local `passwd` file for HTTP Basic-style authentication via the global `authenticate(user, pwd)` function:

```text
# helma-home/passwd
admin:<hash>
deploy:<hash>
```

```javascript
if (authenticate(req.username, req.password)) {
    // grant access
} else {
    res.realm = "Admin Area";
    res.status = 401;
}
```

`authenticate()` reads the `passwd` file (cached, reloaded on mtime change) and calls `CryptResource.authenticate()`. The implementation in `src/main/java/helma/util/CryptResource.java`:

1. First tries Unix DES-style `crypt` against the stored hash
2. If that fails, tries MD5 against the stored hash

There is **no `passwordEncoding` switch** — both formats are accepted automatically. To create a hashed entry, use Unix `crypt(3)` or compute MD5 of the password yourself.

## Custom Authentication

For any system with real users, build your own auth using a modern password hash (bcrypt/scrypt/Argon2). Helma's `session.login(userNode)` lets you attach a User HopObject after your own credential check.

### Bcrypt Example

Use jBCrypt — drop the JAR into `apps/myapp/lib/`:

```javascript
// User/auth.js
var BCrypt = Packages.org.mindrot.jbcrypt.BCrypt;

User.prototype.setPassword = function(plain) {
    this.password_hash = BCrypt.hashpw(plain, BCrypt.gensalt(12));
};

User.prototype.verifyPassword = function(plain) {
    return BCrypt.checkpw(plain, this.password_hash);
};

// Root/auth.js
function login_action_post() {
    var u = root.users.getByEmail(req.postParams.email);
    if (u && u.verifyPassword(req.postParams.pwd)) {
        session.login(u);              // attach user to session
        res.redirect("/");
    } else {
        res.message = "Invalid credentials";
    }
}
```

### Stay-Logged-In Cookies

```javascript
function login_action_post() {
    var u = lookupUser(req.postParams.email);
    if (u && u.verifyPassword(req.postParams.pwd)) {
        session.login(u);
        if (req.postParams.rememberMe) {
            var token = generateRandomToken();
            u.persistentLoginTokenHash = hashToken(token);
            res.setCookie("rememberme", u._id + ":" + token, 30, "/");
        }
        res.redirect("/");
    }
}

function onRequest() {
    if (!session.user) {
        var cookie = req.cookies.rememberme;
        if (cookie) {
            var [id, token] = cookie.getValue().split(":");
            var u = root.users.get(id);
            if (u && verifyTokenHash(token, u.persistentLoginTokenHash)) {
                session.login(u);
            }
        }
    }
}
```

## Authorisation: Implement Your Own

Helma has **no built-in `getPermission()` dispatch hook**. The framework's action lookup (`RequestEvaluator.getAction()`) does not consult any `getPermission` function on the prototype. If you want authorisation gating, implement it explicitly in `onRequest` or at the top of each action:

```javascript
// Post/main.js
function onRequest() {
    if (!session.user) {
        res.redirect("/login");
    }
    if (req.action === "delete" && session.user !== this.author) {
        res.status = 403;
        renderSkin("forbidden");
        res.stop();
    }
}
```

A common project convention is to define helper methods like `Post.prototype.canEdit = function(user) { ... }` and call them from each action — but this is convention, not framework behaviour.

## HTTP Basic Auth

```javascript
function onRequest() {
    if (!session.user) {
        var auth = req.getHeader("Authorization");
        if (auth) {
            var parts = auth.split(" ");
            if (parts[0] === "Basic") {
                var decoded = String(new java.lang.String(
                    Packages.org.apache.commons.codec.binary.Base64.decodeBase64(parts[1])));
                var [user, pwd] = decoded.split(":");
                if (session.login(user, pwd)) {     // plaintext built-in
                    return;
                }
            }
        }
        res.realm = "MyApp";
        res.status = 401;
        res.write("Unauthorized");
        return;
    }
}
```

Setting `res.realm` and `res.status = 401` makes Helma emit a `WWW-Authenticate: Basic realm="..."` header.

## CSRF Protection

Helma does not bundle CSRF protection. Common pattern:

```javascript
function ensureCsrf() {
    if (!session.data.csrf) {
        var b = new java.lang.StringBuilder();
        for (var i = 0; i < 32; i++) {
            b.append((Math.random() * 36 | 0).toString(36));
        }
        session.data.csrf = b.toString();
    }
    return session.data.csrf;
}

function csrf_macro() {
    res.write(ensureCsrf());
}

function delete_action_post() {
    if (req.postParams.csrf !== session.data.csrf) {
        res.status = 403;
        return;
    }
    this.remove();
}
```

[Jala](../modules/jala.md)'s `jala.Form` handles CSRF automatically.

## Securing the Management App

The bundled `manage` app does **not** ship with default credentials. Configure it via the manage app's own admin UI on first access. For production, put it behind a reverse proxy with IP-based access rules — Helma does **not** have a built-in HTTP-level `allowAdmin` setting or any HTTP IP allowlist.

Or remove the bare `manage` line from `apps.properties` entirely to disable the management app.

## Best Practices

- **Never use the built-in plaintext `session.login(user, pwd)` for real users.** Use bcrypt or similar.
- Always serve over HTTPS. Set `cookies.secure = true` in `app.properties`.
- Set a short `sessionTimeout` for sensitive apps.
- Implement CSRF tokens on every state-changing action.
- Don't rely on hiding UI elements as access control — gate the action functions.
- Log login/logout events: `onLogout` runs natively; log logins explicitly in your action.
- Rate-limit login attempts (Helma has no built-in rate limiter; use a reverse proxy or `app.data`).
