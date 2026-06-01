# Xml Extension

`Xml` is the singleton for converting HopObjects to/from XML.

Implementation: `src/main/java/helma/scripting/rhino/extensions/XmlObject.java`.

`Xml` is not a constructor — it's a pre-created object in the global scope. Use its methods directly:

```javascript
Xml.writeToString(node);
Xml.read("/path/to/file.xml");
```

## API

### Write

#### `Xml.write(node, filePath)` → boolean
#### `Xml.write(node, filePath, dbmode)` → boolean

Serialise a HopObject to an XML file. `dbmode = true` includes the embedded-DB internal IDs (for round-trip with `Xml.read`).

```javascript
Xml.write(post, "/tmp/post.xml");
Xml.write(post, "/tmp/post.xml", true);     // round-trippable
```

#### `Xml.writeToString(node)` → String
#### `Xml.writeToString(node, dbmode)` → String

Serialise to a string instead of a file.

```javascript
var xml = Xml.writeToString(post);
res.contentType = "application/xml";
res.write(xml);
```

### Read

#### `Xml.read(filePath)` → HopObject
#### `Xml.read(filePath, node)` → HopObject (populates existing node)

Read an XML file into a new HopObject (or merge into an existing one).

```javascript
var imported = Xml.read("/tmp/post.xml");
// Now imported is a HopObject

// Or merge into an existing node:
var post = new Post();
Xml.read("/tmp/post.xml", post);
```

#### `Xml.readFromString(str)` → HopObject
#### `Xml.readFromString(str, node)` → HopObject

Same but from a string.

```javascript
var node = Xml.readFromString("<?xml...");
```

### Generic XML

For arbitrary XML (not Helma-specific), use:

#### `Xml.get(url)` → HopObject
#### `Xml.get(url, conversionRules)` → HopObject

Fetch and parse an XML document from a URL into a tree of HopObjects.

#### `Xml.getFromString(str)` → HopObject
#### `Xml.getFromString(str, conversionRules)` → HopObject

Same but from a string.

The optional `conversionRules` parameter is a path to a `.properties` file that maps XML element names to HopObject prototypes. See the Helma `XmlConverter` source for the syntax.

## Wire Format

Helma's XML format for HopObjects:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<xmlroot xmlns:hop="http://www.helma.org/docs/guide/features/database">
  <hopobject id="42" name="hello-world" prototype="Post"
             created="1719000000" lastmodified="1719000123">
    <hop:property name="title"><![CDATA[Hello, world]]></hop:property>
    <hop:property name="body"><![CDATA[Long content...]]></hop:property>
    <hop:property name="created"><hop:date>1719000000</hop:date></hop:property>
    <hop:property name="viewCount"><hop:int>42</hop:int></hop:property>
    <hop:child idref="43" prototyperef="Comment" nameref="great-post"/>
  </hopobject>
</xmlroot>
```

Property types: `<hop:string>`, `<hop:int>`, `<hop:float>`, `<hop:date>`, `<hop:node>`.

Same format as the embedded XML database — `db/<app>/<id>.xml`.

## Example: Backup / Restore

```javascript
function backup_action() {
    var allPosts = root.posts.list();
    var dump = [];
    for each (var p in allPosts) {
        dump.push(Xml.writeToString(p));
    }
    res.contentType = "application/xml";
    res.write(dump.join("\n"));
}

function restore_action_post() {
    var nodes = req.postParams.dump.split("\n");
    for each (var xml in nodes) {
        Xml.readFromString(xml, new Post());
    }
}
```

## Registration

`Xml` is registered as a singleton in `GlobalObject.init()`:

```java
put("Xml", this, Context.toObject(new XmlObject(core), this));
```

## See Also

- [`Xml.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/scripting/rhino/extensions/XmlObject.java) — implementation
- [`XmlConverter.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/objectmodel/dom/XmlConverter.java) — conversion rules format
- [Embedded Database](../database/embedded-database.md) — same format
- [`getXmlDocument()`](../scripting/global-functions.md) — for parsing arbitrary XML to DOM
