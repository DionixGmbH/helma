# Extensions

Helma's **scripting extensions** are Java classes registered as global constructors or singletons in the JavaScript scope. They expose Java functionality to your application code.

This section documents the seven built-in extensions plus how to write your own.

| Page | Class | JS Symbol |
|---|---|---|
| [DatabaseObject](database-object.md) | `helma.scripting.rhino.extensions.DatabaseObject` | (returned by `getDBConnection`) |
| [File](file.md) | `FileObject` | `File` |
| [Ftp](ftp.md) | `FtpObject` | `Ftp` / `FtpClient` |
| [Image](image.md) | `ImageObject` | `Image` |
| [Mail](mail.md) | `MailObject` | `Mail` |
| [Xml](xml.md) | `XmlObject` | `Xml` (singleton) |
| [XmlRpc](xmlrpc.md) | `XmlRpcObject` | `Remote` |
| [Writing Java Extensions](writing-extensions.md) | `HelmaExtension` SPI | (custom) |

## How Extensions Register

Two registration points:

- **`RhinoCore.java`** — lazy-loads `File`, `Ftp`, `Image`, `Remote` constructors and the `Mail` constructor; class names registered with `ScriptableObject.defineProperty()`.
- **`GlobalObject.java`** — registers the `Xml` singleton and the `DatabaseObject` factory function `getDBConnection`.

Each extension class follows the Rhino convention: methods named `jsFunction_X` become `X` on the JS object; `jsGet_X` / `jsSet_X` become getters/setters; `jsStaticFunction_X` becomes a static.

In practice, the seven built-in extensions use direct Rhino wiring (`defineFunctionProperties`, `FunctionObject`, etc.) rather than the `jsX_` convention — but the result is the same from JavaScript's perspective.
