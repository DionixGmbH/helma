# Transactions

Each Helma request runs in exactly one transaction, coordinated by `helma.objectmodel.db.Transactor`. This page explains the transaction model.

## What a Transaction Covers

A transaction encompasses:

- All writes to the **embedded XML database** during the request
- All writes via JDBC to any `DbSource` accessed during the request — each `DbSource` contributes its own JDBC `Connection` with auto-commit OFF
- The dirty-node tracking for in-memory caching

When the request handler returns normally, `Transactor.commit()` runs and:

1. Flushes dirty embedded-DB nodes to disk via `XmlDatabase.commit()`
2. Calls `commit()` on every JDBC connection acquired during the request
3. Fires `NodeChangeListener` events for added/modified/removed nodes
4. Releases connections back to their pools

On an unhandled exception, `Transactor.abort()` runs:

1. Discards in-memory changes to embedded-DB nodes
2. Calls `rollback()` on every JDBC connection
3. Reverts cached nodes to their pre-request state
4. Releases connections

## Lifetime

A `Transactor` is a `ThreadLocal` — one per evaluator thread. `Transactor.begin(txname)` starts a transaction; `commit()` or `abort()` ends it. Inside a request, the framework calls these for you; you usually never touch them directly.

If you need to commit mid-request (e.g. to release locks for a long-running cron job), use `res.commit()`:

```javascript
function bigJob() {
    for (var i = 0; i < 10000; i++) {
        processItem(i);
        if (i % 100 === 0) {
            res.commit();      // commits current tx, starts new one
        }
    }
}
```

Similarly `res.rollback()` aborts the current tx and starts a fresh one.

## ConcurrencyException Retries

Helma uses optimistic concurrency control. When two requests write to the same Node simultaneously, the slower one throws `ConcurrencyException`. The framework catches this and **retries** the entire request with exponential backoff. The retry loop guards on `++tries < 8`, so the request runs **8 times maximum** — the initial try plus up to 7 retries; the 8th conflict gives up.

```text
attempt 1: initial try (tries=0)
  conflict → tries becomes 1; 1 < 8 → wait, retry
attempt 2: retry (tries=1), wait ~800-2400ms
...
attempt 8: final retry (tries=7), wait ~5600-16800ms
  conflict → tries becomes 8; 8 < 8 is false → give up,
              emit "Application too busy, please try again later"
```

(see `RequestEvaluator.run()` at `src/main/java/helma/framework/core/RequestEvaluator.java:553`.)

Implications:

- An action handler is **not** guaranteed to run only once. Make it idempotent.
- Side effects outside the transaction (sending email, calling external APIs) may happen N times. Either guard them with a "have I done this already" check or move them to `onPersist` which still runs on the final successful attempt.

## res.abort()

Calling `res.abort()` throws an `AbortException`. The transactor rolls back, but the response is *not* reset — whatever you've already `res.write()`-ten is sent to the client. Use this to abort the DB transaction while still showing a "Something went wrong" page.

```javascript
function risky_action() {
    try {
        doRiskyWork();
    } catch (e) {
        res.write("<h1>Operation failed</h1>");
        res.abort();           // rollback DB, send response
    }
}
```

For a complete request termination (no commit, no response), throw an exception from your code without catching it.

## res.stop() / Redirects

`res.redirect()` and `res.stop()` throw a `RedirectException`. The framework treats this specially:

- The DB transaction **is committed** (redirects are normal completion)
- The current response buffer is replaced with the redirect/empty response
- Any `res.message` set is preserved across the redirect via `session.storeResponseMessages()`

## Internal Invocation

Functions invoked via `app.invoke()` (synchronous) or `app.invokeAsync()` (background) run in their own transactor:

```javascript
// Synchronous - runs in a fresh transaction
var result = app.invoke(null, "Root.computeStats", []);

// Async - runs in a new RequestEvaluator
var future = app.invokeAsync(null, "Root.sendDailyDigests", []);
future.waitForResult(60000);
```

Each such invocation grabs an evaluator from the pool and starts a fresh transaction. They do **not** participate in the current HTTP request's transaction.

## Cron Jobs

Cron jobs are also internal invocations — see `Application.executeCronJobs()`. Each registered job:

1. Acquires a RequestEvaluator
2. Starts a transaction
3. Calls the configured function
4. Commits or rolls back as usual

If a job throws, it's logged and the next iteration runs normally.

## Persisting Mid-Request

`node.persist()` forces a Node's representation to be written to the database **immediately** within the current transaction. The change is still part of the transaction — if the request aborts, the persisted state will roll back along with everything else.

Why use it?

- You need the auto-generated ID before the transaction commits (e.g. to use in a redirect URL)
- You need the row to be visible to a child INSERT statement issued via raw JDBC in the same transaction

```javascript
var post = new Post();
post.title = req.postParams.title;
this.add(post);
post.persist();              // post._id is now available
res.redirect(post.href());
```

## Cross-DbSource Transactions

When your prototypes are spread across multiple `DbSource`s, each contributes its own JDBC connection to the transaction. **Helma does not perform a distributed transaction (2PC) across them** — it commits each connection sequentially. If the third connection's `commit()` fails after the first two succeeded, the first two are already committed.

For strong cross-database consistency, use one DbSource for everything that must commit atomically.

## Inspecting the Active Transaction

```javascript
// From JavaScript - via raw Java
var tx = Packages.helma.objectmodel.db.Transactor.getInstance();
tx.getTransactionName();       // current tx name
tx.isActive();
```

## Transaction Lifecycle Hooks

There is **no** `onCommit` hook on individual HopObjects. The closest you get is:

- `onPersist()` on the HopObject — runs *immediately before* the node is written, inside the transaction
- Inside `onPersist`, modify properties to ensure correct on-disk state (`this.modified = new Date()`)

If you need post-commit behaviour (e.g. clear an external cache after the row is durable), do the work *after* the action returns successfully and you've already called `res.commit()`. There is no "did the commit actually succeed" callback because by definition you can't observe failure of the commit from within the request.

## Auto-Commit?

By default, Helma sets auto-commit `OFF` on every JDBC connection it acquires. If you grab a raw connection via `getDBConnection("foo")` and want auto-commit behaviour:

```javascript
var db = getDBConnection("foo");
db.connection.setAutoCommit(true);
```

But beware: this bypasses Helma's transaction coordination and writes will not roll back on action failure.

## Summary

- One request = one transaction (modulo `res.commit()`)
- Concurrent writes to the same Node → automatic retry up to 8 times
- `res.abort()` rolls back without resetting the response
- Make actions idempotent — they can run multiple times due to retry
- Multi-DbSource is not 2PC — commits are sequential
