# Object-Relational Mapping

Helma's ORM is **declarative**. You don't write SQL. You don't write DAO classes. You write a `type.properties` file that maps prototype properties to columns and `JOIN`s, and the framework does the rest.

## The Mapping File

Each prototype directory may contain a `type.properties`. If present, instances of that prototype are persisted relationally; if absent, they live in the embedded XML database.

```properties
# Post/type.properties

# Where it lives
_db    = main         # DbSource name from db.properties
_table = posts        # Table name
_id    = post_id      # Column for the primary key

# Generation
_idgen = [uuid]       # UUID v7 — or [max], [hop], or a sequence name

# Inheritance and structure
_extends = HopObject  # parent prototype
_name = slug          # column used as element name (URL segment)

# Property mappings
title = title
body = body_html
created = created_at
published = is_published

# Reference: 1-to-1
author.object = User
author.local  = author_id
author.foreign = user_id

# Collection: 1-to-many
comments.collection = Comment
comments.local  = post_id
comments.foreign = post_id
comments.order = created desc
comments.accessname = slug
```

## Concepts

### Property mapping

`property = column_name` declares a primitive (string/number/date/boolean) mapping. The column type is inferred from JDBC metadata.

### Reference

`prop.object = OtherProto` declares a 1-to-1 reference. `prop.local` is the column in **this** table containing the foreign key; `prop.foreign` is the referenced column in the other table.

```javascript
post.author          // a User HopObject
post.author = root.users.alice
                     // sets author_id to alice.user_id
```

### Collection

`prop.collection = OtherProto` declares a 1-to-many. `local`/`foreign` map this prototype's primary key to the foreign-table column.

```javascript
post.comments        // a virtual collection
post.comments.size() // SELECT COUNT(*) FROM comments WHERE post_id = ?
post.comments.list() // SELECT * FROM comments WHERE post_id = ? ORDER BY created DESC
post.comments.add(c) // INSERT (assigns c.post_id = post.id)
```

### Access name

`prop.accessname = column` lets you access child by a string key:

```javascript
post.comments["first-comment-slug"]
```

This issues `SELECT * FROM comments WHERE post_id = ? AND slug = ?`.

### Order, group, filter

```properties
comments.order = created desc
comments.group = author
comments.group.order = name asc
comments.filter = is_spam = 0
comments.maxSize = 10
```

### Inheritance

```properties
_extends = Document
```

`Post` shares `Document`'s table or extends with own columns. With `_prototype = type_column`, multiple prototypes can share one table — each row's `_prototype` column says which prototype it is. The `_extensionId` lets you customise the discriminator value.

## Lifecycle

### Loading

`root.posts.get("hello-world")`:

1. Builds a SQL query against the `posts` table
2. Reads the row, instantiates a Node
3. Caches the Node in the NodeManager
4. Wraps the Node in a HopObject and returns it

Subsequent access to the same row hits the cache.

### Updating

```javascript
post.title = "New title";
```

This:

1. Sets the property on the Node
2. Marks the Node dirty in the current transactor
3. At `Transactor.commit()` (end of request), executes `UPDATE posts SET title = ? WHERE post_id = ?`

### Inserting

```javascript
var post = new Post();
post.title = "Hi";
root.posts.add(post);
```

Creates a new Node, generates an ID (via the configured `_idgen`), and on commit issues `INSERT`.

### Deleting

```javascript
post.remove();
```

Removes the Node from caches and on commit issues `DELETE`.

`post.remove()` cascades children automatically only for `_children` collections — explicitly map cascade via the DB or do it manually.

## SQL Generation

Helma generates SQL on demand:

| Operation | Generated SQL |
|---|---|
| Load by ID | `SELECT a.* FROM posts a WHERE a.post_id = ?` |
| Load with joined references | `SELECT a.*, JOIN_x.* FROM posts a LEFT OUTER JOIN users JOIN_x ON a.author_id = JOIN_x.user_id WHERE a.post_id = ?` (when `author.aggressiveLoading = true`) |
| Save new | `INSERT INTO posts (post_id, title, body, author_id, created_at, published_at) VALUES (?, ?, ?, ?, ?, ?)` |
| Update | `UPDATE posts SET title = ?, body = ? WHERE post_id = ?` |
| Delete | `DELETE FROM posts WHERE post_id = ?` |
| Count children | `SELECT COUNT(*) FROM comments WHERE post_id = ?` |
| List children | `SELECT * FROM comments WHERE post_id = ? ORDER BY created DESC` |
| Access child by name | `SELECT * FROM comments WHERE post_id = ? AND slug = ?` |

All queries are **prepared statements**. Helma caches the prepared statement strings; only the parameters change per call.

## Caching

The `NodeManager` LRU cache holds recently-accessed Nodes by primary key. Default size: 1000 (`app.properties::cacheNodes`).

On a miss, a SQL `SELECT` fetches the row. On hit, no SQL — Helma returns the cached Node.

When a Node is modified, its cache entry is updated. When deleted, removed.

`app.clearCache()` empties the cache.

## Lazy Loading

References are loaded lazily by default:

```javascript
post.author          // single SQL SELECT, but only on first access
                     // subsequent access uses the cached User Node
```

For aggressive loading (JOIN at load time):

```properties
author.aggressiveLoading = true
```

…and Helma will pull `author` columns in the same SELECT that loads the post — saving a round trip.

For aggressive caching of collections:

```properties
comments.aggressiveCaching = true
```

Helma caches the entire child Node list rather than just the IDs. Faster reads, more memory.

## Virtual Collections

A collection can be **virtual** — not stored as a child relation but computed by query:

```properties
# Root: a virtual collection of recent posts
recentPosts.collection = Post
recentPosts.order = created desc
recentPosts.maxSize = 10
```

`root.recentPosts.list()` executes:

```sql
SELECT * FROM posts ORDER BY created DESC LIMIT 10
```

Virtual collections cannot have `add()` or `remove()` called on them; they are read-only views.

## Grouped Collections

`group` adds a virtual layer of grouping nodes:

```properties
postsByMonth.collection = Post
postsByMonth.group = SUBSTRING(created::TEXT, 1, 7)    # or use a date_trunc function
postsByMonth.group.order = SUBSTRING(created::TEXT, 1, 7) desc
postsByMonth.group.prototype = Month
```

`root.postsByMonth.size()` returns the number of distinct months. `root.postsByMonth.get(0)` returns a synthetic "Month" HopObject for the most recent month, which has its own list of posts.

This is unique to Helma and powerful for time-bucketed UIs.

## Constraints and Foreign Keys

Helma does **not** create constraints. Schema management is your responsibility. Create the tables and indexes via `psql`, `mysql`, or migrations. Helma reads metadata via JDBC to determine column types.

## What Helma Does Not Do

- No schema migration framework
- No SQL-level transactions beyond per-request commits
- No 2-phase commit across multiple DbSources
- No async query execution
- No connection retry / failover (rely on JDBC driver)

For schema management, use [Flyway](https://flywaydb.org), [Liquibase](https://www.liquibase.org), or hand-rolled migrations.

## See Also

- [Type Properties Reference](type-properties.md) — every setting
- [Relations](relations.md) — the four types in detail
- [Data Sources](data-sources.md) — connecting to PostgreSQL, MySQL, etc.
- [Custom Queries](queries.md) — raw JDBC when ORM isn't enough
