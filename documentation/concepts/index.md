# Concepts

This section explains *how Helma thinks*. Once you have these mental models, the rest of the documentation falls into place.

| Page | Topic |
|---|---|
| [Architecture Overview](architecture.md) | The server, applications, evaluators, scripting engine. |
| [Request Lifecycle](request-lifecycle.md) | What happens between socket-accept and response-flush. |
| [Prototypes & Inheritance](prototypes.md) | The unit of code organisation. |
| [Object Model](object-model.md) | Nodes, HopObjects, persistence layers. |
| [Repositories](repositories.md) | How Helma finds and loads code. |
| [Scripting Environment](scripting-environment.md) | Rhino, globals, CommonJS, Java interop. |
| [Sessions & Users](sessions-and-users.md) | The Session object, user authentication, login state. |
| [Transactions](transactions.md) | How Helma coordinates DB writes with request execution. |

## The Core Idea

A Helma application is a tree of **HopObjects**. Some live in memory only; others are mapped to relational tables; the rest are auto-persisted to an embedded XML store. The URL is the path through that tree. Each node in the tree has a **prototype** which defines the JavaScript functions, skin templates, and DB mapping for objects of that type.

A request:

1. Walks the URL path, segment by segment, asking each HopObject for its child by that name.
2. The last segment is interpreted as an **action** — `_action` suffixed function.
3. The action calls `renderSkin()` to produce HTML by interpolating **macros** in a skin template.
4. Macros call back into JavaScript to pull data from the HopObject.
5. The accumulated response buffer is sent to the client.

That's it. Everything else — DB mapping, login, cron, file upload — is layered on top of this.
