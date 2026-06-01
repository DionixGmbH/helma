# Type Properties Reference

The `type.properties` file inside a prototype directory configures the database mapping of that prototype. This page is the complete, source-verified reference.

All special properties are prefixed with an underscore `_`. All non-underscored top-level keys are **property mappings** — they declare a JavaScript property and its database representation.

Property names are case-insensitive (Helma uses `ResourceProperties` with `ignoreCase = true`).

## Top-Level `_` Properties

### `_db`

The name of a `DbSource` from `db.properties`.

```properties
_db = main
```

If unset, the prototype is stored in the embedded XML database. Inherits from the `_extends` parent.

### `_table`

The name of the database table.

```properties
_table = posts
```

Required when `_db` is set. Tables are addressed unqualified — use `<schema>.<table>` for namespaced tables on PostgreSQL/Oracle. Inherits from parent.

### `_id`

The column used as the primary key.

```properties
_id = post_id
```

Defaults to `ID`. Inherits from parent.

### `_idgen`

ID generation strategy. See [ID Generation](id-generation.md).

| Value | Strategy |
|---|---|
| *(unset)* / `[max]` | `SELECT MAX(id) + 1` |
| `[hop]` | Embedded DB sequence |
| `[uuid]` | UUID v7 |
| *sequence-name* | Database sequence (Oracle, PostgreSQL, H2) |

```properties
_idgen = [uuid]
```

### `_name`

Column used as the HopObject's **element name** — the URL segment name and `node.name`.

```properties
_name = slug
```

Defaults to `_id`. Inherits from parent.

### `_prototype`

Column storing the prototype name — for table inheritance where multiple prototypes share one table.

```properties
_prototype = type
```

Required when `_extends` shares a table with the parent. Each row's value determines which prototype to instantiate.

### `_extends`

The parent prototype.

```properties
_extends = HopObject
```

`HopObject` is the implicit parent if unset. Child inherits DB mapping, JS prototype chain, and skin lookup fall-back.

### `_extensionId`

The value stored in the `_prototype` column to identify this prototype.

```properties
_extensionId = article
```

Defaults to the prototype name.

### `_parent`

Comma- or semicolon-separated list of parent descriptors:

```properties
_parent = author
_parent = blog.posts, root
_parent = owner.blog.posts, root
```

Each descriptor is one of:

| Form | Meaning |
|---|---|
| `prop` | Try `this[prop]` as the parent. |
| `prop.virtual` | Walk through `prop`, then virtual sub-relation `virtual`. |
| `prop.virtual.collection` | Walk through `prop`, `virtual`, `collection`. |
| `root` | Application root. |
| `prop[name]` | `[name]` suffix is ignored. |

### `_children`

The special subnode mapping. Value can be a descriptor string or nested subproperties.

```properties
# Old-style descriptor
_children = collection(Comment)

# New-style subproperties
_children.collection = Comment
_children.local = post_id
_children.foreign = post_id
_children.order = created desc
```

If `_children.accessname` or `_children.group` is set, the children relation is also used as the property relation.

## Property Mapping Syntax

A non-`_` top-level key declares a property mapping with three forms:

| Form | Type |
|---|---|
| `title = column_name` | Primitive (scalar column) |
| `author = object(User)` | Reference (1-to-1) — old style |
| `comments = collection(Comment)` | Collection (1-to-many) — old style |
| `author.object = User` | Reference — new style |
| `comments.collection = Comment` | Collection — new style |
| `feed.mountpoint = FeedItem` | Mountpoint collection — virtual with prototype |

### Relation kinds

| Descriptor | Reference type |
|---|---|
| empty / column name | `PRIMITIVE` |
| `collection(Type)` / `.collection = Type` | `COLLECTION` (virtual unless property name is `_children`) |
| `mountpoint(Type)` / `.mountpoint = Type` | `COLLECTION` (virtual + with prototype) |
| `object(Type)` / `.object = Type` | `REFERENCE` |

## Relation Subproperties

For relations (`collection`, `object`, `mountpoint`):

### Flags

| Key | Values | Effect |
|---|---|---|
| `.readonly` | `true` / other | Blocks writes via this relation. Default false. |
| `.private` | `true` / other | Marks private; data changes don't propagate. |
| `.loadmode` | `aggressive`, `lazy`, other | Eager full-node load / ID-only list. |
| `.cachemode` | `aggressive`, other | Cache the full child list. |
| `.sortmode` | `auto`, other | Maintain sort order automatically. |

### Sorting / filtering

