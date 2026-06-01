# helma.Http

HTTP client wrapper with cookie support, proxy support, authentication, redirects.

```javascript
app.addRepository("modules/helma/Http.js");
```

## Constructor

```javascript
var http = new helma.Http();
```

## Configuration

```javascript
http.setProxy("proxy.example.com:8080");
http.setUserAgent("MyApp/1.0");
http.setTimeout(30000);                    // socket timeout in ms
http.setReadTimeout(30000);                 // read timeout in ms
http.setMaxResponseSize(10 * 1024 * 1024); // 10 MB max
http.setFollowRedirects(true);              // default false
http.setBinary(true);                       // binary response handling
http.setContent("form data here");          // for POST/PUT
http.setContentType("application/json");
http.setHeader("Authorization", "Bearer ...");
http.setCookies(cookieJar);                 // pass cookies
http.setCredentials("user", "password");    // HTTP basic auth
http.setResponseHandler(handler);           // custom response processor
```

## Making Requests

### Get a URL

```javascript
var response = http.getUrl("https://example.com/data");

// With options
var response = http.getUrl("https://example.com/data", {
    binary: true,
    method: "POST"
});
```

The response object has:

- `content` — body as string or byte[]
- `contentType`
- `code` — HTTP status
- `headers` — headers map
- `cookies` — set-cookies from server
- `length` — Content-Length
- `lastModified`
- `eTag`

### Convenience

```javascript
var json = http.getUrl(url).content;
var data = JSON.parse(json);
```

## Cookies

`helma.Http.Cookie` — a cookie object:

```javascript
var c = new helma.Http.Cookie("name", "value");
print(c.getFieldValue());        // "name=value"

// Parse Set-Cookie header
var c = helma.Http.Cookie.parse("session=abc; Path=/; HttpOnly");
```

## Static Methods

### `helma.Http.evalUrl(url)`

Evaluate a URL and return the response object.

### `helma.Http.setProxy(proxyString)` / `helma.Http.getProxy()`

Global proxy setting.

### `helma.Http.isAuthorized(name, pwd)`

Check credentials against a remote server.

## Example: REST API Call

```javascript
app.addRepository("modules/helma/Http.js");
var http = new Http();
http.setHeader("Content-Type", "application/json");
http.setHeader("Authorization", "Bearer " + token);

http.setContent(JSON.stringify({ title: "Hello", body: "..." }));
var resp = http.getUrl("https://api.example.com/posts", { method: "POST" });

if (resp.code === 201) {
    var created = JSON.parse(resp.content);
    print("Created post id " + created.id);
}
```

## Example: Crawl with Cookies

```javascript
var http = new Http();
http.setFollowRedirects(true);

// Login
http.setContent("username=alice&password=secret");
var loginResp = http.getUrl("https://example.com/login", { method: "POST" });
var sessionCookies = loginResp.cookies;

// Fetch protected page
http.setCookies(sessionCookies);
var protectedResp = http.getUrl("https://example.com/dashboard");
print(protectedResp.content);
```

## See Also

- [`modules/helma/Http.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Http.js)
- [`getURL()`](../../reference/global-object.md) — built-in global function (simpler, less featureful)
