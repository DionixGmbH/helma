# ID Generation

Helma supports several ID generation strategies for new HopObjects. Configure with `_idgen` in `type.properties`.

## Strategies at a Glance

| `_idgen` value | Strategy | Best for |
|---|---|---|
| *(unset, default)* | `SELECT MAX(id) + 1` | Small/medium apps, integer keys |
| `[max]` | Same — explicit | Same |
| `[hop]` | Helma's embedded sequence | Apps using the embedded XML DB |
| `[uuid]` | UUID v7 (time-ordered) | New apps, distributed systems, large-scale |
| `mysequence` | A DB sequence | PostgreSQL, Oracle, H2 |

## `[max]` — `SELECT MAX(id) + 1`

The default. On insert:

1. `SELECT MAX(<idcolumn>) FROM <table>` — get the current largest ID.
2. Add 1.
3. `INSERT` with that ID.

**Pros**: works with any integer column on any DB. No sequence required.

**Cons**: races between two concurrent inserts can produce duplicate keys → `ConcurrencyException` and a retry. With the framework's retry-up-to-8 logic this is usually invisible, but on very write-heavy workloads it adds latency.

```properties
_idgen = [max]   # or unset
```

**MySQL/MariaDB note**: On MySQL/MariaDB, Helma checks `[uuid]` first; otherwise the strategy is **forced to `[max]`** regardless of `_idgen` value. This means `[hop]` and named sequences do not work on MySQL — only `[max]` and `[uuid]` produce predictable results. The check is in `NodeManager.generateID()`.

## `[uuid]` — UUID v7

Generates a [UUID v7](https://datatracker.ietf.org/doc/rfc9562/) — time-ordered, globally unique, no database round-trip.

```properties
_idgen = [uuid]
```

The ID column should be `CHAR(36)`:

```sql
CREATE TABLE posts (
    post_id CHAR(36) PRIMARY KEY,
    ...
);
```

**UUID v7 format**: `01938a4b-2c0e-7a3f-9d11-abc123456789` — the first 48 bits encode milliseconds since epoch, followed by version, variant, and random bits.

**Pros**:

- No coordination with the database
- Time-ordered — efficient for B-tree indexes
- Globally unique — useful for sharded/distributed systems
- IDs can be generated client-side or by middle tier without round-trip

**Cons**:

- Larger than int (36 chars vs 8 bytes)
- Not human-friendly

Implementation: `helma.objectmodel.db.UUIDv7Generator`.

## `[hop]` — Helma's embedded ID generator

Uses an internal counter stored in the embedded XML database. Suitable for prototypes that live in the XML DB rather than SQL.

```properties
_idgen = [hop]
```

**Pros**:

- Works without a sequence or `MAX(id)`
- Fast — single in-memory increment

**Cons**:

- Limited to one app instance (no clustering)
- Embedded DB itself isn't designed for high throughput

If the prototype has no `_db` mapping at all, `[hop]` is implicit and writes to the embedded DB.

## Database Sequence

For PostgreSQL, Oracle, H2 — any DB supporting `nextval()`-style sequences:

```properties
_idgen = posts_id_seq    # the sequence name
```

Helma issues `SELECT NEXTVAL('posts_id_seq')` (PostgreSQL) or `SELECT mysequence.nextval FROM dual` (Oracle) before each `INSERT`.

Create the sequence beforehand:

```sql
-- PostgreSQL
CREATE SEQUENCE posts_id_seq START 1;

-- Oracle
CREATE SEQUENCE posts_id_seq START WITH 1 INCREMENT BY 1;
```

**Pros**:

- Database-native, no race conditions
- Compatible with DB clustering / replication

**Cons**:

- DB-specific — your `_idgen` setting becomes non-portable
- Needs schema setup before deployment

## Choosing

| If you... | Use |
|---|---|
| Are starting a new app | `[uuid]` |
| Are on MySQL/MariaDB | `[uuid]` or `[max]` (anything else is ignored — Helma forces `[max]`) |
| Are on PostgreSQL with sequences | sequence name (or `[uuid]`) |
| Are on Oracle | sequence name (or `[uuid]`) |
| Want zero DB round-trip on insert | `[uuid]` |
| Have a legacy table with auto-increment | `[max]` (Helma reads MAX before insert) |
| Are storing in embedded DB only | `[hop]` (implicit; not for MySQL) |

## Setting the ID Manually

For data migration or imports, you can set the ID directly:

```javascript
var post = new Post();
post._id = "01938a4b-2c0e-7a3f-9d11-abc123456789";   // manually set
post.title = "Migrated";
root.posts.add(post);
```

Be careful: bypasses `_idgen` and can create duplicates.

## ID Column Type

The column type for the ID:

| Strategy | Recommended column type |
|---|---|
| `[max]` | `BIGINT` or `INTEGER` |
| `[hop]` | string field in the XML DB |
| `[uuid]` | `CHAR(36)` or `UUID` (PostgreSQL) |
| Sequence | `BIGINT` |

Helma reads column types via JDBC metadata. For mismatched types (e.g. UUID strategy with INT column), you'll get type-conversion errors on insert.

## Generation Timing

IDs are generated at **insert time** — i.e. at `Transactor.commit()` for a freshly-added Node. To get the ID earlier:

```javascript
var post = new Post();
this.add(post);
post.persist();      // forces immediate insert
res.write(post._id); // ID now available
```

This is useful when you need the ID for a redirect URL or a child INSERT in the same transaction.

## Migrating ID Strategies

Switching `_idgen` for an existing table:

- `[max]` → `[uuid]`: change the column type from int to char(36); existing rows keep integer-like UUID strings, new rows get UUIDs. Plan a data migration to convert old IDs.
- `[max]` → sequence: set the sequence's `START WITH` to `MAX(current_id) + 1` first.
- Sequence → `[uuid]`: same as `[max]` → `[uuid]`.

Helma doesn't help with these migrations — they're schema-level changes.

## See Also

- [Type Properties Reference](type-properties.md)
- [Object-Relational Mapping](orm.md)
- [Embedded Database](embedded-database.md) — when `[hop]` is implicit
