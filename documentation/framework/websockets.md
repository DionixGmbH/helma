# WebSockets

Helma serves WebSocket connections alongside ordinary HTTP, using the same
prototype/path conventions you already use for actions. A connection is declared
by a function suffix, its lifecycle is handled by sibling functions, and you can
push messages to connected clients from anywhere in your application — an action,
a macro, or a cron job.

## Declaring an endpoint

A `<name>_socket` function is a **handshake gate**, reached at the same URL an
action would be. It runs once, when a client tries to open the connection, and
decides whether to accept it:

```javascript
// in Root/Root.js  →  ws://host/<app>/chat
function chat_socket() {
    // `this` is the resolved object, `session`/`req` are available
    if (!session.user) {
        return false;        // reject the upgrade (HTTP 403)
    }
    // return true / undefined → accept
}
```

Put `chat_socket` on a `Room` prototype instead and it is reached at
`ws://host/<app>/<room>/chat`, with `this` bound to the resolved `Room` — exactly
like action resolution. Returning `false` rejects the handshake with `403`; if no
`<name>_socket` function exists at the path the handshake fails with `404`.

!!! note "URL prefix"
    The endpoint lives under the application's mount point. With the default mount
    (the application name) the URL is `ws://host/<app>/chat`. Mount the app at `/`
    to get `ws://host/chat`.

## Lifecycle handlers

Connection events are dispatched to sibling functions named after the endpoint,
the same way `foo_action_post` qualifies `foo_action`:

```javascript
function chat_socket_open(socket) {
    socket.subscribe(this.name);                    // join a channel
    app.publish(this.name, session.user.name + " joined");
}

function chat_socket_message(socket, message) {
    // runs in its own transaction — full HopObject/DB access here
    var post = new Post();
    post.text = message;
    this.add(post);
    app.publish(this.name, post.text);
}

function chat_socket_close(socket, code, reason) {
    app.publish(this.name, session.user.name + " left");
}

function chat_socket_error(socket, error) {
    app.log("socket error: " + error);
}
```

All four are optional — an undefined handler is simply a no-op. Each runs on a
pooled evaluator with a live transaction and the connection's session, so the
environment matches an ordinary action (except there is no `req`/`res` — you have
`socket` instead).

## The `socket` object

Every handler receives a `socket` argument representing the connection:

| Member | Description |
|---|---|
| `socket.send(message)` | Send a message to this client. |
| `socket.close()` / `socket.close(code, reason)` | Close the connection (default code `1000`). |
| `socket.subscribe(channel)` | Subscribe this connection to a channel. |
| `socket.unsubscribe(channel)` | Unsubscribe from a channel. |
| `socket.id` | Stable unique connection id. |
| `socket.isOpen()` | Whether the connection is still open. |
| `socket.data` | A transient HopObject scoped to this connection, for per-connection state. |
| `socket.session` | The connection's `SessionBean` (resolved from the cookie at handshake). |

```javascript
function chat_socket_message(socket, message) {
    socket.data.lastMessage = message;              // remembered for the connection
    if (message == "/quit") {
        socket.close(1000, "bye");
    }
}
```

## Channels and fan-out

A single `socket.send` reaches one client. To reach **many**, subscribe
connections to a channel and publish to it. Publishing works from anywhere — most
usefully from an ordinary HTTP action:

```javascript
// Post/Post.js — a normal HTTP action
function comment_action_post() {
    var c = this.addComment(req.data.body);
    app.publish("post-" + this._id, c.render());    // live update to all watchers
    res.redirect(this.href());
}
```

On the `app` bean:

- `app.publish(channel, message)` — deliver `message` to every connection
  subscribed to `channel`.
- `app.getSockets()` / `app.getSockets(channel)` — the open connections (all, or
  on a channel).
- `app.countSockets()` — number of open connections.

## Sending data

A string is sent verbatim. For structured data, stringify it yourself:

```javascript
socket.send("plain text");
socket.send(JSON.stringify({ type: "post", id: post._id, text: post.text }));
app.publish("room-42", JSON.stringify({ event: "joined", user: name }));
```

Non-string values are coerced with `String()`, so always `JSON.stringify` objects
before sending.

## Authentication

The handshake resolves the session from the `HopSession` cookie, which the browser
sends automatically with the upgrade request for the same origin. The gate runs
with that session, so cookie-based login carries over:

```javascript
function chat_socket() {
    if (!session.user) return false;                // refuse anonymous clients
}
```

A connection without a recognised session cookie gets a fresh anonymous session
(so `socket.data` and `session.data` still work); reject it in the gate if your
endpoint requires a logged-in user.

## Execution model

Each inbound message is dispatched through the application's evaluator pool (the
same pool that serves HTTP requests), inside its own transaction. Consequences:

- Handlers have full HopObject and database access, and commit like an action.
- An **idle** connection costs only memory; a pooled thread is borrowed only while
  a message is being processed. Throughput is therefore governed by `maxThreads`,
  not by the number of open connections.
- A slow handler ties up an evaluator exactly as a slow action does. Keep message
  handlers quick, and size `maxThreads` for your expected message rate.

Messages from one connection are processed in order (the connection's read thread
blocks until each handler returns).

## Clustering

`app.publish` fans out to the connections held by **this** JVM. WebSocket
connections are node-local — an open socket cannot migrate between instances — so
when you run several Helma instances behind a load balancer, use sticky sessions
to keep a client's HTTP and WebSocket traffic on the same node.

Cluster-wide fan-out is a pluggable concern. The registry is a `SocketManager`
selected by the [`socketManagerImpl`](../reference/app-properties.md#socketmanagerimpl)
property; the default is single-JVM. A clustered implementation can broadcast
published messages to peer instances without any change to the application code
above — see [Helma-Swarm](https://github.com/DionixGmbH/helma-swarm).

## A complete example

```javascript
// Root/Root.js
function chat_socket() {
    if (!session.user) return false;
}

function chat_socket_open(socket) {
    socket.subscribe("lobby");
    app.publish("lobby", JSON.stringify({ event: "join", user: session.user.name }));
}

function chat_socket_message(socket, message) {
    app.publish("lobby", JSON.stringify({ user: session.user.name, text: message }));
}

function chat_socket_close(socket, code, reason) {
    app.publish("lobby", JSON.stringify({ event: "leave", user: session.user.name }));
}
```

```html
<!-- client -->
<script>
  var ws = new WebSocket("ws://" + location.host + "/myapp/chat");
  ws.onmessage = function (e) {
      var m = JSON.parse(e.data);
      console.log(m);
  };
  ws.onopen = function () { ws.send("hello"); };
</script>
```

## See also

- [Actions](actions.md) — the path/suffix resolution WebSocket endpoints reuse.
- [Internal Invocation](internal-invocation.md) — the evaluator-pool dispatch model.
- [Application Bean](../reference/app-bean.md#websockets) — `publish` / `getSockets` / `countSockets`.
- [app.properties](../reference/app-properties.md#socketmanagerimpl) — `socketManagerImpl`.
