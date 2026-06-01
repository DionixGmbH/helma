# Image Extension

`Image` is the global JS constructor for creating and inspecting images.

Implementation: `src/main/java/helma/scripting/rhino/extensions/ImageObject.java` (using `helma.image.ImageGenerator` and Java 2D).

For higher-level image manipulation see [`helma.Image`](../modules/helma/image.md) and [`jala.ImageFilter`](../modules/jala.md).

## Constructor

```javascript
// From a file path
var img = new Image("/var/www/images/photo.jpg");

// From an InputStream, byte[], MimePart, FileObject, BufferedImage, or URL string
var img = new Image(uploadedPart);          // MimePart from req.postParams
var img = new Image(byteArray);
var img = new Image("https://example.com/photo.jpg");

// Create a new blank image at given size
var img = new Image(800, 600);

// Apply a filter
var img = new Image(otherImage, filter);
```

The constructor delegates to `helma.image.ImageGenerator`. The returned object is a `helma.image.ImageWrapper` — see [`helma.Image`](../modules/helma/image.md) for its API.

## Static Methods

### `Image.getInfo(arg)` → ImageInfo

Read image metadata without fully decoding pixels. Returns an `ImageInfo` object with:

- `width`, `height`
- `bitsPerPixel`
- `mimeType`
- `format` ("PNG", "JPEG", "GIF", ...)
- `progressive` (boolean for JPEGs)

```javascript
var info = Image.getInfo("/var/www/photo.jpg");
res.write(info.width + " × " + info.height + " " + info.mimeType);
```

Accepts the same argument types as the constructor.

## Image Manipulation

The constructed `Image` object exposes Java methods inherited from `helma.image.ImageWrapper`. Key methods:

```javascript
img.getWidth();
img.getHeight();
img.resize(width, height);
img.crop(x, y, width, height);
img.rotate(degrees);

img.drawString(text, x, y);
img.setColor(0xff, 0x00, 0x00);   // RGB
img.setFont("Arial", "BOLD", 14);

img.saveAs(path);                  // save to file
var bytes = img.toByteArray("jpg"); // serialize to byte[]
```

## Example: Generate Thumbnail

```javascript
function thumbnail_action() {
    var src = new Image(this.imageData);
    var dest = new Image(150, 100);
    dest.drawImage(src, 0, 0, 150, 100);
    res.contentType = "image/jpeg";
    res.writeBinary(dest.toByteArray("jpeg"));
}
```

## Example: Watermark

```javascript
function watermark_action() {
    var img = new Image(this.imageData);
    img.setColor(255, 255, 255);
    img.drawString("© Example", 10, img.getHeight() - 20);
    res.contentType = "image/jpeg";
    res.writeBinary(img.toByteArray("jpeg"));
}
```

## Supported Formats

- **Read**: PNG, JPEG, GIF, BMP, WebP (via the bundled `webp-imageio` dependency)
- **Write**: PNG, JPEG, GIF, BMP, WebP

## Registration

`Image` is registered as a global lazy property by `RhinoCore.java`.

## See Also

- [`helma.Image` module](../modules/helma/image.md) — higher-level API
- [`jala.ImageFilter`](../modules/jala.md) — additional filters (sharpen, blur, unsharp mask)
- [`ImageObject.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/scripting/rhino/extensions/ImageObject.java)
