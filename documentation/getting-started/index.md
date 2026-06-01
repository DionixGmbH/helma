# Getting Started

This section walks you from a freshly-cloned repository to a running Helma application in under five minutes.

| Page | What you'll learn |
|---|---|
| [Installation](installation.md) | Requirements, building from source, downloading a release. |
| [Your First Application](your-first-app.md) | Hello World, then a tiny blog. |
| [Project Structure](project-structure.md) | What every file and directory means. |
| [Running Helma](running.md) | Starting, stopping, reloading, the management UI. |

## Prerequisites

- Java 25 or higher (build target in `build.gradle`)
- For development from source: Node.js (LTS) and rsync ≥ 3.1.0
- Optional: [devbox](https://www.jetify.com/devbox) — the repository ships a `devbox.json` that pins the toolchain

## TL;DR

```bash
git clone https://github.com/DionixGmbH/helma.git
cd helma
./gradlew run        # builds and starts Helma on http://localhost:8080
```

When Helma is running you should see in the log:

```
Starting HTTP server on port 8080
Helma ready to serve.
```

Open [http://localhost:8080](http://localhost:8080) — the management application greets you and lists the bundled applications (`base`, `welcome`, `manage`).
