# helma.Color

Color manipulation — RGB/HSL conversion, named colors.

```javascript
app.addRepository("modules/helma/Color.js");
```

## Constructor

```javascript
var c = new helma.Color(R, G, B);    // 0..255 each
```

## Methods

### `valueOf(channel)` → int

Get the integer RGB value, or one channel.

```javascript
c.valueOf();          // 0xRRGGBB integer
c.valueOf("r");       // R channel value
```

### `toString()` → String

Hex color string like `"#a1b2c3"`.

### `getName()` → String

Closest matching CSS color name, or null.

## Static

### `helma.Color.fromName(name)` → helma.Color

Construct from a CSS color name.

```javascript
var red = helma.Color.fromName("red");
```

### `helma.Color.fromHsl(H, S, L)` → helma.Color

Construct from HSL components.

```javascript
var c = helma.Color.fromHsl(0, 100, 50);    // red
```

## Constants

### `helma.Color.COLORNAMES`

Array of all known CSS color names.

### `helma.Color.COLORVALUES`

Array of corresponding RGB values.

## See Also

- [`modules/helma/Color.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Color.js)
