# Type Properties Reference

Each prototype in Helma can have a `type.properties` file that defines how objects of that type are mapped to a relational database. All mapping properties are prefixed with an underscore (`_`).

## Database Mapping

### `_db`

<!-- TODO -->

### `_table`

<!-- TODO -->

## Identity

### `_id`

<!-- TODO -->

### `_idgen`

Defines the ID generation strategy for new objects of this type.

| Value | Description |
|-------|-------------|
| *(unset)* | Default. Uses `SELECT MAX(id) + 1` to generate the next integer ID. |
| `[max]` | Explicitly selects the `SELECT MAX(id) + 1` strategy. |
| `[hop]` | Uses Helma's internal embedded database ID generator. |
| `[uuid]` | Generates a UUID v7 (RFC 9562) identifier. Time-ordered, globally unique, no database coordination needed. The ID column should be `CHAR(36)`. |
| *sequence name* | Uses a database sequence (Oracle, PostgreSQL, H2). The value is the sequence name, e.g. `my_sequence`. |

On MySQL/MariaDB, the default strategy is always `[max]` regardless of the `_idgen` setting, unless `[uuid]` or `[hop]` is explicitly configured.

**Example:**

```properties
_db = myDataSource
_table = events
_id = event_id
_idgen = [uuid]
```

### `_name`

<!-- TODO -->

### `_prototype`

<!-- TODO -->

### `_extensionId`

<!-- TODO -->

## Inheritance

### `_extends`

<!-- TODO -->

### `_parent`

<!-- TODO -->

## Collections

### `_children`

<!-- TODO -->

## Property Mappings

<!-- TODO: document non-underscore property mappings, e.g. `propertyName = column_name` -->
