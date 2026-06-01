# Number

`modules/core/Number.js` extends `Number.prototype` and adds the `Number.Sorter` class.

## Instance Methods

### `format(pattern, locale)` → String

Format a number using a Java `DecimalFormat` pattern.

```javascript
(1234.5678).format("#,##0.00");           // "1,234.57"
(0.5).format("0.0%");                      // "50.0%"
(42).format("000");                        // "042"
(1000000).format("#,###");                 // "1,000,000"
```

With locale:

```javascript
(1234.5).format("#,##0.00", "de");         // "1.234,50"  (German uses , as decimal)
```

### `toPercent(total, pattern, locale)` → String

Compute this number as a percentage of `total`, formatted.

```javascript
(50).toPercent(200);                       // "25.00%"
(50).toPercent(200, "0%");                 // "25%"
```

## Static Classes

### `Number.Sorter(field, order)`

A comparator function builder for sorting arrays of objects by a numeric field.

```javascript
var posts = [...];
posts.sort(new Number.Sorter("viewCount", "desc"));
```

Equivalent to `posts.sort((a, b) => b.viewCount - a.viewCount)`.

## See Also

- [`DecimalFormat` Javadoc](https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html)
- [`modules/core/Number.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/core/Number.js)
