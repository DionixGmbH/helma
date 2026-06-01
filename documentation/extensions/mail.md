# Mail Extension

`Mail` is the global JS constructor for sending emails via SMTP using JavaMail.

Implementation: `src/main/java/helma/scripting/rhino/extensions/MailObject.java`.

For higher-level mail (HTML, multipart, attachments) see [`helma.Mail`](../modules/helma/mail.md) — which is built on top of this class.

## Constructor

```javascript
var mail = new Mail();         // uses default mail.host / smtp
```

The constructor takes no JS arguments. The SMTP host is read from:

1. `app.properties::mail.host`
2. `app.properties::smtp`
3. `server.properties::smtp`

If none is set, sending will fail at `send()` time.

## API

### `addTo(addr, name)`

```javascript
mail.addTo("alice@example.com");
mail.addTo("bob@example.com", "Bob Smith");
```

Adds a To recipient. Repeated calls accumulate.

### `addCC(addr, name)`
### `addBCC(addr, name)`

Same as `addTo` but for CC and BCC.

### `setFrom(addr, name)`

```javascript
mail.setFrom("noreply@example.com");
mail.setFrom("alice@example.com", "Alice");
```

### `setReplyTo(addr)`

Reply-To header.

### `setSubject(subject)`

```javascript
mail.setSubject("Welcome to Example!");
```

### `setText(text)`

```javascript
mail.setText("Hello, world!");
```

Sets the plain-text body. Replaces any previous text.

### `addText(text)`

Append to the text body.

### `addPart(content, filename)`

```javascript
// String content
mail.addPart("Inline content");

// With filename — becomes an attachment
mail.addPart(byteArray, "report.pdf");

// Add a file
var file = new java.io.File("/tmp/report.pdf");
mail.addPart(file);
```

For multipart support (HTML body with text fallback), use [`helma.Mail`](../modules/helma/mail.md).

### `setMultipartType(subtype)`

`"mixed"` (default for attachments), `"alternative"` (for HTML+text), `"related"` (for inline images).

### `getMultipartType()`

Returns the current subtype.

### `send()`

```javascript
mail.send();
```

Connect to SMTP, send the message. Throws on failure.

### Status

```javascript
mail.status      // numeric status code (OK = 0, etc.)
```

After `send()`, check `mail.status === 0` for success.

## Example: Simple Mail

```javascript
function sendWelcome(user) {
    var mail = new Mail();
    mail.setFrom("noreply@example.com", "Example");
    mail.addTo(user.email, user.name);
    mail.setSubject("Welcome!");
    mail.setText("Hello " + user.name + ",\n\nThanks for signing up.\n");
    try {
        mail.send();
    } catch (e) {
        app.logError("Failed to send welcome mail to " + user.email, e);
    }
}
```

## Example: With Attachment

```javascript
function sendInvoice(user, pdfBytes) {
    var mail = new Mail();
    mail.setFrom("billing@example.com");
    mail.addTo(user.email);
    mail.setSubject("Your invoice");
    mail.setText("Please find your invoice attached.");
    mail.addPart(pdfBytes, "invoice.pdf");
    mail.send();
}
```

## Properties

### `Mail.props`

The static property `Mail.props` exposes the JavaMail session properties — useful for reading defaults or overriding for advanced setups.

## Configuration

In `app.properties`:

```properties
mail.host = smtp.example.com
mail.port = 587
mail.username = noreply@example.com
mail.password = secret
mail.starttls.enable = true
mail.smtp.auth = true
mail.from = noreply@example.com         # default From
```

Standard JavaMail properties (`mail.smtp.*`, `mail.transport.protocol`, etc.) are also honoured.

## Registration

`Mail` is registered by `RhinoCore.java` via `MailObject.init(global, app.getProperties())`.

## Limitations

- No DKIM signing (use an external mailer for that)
- No retry-on-failure logic — implement yourself or use `helma.Mail`
- Synchronous send — blocks the request thread

For better mail use [`helma.Mail`](../modules/helma/mail.md) which supports HTML bodies, multipart/alternative, and attachment handling.

## See Also

- [`helma.Mail` module](../modules/helma/mail.md) — higher-level API
- [JavaMail docs](https://eclipse-ee4j.github.io/mail/)
- [`MailObject.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/scripting/rhino/extensions/MailObject.java)
