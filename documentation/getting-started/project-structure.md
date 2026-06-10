# Project Structure

The Helma source tree mixes a Gradle Java build with the bundled JavaScript modules and runtime layout. This page explains what every directory is for.

## Top-Level Layout

```
helma/
├── apps/                     ← your applications (created at runtime)
├── apps.properties           ← which apps to start
├── bin/                      ← `helma`, `helma.bat` startup scripts
├── build/                    ← Gradle output (.class files, javadoc, dist)
├── db/                       ← embedded XML database files
├── documentation/            ← this MkDocs documentation
├── gradle/                   ← Gradle wrapper
├── lib/                      ← runtime JARs (Rhino, Jetty, Commons, etc.)
├── log/                      ← rotating log files
├── modules/                  ← bundled JavaScript module libraries
│   ├── core/                 ← extends built-in JS prototypes (String, Array...)
│   ├── helma/                ← helma.File, helma.Http, helma.Mail, etc.
│   ├── jala/                 ← Jala toolkit (forms, RSS, captcha, i18n...)
│   └── tools/                ← Global tools loaded automatically
├── launcher/                 ← bootstrap module (Gradle subproject)
├── launcher.jar              ← thin launcher with all-deps classpath
├── server.properties         ← server-wide config
├── src/                      ← Java source
│   └── main/
│       ├── java/             ← Helma Java sources
│       └── resources/        ← XML/XSL resources
├── build.gradle              ← Gradle build script
├── devbox.json               ← Devbox toolchain (JDK 25)
└── README.md
```

## Inside an Application

```
apps/myapp/
├── app.properties            ← per-app overrides for server.properties
├── db.properties             ← DB data sources (db.properties only — not in db/)
├── class.properties          ← Java class → prototype mapping
├── cron.properties           ← scheduled JS function calls
├── messages.properties       ← i18n strings (optional)
├── Root/                     ← Root prototype (REQUIRED)
│   ├── main.js               ← actions, functions, macros
│   ├── main.skin             ← skin templates
│   ├── login.skin
│   ├── type.properties       ← DB mapping (optional)
│   └── ...
├── User/                     ← User prototype (used by app.authenticate)
│   ├── ...
└── Post/                     ← arbitrary prototype
    └── ...
```

### Prototype directory conventions

Inside a prototype directory, the file extension determines the resource type. See `TypeManager.checkFiles()` in `src/main/java/helma/framework/core/TypeManager.java`:

| Extension | Role | Loaded as |
|---|---|---|
| `.js` | JavaScript functions, actions, macros | Code resource compiled by Rhino |
| `.skin` | Template with macros | Skin (lazy-parsed) |
| `.properties` | `type.properties` and `<name>.properties` | DB mapping |
| `.hac` | Legacy Helma Action — synonym for `function name_action()` | Code resource |
| `.hsp` | Legacy Helma Server Page — converted to JS at load time | Code resource |

Inside `.js` files, the naming convention determines the role:

| Suffix | Role | URL form |
|---|---|---|
| `foo_action` | Action invoked when path ends with `/foo` | `/object/foo` |
| `foo_action_post` | Action only on POST | |
| `foo_action_ajax` | Action only on AJAX (XMLHttpRequest) | |
| `foo_action_ajax_post` | AJAX + POST | |
| `foo_socket` | WebSocket handshake gate at path ending `/foo` | `ws://…/object/foo` |
| `foo_socket_open` / `foo_socket_message` / `foo_socket_close` / `foo_socket_error` | WebSocket lifecycle handlers | |
| `foo_macro` | Skin macro `<% foo %>` (when bound to this) | |
| `foo_filter` | Skin filter, used as `<% bar \| foo %>` | |
| `getFoo` / `setFoo` | Reflected as JS property `foo` on the prototype | |
| `onRequest` | Called before every action | |
| `onResponse` | Called after every action | |
| `onCodeUpdate` | Called when the prototype's code has changed and recompiled | |
| `onPersist` | Called immediately before saving the object | |
| `onInit` | Called when a HopObject is reloaded from the DB | |
| `onLogout` | Called on the User node when `session.logout()` runs or the session times out | |

See [Actions](../framework/actions.md) for the full action resolution algorithm.

## Source Tree

```
src/main/java/helma/
├── main/                ← Server bootstrap, JettyServer, ApplicationManager
├── framework/           ← RequestTrans, ResponseTrans, beans, FutureResult
│   ├── core/            ← Application, RequestEvaluator, Session, Prototype, Skin
│   └── repository/      ← FileRepository, ZipRepository, SingleFileRepository
├── objectmodel/         ← INode, IDatabase, transient nodes
│   ├── db/              ← DbMapping, Relation, Node, NodeManager (relational ORM)
│   └── dom/             ← XmlDatabase, XmlReader, XmlWriter (embedded DB)
├── scripting/           ← ScriptingEngine interface
│   └── rhino/           ← Rhino implementation: RhinoCore, RhinoEngine, GlobalObject
│       ├── extensions/  ← FileObject, MailObject, ImageObject, FtpObject, etc.
│       └── debug/       ← Tracer, Profiler, HelmaDebugger
├── servlet/             ← AbstractServletClient, EmbeddedServletClient
├── extensions/          ← HelmaExtension SPI for Java extensions
├── image/               ← ImageGenerator, ImageInfo, image filters
└── util/                ← CronJob, ResourceProperties, Logger, Crypt, MarkdownProcessor, ...
```

## Resource Lookup Order

When resolving a resource (skin, function, property), Helma walks several layers:

1. **Skin path** (`res.skinpath` array) — directories or HopObjects checked first.
2. **Prototype repositories** in registration order — `apps/myapp/MyProto/` plus any `app.addRepository()` calls.
3. **Parent prototype chain** — defined by `_extends` in `type.properties` or implicit `hopobject` parent.
4. **Global prototype** (`apps/myapp/Global/`) — last-resort fallback for skins and macros.

`SkinManager.getSkin()` at `src/main/java/helma/framework/core/SkinManager.java:45` implements skin resolution. `Prototype.checkForUpdates()` at `src/main/java/helma/framework/core/Prototype.java:145` rescans every prototype directory on each request when in dev mode.

## Build Output

```
build/
├── classes/main/        ← compiled .class files
├── docs/
│   ├── javadoc/         ← Javadoc HTML
│   ├── jsdoc/           ← JSDoc HTML for modules/{core,helma,jala}
│   └── site/            ← MkDocs HTML (after running `mkdocs build`)
├── install/helma/       ← assembled installation
└── distributions/       ← .zip and .tar.gz release archives
```

Run `./gradlew distZip` or `./gradlew distTar` to build redistributable archives.
