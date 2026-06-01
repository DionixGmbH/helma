# String

`modules/core/String.js` adds a large set of utility methods to `String.prototype` and the `String` constructor. The file is bundled in the distribution but is **not** auto-loaded; add it explicitly with `app.addRepository("modules/core/all.js")` or via `apps.properties`.

## Validation

### `isDateFormat()` → boolean

Test whether this string is a valid Java date format pattern.

### `isUrl()` → boolean

Test whether this string is a valid URL.

### `isFileName()` → boolean

Test whether this string is a valid file name (no invalid characters).

### `isHexColor()` → boolean

Test whether this string is a hex color (`#RGB`, `#RRGGBB`, etc.).

### `isAlphanumeric()` → boolean

Test whether this string contains only letters and digits.

### `isAlpha()` → boolean

Test whether this string contains only letters.

### `isNumeric()` → boolean

Test whether this string contains only digits.

### `isEmail()` → boolean

Test whether this string is a valid email address.

## Conversion

### `toDate(format, timezone)` → Date

Parse this string into a Date using a Java SimpleDateFormat pattern.

```javascript
"2026-06-01".toDate("yyyy-MM-dd");
"2026-06-01 12:00".toDate("yyyy-MM-dd HH:mm", "Europe/Vienna");
```

### `toFileName()` → String

Convert this string to a safe filename — replaces unsafe characters with underscores.

### `toHexColor()` → String

Convert to a hex color string.

### `toAlphanumeric()` → String

Strip non-alphanumeric characters.

## Encoding

### `encode()` → String

HTML-encode this string. Equivalent to the global `encode()`.

### `encodeXml()` → String

XML-encode this string.

### `encodeForm()` → String

Encode for safe use as a form field value (preserves whitespace).

### `format(...)` → String

`printf`-style formatting.

```javascript
"Hello, %s! You are %d years old.".format("Alice", 42);
// → "Hello, Alice! You are 42 years old."
```

### `stripTags()` → String

Remove HTML/XML tags.

### `enbase64()` → String / `debase64()` → String

Base64 encode/decode.

```javascript
"Hello".enbase64();              // "SGVsbG8="
"SGVsbG8=".debase64();           // "Hello"
```

### `md5()` → String

Compute the MD5 hash as a hex string.

```javascript
"password123".md5();   // "482c811da5d5b4bc6d497ffa98491e38"
```

## String Manipulation

### `capitalize(limit)` → String

Capitalize the first character of each word (or the first `limit` words).

```javascript
"hello world".capitalize();        // "Hello World"
"hello world".capitalize(1);       // "Hello world"
```

### `titleize()` → String

Convert to title case — like `capitalize` but with smart article handling.

### `embody(limit, clipping, delimiter)` → String

Truncate to `limit` characters with optional clipping suffix.

### `head(limit, clipping, delimiter)` → String

Same as `embody` — alias.

### `tail(limit, clipping, delimiter)` → String

Truncate from the end (keep last N characters).

### `clip(limit, clipping, delimiter)` → String

Alias for `head`.

### `group(interval, str, ignoreWhiteSpace)` → String

Group characters into chunks of `interval`, separated by `str`.

```javascript
"1234567890".group(4, "-");          // "1234-5678-90"
"1234567890".group(3, " ");          // "123 456 789 0"
```

### `unwrap(removeTags, replacement)` → String

Remove line breaks, optionally remove tags too.

### `pad(str, length, mode)` → String

Pad to a given length on left (`"left"`), right (`"right"`), or both sides (`"middle"`).

```javascript
"abc".pad("0", 6, "left");           // "000abc"
"abc".pad(" ", 6, "right");          // "abc   "
"abc".pad("-", 7, "middle");         // "--abc--"
```

### `contains(substr, fromIndex)` → boolean

Test whether this string contains a substring.

```javascript
"hello world".contains("world");     // true
```

### `count(str)` → int

Count occurrences of a substring.

```javascript
"abcabc".count("a");                // 2
```

### `diff(otherString, separator)` → String

Return characters that differ between this and another string.

### `entitize()` → String

Convert characters to HTML entities (`&copy;`, etc.).

## Static Methods

### `String.Sorter(field, order)` → Function

Comparator-function builder for string fields.

```javascript
items.sort(new String.Sorter("title", "asc"));
```

### `String.compose(...args)` → String

Concatenate arguments — `String.compose("a", "b", "c")` = `"abc"`.

### `String.random(length, mode)` → String

Generate a random string.

```javascript
String.random(10);                   // 10 random chars
String.random(10, "alpha");          // letters only
String.random(10, "numeric");        // digits only
```

### `String.join(str1, str2, glue)` → String

Join strings with glue (handles null/empty gracefully).

```javascript
String.join("a", null, ", ");        // "a"
String.join(null, "b", ", ");        // "b"
String.join("a", "b", ", ");         // "a, b"
```

## Constants

- `String.NULLSTR` — empty string `""`

## See Also

- [`modules/core/String.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/core/String.js)
