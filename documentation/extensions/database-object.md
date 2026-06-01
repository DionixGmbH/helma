# DatabaseObject Extension

`DatabaseObject` wraps a JDBC connection for raw SQL execution from JavaScript. It's returned by the global `getDBConnection(name)`.

Implementation: `src/main/java/helma/scripting/rhino/extensions/DatabaseObject.java`.

## Obtaining

```javascript
var db = getDBConnection("main");      // name from db.properties
```

`getDBConnection()` is a global function defined in `GlobalObject.java`. It looks up the `DbSource` for the given name and wraps a connection.

There's no public constructor — you cannot do `new DatabaseObject(...)` from JS.

## API

### `connect(url, user, password)` → boolean

Open a JDBC connection.

```javascript
db.connect("jdbc:postgresql://localhost/test", "user", "pwd");
```

Usually called for you by `getDBConnection`.

### `disconnect()` → boolean
### `release()` → boolean (alias)

Close the connection.

### `executeRetrieval(sql)` → RowSet

Run a SELECT query. Returns a [`RowSet`](#rowset) cursor.

```javascript
var rs = db.executeRetrieval("SELECT * FROM posts WHERE author_id = 42");
while (rs.next()) {
    res.write(rs.getColumnItem("title") + "\n");
}
```

Note: there's no built-in parameter binding via `?` placeholders in this method. For parameterised queries, use `executePreparedRetrieval()` or grab the raw `connection` field.

### `executePreparedRetrieval(preparedStatement)` → RowSet

Execute a pre-prepared statement and return a RowSet.

```javascript
var conn = db.getConnection();
var stmt = conn.prepareStatement("SELECT * FROM posts WHERE author_id = ?");
stmt.setInt(1, 42);
var rs = db.executePreparedRetrieval(stmt);
while (rs.next()) {
    // ...
}
```

### `executeCommand(sql)` → int

Run an INSERT/UPDATE/DELETE/DDL statement. Returns rows affected.

```javascript
var n = db.executeCommand("UPDATE posts SET views = views + 1 WHERE post_id = 42");
```

### `getMetaData()` → DatabaseMetaData

Return the JDBC `DatabaseMetaData` object (cached).

### `getLastError()` → Throwable | null

The most recent exception, or null.

### Internal access

```javascript
db.connection      // the raw java.sql.Connection
```

## RowSet

A cursor over query results.

### `next()` → boolean

Advance to the next row. Returns false at end of results.

### `hasMoreRows()` → boolean

True if more rows are available.

### `getColumnCount()` → int

Number of columns in the result.

### `getColumnName(idx)` → String

Column name at 1-based index `idx`.

### `getColumnDatatypeNumber(idx)` → int

`java.sql.Types` constant for the column.

### `getColumnDatatypeName(idx)` → String

Display name of the column type.

### `getColumnItem(name)` → Object

Get the current row's value for the named column. Helma converts SQL types to JS types automatically.

### `getProperty(index)` → Object

Get by 1-based index.

### `getProperties()` → Enumeration

Iterate column names.

### `getMetaData()` → ResultSetMetaData

The full JDBC metadata for this result.

### `getLastError()` → Throwable | null

The most recent error.

### `release()` → void

Close the cursor.

### Special property: `length`

```javascript
rs.length      // number of rows fetched so far
```

## Example: Full lifecycle

```javascript
var db = getDBConnection("main");

try {
    // SELECT
    var rs = db.executeRetrieval("SELECT post_id, title FROM posts");
    while (rs.next()) {
        res.write(rs.getColumnItem("post_id") + ": " + rs.getColumnItem("title") + "\n");
    }
    rs.release();

    // UPDATE
    var n = db.executeCommand("UPDATE counters SET value = value + 1 WHERE name = 'visits'");
    res.write("Updated " + n + " row(s)\n");
} catch (e) {
    app.logError("DB error", e);
}
// connection is reclaimed at request end
```

## Transactions

`DatabaseObject` participates in Helma's per-request transaction. The connection is acquired from the `DbSource` pool with `autoCommit = false`; commits happen at `Transactor.commit()` (end of successful request).

To commit mid-request:

```javascript
res.commit();          // commits and starts new tx
```

## When to Use

- Quick ad-hoc SELECTs from JavaScript actions
- One-off UPDATEs / inserts where the ORM doesn't fit
- Stored-procedure invocation
- Bulk operations

For typical CRUD on mapped HopObjects, prefer the ORM — it caches, manages transactions, and handles concurrency.

## See Also

- [Database: Custom Queries](../database/queries.md)
- [Database: Data Sources](../database/data-sources.md)
- [Reference: `getDBConnection()`](../reference/global-object.md)
- [`DatabaseObject.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/scripting/rhino/extensions/DatabaseObject.java)
