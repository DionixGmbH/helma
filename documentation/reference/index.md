# Reference

This section is the API and configuration reference. Use it to look up the exact name, signature, default, or possible value of something.

## Globals

| Page | Topic |
|---|---|
| [Application Bean (`app`)](app-bean.md) | All `app.*` methods and properties |
| [Request Bean (`req`)](req-bean.md) | All `req.*` methods and properties |
| [Response Bean (`res`)](res-bean.md) | All `res.*` methods and properties |
| [Session Bean (`session`)](session-bean.md) | All `session.*` methods and properties |
| [Path (`path`)](path.md) | The request-path wrapper |
| [HopObject](hopobject.md) | The base class of persistent objects |
| [Global Object](global-object.md) | Free functions in the global scope |

## Configuration Files

| Page | Topic |
|---|---|
| [`server.properties`](server-properties.md) | Server-wide settings |
| [`apps.properties`](apps-properties.md) | Which apps to start and how |
| [`app.properties`](app-properties.md) | Per-application settings |
| [`db.properties`](db-properties.md) | JDBC DataSource definitions |
| [`cron.properties`](cron-properties.md) | Scheduled jobs |
| [`class.properties`](class-properties.md) | Java class → prototype mapping |

## Command Line

| Page | Topic |
|---|---|
| [Command-line Interface](cli.md) | Flags accepted by `./bin/helma` |

## HTTP

| Page | Topic |
|---|---|
| [HTTP Status & Errors](http-status.md) | Status codes used by the framework |

---

## How This Reference Is Organised

Each page is structured the same way:

1. **Property/method name** in bold
2. **Type signature** when applicable
3. **Description** — what it does
4. **Parameters** — when applicable
5. **Returns** — what comes back
6. **Default** — when applicable
7. **Example** — minimal usage
8. **See also** — related items

Look in the navigation tree on the left to drill into a specific bean or config file.
