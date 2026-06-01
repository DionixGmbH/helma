# Cron Jobs

Helma includes a built-in scheduler for running JavaScript functions at fixed intervals. Jobs are dispatched by the application's worker thread once per minute.

## Defining Cron Jobs Statically: `cron.properties`

Inside an application directory, create `cron.properties`:

```properties
# Every minute, run Root.heartbeat
heartbeat.function = heartbeat

# Every hour at minute 0, run Root.hourly
hourly.function = hourly
hourly.minute = 0

# Daily at 3:15 AM, run Root.dailyReport
dailyReport.function = dailyReport
dailyReport.hour = 3
dailyReport.minute = 15

# Weekday cleanup at 22:30 (Mon-Fri)
nightlyCleanup.function = nightlyCleanup
nightlyCleanup.weekday = monday,tuesday,wednesday,thursday,friday
nightlyCleanup.hour = 22
nightlyCleanup.minute = 30

# Generate monthly invoices on day 1 at 00:05
monthlyInvoice.function = generateInvoices
monthlyInvoice.day = 1
monthlyInvoice.hour = 0
monthlyInvoice.minute = 5

# Custom timeout (in seconds) for a long-running job
expensiveJob.function = doExpensiveWork
expensiveJob.hour = 4
expensiveJob.timeout = 1800     # 30 minutes
```

### Property Format

| Property | Possible values | Default |
|---|---|---|
| `<name>.function` | A function name on the Root prototype (`func` or `obj.func` or `obj.sub.func`) | required |
| `<name>.year` | Comma list of years and/or ranges, e.g. `2025` or `2025-2030` or `*` | `*` |
| `<name>.month` | Comma list of month names: `january, february, ...` or `*` | `*` |
| `<name>.day` | Comma list of day-of-month numbers and/or ranges (1-31) or `*` | `*` |
| `<name>.weekday` | Comma list of weekday names: `monday, tuesday, ...` or `*` | `*` |
| `<name>.hour` | Comma list of hours and/or ranges (0-23) or `*` | `*` |
| `<name>.minute` | Comma list of minutes and/or ranges (0-59) or `*` | `*` |
| `<name>.timeout` | Maximum runtime in seconds | 600 |

An unspecified value defaults to `*` (wildcard).

### Examples

- Every 15 minutes: `cron.demo.minute = 0,15,30,45`
- Every weekday at noon: `cron.demo.weekday = monday,tuesday,wednesday,thursday,friday`; `cron.demo.hour = 12`; `cron.demo.minute = 0`
- Every Wednesday and Saturday at 03:30: `cron.demo.weekday = wednesday,saturday`; `cron.demo.hour = 3`; `cron.demo.minute = 30`
- Quarterly: `cron.demo.month = january,april,july,october`; `cron.demo.day = 1`; `cron.demo.hour = 0`

## Defining Cron Jobs Dynamically: `app.addCronJob()`

Same as `cron.properties` but at runtime:

```javascript
// In Global/main.js or Root/main.js at startup
app.addCronJob("heartbeat");                              // every minute
app.addCronJob("daily", "*", "*", "*", "*", "3", "15");   // year, month, day, weekday, hour, minute
```

Signature: `app.addCronJob(functionName, year, month, day, weekday, hour, minute)` — all but the first are strings or `null`/omitted for `*`.

Remove: `app.removeCronJob(functionName)`.

List current jobs: `app.getCronJobs()` returns a read-only `Map<String, CronJob>`.

## What a Cron Function Looks Like

```javascript
// Root/main.js
function heartbeat() {
    app.log("Still alive");
}

function dailyReport() {
    var users = root.users.list();
    for each (var u in users) {
        sendDailyReport(u);
    }
}
```

Cron functions are invoked **without arguments**. Each runs:

- On a fresh request evaluator
- Inside its own DB transaction
- With no `req`, `res`, or `session` (these globals are `null`)
- With the configured timeout — exceeding it kills the thread and aborts the transaction

## How Scheduling Works

The application's worker thread (`Application.run()` at `src/main/java/helma/framework/core/Application.java:run()`) wakes up at every full minute. For each registered job:

1. Build a `Calendar` from the current time.
2. For each component (year, month, day-of-month, weekday, hour, minute), check if it matches the job's allowed set.
3. If all match: invoke the function asynchronously via `RequestEvaluator.invokeInternal()`.

Multiple jobs can fire in the same minute. They run sequentially on separate evaluators — they do NOT block the request handlers.

If two cron jobs are configured to run at the same minute, they're sorted by `timeout` ascending so the shortest jobs run first.

## Best Practices

- **Idempotency**: Like actions, cron functions can fail and may need to retry. Make them idempotent: don't send the same email twice if the function runs again.
- **Concurrency**: Two cron iterations of the *same* job don't overlap; the next iteration won't start until the previous returns or times out.
- **Long jobs**: For jobs longer than a few minutes, raise `<job>.timeout` accordingly. Default is 600 seconds (10 minutes).
- **Mid-job commits**: Call `res.commit()` (where `res` is *not* available!) — actually, in cron context you don't have `res`. Use `Packages.helma.objectmodel.db.Transactor.getInstance().commit()` and `.begin(...)` to checkpoint long-running work, or split the job into multiple functions.
- **Resource use**: A cron job competes with HTTP requests for evaluator threads. Set `maxThreads` high enough to leave headroom.
- **Time zone**: The scheduler uses the JVM's default timezone. Set with `-Duser.timezone=Europe/Vienna` on JVM startup.

## Disabling Cron Jobs

To stop a job without removing it from `cron.properties`, delete its `.function` line — the job is dropped during parsing.

To disable *all* cron processing for an app:

```properties
# app.properties
cron = false
```

Or set the JVM flag `-Dhelma.cron.disabled=true` server-wide.

## Inspecting Active Jobs

From a skin or action:

```javascript
var jobs = app.getCronJobs();        // Map<String, CronJob>
for each (var name in jobs.keySet().toArray()) {
    var job = jobs.get(name);
    res.write(name + " → " + job.getFunction() + " (timeout " + job.getTimeout() + "ms)\n");
}
```

## Example: Daily Email Report

```javascript
// cron.properties
dailyReport.function = dailyReport
dailyReport.hour = 8
dailyReport.minute = 0
dailyReport.timeout = 600

// Root/cron.js
function dailyReport() {
    var since = new Date(Date.now() - 24 * 60 * 60 * 1000);
    var posts = root.posts.filter(p => p.created > since);

    var html = renderSkinAsString("dailyReport", { posts: posts });

    for each (var user in root.users.list()) {
        if (user.subscribesToDigest) {
            sendMail(user.email, "Daily Digest", html);
        }
    }
}

function sendMail(to, subject, html) {
    var mail = new Packages.helma.scripting.rhino.extensions.MailObject();
    mail.setTo(to);
    mail.setSubject(subject);
    mail.setHtmlPart(html);
    mail.send();
}
```

## Example: Hourly Cache Refresh

```javascript
// cron.properties
refresh.function = refreshCache
refresh.minute = 0

// Root/cron.js
function refreshCache() {
    var data = computeExpensiveStats();
    app.data.cachedStats = data;
    app.data.statsUpdatedAt = new Date();
}

// In a regular action:
function stats_action() {
    res.write(JSON.stringify(app.data.cachedStats || {}));
}
```

## See Also

- [Transactions](../concepts/transactions.md) — what makes cron jobs transactional
- [`app.invoke()` and `app.invokeAsync()`](internal-invocation.md) — one-off invocation outside of HTTP
- [Reference: `cron.properties`](../reference/cron-properties.md)
