# cron.properties

`cron.properties` defines scheduled JavaScript function invocations for an application. Lives at `apps/<app>/cron.properties`.

See [Cron Jobs](../framework/cron-jobs.md) for the conceptual overview.

## Format

```properties
<jobname>.function = functionName
<jobname>.year     = years
<jobname>.month    = months
<jobname>.day      = days
<jobname>.weekday  = weekdays
<jobname>.hour     = hours
<jobname>.minute   = minutes
<jobname>.timeout  = seconds
```

The `<jobname>` is your label for the job. The first part of each line (before the dot) groups all settings for one job together.

## Settings

| Property | Possible values | Default |
|---|---|---|
| `<job>.function` | JavaScript function name on `Root` (or `Object.fn` for nested) | required |
| `<job>.year` | Comma-list of years (`2025`) or ranges (`2025-2030`) or `*` | `*` |
| `<job>.month` | Month names: `january, february, ...` or `*` | `*` |
| `<job>.day` | Day of month numbers/ranges (1-31) or `*` | `*` |
| `<job>.weekday` | Weekday names: `monday, tuesday, ...` or `*` | `*` |
| `<job>.hour` | Hour numbers/ranges (0-23) or `*` | `*` |
| `<job>.minute` | Minute numbers/ranges (0-59) or `*` | `*` |
| `<job>.timeout` | Max runtime in seconds | 600 (10 minutes) |

A property defaulting to `*` matches every value. Omit any property to use its default.

## Value Format

### Single value

```properties
nightly.hour = 3
nightly.minute = 0
```

### Comma list

```properties
quarterly.month = january, april, july, october
quarterly.day = 1
quarterly.hour = 0
```

### Range

```properties
businessHours.hour = 9-17     # 9 AM through 5 PM
businessHours.minute = 0
```

### Mixed

```properties
oddHours.hour = 1, 3, 5, 7-23
```

### Wildcard

```properties
everyMinute.minute = *
everyMinute.hour = *
# (everything else defaults to *)
```

## Examples

### Every minute

```properties
heartbeat.function = heartbeat
```

(All defaults — runs every minute of every hour of every day.)

### Hourly

```properties
hourly.function = hourly
hourly.minute = 0
```

### Daily at 3:15 AM

```properties
dailyReport.function = dailyReport
dailyReport.hour = 3
dailyReport.minute = 15
```

### Weekday cleanup

```properties
weekdayCleanup.function = cleanup
weekdayCleanup.weekday = monday, tuesday, wednesday, thursday, friday
weekdayCleanup.hour = 22
weekdayCleanup.minute = 30
```

### Monthly invoice

```properties
monthlyInvoice.function = generateInvoices
monthlyInvoice.day = 1
monthlyInvoice.hour = 0
monthlyInvoice.minute = 5
```

### Yearly tax report

```properties
yearlyTax.function = taxReport
yearlyTax.month = january
yearlyTax.day = 1
yearlyTax.hour = 0
yearlyTax.minute = 5
```

### Every 15 minutes during business hours

```properties
businessCheck.function = checkSomething
businessCheck.weekday = monday, tuesday, wednesday, thursday, friday
businessCheck.hour = 9-17
businessCheck.minute = 0, 15, 30, 45
```

### Long-running job with custom timeout

```properties
expensiveJob.function = doExpensiveWork
expensiveJob.hour = 4
expensiveJob.minute = 0
expensiveJob.timeout = 7200       # 2 hours
```

## Dynamic Cron Jobs

You can also add jobs at runtime from JavaScript:

```javascript
app.addCronJob("dailyDigest", "*", "*", "*", "*", "8", "0");
```

See [`app.addCronJob`](app-bean.md#cron-jobs).

## Reloading

Helma re-reads `cron.properties` when its mtime changes. Adding a new job, editing an existing one, or removing one are picked up at the next minute boundary.

## Disabling

Setting `cron = false` in `app.properties` disables cron processing for the entire application.

```properties
# app.properties
cron = false
```

## Timezone

The scheduler uses the JVM's default timezone. Override at startup:

```bash
java -Duser.timezone=Europe/Vienna -jar launcher.jar -h ...
```

## See Also

- [Framework: Cron Jobs](../framework/cron-jobs.md) — full guide
- [Reference: `app.addCronJob()`](app-bean.md)
- [`CronJob.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/util/CronJob.java) — source
