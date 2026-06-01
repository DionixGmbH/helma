# helma.Url

URL parser — splits a URL into its components.

```javascript
app.addRepository("modules/helma/Url.js");
```

## Constructor

```javascript
var url = new helma.Url("https://user:pass@www.example.co.uk:8080/path/to/file.html?q=1#section");
```

## Fields

After construction, these properties are populated:

| Field | Example value |
|---|---|
| `protocol` | `"https"` |
| `user` | `"user"` |
| `password` | `"pass"` |
| `host` | `"www.example.co.uk"` |
| `domain` | `"example.co.uk"` |
| `domainName` | `"example"` |
| `topLevelDomain` | `"co.uk"` |
| `pathString` | `"/path/to/file.html"` |
| `path` | `["path", "to"]` (array of segments) |
| `file` | `"file.html"` |
| `queryString` | `"q=1"` |
| `query` | `{ q: "1" }` (parsed) |

## Example

```javascript
app.addRepository("modules/helma/Url.js");
var url = new Url(req.getHeader("Referer"));

if (url.host === "example.com" || url.host.endsWith(".example.com")) {
    // same-site referrer
}

if (url.query.utm_source) {
    trackCampaign(url.query.utm_source);
}
```

## Static

### `helma.Url.PATTERN`

The compiled regex used for parsing.

## See Also

- [`modules/helma/Url.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Url.js)
- [`URL` built-in JS class](https://developer.mozilla.org/en-US/docs/Web/API/URL) — alternative, more standard
