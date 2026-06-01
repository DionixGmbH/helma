# Installation

## System Requirements

| Component | Minimum | Recommended |
|---|---|---|
| Java | 25 | 25 LTS |
| RAM (server) | 256 MB | 1 GB |
| Disk | 100 MB free | 500 MB for logs and DB |

The `Server.checkJavaVersion()` method at `src/main/java/helma/main/Server.java:152` enforces Java 25 at startup and exits with an error otherwise.

For building Helma from source you additionally need:

- [Node.js](https://nodejs.org) LTS — used by the `jsdoc` Gradle task to render JavaScript API documentation.
- [Rsync](https://rsync.samba.org) ≥ 3.1.0 — used by the `update` Gradle task to deploy to an installation directory while preserving non-Windows file ownership.
- [Gradle](https://gradle.org) is bundled via the wrapper (`./gradlew`); no separate install needed.

## Option A — Use a Release

1. Download the latest release archive from [github.com/DionixGmbH/helma/releases](https://github.com/DionixGmbH/helma/releases).
2. Unpack it. The archive contains a self-contained `helma-<version>` directory.
3. Launch:

    === "macOS / Linux"

        ```bash
        cd helma-<version>
        ./bin/helma
        ```

    === "Windows"

        ```powershell
        cd helma-<version>
        bin\helma.bat
        ```

4. Open [http://localhost:8080](http://localhost:8080).

## Option B — Build from Source

```bash
git clone https://github.com/DionixGmbH/helma.git
cd helma
./gradlew run
```

The `run` task:

1. Compiles all Java sources (`compileJava`)
2. Generates JavaDoc and JSDoc (`build` task dependency)
3. Generates the dependency license report
4. Starts `helma.main.Server` with the JVM args `-Dorg.eclipse.jetty.LEVEL=WARN -Dapple.awt.UIElement=true`

To **install** the build output into the project directory (so `./bin/helma` works from a release-style layout), run:

```bash
./gradlew update
# answer "yes" at the prompt
```

This rsyncs `build/install/helma/` over the project directory and creates a backup of the previous installation in `backups/<timestamp>/`.

## Option C — Devbox

The repository ships with a [devbox.json](https://github.com/jetify-com/devbox) that pins a Java 25 JDK and `act` (for testing GitHub Actions locally). To enter a shell with the toolchain ready:

```bash
devbox shell
./gradlew run
```

With direnv installed and `.envrc` allowed (`direnv allow`), `cd helma` enters the devbox shell automatically.

## Verifying the Install

```bash
java -jar launcher.jar -h "$(pwd)" -w 8080
```

`-h` sets `helma.home` and `-w` sets the HTTP port. See the [CLI reference](../reference/cli.md) for the full list of flags.

Watch the log for:

```
Reading server properties from <path>/server.properties
Starting HTTP server on port 8080
Helma ready to serve.
```

Browse to `http://localhost:8080`. The bundled management app is served at `/manage` and the welcome app at `/`.

## Where Things Are

After install, the directory layout looks like this:

```
helma/
├── bin/                  helma, helma.bat — start scripts
├── lib/                  Helma + dependencies (Rhino, Jetty, Commons, etc.)
├── apps/                 your applications (manage, welcome, base, plus your own)
├── db/                   embedded XML DB files (one subdir per app)
├── log/                  rotating log files
├── modules/              bundled JavaScript modules (core, helma, jala, tools)
├── docs/                 JavaDoc + JSDoc + license report (after a build)
├── server.properties     server-wide config
├── apps.properties       which apps to start and how
└── launcher.jar          bootstrap classloader
```

See [Project Structure](project-structure.md) for the full breakdown.

## Uninstalling

Helma writes only inside its own directory; remove it with `rm -rf helma-<version>`. The embedded database is in `db/`; clear it to reset all unmapped persistent data. If you point an app at an external relational database via `db.properties`, that database persists independently and is not affected.
