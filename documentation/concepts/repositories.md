# Repositories

A **repository** is a source of code and skin resources for an application. Helma supports three repository types and lets you compose them: an application can pull code from a directory *and* a zip file *and* a single file simultaneously.

## Repository Types

| Class | Path | Use case |
|---|---|---|
| `FileRepository` | a directory | Normal development |
| `ZipRepository` | a `.zip` file | Distributing a module without unpacking |
| `MultiFileRepository` | a parent directory | Loading every subdirectory as a separate repository |
| `SingleFileRepository` | one `.js` file | Single-script "module" |

All implement `helma.framework.repository.Repository`. Each contains `Resource` objects (files) and optionally child `Repository` objects (subdirectories).

## Registering Repositories

### Statically — `apps.properties`

```properties
myapp.repository.0 = /var/www/myapp
myapp.repository.1 = /var/www/shared.zip
myapp.repository.2 = ${user.home}/dev/myapp-plugin
```

The numeric index controls **order**: lower numbers are searched first. When the same prototype directory exists in multiple repositories, files from lower-indexed repositories win.

### Statically — Default Discovery

Without explicit `repository.N` settings, Helma uses `apps/<appname>/` (or `<app>.appdir` if overridden) as repository 0.

### Dynamically — From JavaScript

```javascript
// Add at request time
app.addRepository("/var/www/plugins/my-plugin.zip");

// Add a directory
app.addRepository("/var/www/plugins/extras");
```

`app.addRepository()` is typically called from `Global/main.js` so plugins are loaded at startup. The `addRepository()` method (`ApplicationBean.addRepository()` at `src/main/java/helma/framework/core/ApplicationBean.java:142`) accepts:

- A directory path → `FileRepository`
- A `.zip` file path → `ZipRepository`
- Any other file path → `SingleFileRepository`
- An existing `Repository` object

If the path doesn't exist, Helma tries appending `.zip` and `.js` before giving up.

## Resource Resolution

Resources are looked up by **short name** (file name with extension). When resolving `Post/main.skin`:

1. Walk repositories in registration order
2. For each repository, look for a child repository called `Post`
3. Inside `Post`, look for `main.skin`
4. Return the first match — but also remember any overloaded copies (`Resource.getOverloadedResource()`)

The overload chain lets you do:

```
repo-0/Post/main.skin   ← seen first (wins)
repo-1/Post/main.skin   ← overloaded, retrievable via getOverloadedResource()
```

## Built-in Modules as Repositories

Helma's bundled JS modules live under `modules/` in the distribution. They are **not** automatically added as repositories to every application — each app must opt in by listing them in `apps.properties` or calling `app.addRepository(...)`:

- `modules/core/all.js` — String/Array/Date/Number/Object extensions. Enable via `app.addRepository("modules/core/all.js")`.
- `modules/helma/all.js` (or individual files) — `helma.File`, `helma.Mail`, `helma.Http`, etc.
- `modules/jala/code/all.js` (or individual files) — `jala.Form`, `jala.Captcha`, `jala.I18n`, etc.
- `modules/tools` — the bundled dev tools. The distribution's default `apps.properties` adds this to the `welcome` app (`welcome.repository.1 = modules/tools`); other apps must opt in individually.

## CommonJS Modules

CommonJS `require()` is a separate mechanism from repositories. Its search roots are the app directory (`app.getAppDir()`) and optionally the directory pointed to by `app.properties::commonjs.dir` — not the registered repositories. See [CommonJS Modules](../scripting/commonjs-require.md) for the full algorithm.

## Use Cases

### Sharing utilities across apps

Put common code in `/usr/share/helma-modules/utils/` and add:

```properties
appA.repository.1 = /usr/share/helma-modules/utils
appB.repository.1 = /usr/share/helma-modules/utils
```

Both apps see the same `utils/` prototypes.

### Distributing a plugin as a single ZIP

Pack `Comment/`, `comment.skin`, `Global/comment-init.js` into `comment-plugin.zip`. Distribute the zip. Users add one line:

```properties
myapp.repository.5 = ./plugins/comment-plugin.zip
```

The plugin is loaded without unpacking.

### Layering an admin extension

`apps/blog/` defines a basic blog. `apps/blog-admin/` is a thin repository that adds admin actions:

```
blog-admin/
└── Post/
    └── admin.js   ← function admin_action() { ... }
```

Plus the line:

```properties
blog.repository.10 = /var/www/blog-admin
```

`/post/admin` now works thanks to the admin layer.

## Implementation Notes

- `Repository.getChecksum()` is a fast hash of all child checksums. Helma compares this checksum every request to detect added/removed files (`Prototype.checkForUpdates()` at `src/main/java/helma/framework/core/Prototype.java:145`).
- `ZipRepository` opens the JAR/ZIP via `java.util.zip.ZipFile`, caches the central directory in memory, and reads entries on demand.
- `FileRepository` uses `java.nio.file.WatchService` is **not** used — Helma polls mtimes on each request rather than watching. This is more reliable on networked filesystems.
- A single repository can be both a top-level "script root" (queryable by `Application.getRepositories()`) and a nested child of another repository. The `isScriptRoot()` flag distinguishes the two cases.

## When to Add a Repository

You usually don't need to. A new application directory under `apps/` is auto-discovered. You add repositories when:

- You want to load a plugin from outside the app's main directory
- You want to share a module library across multiple apps
- You're shipping a self-contained module as a ZIP
- You're loading dynamic modules at runtime (e.g. user-uploaded code)
