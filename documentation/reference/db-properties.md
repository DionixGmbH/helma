# db.properties

`db.properties` defines the JDBC data sources Helma can use. Lives at the project root (server-wide) and/or per-application (`apps/<app>/db.properties`).

## Format

```properties
<sourcename>.url      = jdbc:...
<sourcename>.driver   = fully.qualified.DriverClassName
<sourcename>.user     = username
<sourcename>.password = password
<sourcename>.subtype  = optional dialect hint
```

## Per-DbSource Properties

### `<name>.url`

JDBC connection URL.

```properties
main.url = jdbc:postgresql://db.example.com:5432/myapp
```

### `<name>.driver`

JDBC driver class name. Must be on the classpath.

```properties
main.driver = org.postgresql.Driver
```

### `<name>.user`

Database username.

```properties
main.user = helma
```

### `<name>.password`

Database password.

```properties
main.password = ...
```

### `<name>.subtype`

Optional dialect hint when Helma can't autodetect. Values: `oracle`, `mysql`, `postgresql`, `mssql`, `h2`, `hsqldb`, `sqlite`, `mariadb`.

```properties
main.subtype = postgresql
```

## Resolution Order

When `_db = main` is set in `type.properties`, Helma looks up `main` from:

1. The application's `db.properties` (per-app overrides)
2. The server-wide `db.properties`

Per-app values overlay server-wide values, allowing apps to share defaults while overriding specifics (e.g. different passwords).

## Examples

### PostgreSQL

```properties
main.url     = jdbc:postgresql://localhost:5432/blog?ApplicationName=helma
main.driver  = org.postgresql.Driver
main.user    = blog
main.password = secret
main.subtype = postgresql
```

### MySQL

```properties
main.url     = jdbc:mysql://localhost:3306/blog?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC
main.driver  = com.mysql.cj.jdbc.Driver
main.user    = blog
main.password = secret
main.subtype = mysql
```

### SQLite (development)

```properties
main.url     = jdbc:sqlite:db/main.sqlite
main.driver  = org.sqlite.JDBC
main.user    = unused
main.password = unused
```

### H2 (embedded)

```properties
main.url     = jdbc:h2:./db/main;AUTO_SERVER=TRUE
main.driver  = org.h2.Driver
main.user    = sa
main.password =
```

### Oracle

```properties
main.url     = jdbc:oracle:thin:@//db.example.com:1521/XE
main.driver  = oracle.jdbc.OracleDriver
main.user    = blog
main.password = secret
main.subtype = oracle
```

### Multiple DbSources

```properties
# Main application database
main.url = jdbc:postgresql://main.db.example.com:5432/blog
main.driver = org.postgresql.Driver
main.user = blog
main.password = secret

# Analytics warehouse
analytics.url = jdbc:postgresql://analytics.db.example.com:5432/analytics
analytics.driver = org.postgresql.Driver
analytics.user = blog_ro
analytics.password = other-secret

# Session store
sessions.url = jdbc:postgresql://sessions.db.example.com:5432/sessions
sessions.driver = org.postgresql.Driver
sessions.user = sessions
sessions.password = third-secret
```

Use in `type.properties`:

```properties
# Post: in main
_db = main
_table = posts

# Event: in analytics
_db = analytics
_table = events

# UserSession: in sessions
_db = sessions
_table = sessions
```

## Driver Placement

The JDBC driver JAR must be on the classpath:

- **Server-wide**: drop in `lib/ext/`
- **Per-app**: drop in `apps/<app>/lib/` — discovered by the app's classloader

## Connection Pooling

Helma maintains an unbounded internal pool per DbSource. For high-concurrency apps, use an external pooler:

```properties
# Via HikariCP wrapped JNDI source (typically configured in Jetty XML)
main.url     = (configured via DataSource lookup)
main.lookup  = jdbc/main
```

But built-in is usually fine.

## Security

- **Never commit `db.properties` with real passwords** to a repository. Use environment-variable substitution or a separate untracked file.
- Restrict file permissions: `chmod 600 db.properties`.

## See Also

- [Database: Data Sources](../database/data-sources.md)
- [Database: Object-Relational Mapping](../database/orm.md)
- [Database: Type Properties](../database/type-properties.md)
