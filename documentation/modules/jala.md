# Jala Toolkit

**Jala** (Java And Light Application library) is a sister project to Helma offering high-level utilities for forms, internationalization, captcha, RSS, podcasts, indexing, and more. It lives in `modules/jala/code/`.

## Loading

Load each module via `app.addRepository`:

```javascript
// Global/main.js
app.addRepository("modules/jala/code/Form.js");
app.addRepository("modules/jala/code/Captcha.js");
app.addRepository("modules/jala/code/I18n.js");
```

After that the module's namespace (`jala.Form`, `jala.Captcha`, `jala.I18n`) is reachable from any code:

```javascript
var Form = jala.Form;
var form = new Form("signup", {});
```

To load **every** Jala module at once, use the `all.js` aggregator:

```javascript
app.addRepository("modules/jala/code/all.js");
```

Passing the bare `modules/jala/code` directory does **not** load the top-level `.js` files inside it — point at `all.js` or individual files.

## Modules at a Glance

| Module | Purpose |
|---|---|
| `jala.AsyncRequest` | Asynchronous remote calls |
| `jala.BitTorrent` | BitTorrent metadata (bencode/bdecode) |
| `jala.Captcha` | Image captcha generation |
| `jala.Date` | Calendar rendering, date editors |
| `jala.db` | H2 embedded database + helpers (loaded by `Database.js`) |
| `jala.DnsClient` | DNS lookups |
| `jala.Form` | Form rendering and validation framework |
| `jala.global` helpers | Type-checking helpers (`isString`, `isArray`, etc., from `Global.js`) |
| `jala.History` | Request history stack |
| `jala.HopObject` extensions | HopObject helpers (`getAccessName`, state predicates) |
| `jala.HtmlDocument` | HTML scraping with XPath |
| `jala.I18n` | Gettext-style i18n |
| `jala.ImageFilter` | Image filters (sharpen, blur, unsharp mask) |
| `jala.IndexManager` | Asynchronous Lucene index manager |
| `jala.ListRenderer` | Paged list renderer |
| `jala.Mp3` | MP3 tag (ID3v1/v2) editor |
| `jala.PodcastWriter` | iTunes-flavoured podcast RSS writer |
| `jala.RemoteContent` | Remote content cache (HTTP) |
| `jala.Rss20Writer` | RSS 2.0 feed writer |
| `jala.Utilities` | Misc utilities (`createPassword`, `diffObjects`) |
| `jala.XmlWriter` | Generic XML writer with namespace support |

## Highlights

### Form

The most powerful Jala module. `jala.Form` is a declarative form framework with validation, CSRF, and rendering:

```javascript
app.addRepository("modules/jala/code/Form.js");

var form = new jala.Form("signup", {});
form.addComponent(new jala.Form.Input("email").require("isemail").setLabel("Email"));
form.addComponent(new jala.Form.Password("password").require("minlength", 8));
form.addComponent(new jala.Form.Submit("submit").setValue("Sign up"));

function signup_action_post() {
    var tracker = form.validate(req.postParams);
    if (tracker.hasError()) {
        form.render();
    } else {
        var user = new User();
        form.save(tracker, user);
        root.users.add(user);
        res.redirect(user.href());
    }
}
```

Form components: `Input`, `Password`, `Hidden`, `Textarea`, `Date`, `Select`, `Radio`, `Checkbox`, `File`, `Image`, `Button`, `Submit`, `Skin`, `Fieldset`.

Validators: `isemail`, `isurl`, `minlength`, `maxlength`, `required`, plus custom functions.

### I18n

Gettext-style internationalization:

```javascript
app.addRepository("modules/jala/code/I18n.js");

var i18n = new jala.I18n();
i18n.setMessages(loadCatalog());          // POT/PO-parsed messages
i18n.setLocaleGetter(() => session.user.locale);

// gettext for singular
gettext("Hello, %s!", session.user.name);

// ngettext for plural
ngettext("%d post", "%d posts", post.count, post.count);

// markgettext for extraction without translation now
var key = markgettext("Welcome");
```

Use `./gradlew xgettext` to extract translatable strings into a `.pot` file.

### Captcha

```javascript
app.addRepository("modules/jala/code/Captcha.js");

var c = new jala.Captcha();
c.renderImage();             // writes PNG to response
session.data.captcha = c.getCaptcha();  // store expected answer

function verify(input) {
    return c.validate(input);
}
```

### IndexManager

Asynchronous Lucene index manager with a queue:

```javascript
app.addRepository("modules/jala/code/IndexManager.js");

var im = new jala.IndexManager("posts", "/var/index/posts", "english");
im.start();
im.add(post.toIndexDocument());
im.remove(otherPost._id);
im.optimize();
```

Works in the background — main thread isn't blocked.

### ListRenderer

```javascript
app.addRepository("modules/jala/code/ListRenderer.js");

var lr = new jala.ListRenderer(root.posts.list(), customRenderer);
lr.setPageSize(20);
lr.setItemSkin("post.teaser");
lr.setUrlParameterName("page");
lr.renderList();
lr.renderPageNavigation();
```

### Date.Calendar

```javascript
app.addRepository("modules/jala/code/Date.js");

var cal = new jala.Date.Calendar(root.posts);
cal.setHrefFormat("/blog/%Y/%m/%d/");
cal.setRenderer(customRenderer);
cal.render(today);
```

Generates a clickable calendar with posts highlighted on dates with content.

### RSS 2.0 / Podcast

```javascript
app.addRepository("modules/jala/code/Rss20Writer.js");

var rss = new jala.Rss20Writer({
    title: "My Blog",
    link: "https://example.com",
    description: "Posts"
});

for each (var post in root.posts.list({ maxSize: 20 })) {
    rss.addItem(rss.createItem({
        title: post.title,
        link: post.href(),
        description: post.body,
        pubDate: post.created,
        guid: post._id
    }));
}

res.contentType = "application/rss+xml";
res.write(rss.write());
```

For podcasts, use `jala.PodcastWriter` which adds iTunes-specific tags.

### Utilities

```javascript
app.addRepository("modules/jala/code/Utilities.js");

var u = new jala.Utilities();

u.createPassword(12, 3);          // random 12-char password, level 3 (incl. symbols)
u.diffObjects(obj1, obj2);        // recursive diff
u.patchObject(target, diff);      // apply diff
```

`new jala.Utilities()` is the standard way to invoke. There's also a pre-instantiated `jala.util` available after loading.

### Global Type Helpers

```javascript
app.addRepository("modules/jala/code/Global.js");

jala.isString(x);
jala.isArray(x);
jala.isDate(x);
jala.isObject(x);
jala.isFunction(x);
```

(Function names follow the file's `Global.js` exports.)

## Loading All

```javascript
app.addRepository("modules/jala/code/all.js");   // loads every jala.* module
```

Useful in `Global/main.js` if your app uses several Jala features.

## See Also

- [Bundled JS module list](index.md)
- [Tools](tools.md) — the `modules/tools` bundle
- [`modules/jala/code/`](https://github.com/DionixGmbH/helma/src/branch/main/modules/jala/code) — sources
- [`modules/jala/docs/`](https://github.com/DionixGmbH/helma/src/branch/main/modules/jala/docs) — Jala's own docs
