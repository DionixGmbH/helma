# Writing Java Extensions

A **Helma extension** is a Java class that registers global JavaScript objects, prototypes, and event listeners with running Helma applications. Extensions are loaded at server startup and called for lifecycle events.

## The `HelmaExtension` SPI

Extensions implement `helma.extensions.HelmaExtension` (`src/main/java/helma/extensions/HelmaExtension.java`):

```java
package com.example;

import helma.extensions.HelmaExtension;
import helma.extensions.ConfigurationException;
import helma.framework.core.Application;
import helma.main.Server;
import helma.scripting.ScriptingEngine;

import java.util.HashMap;

public class MyExtension extends HelmaExtension {

    @Override
    public String getName() {
        return "MyExtension";
    }

    @Override
    public void init(Server server) throws ConfigurationException {
        // Called once when Helma starts.
        // Check that required classes are available.
        try {
            Class.forName("com.example.SomeDependency");
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(
                "MyExtension requires com.example.SomeDependency on the classpath"
            );
        }
    }

    @Override
    public void applicationStarted(Application app) throws ConfigurationException {
        // Called every time an Application starts.
    }

    @Override
    public void applicationStopped(Application app) {
        // Called when an Application stops (config change or shutdown).
    }

    @Override
    public void applicationUpdated(Application app) {
        // Called when an Application's properties change.
        // Also called once before applicationStarted() at startup.
    }

    @Override
    public HashMap initScripting(Application app, ScriptingEngine engine)
            throws ConfigurationException {
        // Called when each RequestEvaluator's scripting engine is created.
        // Return a HashMap of global vars to expose to JavaScript.
        HashMap globals = new HashMap();
        globals.put("myGlobal", new MyGlobalObject(app));
        return globals;
    }
}
```

## Registration

Add the extension class name to `server.properties`:

```properties
extensions = com.example.MyExtension
```

Multiple extensions:

```properties
extensions = com.example.Ext1, com.example.Ext2, org.foo.Ext3
```

The JAR containing the class must be on the classpath. Place it in `lib/ext/`.

## Method Lifecycle

```
Server startup:
  for each extension class in server.properties::extensions:
    instance = new ExtensionClass()
    instance.init(server)

For each application start:
  for each extension:
    instance.applicationUpdated(app)
    instance.applicationStarted(app)

When a RequestEvaluator initialises its scripting engine:
  for each extension:
    globals = instance.initScripting(app, engine)
    // globals merged into the engine's global scope

When app properties change:
  for each extension:
    instance.applicationUpdated(app)

For each application stop:
  for each extension:
    instance.applicationStopped(app)
```

## Exposing Globals

`initScripting()` returns a `HashMap<String, Object>` where keys are JavaScript variable names and values are Java objects to expose.

### Plain Java objects

Any Java object can be exposed. Its public methods and bean properties become accessible:

```java
public class StatsCollector {
    public long getRequestCount() { return ...; }
    public void recordRequest() { ... }
    public Map getCounters() { return ...; }
}

// In initScripting:
globals.put("stats", new StatsCollector());

// In JavaScript:
stats.recordRequest();
print(stats.requestCount);     // bean property
```

### Scriptable objects

For full control over JS semantics, implement `org.mozilla.javascript.Scriptable`:

```java
public class MyScriptable extends ScriptableObject {
    @Override
    public String getClassName() { return "MyScriptable"; }

    @JSFunction
    public String hello(String name) {
        return "Hello, " + name;
    }
}
```

The `@JSFunction` annotation or the `jsFunction_` prefix marks methods as JS-callable.

### Function/Constructor

For globals that should be JS constructors:

```java
public class MyClass extends ScriptableObject {
    public MyClass() {}
    public MyClass(String s) { /* JS constructor */ }
    @Override public String getClassName() { return "MyClass"; }
}

// In initScripting:
ScriptableObject.defineClass(scope, MyClass.class);
```

