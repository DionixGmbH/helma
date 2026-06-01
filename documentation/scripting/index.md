# Scripting

Helma's scripting layer is built on [Mozilla Rhino](https://github.com/mozilla/rhino). This section covers how Rhino is wired into Helma and what you can do with the full Java platform from your JavaScript code.

| Page | Topic |
|---|---|
| [Rhino Engine](rhino-engine.md) | How Helma uses Rhino internally. |
| [Global Functions](global-functions.md) | Every free function in the global scope. |
| [CommonJS Modules](commonjs-require.md) | `require`, `module.exports`. |
| [Java Interoperability](java-interop.md) | Calling Java from JavaScript. |
| [HopObject Constructors](hopobject-constructors.md) | `new Post()`, custom constructors. |
| [Debugging](debugging.md) | Tracer, the Rhino debugger UI. |
| [Profiling](profiling.md) | Per-request profiler. |
