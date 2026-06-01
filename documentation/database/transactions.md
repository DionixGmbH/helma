# Transactions

This page covers database-specific transaction semantics. For the framework-level transaction lifecycle, see [Concepts: Transactions](../concepts/transactions.md).

## One Request, One Transaction

By default each HTTP request runs in exactly one `Transactor` instance:

1. `Transactor.begin(txname)` — called at request start
2. Your action executes — DB writes accumulate in the transactor
3. `Transactor.commit()` — called at request end on success
4. Or `Transactor.abort()` — on failure or `res.abort()`

A `Transactor` is `ThreadLocal`. Use `Packages.helma.objectmodel.db.Transactor.getInstance()` to inspect the current one:

```javascript
var tx = Packages.helma.objectmodel.db.Transactor.getInstance();
if (tx) {
    print(tx.getTransactionName());
    print(tx.isActive());
}
```

## JDBC-Level Behaviour

- Each `DbSource` lazily acquires a JDBC connection when first used in the transaction
- `autoCommit` is set to `false` on every connection Helma owns
- On `Transactor.commit()`: `connection.commit()` is called for each acquired connection
- On `Transactor.abort()`: `connection.rollback()` is called
- After commit/abort, connections are returned to the pool

Multiple DbSources participate sequentially — Helma does **not** perform 2PC. If `commit()` on the second connection fails after the first succeeded, the first commit is durable while the second isn't. For strong consistency across databases, use one DbSource for everything that must commit atomically.

## Forcing a Commit Mid-Request

Use `res.commit()`:

```javascript
function bigImport() {
    for (var i = 0; i < 100000; i++) {
        importRow(rows[i]);
        if (i % 1000 === 0) {
            res.commit();        // commit, start new tx
        }
    }
}
```

`res.commit()`:

1. Calls the current `Transactor.commit()`
2. Starts a new transaction with the same name

`res.rollback()` does the same but aborts.

In cron/external invocations where `res` isn't available:

```javascript
var Transactor = Packages.helma.objectmodel.db.Transactor;
var tx = Transactor.getInstance();
var name = tx.getTransactionName();
tx.commit();
tx.begin(name);
```

## Concurrency Control

Helma uses **optimistic locking**. Each Node carries a version number. On UPDATE:

1. `UPDATE ... WHERE id = ? AND version = ?` is issued
2. If the row count is 0, another transaction modified this row → throw `ConcurrencyException`
3. The framework catches `ConcurrencyException` and retries with exponential backoff. The local counter `tries` starts at 0 and is incremented (`++tries`) on each conflict; while the post-increment value is `< 8` the request is retried, otherwise it aborts. So **8 attempts maximum**: the initial try plus up to 7 retries.

Retry timing:

```
attempt 1 (tries=0): initial try
  conflict → tries becomes 1; 1 < 8 → wait, retry
attempt 2 (tries=1): retry
  conflict → tries becomes 2; 2 < 8 → wait, retry
...
attempt 8 (tries=7): final retry
  conflict → tries becomes 8; 8 < 8 is false → give up, emit
              "Application too busy, please try again later"
```

Backoff per retry is `800 * tries + random(0..800*tries*2)` ms — roughly 800-2400ms on retry 1, growing to 5600-16800ms on retry 7.

In practice, ConcurrencyExceptions are rare unless you have a hot row. Mitigations:

- Re-design to avoid the hot row (e.g. shard counters)
- Use raw SQL with row-level locks
- Increase retry count by forking the framework (the 8 is a hardcoded constant)

## Isolation Level

Helma doesn't change the JDBC isolation level — it uses the driver's default. For PostgreSQL that's `READ COMMITTED`; for MySQL that's `REPEATABLE READ`. Change via JDBC URL or driver-specific connection properties.

For most applications, `READ COMMITTED` is fine. For consistency-critical work, use `SERIALIZABLE` and rely on Helma's retry behaviour for conflicts.

## Transaction Boundaries vs Request Boundaries

By default, transaction boundary = request boundary. This is convenient but has trade-offs:

| Boundary | Implication |
|---|---|
| Long-running action (60s) | Transaction held for 60s — long-running locks |
| Mid-request `res.commit()` | Multiple smaller transactions — better concurrency, but lost-update risk |
| Cron job (10min default) | Long transaction unless `res.commit()`-ed |
| `app.invoke()` | Fresh transaction, separate from caller |

## Manual Connection Management

Sometimes you want raw JDBC outside the transactor:

```javascript
var src = app.getDbSource("main");
var conn = src.getConnection();    // raw, joined to current tx
// or:
var conn = java.sql.DriverManager.getConnection(url, user, pwd);  // entirely separate
```

The raw `getConnection()` from `DbSource` participates in the current transaction. A `DriverManager.getConnection()` is entirely independent — manage it yourself.

## DatabaseObject Auto-Commit Wrap

For a quick query outside the request lifecycle:

```javascript
var db = getDBConnection("main");
var rs = db.executeRetrieval("SELECT count(*) FROM posts");
rs.next();
var n = rs.getInt(1);
```

`getDBConnection()` returns a `DatabaseObject` which:

- Uses the current transactor's connection (joined to current tx)
- If no current transactor, creates a one-shot transaction

## Reading Modified Data in the Same Transaction

If you set a property and immediately read another HopObject that's been modified in the same request:

```javascript
post.title = "New";
var allPosts = root.posts.list();   // includes the modified post
```

Helma's NodeManager returns the in-memory dirty version, not the on-disk version. After commit, the on-disk version reflects the change.

## See Also

- [Concepts: Transactions](../concepts/transactions.md) — framework-level lifecycle
- [Custom Queries](queries.md) — raw JDBC
- [Data Sources](data-sources.md) — DbSource and connection pooling
- [`Transactor.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/objectmodel/db/Transactor.java) — implementation