| Key | Value | Effect |
|---|---|---|
| `.order` | SQL ORDER BY fragment | e.g. `created desc, id asc` |
| `.filter` | SQL WHERE fragment with `${prop}` substitution | e.g. `published = true AND author_id = ${owner_id}` |
| `.filter.additionalTables` | extra tables/JOINs | Adds tables to the FROM clause. |
| `.hints` | raw SQL hint text | Inserted after `SELECT` (Oracle hint, etc.) |
| `.updatecriteria` | SQL expression | Custom update-detection. |

### Pagination

| Key | Value | Effect |
|---|---|---|
| `.maxSize` / `.limit` | integer | `LIMIT N`. `0` = no limit. |
| `.offset` | integer | `OFFSET N`. Default 0. |

### Grouping

| Key | Value | Effect |
|---|---|---|
| `.group` | column or expression | Enables group-by; creates virtual group nodes. |
| `.group.order` | SQL ORDER BY fragment | Order of group nodes. |
| `.group.prototype` | prototype name | Prototype for group nodes. |

### Access by name

| Key | Value | Effect |
|---|---|---|
| `.accessname` | column or property name | Allow `collection["name-value"]` lookup. |

### Constraints

| Key | Value | Effect |
|---|---|---|
| `.local` | column or property name | The "this side" of the join. First pair also sets `columnName`. |
| `.foreign` | column or property name | The "other side". |
| `.local.1` ... `.local.9` | additional locals | Multi-column references. |
| `.foreign.1` ... `.foreign.9` | additional foreigns | Multi-column references. |
| `.logicalOperator` | `AND`, `OR`, `XOR` | Default `AND`. |

## Database Column Types

Helma reads column metadata via JDBC. Bindings on PreparedStatement:

| JDBC type | Bind method |
|---|---|
| `BIT`, `BOOLEAN` | `setBoolean()` |
| `TINYINT`, `SMALLINT`, `INTEGER`, `BIGINT` | `setLong()` |
| `REAL`, `FLOAT`, `DOUBLE`, `NUMERIC`, `DECIMAL` | `setDouble()` |
| `CHAR`, `VARCHAR`, `OTHER` | `setString()` |
| `LONGVARCHAR` | string or character stream |
| `CLOB` | character stream |
| `BINARY`, `VARBINARY`, `LONGVARBINARY`, `BLOB` | bytes or binary stream |
| `DATE`, `TIME`, `TIMESTAMP` | `setTimestamp()` |
| `NULL` | `setNull()` |
| default | `setString()` |

## Virtual vs Real Nodes

| Case | Behaviour |
|---|---|
| `object(Type)` | Real 1:1 reference. Not virtual. |
| `collection(Type)` | Virtual collection (unless property name is `_children`). |
| `collection(Type)` + `.accessname` | Children accessed by named column. May create on demand. |
| `mountpoint(Type)` | Virtual collection with specific prototype. |
| `.group` set | Synthetic group-by nodes between parent and children. |

`createOnDemand()` is true for virtual, accessname-keyed collections, group-by nodes, complex references.

`aggressiveLoading` (`.loadmode = aggressive`) fetches full child nodes instead of just IDs.

`aggressiveCaching` (`.cachemode = aggressive`) keeps the entire child list cached.

## Complete Example

```properties
# Post/type.properties

_db    = main
_table = posts
_id    = post_id
_idgen = [uuid]
_name  = slug

_extends = HopObject
_parent  = blog, author

# Primitives
title    = title
body     = body_html
created  = created_at
modified = modified_at
slug     = slug
published = is_published
view_count = views

# References
author.object  = User
author.local   = author_id
author.foreign = user_id
author.loadmode = aggressive

blog.object    = Blog
blog.local     = blog_id
blog.foreign   = blog_id

# Collections
comments.collection = Comment
comments.local      = post_id
comments.foreign    = post_id
comments.order      = created asc
comments.accessname = slug

# Many-to-many via join table
tags.collection = Tag
tags.local      = post_id
tags.foreign    = tag_id
tags.filter.additionalTables = post_tags pt
tags.filter     = pt.post_id = a.post_id AND pt.tag_id = b.tag_id

# Virtual collections
recent10.collection = Post
recent10.local      = post_id
recent10.foreign    = post_id
recent10.order      = created desc
recent10.maxSize    = 10
recent10.readonly   = true

# Grouped
postsByMonth.collection = Post
postsByMonth.local = post_id
postsByMonth.foreign = post_id
postsByMonth.group = DATE_TRUNC('month', created_at)
postsByMonth.group.order = DATE_TRUNC('month', created_at) desc
postsByMonth.group.prototype = Month
```

## See Also

- [Relations](relations.md)
- [ID Generation](id-generation.md)
- [Data Sources](data-sources.md)
- [Custom Queries](queries.md)
- [`DbMapping.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/objectmodel/db/DbMapping.java)
- [`Relation.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/objectmodel/db/Relation.java)
