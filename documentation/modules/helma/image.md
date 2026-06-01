# helma.Image

Image factory and metadata helpers — a thin wrapper around the `Image` extension.

```javascript
app.addRepository("modules/helma/Image.js");
```

## Factory

### `helma.Image(arg)` → Image

Create an image. Accepts any of:

- File path string
- URL string
- `byte[]`
- `java.io.InputStream`
- `java.io.Reader`
- `helma.File`
- `MimePart`
- existing `Image`
- `java.awt.image.BufferedImage`

```javascript
var img = helma.Image("/var/photos/sunset.jpg");
var img = helma.Image(req.postParams.upload);    // from file upload
var img = helma.Image("https://example.com/x.png");
```

## Static Methods

### `helma.Image.getInfo(arg)` → ImageInfo

Quickly read image metadata without decoding pixels.

```javascript
var info = helma.Image.getInfo("/var/photos/sunset.jpg");
print(info.width + " × " + info.height + " " + info.mimeType);
```

Returns an `ImageInfo` with:

- `width`, `height`
- `bitsPerPixel`
- `mimeType`
- `format` — "PNG", "JPEG", "GIF", "BMP", "WebP"
- `progressive` — boolean

### `helma.Image.spacer()` → Image

Return a 1×1 transparent GIF — useful for layout placeholders.

## Manipulation

The returned image object is a `helma.image.ImageWrapper`. Operations:

```javascript
img.getWidth();
img.getHeight();
img.resize(w, h);                    // bilinear resize
img.crop(x, y, w, h);                // crop region
img.rotate(degrees);

// Drawing on top
img.setColor(0xff, 0x80, 0x00);
img.setFont("Arial", "BOLD", 14);
img.drawString("Watermark", 10, 20);
img.drawImage(otherImage, x, y, w, h);
img.drawLine(x1, y1, x2, y2);
img.fillRect(x, y, w, h);

// Save
img.saveAs("/path/to/output.jpg");
var bytes = img.toByteArray("jpeg");
var bytes = img.toByteArray("png", 0.9);   // optional quality
```

## Example: Thumbnail Generator

```javascript
function thumbnail_action() {
    app.addRepository("modules/helma/Image.js");
    var img = Image(this.imageBytes);

    var aspect = img.getWidth() / img.getHeight();
    var thumbW = 150;
    var thumbH = Math.round(150 / aspect);

    img.resize(thumbW, thumbH);

    res.contentType = "image/jpeg";
    res.writeBinary(img.toByteArray("jpeg", 0.85));
}
```

## See Also

- [`Image` extension](../../extensions/image.md) — underlying class
- [`jala.ImageFilter`](../jala.md) — sharpen, blur, unsharp mask
- [`modules/helma/Image.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Image.js)
