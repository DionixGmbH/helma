# helma.Chart

Excel-to-HTML chart rendering — reads a `.xls` file and produces an HTML chart.

```javascript
app.addRepository("modules/helma/Chart.js");
```

## Constructor

```javascript
var chart = new helma.Chart(filePath, prefix, sheetName);
```

Parameters:

- `filePath` — path to the `.xls` file
- `prefix` — CSS-class/ID prefix for the generated HTML
- `sheetName` — which sheet to render

## Methods

### `render()`

Render the chart to the current response buffer.

### `renderAsString()` → String

Render to a string.

### `toString()` → String

## Static

### `helma.Chart.example(file)`

Render a sample chart.

## Implementation

Uses JExcelAPI (`jxl-2.5.7.jar`) to read Excel files. Generates HTML tables and SVG/CSS-based bar charts.

## See Also

- [`modules/helma/Chart.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Chart.js)
