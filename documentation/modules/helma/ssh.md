# helma.Ssh

SSH/SCP client wrapper. Built on Ganymed SSH-2.

```javascript
app.addRepository("modules/helma/Ssh.js");
```

## Constructor

```javascript
var ssh = new helma.Ssh(server);
var ssh = new helma.Ssh(server, knownHostsFile);
```

## Configuration

```javascript
ssh.setParanoid(true);                    // verify host fingerprint
ssh.addKnownHosts("/path/to/known_hosts");
```

## Authentication

```javascript
// Password auth
ssh.connect("alice", "password");

// Key auth
ssh.connectWithKey("alice", "/home/alice/.ssh/id_rsa");
ssh.connectWithKey("alice", "/home/alice/.ssh/id_rsa", "passphrase");
```

## File Transfer (SCP)

```javascript
// Upload
ssh.put("/local/file.txt", "/remote/path", "0644");

// Download
ssh.get("/remote/file.txt", "/local/dir");
```

## Remote Execution

```javascript
var result = ssh.execCommand("ls /var/log");
print(result.stdout);
print(result.stderr);
print(result.exitStatus);
```

## Connection

```javascript
ssh.isConnected();
ssh.disconnect();
```

## Example: Deploy

```javascript
function deploy() {
    app.addRepository("modules/helma/Ssh.js");
    var ssh = new Ssh("deploy.example.com");
    ssh.connectWithKey("deployuser", "/home/helma/.ssh/id_rsa");
    try {
        ssh.put("/tmp/build.tar.gz", "/var/www/deploys/");
        var r = ssh.execCommand("cd /var/www && tar xzf deploys/build.tar.gz && systemctl restart myapp");
        if (r.exitStatus !== 0) {
            app.logError("Deploy failed: " + r.stderr);
        }
    } finally {
        ssh.disconnect();
    }
}
```

## See Also

- [`modules/helma/Ssh.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Ssh.js)
- Ganymed SSH-2: `modules/helma/ganymed-ssh2-build208.jar`
