# helma.Zip

Read and write ZIP archives.

```javascript
app.addRepository("modules/helma/Zip.js");
```

## Constructor

```javascript
// Read existing
var zip = new helma.Zip(file);

// Create new
var zip = new helma.Zip();   // memory buffer
```

## Reading

```javascript
zip.list();                              // array of entries
zip.extract("file.txt", "/dest/dir");    // extract one entry
zip.extractAll("/dest/dir");             // extract all
```

## Writing

```javascript
zip.add(file, level);                     // add a file
zip.add(file, level, "pathPrefix");       // add with path prefix
zip.addData(buf, "name.txt", level);      // add bytes as named entry
zip.close();                              // finalize
zip.getData();                            // get as byte[]
zip.save("/path/to/output.zip");          // save to disk
```

`level` is 0-9 compression level (0=none, 9=max).

## Static

### `helma.Zip.extractData(zipData)` → Map

Extract a zip from bytes into a map of `name → data`.

## Sub-Classes

### `helma.Zip.Content()`

Internal accumulator for entries:

```javascript
var content = new helma.Zip.Content();
content.add(entry);
content.toString();
```

### `helma.Zip.Entry(entry)`

Wraps a single zip entry. `entry.toString()` returns the name.

## Example: Backup Directory

```javascript
app.addRepository("modules/helma/Zip.js");
app.addRepository("modules/helma/File.js");

function backup() {
    var zip = new Zip();
    var dir = new File("/var/www/data");
    var entries = dir.listRecursive();
    for each (var name in entries) {
        var f = new File(dir.getPath(), name);
        if (f.isFile()) {
            zip.add(f, 9);
        }
    }
    zip.close();
    zip.save("/tmp/backup-" + Date.now() + ".zip");
}
```

## Example: Serve a ZIP to the client

```javascript
function download_action() {
    app.addRepository("modules/helma/Zip.js");
    var zip = new Zip();
    for each (var doc in this.docs.list()) {
        zip.addData(java.lang.String(doc.content).getBytes(), doc.name + ".txt", 5);
    }
    zip.close();

    res.contentType = "application/zip";
    res.setHeader("Content-Disposition", "attachment; filename=\"docs.zip\"");
    res.writeBinary(zip.getData());
}
```

## See Also

- [`modules/helma/Zip.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Zip.js)
