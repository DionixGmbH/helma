# helma.Ftp

FTP client wrapper. Higher-level than the `Ftp` extension, with additional encoding options and timeouts.

```javascript
app.addRepository("modules/helma/Ftp.js");
```

## Constructor

```javascript
var ftp = new helma.Ftp(server);
```

## Timeouts

```javascript
ftp.setTimeout(60000);        // socket timeout in ms
ftp.setReadTimeout(30000);    // read timeout in ms
```

## Authentication

```javascript
ftp.login("user", "password");
ftp.logout();
```

## Mode

```javascript
ftp.binary();        // binary transfer
ftp.ascii();         // ASCII transfer
ftp.active();        // active mode (default)
ftp.passive();       // passive mode (for firewalls)
```

## Directory Operations

```javascript
ftp.cd("/remote/path");       // change remote working dir
ftp.pwd();                    // current remote dir
ftp.lcd("/local/path");       // change local working dir
ftp.mkdir("newdir");          // create remote dir
ftp.rmdir("olddir");          // remove remote dir
ftp.dir(".");                 // list dir contents
```

## File Transfer

```javascript
// Upload
ftp.putFile("/local/file.txt", "remote.txt");
ftp.putString("content here", "remote.txt", "UTF-8");
ftp.putBytes(byteArray, "remote.bin");

// Download
ftp.getFile("remote.txt", "/local/file.txt");
var content = ftp.getString("remote.txt");

// Operations
ftp.deleteFile("remote.txt");
ftp.renameFile("oldname", "newname");
```

## Example: Periodic Backup

```javascript
// cron.properties: hourly.function = uploadBackup ; hourly.hour = 3
function uploadBackup() {
    app.addRepository("modules/helma/Ftp.js");
    var ftp = new Ftp(getProperty("backupHost"));
    ftp.setTimeout(60000);

    if (!ftp.login(getProperty("backupUser"), getProperty("backupPwd"))) {
        app.logError("FTP login failed");
        return;
    }

    try {
        ftp.passive();
        ftp.binary();
        var today = new Date().toISOString().slice(0, 10);
        ftp.cd("/backups");
        ftp.mkdir(today);
        ftp.cd(today);
        ftp.putFile("/tmp/dump.tar.gz", "dump-" + today + ".tar.gz");
        app.log("Backup uploaded");
    } finally {
        ftp.logout();
    }
}
```

## See Also

- [`Ftp` extension](../../extensions/ftp.md) — lower-level API
- [`modules/helma/Ftp.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Ftp.js)
