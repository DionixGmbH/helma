# helma.Mail

High-level email sending with HTML/multipart support, attachments, encodings.

```javascript
app.addRepository("modules/helma/Mail.js");
```

## Constructor

```javascript
var mail = new helma.Mail(host, port);
```

Both `host` and `port` are optional — defaults read from `app.properties::smtp` and `app.properties::smtpPort`.

## Authentication

```javascript
mail.setAuthentication("user", "password");
mail.setDebug(true);              // verbose SMTP transcript to log
```

## Headers

```javascript
mail.setFrom("alice@example.com", "Alice");
mail.setTo("bob@example.com");
mail.addTo("carol@example.com", "Carol");
mail.addCC("dan@example.com");
mail.addBCC("eve@example.com");
mail.setReplyTo("noreply@example.com");
mail.setSubject("Hello!");
mail.setHeader("X-Custom", "value");
mail.addHeader("List-Id", "myapp");
mail.getHeader("Subject");
mail.removeHeader("X-Custom");
```

## Body

```javascript
mail.setText("Plain text body");
mail.addText("more text");
```

For HTML/text multipart:

```javascript
mail.setMultipartType("alternative");
mail.addPart("Plain version", null, "text/plain");
mail.addPart("<h1>HTML version</h1>", null, "text/html");
```

## Attachments

```javascript
// Attach a string
mail.addPart("File contents", "filename.txt");

// Attach bytes
mail.addPart(byteArray, "report.pdf", "application/pdf");

// Attach a file
var f = new java.io.File("/var/files/report.pdf");
mail.addPart(f, "report.pdf");
```

## Sending

```javascript
mail.send();
```

Throws on failure. The thrown exception has details from JavaMail.

## Other

```javascript
mail.writeToFile("/var/spool/outbox");      // save to disk instead of sending
mail.getSource();                            // get RFC 822 source
mail.getMessage();                           // underlying JavaMail Message
mail.getBuffer();                            // current text buffer
mail.getMultipart();                         // current multipart wrapper
```

## Static

### `helma.Mail.toString()` → String

### `helma.Mail.example(host, sender, addr, subject, text)`

Send a test mail.

## Example: HTML Newsletter

```javascript
app.addRepository("modules/helma/Mail.js");
var mail = new Mail();
mail.setFrom("newsletter@example.com", "Example Newsletter");
mail.setTo("subscriber@example.com");
mail.setSubject("This week in Example");
mail.setMultipartType("alternative");

mail.addPart("Visit our site to read the news.", null, "text/plain");

var html = renderSkinAsString("newsletter", { week: this.weekNumber });
mail.addPart(html, null, "text/html");

mail.send();
```

## Example: Receipt with PDF Attachment

```javascript
var mail = new helma.Mail();
mail.setFrom("billing@example.com");
mail.setTo(user.email);
mail.setSubject("Your receipt for order #" + order.id);
mail.setText("Please find your receipt attached.");
mail.addPart(generatePdf(order), "receipt-" + order.id + ".pdf", "application/pdf");
mail.send();
```

## Configuration

In `app.properties`:

```properties
smtp = smtp.example.com
smtpPort = 587
mail.user = noreply@example.com
mail.password = secret
mail.starttls.enable = true
mail.smtp.auth = true
mail.from = noreply@example.com         # default From address
```

Plus all standard JavaMail properties (`mail.smtp.*`).

## See Also

- [`Mail` extension](../../extensions/mail.md) — underlying class
- [JavaMail docs](https://eclipse-ee4j.github.io/mail/)
- [`modules/helma/Mail.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Mail.js)
