# Database

Helma was one of the first frameworks to offer codeless object-relational mapping. This section explains everything about how persistent objects are stored, queried, and related.

| Page | Topic |
|---|---|
| [Object-Relational Mapping](orm.md) | The conceptual model. |
| [Type Properties Reference](type-properties.md) | Every option in `type.properties`. |
| [Relations](relations.md) | The four relation types — primitive, reference, collection, complex. |
| [Data Sources](data-sources.md) | `db.properties` and JDBC configuration. |
| [ID Generation](id-generation.md) | UUID v7, sequence, `MAX(id)+1`, `[hop]`. |
| [Embedded Database](embedded-database.md) | The XML-backed object store. |
| [Transactions](transactions.md) | DB transaction semantics. |
| [Custom Queries](queries.md) | When ORM isn't enough — raw JDBC. |

## At a Glance

A prototype with this `type.properties`:

```properties
_db = main
_table = posts
_id = post_id
_idgen = [uuid]

title = title
body = body
published = published_at
author.object = User
author.local = author_id
author.foreign = user_id

comments.collection = Comment
comments.local = post_id
comments.foreign = post_id
comments.order = created desc
```

…becomes:

```sql
CREATE TABLE posts (
    post_id CHAR(36) PRIMARY KEY,
    title TEXT,
    body TEXT,
    published_at TIMESTAMPTZ,
    author_id BIGINT REFERENCES users(user_id)
);
```

…and gives you, from JavaScript:

```javascript
var p = new Post();
p.title = "Hello world";
p.body = "Long content";
p.author = root.users.alice;
root.posts.add(p);

// Querying
var all = root.posts.list();
var howMany = root.posts.size();
var first = root.posts.get(0);
var named = root.posts.get("hello-world");

// Relations
var comments = p.comments.list();
var n = p.comments.size();

// Save
p.title = "Edited";       // marks dirty
res.commit();             // flushes UPDATE
```

No SQL written. No annotations. No DAO classes.
