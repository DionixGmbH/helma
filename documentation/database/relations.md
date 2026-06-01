# Relations

A **relation** is the metadata that describes how one HopObject property links to another HopObject or to a database column. Helma supports four relation types, defined in `helma.objectmodel.db.Relation.java`.

## The Four Types

| Type | Constant | Use case | `type.properties` syntax |
|---|---|---|---|
| `PRIMITIVE` | 0 | Scalar property → DB column | `property = column` |
| `REFERENCE` | 1 | 1-to-1 to another prototype | `prop.object = OtherProto` |
| `COLLECTION` | 2 | 1-to-many child list | `prop.collection = OtherProto` |
| `COMPLEX_REFERENCE` | 3 | Reference with multi-column key | Internal — auto-detected |

## Primitive

A primitive maps a JS property to a database column of a primitive SQL type (string, int, date, blob, etc.).

```properties
title = title_column
view_count = views
published_at = published
is_active = active
```

The column type is determined by JDBC metadata; the conversion JS↔SQL is automatic:

| JS type | SQL type |
|---|---|
| `String` | `VARCHAR`, `TEXT`, `CHAR`, `CLOB` |
| `Number` | `INTEGER`, `BIGINT`, `FLOAT`, `DOUBLE`, `NUMERIC` |
| `Boolean` | `BOOLEAN`, `BIT`, `TINYINT(1)` |
| `Date` | `TIMESTAMP`, `DATE`, `TIME` |
| `byte[]` | `BLOB`, `VARBINARY`, `BYTEA` |
| `null` | `NULL` |

Set the JS property → row column is updated on commit. Read the JS property → fetches from cached Node or DB.

```javascript
post.title = "Hello";       // marks dirty
post.view_count++;           // marks dirty
post.published_at = new Date();
```

## Reference (1-to-1)

A reference links one HopObject to another via a foreign key column.

```properties
author.object  = User
author.local   = author_id   # column in posts table
author.foreign = user_id      # column in users table
```

If `.local` is omitted, defaults to the property name. If `.foreign` is omitted, defaults to the other prototype's `_id`.

```javascript
post.author         // a User HopObject (or null)
post.author = user  // sets post.author_id = user.user_id
post.author = null  // clears the foreign key (post.author_id = null)
```

### Loading behaviour

- **Lazy** (default): on `post.author`, issues `SELECT * FROM users WHERE user_id = ?` if not cached.
- **Aggressive** (`author.aggressiveLoading = true`): on `post` load, JOIN-loads the author in the same query.

### Setting

`post.author = user` sets the foreign-key column. **The user object is not modified.**

If you assign a non-HopObject (e.g. a string), it's coerced to the foreign key value directly:

```javascript
post.author = "01938a4b-...";  // sets author_id directly, no lookup
```

## Collection (1-to-many)

A collection is the inverse of a reference — many of `OtherProto` per `this`.

```properties
comments.collection = Comment
comments.local      = post_id   # column in posts (usually post_id)
comments.foreign    = post_id   # column in comments (usually post_id)
```

### Collection API

```javascript
post.comments              // a virtual collection (NOT an Array)
post.comments.size()       // SELECT COUNT(*)
post.comments.length       // same — alias for size()
post.comments.get(0)       // first child
post.comments.get(5)       // sixth child (with current order)
post.comments.list()       // returns all as Array

post.comments.add(c)       // INSERT — sets c.post_id = post.post_id
post.comments.remove(c)    // DELETE child

for each (var c in post.comments.list()) {
    res.write(c.text);
}

// Or for paginated:
var page = post.comments.list({ offset: 20, maxSize: 10 });
```

Note: `for each` directly on `post.comments` iterates through the collection.

### Ordering

```properties
comments.order = created desc, id asc
```

Multiple sort keys, comma-separated.

### Filtering

```properties
publishedComments.collection = Comment
publishedComments.local  = post_id
publishedComments.foreign = post_id
publishedComments.filter = is_approved = true
```

The filter is `AND`-appended to the WHERE clause.

### Access by name

```properties
comments.accessname = slug
```

