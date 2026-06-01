# JSON

`modules/core/JSON.js` is a **compatibility stub** kept for legacy code. Modern Rhino has built-in `JSON.parse()` and `JSON.stringify()` (always available), so this file mostly exists for backwards compatibility.

## Built-in JSON (Use This)

```javascript
JSON.stringify({ a: 1, b: [2, 3] });           // '{"a":1,"b":[2,3]}'
JSON.stringify(obj, null, 2);                  // pretty-printed
JSON.stringify(obj, function(key, value) {     // with replacer
    return typeof value === "function" ? undefined : value;
});

JSON.parse('{"a": 1}');                         // { a: 1 }
JSON.parse(text, function(key, value) {         // with reviver
    return key === "date" ? new Date(value) : value;
});
```

Available everywhere in any Helma app. No `require` needed.

## Serializing HopObjects

HopObjects do **not** have a default `toJSON()` — define your own on the prototype:

```javascript
// Post/main.js
Post.prototype.toJSON = function() {
    return {
        id: this._id,
        title: this.title,
        body: this.body,
        author: this.author ? this.author.name : null,
        created: this.created.toISOString()
    };
};
```

`JSON.stringify(post)` will then use `toJSON()` automatically.

## See Also

- [MDN: JSON](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/JSON)
- [`modules/core/JSON.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/core/JSON.js)
