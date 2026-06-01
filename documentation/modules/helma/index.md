# Helma Modules (`modules/helma/`)

The `modules/helma/` directory contains the Helma toolkit — high-level JavaScript modules wrapping common functionality.

## Loading

Each module is enabled by adding its `.js` file as a repository to your app. Typically done once at startup:

```javascript
// Global/main.js
app.addRepository("modules/helma/File.js");
app.addRepository("modules/helma/Mail.js");
app.addRepository("modules/helma/Http.js");
```

After that the module's namespace (e.g. `helma.File`, `helma.Mail`) is reachable from any code:

```javascript
// In any action
var f = new helma.File("/tmp/foo");
var mail = new helma.Mail();
```

To load **every** helma module at once, use the `all.js` aggregator (which itself loads every `helma.*` module):

```javascript
app.addRepository("modules/helma/all.js");
```

Passing the bare `modules/helma` directory to `addRepository` does **not** load the top-level `.js` files inside it — pass the file (or `all.js`).

## Bundled Modules

| Module | Purpose | File |
|---|---|---|
| [Aspects](aspects.md) | Aspect-oriented programming utilities | `modules/helma/Aspects.js` |
| [Chart](chart.md) | Chart generation | `modules/helma/Chart.js` |
| [Color](color.md) | Color manipulation | `modules/helma/Color.js` |
| [Database](database.md) | High-level DB wrappers (over `getDBConnection`) | `modules/helma/Database.js` |
| [File](file.md) | File system operations | `modules/helma/File.js` |
| [Ftp](ftp.md) | FTP client | `modules/helma/Ftp.js` |
| [Group](group.md) | JGroups-based distributed group API | `modules/helma/Group.js` |
| [Html](html.md) | HTML construction helpers | `modules/helma/Html.js` |
| [Http](http.md) | HTTP client | `modules/helma/Http.js` |
| [Image](image.md) | Image manipulation | `modules/helma/Image.js` |
| [Mail](mail.md) | Email sending | `modules/helma/Mail.js` |
| [Search](search.md) | Full-text search (Lucene-based) | `modules/helma/Search.js` |
| [Skin](skin.md) | Skin loading and rendering helpers | `modules/helma/Skin.js` |
| [Ssh](ssh.md) | SSH/SCP client | `modules/helma/Ssh.js` |
| [Url](url.md) | URL parsing | `modules/helma/Url.js` |
| [Zip](zip.md) | ZIP archive read/write | `modules/helma/Zip.js` |

## Aliasing for Convenience

You can stash a module on the global scope for shorter access:

```javascript
// Global/main.js
app.addRepository("modules/helma/Mail.js");
app.addRepository("modules/helma/File.js");

global.Mail = helma.Mail;
global.File = helma.File;
```

Now `new Mail(...)`, `new File(...)` work everywhere.

## See Also

- [Concepts: Scripting Environment](../../concepts/scripting-environment.md)
- [Modules overview](../index.md)
- The individual module pages