Now:

```javascript
post.comments["my-comment-slug"]   // SELECT WHERE post_id = ? AND slug = ?
```

### Pagination

```properties
recentComments.collection = Comment
recentComments.local = post_id
recentComments.foreign = post_id
recentComments.order = created desc
recentComments.maxSize = 10
recentComments.offset = 0
```

`SELECT ... LIMIT 10 OFFSET 0` is generated.

### Grouped collections

```properties
commentsByDate.collection = Comment
commentsByDate.local = post_id
commentsByDate.foreign = post_id
commentsByDate.group = DATE(created)
commentsByDate.group.prototype = Day
commentsByDate.group.order = DATE(created) desc
```

`post.commentsByDate.size()` returns the count of distinct dates. Each item is a synthetic `Day` HopObject.

A grouped collection lets you naturally produce "comments grouped by month" or "logs grouped by hour" without writing SQL.

## Complex Reference

A complex reference uses multiple columns or a non-primary-key reference.

This is detected automatically when:

- The reference has more than one constraint
- The constraints don't use the primary key column of the foreign table

```properties
manager.object = User
manager.constraints = department_id, role
manager.constraints.local = department_id, role_name
manager.constraints.foreign = department_id, role
manager.constraints.logicalOperator = AND
```

Helma sets `reftype = COMPLEX_REFERENCE` and fetches the user via:

```sql
SELECT * FROM users WHERE department_id = ? AND role = ?
```

You typically don't need to manually choose between `REFERENCE` and `COMPLEX_REFERENCE` — Helma decides.

## Virtual Relations

A relation can be **virtual** — it doesn't map to a stored column but is computed at query time.

A virtual collection is one where `prop.virtual = true` (or implicit from `mountpoint`, group, etc.).

```properties
# Root: a virtual collection of all unmoderated comments
unmoderated.collection = Comment
unmoderated.filter = is_approved = false
unmoderated.order = created desc
```

`root.unmoderated` is virtual — `add()` and `remove()` don't work on virtual collections (use `c.parent = ...` to associate).

## Mountpoints

A mountpoint relation lets you "mount" objects from one place to appear as if they were children of another:

```properties
sharedPosts.mountpoint = Post
sharedPosts.local = blog_id
sharedPosts.foreign = blog_id
sharedPosts.filter = is_shared = true
```

`blog.sharedPosts` makes shared Posts appear as children of the blog, without storing a separate relation. Mountpoints are URL-resolvable: `blog/sharedPosts/post-slug` works as a URL.

## Cardinality Helpers

For dynamic checks:

```javascript
if (post.author) { ... }                       // reference: null check
if (post.comments.size() > 0) { ... }          // collection
post.comments.contains(c)                      // is c a child?
```

## When to Use Which

- **One scalar per row** → `PRIMITIVE` (just `prop = column`)
- **A pointer to another HopObject** → `REFERENCE` (`.object`)
- **A list of HopObjects you own** → `COLLECTION` (`.collection`)
- **A list of HopObjects from elsewhere** → `mountpoint` or virtual collection
- **Multi-column foreign key** → automatic `COMPLEX_REFERENCE`

## Performance

| Operation | Cost |
|---|---|
| `post.title = "x"` | Memory only, write on commit |
| `post.author` (cached) | Memory only |
| `post.author` (uncached) | 1 SELECT |
| `post.author` (aggressive) | 0 SELECT (already loaded) |
| `post.comments.size()` | 1 SELECT (count) |
| `post.comments.list()` | 1 SELECT (full rows) |
| `post.comments.get(0)` | 1 SELECT (LIMIT 1) |
| `post.comments.add(c)` | 1 UPDATE on c (sets foreign key) |
| `post.comments.remove(c)` | 1 DELETE |

Aggressive loading and caching can avoid round trips at the cost of memory.

## See Also

- [Type Properties Reference](type-properties.md) — all `.collection.*` options
- [Custom Queries](queries.md) — when Helma's query layer can't express what you want
- [`Relation.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/objectmodel/db/Relation.java) — the implementation
