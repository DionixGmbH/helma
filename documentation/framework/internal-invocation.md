# Internal Invocation

`app.invoke()` and `app.invokeAsync()` let you call a JavaScript function from outside an HTTP request — from a cron job, from another invocation, or from a thread that holds a reference to the `Application` object.

## Synchronous: `app.invoke()`

```javascript
var result = app.invoke(thisObj, functionOrName, args);
var result = app.invoke(thisObj, functionOrName, args, timeoutMillis);
```

Parameters:

- `thisObj` — the object to use as `this` in the function call. Pass `null` for global functions.
- `functionOrName` — a function reference or a string `"prototype.functionname"` or `"functionname"`.
- `args` — an `Array` of arguments.
- `timeoutMillis` — milliseconds before aborting. Default 30000 (30 seconds).

### Examples

```javascript
// Invoke a global function
var sum = app.invoke(null, "calculate", [1, 2]);

// Invoke a method on a HopObject
var n = app.invoke(post, "wordCount", []);

// Invoke with custom timeout
var result = app.invoke(null, "longRunningTask", [], 600000);   // 10 min

// Method with explicit prototype:
var profile = app.invoke(null, "User.getProfile", [userId]);
```

`app.invoke()` runs the function on a fresh evaluator from the pool, inside a fresh transaction. The caller blocks until the function returns or the timeout expires. If the timeout expires, the function's thread is killed and the call throws `TimeoutException`.

## Asynchronous: `app.invokeAsync()`

```javascript
var future = app.invokeAsync(thisObj, functionOrName, args);
var future = app.invokeAsync(thisObj, functionOrName, args, timeoutMillis);
```

Returns a `FutureResult` object — a handle to the running invocation. Default timeout 15 minutes (`60000L * 15`).

### FutureResult API

```javascript
future.running               // true while the function is running
future.result                // value returned by the function (after completion)
future.exception             // exception thrown (after failure)
future.waitForResult()       // block until completion, return result
future.waitForResult(ms)     // block up to `ms` milliseconds
```

### Examples

```javascript
// Fire-and-forget
app.invokeAsync(null, "sendNotificationEmails", []);

// Wait for completion
var future = app.invokeAsync(null, "buildReport", []);
// ... do other work ...
var report = future.waitForResult();      // blocks until done

// Wait with timeout
var report = future.waitForResult(5000);
if (future.running) {
    res.write("Report still building, refresh in a moment...");
} else {
    res.write(report);
}

// Polling
var future = app.invokeAsync(null, "heavyTask", []);
while (future.running) {
    java.lang.Thread.sleep(100);
    // emit progress, etc.
}
res.write(future.result);
```

## What Happens Under the Hood

Both `invoke` and `invokeAsync`:

1. Grab a free RequestEvaluator from the pool.
2. Set `reqtype = EXTERNAL` (no HTTP path resolution).
3. Start a transaction.
4. Enter the Rhino scripting context.
5. Call the function.
6. Commit or roll back the transaction based on whether the function threw.
7. Release the evaluator back to the pool.

The invocation has **no `req`, no `res`, no `session`**. You're effectively running outside the request lifecycle.

## Common Use Cases

### Triggering a background email

```javascript
function someAction() {
    saveOrder();
    res.message = "Order saved";
    app.invokeAsync(null, "sendOrderConfirmation", [orderId]);
    res.redirect(this.href());
}

function sendOrderConfirmation(orderId) {
    var order = root.orders.get(orderId);
    // ... send email via helma.Mail
}
```

The email is sent in the background while the user is redirected.

### Multi-step wizard with intermediate persistence

```javascript
function step3_action_post() {
    // step1 and step2 data is in session.data
    var data = Object.assign({}, session.data.wizard, req.postParams);
    // commit immediately so other requests see the row
    var newPost = app.invoke(root, "createPost", [data], 5000);
    res.redirect(newPost.href());
}

function createPost(data) {
    var p = new Post();
    p.title = data.title;
    p.body  = data.body;
    root.posts.add(p);
    return p;
}
```

### Cron-style timing without cron.properties

```javascript
function trigger_action() {
    app.invokeAsync(null, "expensiveTask", []);
    res.write("Started.");
}

function expensiveTask() {
    // long-running work; runs in its own transaction
}
```

## Important Caveats

### No request context

You can't access `req`, `res`, or `session` inside an invoked function — they're `null`. If you need to pass HTTP-derived data, pass it through arguments.

### Separate transaction

The invoked function runs in **its own** transaction. Changes are not visible to the caller until commit. Pass IDs to look up freshly committed rows.

### Pool exhaustion

`app.invoke` competes with HTTP requests for evaluators. If all evaluators are busy, the call blocks. Set `maxThreads` high enough to leave headroom for invocations.

### Exception handling

```javascript
try {
    var x = app.invoke(null, "mayFail", []);
} catch (e) {
    app.logError("Sub-invocation failed", e);
}
```

For async:

```javascript
var f = app.invokeAsync(null, "mayFail", []);
f.waitForResult();
if (f.exception) {
    app.logError("Async invocation failed", f.exception);
}
```

### Timeout behaviour

When `invoke()` times out, the function's worker thread is killed via `RequestEvaluator.stopTransactor()`. This:

1. Aborts the in-progress transaction.
2. Releases JDBC connections.
3. Returns to the pool.

The function does not get a chance to clean up. If you're holding external resources, use a `try/finally` block that catches `InterruptedException` and the various abort signals.

## See Also

- [Cron Jobs](cron-jobs.md) — scheduled invocation
- [Transactions](../concepts/transactions.md) — invocation transaction model
- [Reference: `app.invoke()` and `app.invokeAsync()`](../reference/app-bean.md#invocation)
