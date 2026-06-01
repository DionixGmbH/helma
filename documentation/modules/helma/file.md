# helma.File

High-level file system wrapper. Built on top of the `FileObject` extension, with additional methods for binary data, encodings, and directory operations.

```javascript
app.addRepository("modules/helma/File.js");
```

## Constructor

```javascript
var f = new helma.File(path);
```

## Read & Write

```javascript
// Open for reading or writing
f.open();
f.open({ encoding: "ISO-8859-1", append: true });

// Read
var line = f.readln();
var all = f.readAll();
var bytes = f.toByteArray();

// Write
f.write("text");
f.writeln("line");
f.flush();
f.close();

// EOF check
f.eof();
```

## State

```javascript
f.toString();         // path representation
f.isOpened();         // open flag
f.exists();
f.canRead();
f.canWrite();
f.isAbsolute();
f.isFile();
f.isDirectory();
```

## Properties

```javascript
f.getName();          // file name only
f.getParent();        // parent path
f.getPath();          // full path
f.getAbsolutePath();
f.getLength();        // size in bytes
f.lastModified();     // mtime
```

## Filesystem Ops

```javascript
f.remove();             // delete file
f.removeDirectory();    // delete dir (must be empty)
f.makeDirectory();      // create dir (and parents)
f.renameTo(otherFile);
f.hardCopy(destFile);   // physical copy
f.move(destFile);       // rename or copy+remove
```

## Directory Listing

```javascript
var entries = dir.list();             // String[] of names
var entries = dir.list("*.txt");      // glob filter
var entries = dir.listRecursive();    // all descendants
var entries = dir.listRecursive("*.js");
```

## Error Handling

```javascript
if (f.error()) {
    print(f.error());
    f.clearError();
}
```

## Constants

```javascript
helma.File.separator   // platform path separator (/ or \)
```

## Examples

### Write a string to a file

```javascript
app.addRepository("modules/helma/File.js");
var log = new File("/tmp/myapp.log");
log.open({ append: true });
log.writeln(new Date().toISOString() + " - event");
log.close();
```

### Copy a file

```javascript
var src = new File("/var/source/photo.jpg");
var dst = new File("/var/dest/photo.jpg");
src.hardCopy(dst);
```

### List all .md files recursively

```javascript
var dir = new File("/var/www/docs");
var entries = dir.listRecursive("*.md");
for each (var name in entries) {
    print(name);
}
```

### Read binary file

```javascript
var img = new File("/var/photos/sunset.jpg");
var bytes = img.toByteArray();
res.contentType = "image/jpeg";
res.writeBinary(bytes);
```

## See Also

- [`File` extension](../../extensions/file.md) — the underlying lower-level wrapper
- [Framework: File Uploads](../../framework/file-uploads.md)
- [`modules/helma/File.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/File.js)
