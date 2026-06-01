# helma.Database

A higher-level wrapper over [`getDBConnection()`](../../reference/global-object.md) / `DatabaseObject` providing managed connections.

```javascript
app.addRepository("modules/helma/Database.js");
```

## Constructor

```javascript
var db = new helma.Database(source);   // source is a DbSource name from db.properties
```

## Static Methods

### `helma.Database.createInstance(driver, url, name, user, password)` → helma.Database

Create a new database connection without going through `db.properties`.

```javascript
var db = helma.Database.createInstance(
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost/test",
    "mytest",
    "user", "pwd"
);
```

### `helma.Database.getInstance(name)` → helma.Database

Get a previously-created instance by name.

## Instance Methods

### `getConnection()` → java.sql.Connection

The underlying JDBC connection.

### `getProductName()` → String

Name of the DB product (e.g. `"PostgreSQL"`).

### `isOracle()` / `isMySql()` / `isPostgreSql()` → boolean

Convenience type checks.

### `query(sql)` → ResultSet

Execute a SELECT query.

### `execute(sql)` → int

Execute an INSERT/UPDATE/DELETE.

### `getName()` → String / `getDriverName()` → String

Connection metadata.

## Example

```javascript
app.addRepository("modules/helma/Database.js");
var db = new Database("main");
var rs = db.query("SELECT count(*) FROM posts");
rs.next();
print(rs.getInt(1));
```

## See Also

- [Database: Custom Queries](../../database/queries.md)
- [Database: Data Sources](../../database/data-sources.md)
- [`modules/helma/Database.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Database.js)
