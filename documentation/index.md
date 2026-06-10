# Helma

**Helma is a server-side JavaScript web application framework written in Java.** It runs on the JVM, uses [Mozilla Rhino](https://github.com/mozilla/rhino) for scripting, embeds a [Jetty](https://eclipse.dev/jetty/) web server, and pioneered the codeless mapping of application objects to relational database tables — what other frameworks now call ORM.

This documentation describes every feature of the framework as implemented in [src/main/java](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java). It is written for both human developers and AI coding assistants.

## Why Helma?

- **No compilation cycle.** Edit a `.js` file, reload the page — the change is live. The file watcher recompiles prototypes automatically.
- **Object paths as URLs.** `/users/alice/photos/holiday` walks the persistent object graph. No router configuration. No annotations.
- **Codeless ORM.** A `type.properties` file maps a prototype to a database table. `parent.children.add(child)` writes to the DB.
- **Embedded object database.** Unmapped objects are automatically persisted to an embedded XML store. Zero setup.
- **Built-in web server.** Jetty is bundled and configurable. Run `./bin/helma` and serve HTTP on port 8080.
- **Full Java interop.** Every Java class on the classpath is callable from JavaScript. Use any JDBC driver, any image library, any messaging system.

## At a Glance

A minimal Helma application is a directory of `.js` files, `.skin` templates, and optionally a `type.properties` file:

```
apps/myapp/
├── Root/
│   ├── main.skin
│   ├── main.js
│   └── type.properties
└── User/
    ├── show.skin
    ├── actions.js
    └── type.properties
```

A request to `http://localhost:8080/myapp/users/alice` resolves to the `User` HopObject named `alice`, then renders its action — typically `main` — which renders `main.skin`. Macros in the skin call back into JavaScript to pull data.

## Documentation Map

<div class="grid cards" markdown>

-   :material-rocket-launch: **[Getting Started](getting-started/index.md)**

    Install Helma, run the demo app, create your first application.

-   :material-school: **[Concepts](concepts/index.md)**

    The mental model: prototypes, repositories, request lifecycle, the embedded scripting environment.

-   :material-application-cog: **[Framework](framework/index.md)**

    Actions, skins, request and response objects, sessions, cron jobs, file uploads.

-   :material-database: **[Database](database/index.md)**

    The object-relational mapping: `type.properties`, relations, transactions, the embedded DB.

-   :material-language-javascript: **[Scripting](scripting/index.md)**

    The Rhino engine, CommonJS `require`, Java interop, debugging and profiling.

-   :material-book-open-page-variant: **[Reference](reference/index.md)**

    Every property of `app`, `req`, `res`, `session`, every server-wide setting, every CLI flag.

-   :material-package-variant: **[Modules](modules/index.md)**

    The bundled JavaScript libraries: `helma.File`, `helma.Http`, `helma.Mail`, the `jala` toolkit.

-   :material-puzzle: **[Extensions](extensions/index.md)**

    The Java-side scriptable extensions (`File`, `Mail`, `Image`, `Ftp`, `Xml`).

-   :material-server: **[Deployment](deployment/index.md)**

    Standalone, behind a reverse proxy, in a servlet container, Jetty configuration.

</div>

## Status

Helma is mature, stable, and largely feature-complete. The framework was first released around 1999 and powered the Austrian Broadcasting Corporation (ORF) website along with weblog hosts like [antville.org](https://antville.org), [twoday.net](https://twoday.net), and [blogger.de](https://blogger.de). Active maintenance continues at [github.com/DionixGmbH/helma](https://github.com/DionixGmbH/helma) under a permissive license.

## Conventions Used in This Documentation

- File paths are repository-relative unless otherwise noted (e.g. `src/main/java/helma/main/Server.java`).
- Code examples that begin with `// JavaScript` or appear under a `.js` filename run in the embedded Rhino engine inside a Helma application.
- Code examples that begin with `// Skin` or appear under a `.skin` filename are Helma skin templates.
- Configuration examples shown under `app.properties`, `server.properties`, `db.properties`, etc. use Java `.properties` syntax.
- Method signatures in the reference sections list the Java-side declaration with JavaScript-callable names; both work transparently.
