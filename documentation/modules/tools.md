# Tools (`modules/tools/Global/`)

The `modules/tools/Global/` directory contains bundled developer tools that can be enabled per-application. Each tool installs a small admin/dev UI on top of its host app.

## Loading

The tools are designed to be loaded as a **repository**, so the framework picks up the `Global` prototype directory and its associated `.skin` files alongside the `.js` files. The default Helma distribution adds this in `apps.properties` for the `welcome` app:

```properties
welcome.repository.1 = modules/tools
```

To enable any of the tools in your own app, add the same repository:

```properties
# apps.properties
myapp.repository.1 = modules/tools
```

This wires the `Global` directory in `modules/tools/Global` into your app, exposing all of the tool actions and skins together. Loading a single tool's `.js` file alone (`addRepository("modules/tools/Global/helma.shell.js")`) loads the JavaScript but **not** the associated `.skin` files — the tool's UI won't render correctly.

## Bundled Tools

### `helma.auth`

Simple session-based authentication helper. Provides a login skin and helper functions for common auth flows.

Files:

- `Global/helma.auth.js` — login/logout helpers
- `Global/helma.auth.login.skin` — basic login form

### `helma.Inspector`

Web-based HopObject inspector. Browse the live object graph through a UI — useful during development.

Files:

- `Global/helma.Inspector.js` — inspector actions
- `Global/helma.Inspector.main.skin` — inspector UI

!!! warning
    Don't enable the Inspector in production — it exposes data and internal state. Restrict by IP or remove the addRepository call in production.

### `helma.shell`

An in-browser JavaScript shell — REPL evaluating in the live app context.

Files:

- `Global/helma.shell.js` — shell action
- `Global/helma.shell.skin` — shell UI

!!! warning
    Never enable in production. Anyone with access can execute arbitrary JS in your app, including OS commands via Java interop.

### `helma.sqlshell`

In-browser SQL shell — query the configured DbSources from the browser.

Files:

- `Global/helma.sqlshell.js` — sqlshell actions
- `Global/helma.sqlshell.main.skin` — main UI
- `Global/helma.sqlshell.page.skin` — result page
- `Global/helma.sqlshell.selectdb.skin` — DbSource selector

!!! warning
    Same caveat — exposes raw SQL execution. Don't enable in production.

### `helma.Markup`

Lightweight markup-to-HTML converter. Useful for transforming user-generated content.

Files:

- `Global/helma.Markup.js`

## Production Usage

In production, do not add `modules/tools` as a repository for any internet-facing app — the shell and SQL shell are HTTP actions that would let any visitor execute arbitrary code or SQL queries. Helma has no built-in HTTP-level IP allowlist. To protect the tools when needed:

- Restrict the HTTP routes at the reverse proxy (nginx `allow`/`deny`, Apache `Require ip`, Caddy IP matchers).
- Implement an `onRequest` check inside the host app that rejects requests without an authenticated admin session.
- Or just don't enable `modules/tools` outside development environments.

## See Also

- [`modules/tools/Global/`](https://github.com/DionixGmbH/helma/src/branch/main/modules/tools/Global) — source files
- [Modules overview](index.md)
