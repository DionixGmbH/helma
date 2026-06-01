# Date

`modules/core/Date.js` extends `Date.prototype` with formatting and arithmetic helpers.

## Instance Methods

### `format(pattern, locale, timezone)` → String

Format the date using a Java `SimpleDateFormat` pattern.

```javascript
new Date().format("yyyy-MM-dd HH:mm:ss");
// → "2026-06-01 12:34:56"

new Date().format("EEEE, d MMMM yyyy", "de");
// → "Montag, 1. Juni 2026"

new Date().format("HH:mm zzz", null, "America/Los_Angeles");
// → "03:34 PDT"
```

Patterns are standard Java SimpleDateFormat — `yyyy`, `MM`, `dd`, `HH`, `mm`, `ss`, `EEE`, `EEEE`, `MMM`, `MMMM`, `z`, `Z`, etc.

### `toUtc()` → Date

Return a new Date adjusted to UTC.

### `toLocalTime()` → Date

Return a new Date adjusted to the JVM's local timezone.

### `diff(otherDate)` → Object

Compute the difference between two dates, returned as an object:

```javascript
var d1 = new Date("2026-01-01");
var d2 = new Date("2026-06-01");
var diff = d2.diff(d1);
// { years: 0, months: 5, days: 0, hours: 0, minutes: 0, seconds: 0, milliseconds: 0 }
```

### `getTimespan(param)` → Object

Return the difference between this date and now (or a given date) as a span object.

### `getAge(param)` → Object

Return the age (this date is treated as a birthdate; result is the years/months/days since).

### `getExpiry(param)` → Object

Return the time until this date in the future.

### `equals(otherDate, granularity)` → boolean

Test whether two dates are equal at a given granularity (e.g. ignoring time-of-day).

```javascript
d1.equals(d2);                  // millisecond-exact
d1.equals(d2, Date.YEAR);       // same year
```

## Constants

Date.js sets constants for granularity comparisons: `Date.YEAR`, `Date.MONTH`, `Date.DAY`, `Date.HOUR`, `Date.MINUTE`, `Date.SECOND`.

## See Also

- [`modules/core/Date.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/core/Date.js)
- [`SimpleDateFormat` Javadoc](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html)
