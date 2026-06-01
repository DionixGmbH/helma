# Array

`modules/core/Array.js` extends `Array.prototype` and the `Array` constructor with a couple of utility methods.

## Instance Methods

### `contains(item)` → boolean

Test whether the array contains a value (alias for `indexOf(item) !== -1`).

```javascript
[1, 2, 3].contains(2);     // true
["a","b","c"].contains("x"); // false
```

Note: this is now redundant with the standard `Array.prototype.includes()`.

## Static Methods

### `Array.union(...arrays)` → Array

Return the union of multiple arrays — every element from any input array, deduplicated.

```javascript
Array.union([1,2,3], [2,3,4]);     // [1, 2, 3, 4]
Array.union([1], [2], [3], [1,2]); // [1, 2, 3]
```

### `Array.intersection(...arrays)` → Array

Return the intersection — elements present in all input arrays.

```javascript
Array.intersection([1,2,3], [2,3,4]);  // [2, 3]
Array.intersection([1,2,3], [4,5]);    // []
```

## See Also

- [`modules/core/Array.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/core/Array.js)
