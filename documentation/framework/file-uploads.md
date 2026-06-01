# File Uploads

Helma decodes `multipart/form-data` requests via Apache Commons FileUpload. Uploaded files appear as `MimePart` objects in `req.postParams`.

## A Minimal Form

```html
<!-- in a .skin -->
<form method="POST" enctype="multipart/form-data">
    <input type="file" name="photo">
    <input type="text" name="caption">
    <button type="submit">Upload</button>
</form>
```

## Handling the Upload

```javascript
function upload_action_post() {
    var photo = req.postParams.photo;
    if (!photo) {
        res.message = "No file uploaded";
        res.redirect(this.href());
    }

    var name = photo.getName();              // original filename from client
    var type = photo.getContentType();       // e.g. "image/jpeg"
    var size = photo.getContentLength();     // bytes
    var bytes = photo.getContent();          // byte[] (small uploads only)

    // Save to disk
    var dest = new java.io.File("/var/www/uploads/" + name);
    var fos = new java.io.FileOutputStream(dest);
    fos.write(bytes);
    fos.close();

    res.message = "Saved " + name + " (" + size + " bytes)";
    res.redirect(this.href());
}
```

## MimePart API

The uploaded file is a `helma.util.MimePart`:

```javascript
photo.name                  // original filename
photo.contentType           // MIME type
photo.content               // byte array of file contents
photo.contentLength         // size in bytes
photo.lastModified          // last-modified date (often current time)
photo.eTag                  // null for uploads
photo.getContent()          // same as .content
photo.writeToFile(file)     // write to a java.io.File
photo.normalizeFilename()   // strip path components from filename
photo.getParameter(name)    // get a Content-Disposition parameter
```

## Upload Limits

By default, Helma rejects uploads larger than 1024 KB. Configure:

```properties
# apps.properties
myapp.uploadLimit = 10240        # KB
```

Or per-request:

```javascript
// (Configured at app load time; can't change mid-request)
```

When the limit is exceeded, the upload fails and the request gets a 413 Payload Too Large response with an error message in `res.error`.

## Streaming Large Uploads

For files larger than memory permits, write directly to disk instead of reading into `photo.content`. The current Commons FileUpload integration buffers uploads to disk if they exceed an internal threshold; you can stream by checking `photo.isFormField()` and using `photo.openInputStream()`:

```javascript
function upload_action_post() {
    var photo = req.postParams.photo;
    var input = photo.openInputStream();
    var output = new java.io.FileOutputStream("/tmp/" + photo.normalizeFilename());
    try {
        var buf = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, 8192);
        var n;
        while ((n = input.read(buf)) > 0) {
            output.write(buf, 0, n);
        }
    } finally {
        input.close();
        output.close();
    }
    res.write("OK");
}
```

For very large uploads, use the `helma.File` module:

```javascript
var helmaFile = app.addRepository("modules/helma/File.js");
var dest = new helmaFile.File("/var/www/uploads/" + photo.normalizeFilename());
dest.write(photo.content);
```

## Progress Reporting

The `UploadStatus` mechanism reports upload progress. Each request can attach an upload ID; the client can poll a separate endpoint to retrieve progress.

```javascript
// Client side: include ?uploadId=xyz123 in the form action
<form method="POST" enctype="multipart/form-data" action="upload?uploadId=xyz123">

// Server side: progress endpoint
function progress_action() {
    var id = req.params.uploadId;
    var status = session.getUploadStatus(id);
    res.contentType = "application/json";
    res.write(JSON.stringify({
        complete: status ? status.isComplete() : false,
        bytesRead: status ? status.getBytesRead() : 0,
        totalSize: status ? status.getContentLength() : 0
    }));
}
```

`session.getUploadStatus(id)` returns an `UploadStatus` (see `src/main/java/helma/framework/UploadStatus.java`) for the in-flight upload.

## Validation

Always validate uploads:

```javascript
function upload_action_post() {
    var photo = req.postParams.photo;
    if (!photo) {
        res.status = 400;
        res.write("No file");
        return;
    }

    // Whitelist content types
    var allowed = { "image/jpeg": true, "image/png": true, "image/gif": true };
    if (!allowed[photo.contentType]) {
        res.status = 415;
        res.write("Unsupported file type");
        return;
    }

    // Limit size (in addition to uploadLimit)
    if (photo.contentLength > 5 * 1024 * 1024) {
        res.status = 413;
        res.write("Too large");
        return;
    }

    // Sanitize filename — strip path components
    var safe = photo.normalizeFilename();

    // Save somewhere outside the web root
    // ...
}
```

## Multiple Files

`<input type="file" name="photos" multiple>` produces multiple files under the same name. Helma exposes them as an array:

```javascript
var files = req.postParams.photos;
if (!Array.isArray(files)) files = [files];   // single upload returns scalar

for each (var f in files) {
    save(f);
}
```

## Combining with Image Processing

```javascript
function upload_action_post() {
    var photo = req.postParams.photo;
    var image = new helma.Image(photo.content);
    image.resize(800, 600);
    image.crop(0, 0, 800, 600);

    var jpegBytes = image.toByteArray("jpeg");
    // save jpegBytes...
}
```

See the [`Image` extension](../extensions/image.md) and [`helma.Image` module](../modules/helma/image.md).

## CSRF

File uploads are POST requests and just as vulnerable to CSRF as form submits. Include a CSRF token in every upload form. See [Authentication](authentication.md) for the pattern.
