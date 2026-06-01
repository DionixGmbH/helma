# Ftp Extension

`FtpClient` is the FTP client wrapper. The global alias is `Ftp`.

Implementation: `src/main/java/helma/scripting/rhino/extensions/FtpObject.java` (using Apache Commons Net).

## Constructor

```javascript
var ftp = new FtpClient("ftp.example.com");
// or
var ftp = new Ftp("ftp.example.com");      // alias
```

## API

### Authentication

```javascript
ftp.login("user", "password");
ftp.logout();
```

### Working directory

```javascript
ftp.cd("/remote/path");          // change remote dir
ftp.lcd("/local/path");          // change local dir
```

### Directories

```javascript
ftp.mkdir("newdir");
```

### Mode

```javascript
ftp.binary();        // binary transfer mode
ftp.ascii();         // ASCII transfer mode
```

### File transfer

```javascript
// Upload
ftp.putFile("/local/file.txt", "remote.txt");
ftp.putString("file content", "remote.txt");

// Download
ftp.getFile("remote.txt", "/local/file.txt");
var content = ftp.getString("remote.txt");
```

## Example: Backup Upload

```javascript
function uploadBackup() {
    var ftp = new Ftp(getProperty("backupHost"));
    if (!ftp.login(getProperty("backupUser"), getProperty("backupPwd"))) {
        app.logError("FTP login failed");
        return;
    }
    try {
        ftp.binary();
        ftp.cd("/backups/" + new Date().toISOString().slice(0, 10));
        ftp.mkdir("");           // ensure exists
        ftp.putFile("/tmp/backup.tar.gz", "backup.tar.gz");
    } finally {
        ftp.logout();
    }
}
```

## Limitations

- No FTPS / SFTP support (use [`helma.Ssh`](../modules/helma/ssh.md) for SFTP)
- No connection timeout configuration in this wrapper
- Errors are returned as boolean false; check `app.logEvent` for diagnostics

## See Also

- [`helma.Ftp` module](../modules/helma/ftp.md) — slightly higher-level wrapper
- [`helma.Ssh` module](../modules/helma/ssh.md) — for SFTP/SCP
- [`FtpObject.java`](https://github.com/DionixGmbH/helma/src/branch/main/src/main/java/helma/scripting/rhino/extensions/FtpObject.java)
