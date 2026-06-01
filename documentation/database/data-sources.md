# Data Sources

A **DbSource** is Helma's name for a JDBC database connection pool. Define one per database in `db.properties`.

## `db.properties`

Each application can have its own `db.properties` (in the app dir) plus a server-wide one (at `helma.home/db.properties`). Application settings overlay server-wide.

### A complete example

```properties
# Server-wide defaults
main.url     = jdbc:postgresql://localhost:5432/myapp
main.driver  = org.postgresql.Driver
main.user    = helma
main.password = secret

# Per-app override of password only
main.password = another-secret
```

### Per-DbSource settings

| Property | Required | Description |
|---|---|---|
| `<name>.url` | yes | JDBC URL |
| `<name>.driver` | yes | Driver class name |
| `<name>.user` | yes | Database username |
| `<name>.password` | yes | Database password |
| `<name>.subtype` | no | `oracle`, `mysql`, `postgresql`, `mssql`, `h2`, etc. — used for dialect-specific SQL |
| `<name>.subtype.<option>` | no | Subtype-specific options |

When `subtype` is unset, Helma probes the database via `DatabaseMetaData` to guess.

### Driver Discovery

The JDBC driver class must be on the classpath. Place its JAR in:

- `lib/ext/` (server-wide)
- `apps/<appname>/lib/` (per-app, picked up by `AppClassLoader`)

The driver is loaded once when the DbSource is first used.

## Built-in Driver Support

Helma has been tested with:

- PostgreSQL (`org.postgresql.Driver`)
- MySQL / MariaDB (`com.mysql.cj.jdbc.Driver` or `org.mariadb.jdbc.Driver`)
- Oracle (`oracle.jdbc.OracleDriver`)
- MS SQL Server (`com.microsoft.sqlserver.jdbc.SQLServerDriver`)
- H2 (`org.h2.Driver`)
- HSQLDB (`org.hsqldb.jdbc.JDBCDriver`)
- SQLite (`org.sqlite.JDBC`)

Any JDBC-compliant driver should work. Issues with quirky dialects can usually be worked around via `_idgen` and manual SQL.

## Examples

### PostgreSQL

```properties
main.url     = jdbc:postgresql://db.example.com:5432/myapp?ApplicationName=helma
main.driver  = org.postgresql.Driver
main.user    = helma
main.password = ...
```

### MySQL / MariaDB

```properties
main.url     = jdbc:mysql://localhost:3306/myapp?useUnicode=true&characterEncoding=UTF-8&useSSL=false
main.driver  = com.mysql.cj.jdbc.Driver
main.user    = helma
main.password = ...
main.subtype = mysql
```

### SQLite (development)

```properties
main.url     = jdbc:sqlite:db/myapp.sqlite
main.driver  = org.sqlite.JDBC
main.user    = unused
main.password = unused
```

### H2 (embedded)

```properties
main.url     = jdbc:h2:./db/myapp;AUTO_SERVER=TRUE
main.driver  = org.h2.Driver
main.user    = sa
main.password =
```

### Oracle

```properties
main.url     = jdbc:oracle:thin:@//db.example.com:1521/XE
main.driver  = oracle.jdbc.OracleDriver
main.user    = helma
main.password = ...
main.subtype = oracle
```

## Using a DbSource

### In `type.properties`

```properties
_db    = main
_table = posts
```

`_db` references the DbSource name from `db.properties`.

### From JavaScript

```javascript
// Get a wrapped connection
var db = getDBConnection("main");

// Or via app.
var src = app.getDbSource("main");
var conn = src.getConnection();   // raw java.sql.Connection
```

`getDBConnection(name)` returns a `DatabaseObject` — a thin wrapper offering JS-friendly methods:

```javascript
var db = getDBConnection("main");

// Execute a query
var rs = db.executeRetrieval("SELECT * FROM posts WHERE published = ?", [true]);
while (rs.next()) {
    res.write(rs.getString("title") + "\n");
}

// Execute an update
var affected = db.executeCommand("UPDATE posts SET views = views + 1 WHERE post_id = ?", [postId]);

// Get raw connection
var conn = db.connection;
```

The `DatabaseObject` automatically participates in the current transaction (`Transactor.commit` will commit it).

## Connection Pooling

Helma maintains a pool of JDBC connections per DbSource. The pool is unlimited — every active transaction gets its own connection.

Connections are returned to the pool at transaction end. For long-running apps with predictable concurrency, this is fine. For bursty workloads, use a connection pooler external to Helma (PgBouncer, HikariCP via a wrapper DbSource).

## Transaction Behaviour

Auto-commit is set to **false** on every connection Helma acquires. Each request's transactor:

1. Acquires a connection on first use of a DbSource
2. Issues SQL with auto-commit off
3. At end of request, calls `commit()` on every acquired connection
4. Releases connections to the pool

On error, `rollback()` is called instead.

Multi-DbSource transactions are **not 2PC** — Helma commits each connection sequentially.

## Connection Failures

If `getConnection()` throws (DB down, auth wrong, etc.), Helma logs the failure and the request fails with the JDBC exception. There's no automatic retry — rely on JDBC driver-level retry or a connection pooler.

Schema-mismatch issues (column not found, etc.) show up as SQLException at first use of the prototype.

## Schema Introspection

When Helma first accesses a table, it issues `SELECT * FROM table WHERE 1=0` to read column metadata. This populates the `columns` array on the `DbMapping`. From that point on, Helma knows column types and can do proper parameter binding.

If you add a new column to a table, **restart the app or call `app.clearCache()`** — the metadata is cached until prototype reload.

## Cross-Database References

A `prop.object = OtherProto` reference works fine even when `OtherProto`'s `_db` is a different DbSource. Helma issues two separate queries: one against this prototype's table, one against the other. No `JOIN` is generated across DbSources.

## Best Practices

- One DbSource per logical database. Don't define multiple DbSources for the same DB unless you have a specific reason (read replicas, etc.).
- Set `subtype` explicitly for non-default databases — Helma's autodetect is correct most of the time but not always.
- Use a JDBC URL with connection-level options like `applicationName` / `ApplicationName=helma` so the DB server's connection list is descriptive.
- For HA / failover, use the JDBC driver's connection-string failover feature (PostgreSQL's `targetServerType`, MySQL's `failOverReadOnly`).
- Don't `getDBConnection()` and hold the connection across requests — the framework manages connection lifetime.

## See Also

- [Type Properties Reference](type-properties.md) — `_db` setting
- [Custom Queries](queries.md) — raw JDBC via `DatabaseObject`
- [Transactions](transactions.md) — DB transaction semantics
