# File Extension

`File` is the global JavaScript constructor for low-level file/directory operations.

Implementation: `src/main/java/helma/scripting/rhino/extensions/FileObject.java`.

For higher-level operations, prefer the [`helma.File`](../modules/helma/file.md) module — but it wraps this class.

## Constructor

```javascript
var f = new File(path);                  // by absolute or relative path
var f = new File(parentPath, fileName);  // join parent and name
```

## API

### Read/write

```javascript
var f = new File("/var/log/myapp.log");
f.open();                       // open for read+write
var line = f.readln();          // read one line
var all = f.readAll();          // read entire file as string
f.write("text");                // write
f.writeln("line");              // write + newline
f.flush();                      // flush buffer
f.close();                      // close and release handle
```

### State

```javascript
f.isOpened();          // is currently open
f.eof();               // at end of file
f.exists();            // file or directory exists
f.canRead();           // readable
f.canWrite();          // writable
```

### Type

```javascript
f.isFile();
f.isDirectory();
f.isAbsolute();
```

### Properties

```javascript
f.getName();           // file name (no path)
f.getParent();         // parent path
f.getPath();           // full path
f.getAbsolutePath();   // absolute path
f.getLength();         // size in bytes
f.lastModified();      // mtime as ms-since-epoch
```

### Filesystem operations

```javascript
f.mkdir();             // create directory (single level)
f.remove();            // delete file or directory
f.renameTo(otherFile); // rename/move
```

### List directory

```javascript
var dir = new File("/var/log");
var entries = dir.list();              // String[] of names
```

Returns null if `f` isn't a directory.

### Errors

```javascript
f.error();             // last error message
f.clearError();        // reset error state
```

## Example: Append to a Log

```javascript
function logEvent(msg) {
    var log = new File("/var/log/myapp/events.log");
    log.open();
    log.writeln(new Date().toISOString() + " - " + msg);
    log.close();
}
```

## Example: Recursive Listing

```javascript
function ls(dir) {
    var entries = dir.list();
    for each (var name in entries) {
        var entry = new File(dir.getPath(), name);
        if (entry.isDirectory()) {
            ls(entry);          // recurse
        } else {
            res.write(entry.getAbsolutePath() + "\n");
        }
    }
}
ls(new File("/tmp"));
```

## Limitations

`FileObject` is a thin wrapper over `java.io.File` / `RandomAccessFile`. It doesn't support:

- Reading binary data directly (use `helma.File` or `java.io.FileInputStream`)
- Symbolic-link semantics (uses platform default)
- File watching (poll mtime instead)
- Async I/O

For richer operations use [`helma.File`](../modules/helma/file.md), which handles binary content, charset-aware reads, and bulk operations.

## Registration

`File` is registered as a global lazy property by `RhinoCore.java`. Available in every Helma application without `require`.

## See Also

- [`helma.File` module](../modules/helma/file.md) — higher-level API
- [Framework: File Uploads](../framework/file-uploads.md)
- [`FileObject.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/scripting/rhino/extensions/FileObject.java)
