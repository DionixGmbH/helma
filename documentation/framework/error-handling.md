# Error Handling

Helma has a built-in two-stage error pipeline: unhandled exceptions dispatch to an **error action** on the application root; missing paths dispatch to a **notfound action**.

## The Default Behaviour

When an action throws an uncaught exception:

1. The transaction is rolled back.
2. The response buffer is reset.
3. `app.errorCount` is incremented.
4. The framework re-runs the request with `error` as the action name on the **root object**.
5. If the error action also fails, a minimal text response is sent: `Application too busy, please try again later` or the bare exception message.

When a path cannot be resolved (or `NotFoundException` is thrown):

1. Response status is set to 404.
2. The framework re-runs the request with `notfound` as the action name on the root object.
3. If `notfound` also fails, the `error` flow kicks in.

## Customising the Error Page

Define `error_action` (or override the action name with `app.properties::error`) on the **Root** prototype:

```javascript
// Root/main.js
function error_action() {
    res.status = 500;
    res.contentType = "text/html";
    res.data.exception = res.error;             // Throwable
    res.data.scriptStack = res.scriptStack;
    res.data.javaStack = res.javaStack;
    renderSkin("error");
}
```

`Root/error.skin`:

```html
<!DOCTYPE html>
<title>Error</title>
<h1>Something went wrong</h1>

<% if response.exception %>
    <h2><% response.exception %></h2>
    <pre><% response.script-stack %></pre>
<% end if %>

<p><a href="/">Back to home</a></p>
```

The `res.error`, `res.scriptStack`, and `res.javaStack` properties are populated by the framework just before invoking the error action.

## Customising the Not-Found Page

```javascript
// Root/main.js
function notfound_action() {
    res.status = 404;
    res.contentType = "text/html";
    res.data.requestedPath = req.path;
    renderSkin("notfound");
}
```

`Root/notfound.skin`:

```html
<!DOCTYPE html>
<title>404 Not Found</title>
<h1>404 Not Found</h1>
<p>Sorry, <code><% response.requested-path %></code> doesn't exist.</p>
```

## Overriding the Action Names

```properties
# app.properties
error = myErrorHandler
notfound = my404Handler
```

Then define `myErrorHandler_action` and `my404Handler_action` on Root.

## Throwing from JavaScript

```javascript
function delete_action_post() {
    if (!this.canBeDeleted()) {
        throw new Error("This post has comments and can't be deleted.");
    }
    this.remove();
}
```

The error message is captured in `res.error.getMessage()` and shown by your error action.

For 404-style errors (path-not-found at some deep level), throw `NotFoundException`:

```javascript
function getChildElement(name) {
    var post = this.findBySlug(name);
    if (!post) throw new Packages.helma.framework.NotFoundException();
    return post;
}
```

## The `res.abort()` Path

`res.abort()`:

- Rolls back the current transaction
- Throws `AbortException` to bail out of the action
- **Does not** reset the response buffer or dispatch to error action

Use this when you want to emit a "Something went wrong" page yourself but ensure DB writes are discarded:

```javascript
function process_action_post() {
    try {
        beginExpensiveWork();
    } catch (e) {
        res.write("<h1>Sorry, this failed.</h1>");
        res.abort();
    }
}
```

## ConcurrencyException Retries

When two requests modify the same Node, the slower one throws `ConcurrencyException`. The framework **retries** the entire request with exponential backoff, up to 7 retries after the first attempt; the 8th conflict gives up.

The retry happens **transparently** — your code may execute multiple times. Make state-changing operations idempotent.

If the 8th attempt also conflicts, the response is `Application too busy, please try again later` with status 500.

## Timeouts

If an action runs longer than `app.properties::requestTimeout` (default 60 seconds), the framework:

1. Kills the request thread
2. Aborts the transaction
3. Emits status 500 with `Request timed out` in the body
4. Logs `Request timeout for thread <name>`

There is no opportunity for an error action to clean up — the thread is killed.

## Logging Errors

Every unhandled exception is logged via `Application.logError()`:

```text
2026-06-01 12:34:56 ERROR [helma.myapp.event] GET:/posts/123 Error: TypeError
java.lang.RuntimeException: TypeError: Cannot read property "foo" of undefined ...
    at helma.scripting.rhino.RhinoEngine.invoke(...)
    ...
```

The log destination is the application's event log — `helma.<appname>.event.log`. See [Logging](logging.md).

Custom logging within your error handler:

```javascript
function error_action() {
    app.log("ERROR", {
        path: req.path,
        method: req.method,
        user: session.user ? session.user.name : null,
        userAgent: req.getHeader("User-Agent"),
        exception: String(res.error),
        scriptStack: res.scriptStack
    });
    // ...
}
```

## Common Error Patterns

### Validation errors → 400

```javascript
function save_action_post() {
    if (!req.postParams.title) {
        res.status = 400;
        res.data.errors = ["Title is required"];
        renderSkin("edit");
        return;
    }
    // ...
}
```

(No exception thrown; just return early.)

### Permission denied → 403

```javascript
function admin_action() {
    if (!session.user || !session.user.isAdmin) {
        res.status = 403;
        renderSkin("forbidden");
        return;
    }
    // ...
}
```

Helma has **no automatic `getPermission` dispatch** — implement the check inline in `onRequest` or at the top of each action. See [Authentication](authentication.md) for patterns.

### Not Found in deep traversal

```javascript
function getChildElement(name) {
    var item = this.findItem(name);
    if (!item) {
        // Helma will convert this into a 404
        throw new Packages.helma.framework.NotFoundException("Item not found: " + name);
    }
    return item;
}
```

### Database errors

`ConcurrencyException` is handled by the framework's retry logic — you usually don't see it in your code.

`SQLException` from raw JDBC bubbles up as a `RuntimeException`. Catch in actions:

```javascript
function save_action_post() {
    try {
        this.title = req.postParams.title;
    } catch (e) {
        app.logError("Failed to save post", e);
        res.message = "Save failed, please retry";
        res.redirect(this.href());
    }
}
```

## See Also

- [Transactions](../concepts/transactions.md) — what gets rolled back on error
- [Logging](logging.md)
- [Reference: `app.properties::error` and `notfound`](../reference/app-properties.md)
