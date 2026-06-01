# Custom Queries

The ORM covers common patterns (CRUD, 1-to-1, 1-to-many, simple filters and ordering). For everything else, you write SQL.

## Three Approaches

| Approach | Use when |
|---|---|
| Virtual collection with `.filter` | Simple WHERE-clause-only filtering |
| `DatabaseObject` via `getDBConnection()` | Mid-complexity queries returning rows |
| Raw `java.sql.Connection` from `DbSource` | Full control, prepared statements, batches |

## Approach 1: Virtual Collections

`type.properties`:

```properties
# Root/type.properties
publishedPosts.collection = Post
publishedPosts.filter = is_published = true AND deleted = false
publishedPosts.order = published_at desc
publishedPosts.maxSize = 50
```

```javascript
root.publishedPosts.size();
root.publishedPosts.list();
```

Pros: declarative, cached, integrates with the ORM.

Cons: `.filter` is a static SQL fragment — no parameters. Limited to one prototype.

## Approach 2: `DatabaseObject` Wrapper

```javascript
var db = getDBConnection("main");

// SELECT
var rs = db.executeRetrieval(
    "SELECT * FROM posts WHERE author_id = ? AND published_at > ?",
    [42, new Date("2025-01-01")]
);
while (rs.next()) {
    res.write(rs.getString("title") + "\n");
}

// UPDATE / INSERT / DELETE
var affected = db.executeCommand(
    "UPDATE posts SET views = views + 1 WHERE post_id = ?",
    [postId]
);

// Single value
var count = db.executeRetrieval("SELECT count(*) FROM posts", []);
count.next();
var n = count.getInt(1);
```

`DatabaseObject` methods (`src/main/java/helma/scripting/rhino/extensions/DatabaseObject.java`):

| Method | Returns | Use |
|---|---|---|
| `executeRetrieval(sql, params)` | ResultSet | SELECT |
| `executeCommand(sql, params)` | int (rows affected) | INSERT/UPDATE/DELETE |
| `connection` | `java.sql.Connection` | Raw access |
| `disconnect()` | void | Return connection to pool (no-op in tx) |

Parameter binding is positional — the `?` markers are replaced in order by elements of the second array.

## Approach 3: Raw JDBC

For complex queries (batch inserts, stored procedures, vendor-specific SQL):

```javascript
var src = app.getDbSource("main");
var conn = src.getConnection();           // raw java.sql.Connection
var stmt = conn.prepareStatement(
    "INSERT INTO logs (event, user_id, created) VALUES (?, ?, NOW())"
);
try {
    stmt.setString(1, "login");
    stmt.setInt(2, session.user.id);
    stmt.executeUpdate();
} finally {
    stmt.close();
}
```

The connection is the one used by the current transaction — commits happen when the request commits.

## Refreshing the ORM Cache

After a raw SQL UPDATE/DELETE, the `NodeManager` cache may be stale:

```javascript
// Raw update bypasses the ORM
var db = getDBConnection("main");
db.executeCommand("UPDATE posts SET views = views + 1 WHERE post_id = ?", [postId]);

// The cached Node still has the old view_count
var post = root.posts.get(postId);
print(post.views);            // stale!

// Force re-read
app.clearCache();             // global; too aggressive
// or:
post.invalidate();            // mark this Node stale, re-fetch on next access
```

Prefer the ORM path when you want the cache kept consistent automatically:

```javascript
post.views++;     // ORM-aware — cache stays correct
```

Use raw SQL when you need atomicity that the ORM can't express:

```javascript
db.executeCommand("UPDATE counters SET hits = hits + 1 WHERE id = ?", [counterId]);
// Atomic — no read-modify-write race
```

## Mapping Custom Queries to HopObjects

To make a query return HopObjects (not raw rows):

```javascript
// Define a virtual collection on Root
// Root/type.properties:
//   foundPosts.collection = Post
//   foundPosts.local = post_id
//   foundPosts.foreign = post_id
//   foundPosts.filter = ... (dynamic? not directly supported)

// Workaround: load IDs via raw SQL, then look them up via ORM
function search(query) {
    var db = getDBConnection("main");
    var rs = db.executeRetrieval(
        "SELECT post_id FROM posts WHERE to_tsvector('english', title || ' ' || body) @@ plainto_tsquery(?)",
        [query]
    );
    var results = [];
    while (rs.next()) {
        results.push(root.posts.get(rs.getString("post_id")));
    }
    return results;
}
```

Each `root.posts.get(id)` is cached — the second invocation is a hash lookup.

## Batch Inserts

```javascript
var src = app.getDbSource("main");
var conn = src.getConnection();
var stmt = conn.prepareStatement(
    "INSERT INTO logs (event, user_id) VALUES (?, ?)"
);
try {
    for each (var entry in entries) {
        stmt.setString(1, entry.event);
        stmt.setInt(2, entry.userId);
        stmt.addBatch();
    }
    var counts = stmt.executeBatch();
} finally {
    stmt.close();
}
```

`executeBatch()` is much faster than individual `executeUpdate()` calls — drivers can send the whole batch in one round-trip.

## Stored Procedures

```javascript
var conn = app.getDbSource("main").getConnection();
var call = conn.prepareCall("{call my_proc(?, ?)}");
try {
    call.setString(1, "in");
    call.registerOutParameter(2, java.sql.Types.INTEGER);
    call.execute();
    var result = call.getInt(2);
} finally {
    call.close();
}
```

Stored procedures share the transaction with everything else.

## Reading Type Metadata

Helma reads column metadata at first table access:

```javascript
var src = app.getDbSource("main");
var conn = src.getConnection();
var meta = conn.getMetaData();
var cols = meta.getColumns(null, null, "posts", "%");
while (cols.next()) {
    res.write(cols.getString("COLUMN_NAME") + " : " + cols.getString("TYPE_NAME") + "\n");
}
```

You can use this to inspect the schema from inside the app.

## Best Practices

- **Always use prepared statements** with parameter binding. Never concatenate user input into SQL — XSS-style injection works in SQL too.
- **Close ResultSets, Statements, and PreparedStatements** in `finally`. The connection goes back to the pool automatically but result-set memory is yours to free.
- **Don't bypass the ORM when you can avoid it**. Cache consistency, ID generation, and transaction integration are all easier to get right with the ORM.
- **Test rollback behaviour**. After a raw UPDATE, throw an exception in your action — verify the row reverted.
- **Avoid long-running queries during a request**. They hold a connection from the pool and may trigger the request timeout. For heavy reporting, run asynchronously via `app.invokeAsync` or cron.

## See Also

- [Data Sources](data-sources.md) — connection configuration
- [Transactions](transactions.md) — how raw SQL participates
- [`DatabaseObject` extension](../extensions/database-object.md) — full API
