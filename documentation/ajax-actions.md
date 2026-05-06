# AJAX Action Resolution

Helma can differentiate between normal HTTP requests and AJAX requests (XMLHttpRequest). This allows you to define separate action handlers for AJAX calls, while falling back to regular actions when no AJAX-specific handler is found.

## Detection

A request is considered an AJAX request when it includes the `X-Requested-With: XMLHttpRequest` header. This header must be set explicitly by client-side code or by a library that adds it automatically (e.g., jQuery's `$.ajax()`).

You can check this in your server-side JavaScript via `req.isXmlHttpRequest()`.

## Action Resolution Order

When Helma resolves which action function to invoke, it tries candidates from most specific to least specific. For an AJAX `POST` request to the `main` action, the resolution order is:

| Priority | Function Name | Matches |
|----------|--------------|---------|
| 1 | `main_action_ajax_post` | AJAX + POST only |
| 2 | `main_action_ajax` | Any AJAX request |
| 3 | `main_action_post` | Any POST request |
| 4 | `main_action` | Any GET, POST, or HEAD request |

The generic `main_action` fallback (without method suffix) is only tried for GET, POST, and HEAD requests. For other HTTP methods (PUT, DELETE, etc.), a matching handler must be defined: for AJAX requests, `main_action_ajax_put`, `main_action_ajax`, or `main_action_put`; for non-AJAX requests, only `main_action_put`.

For a non-AJAX `POST` request, steps 1 and 2 are skipped:

| Priority | Function Name | Matches |
|----------|--------------|---------|
| 1 | `main_action_post` | Any POST request |
| 2 | `main_action` | Any GET, POST, or HEAD request |

The XML-RPC check (`main_action_xmlrpc`) takes highest priority when applicable and is evaluated before AJAX resolution.

## Example

A typical use case is returning HTML for normal requests and JSON for AJAX requests:

```javascript
// Handles normal GET requests to /example
function main_action() {
    res.data.title = "Example";
    renderSkin("main");
}

// Handles any AJAX request to /example regardless of HTTP method
function main_action_ajax() {
    res.contentType = "application/json";
    res.write(JSON.stringify({ status: "ok" }));
}

// Handles only AJAX POST requests to /example
function main_action_ajax_post() {
    var data = req.postParams;
    // process form data submitted via AJAX
    res.contentType = "application/json";
    res.write(JSON.stringify({ saved: true }));
}
```

## Scripting API

### `req.action`

The `req.action` property always returns the base action name, regardless of which variant was matched:

| Function Matched | `req.action` Value |
|------------------|--------------------|
| `main_action` | `main` |
| `main_action_post` | `main` |
| `main_action_ajax` | `main` |
| `main_action_ajax_post` | `main` |

Use `req.isXmlHttpRequest()` to check whether the request is an AJAX request.

### `req.isXmlHttpRequest()`

Returns `true` if the current request includes the `X-Requested-With: XMLHttpRequest` header, `false` otherwise.