## Lifecycle Hooks

### `init(server)` — once at startup

Verify dependencies are present. Throw `ConfigurationException` to abort startup with an error message.

### `applicationStarted(app)` — per-app start

Set up app-specific state. For example, register your own cron jobs:

```java
app.addCronJob("myExtensionPing", "*", "*", "*", "*", "*", "5");
```

Or set up listeners on the app's NodeManager:

```java
app.getNodeManager().addNodeChangeListener(new MyNodeListener());
```

### `applicationStopped(app)` — per-app stop

Tear down resources. Close connections, cancel threads, flush state.

### `applicationUpdated(app)` — per-app properties change

Re-read configuration. Called before `applicationStarted` on initial startup.

### `initScripting(app, engine)` — per-RequestEvaluator

Return a map of globals. Each RequestEvaluator creates its own scripting engine; this is called for each one.

The globals are merged into the engine's global scope via `RhinoEngine.setGlobals()`.

## Accessing the Application from JS

If you need methods on your exposed object to access Helma state:

```java
public class MyAppGlobal {
    private final Application app;
    public MyAppGlobal(Application app) { this.app = app; }

    public String getAppName() { return app.getName(); }
    public Object getDbConnection(String name) {
        return app.getDbSource(name).getConnection();
    }
}
```

## class.properties Integration

To make `new com.example.MyClass()` in JavaScript return a HopObject of a specific prototype:

```properties
# apps/myapp/class.properties
com.example.MyClass = MyPrototype
```

Now `new com.example.MyClass()` returns a HopObject with prototype `MyPrototype`, so all `apps/myapp/MyPrototype/*.js` methods apply.

## Common Patterns

### Add a global function

```java
public class MyExtension extends HelmaExtension {
    @Override
    public HashMap initScripting(Application app, ScriptingEngine engine) {
        HashMap globals = new HashMap();
        globals.put("myFunction", new Functor(app));
        return globals;
    }

    public static class Functor extends BaseFunction {
        private final Application app;
        public Functor(Application app) { this.app = app; }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            return "Result from " + app.getName();
        }
    }
}
```

### Add a prototype

```java
@Override
public void applicationStarted(Application app) {
    Properties props = new Properties();
    props.put("_db", "main");
    props.put("_table", "tags");
    // ... etc
    app.definePrototype("Tag", props);
}
```

### Listen to all DB changes

```java
@Override
public void applicationStarted(Application app) {
    app.getNodeManager().addNodeChangeListener(new NodeChangeListener() {
        public void nodeChanged(NodeEvent event) {
            // event.getNode(), event.getEvent() — NODE_CREATED, NODE_MODIFIED, NODE_DELETED
        }
    });
}
```

## Demo Extension

A working example is at `src/main/java/helma/extensions/demo/DemoExtension.java`. It demonstrates the full lifecycle and a custom JS function.

To enable:

```properties
# server.properties
extensions = helma.extensions.demo.DemoExtension
```

After restart, `demoFunction()` is available in every app.

## Best Practices

- **Don't block in `applicationStarted`** — it delays app startup. Spawn threads for slow init.
- **Clean up in `applicationStopped`** — orphaned threads accumulate across restarts.
- **Use the app's classloader** — `app.getClassLoader()` — for loading classes by name.
- **Use commons-logging** — `LogFactory.getLog("helma.<myext>")` — so output goes to the right log file.
- **Document `server.properties` keys** your extension consumes.
- **Version your extension** with `getName()` returning something like `"MyExt 1.2.3"` for diagnostics.

## See Also

- [`HelmaExtension.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/extensions/HelmaExtension.java) — the SPI
- [Demo: `DemoExtension.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/extensions/demo/DemoExtension.java)
- [Reference: `class.properties`](../reference/class-properties.md) — wrap Java classes as HopObjects
- [Java Interoperability](../scripting/java-interop.md) — JS-side interop
