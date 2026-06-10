/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.servlet;

import helma.framework.core.Application;
import helma.framework.core.Session;
import helma.framework.core.SocketBean;
import helma.framework.core.SocketConnection;
import helma.objectmodel.INode;
import helma.objectmodel.TransientNode;

import org.eclipse.jetty.ee9.websocket.api.WebSocketAdapter;

/**
 * A single live WebSocket connection, bridging Jetty's WebSocket callbacks to
 * Helma's scripting layer.
 *
 * <p>Each lifecycle event is dispatched through the application's evaluator pool
 * (see {@link Application#invokeSocket}) so the handler runs with a live
 * transaction, HopObject access, and the connection's session &ndash; the same
 * environment as an action. The endpoint object is re-resolved from the request
 * path on every event, so no scripting-engine object is cached across threads.</p>
 *
 * <p>Implements {@link SocketConnection} so the {@link helma.framework.core.SocketManager}
 * can address it for channel fan-out without depending on Jetty.</p>
 */
public class HelmaWebSocket extends WebSocketAdapter implements SocketConnection {

    private final Application app;
    // request path of the endpoint, relative to the app, e.g. "room/chat"
    private final String path;
    private final Session session;
    private final String id;

    // lazily-created per-connection scratch node (socket.data)
    private INode data;

    public HelmaWebSocket(Application app, String path, Session session, String id) {
        this.app = app;
        this.path = path;
        this.session = session;
        this.id = id;
    }

    // --- Jetty WebSocket lifecycle -----------------------------------------

    @Override
    public void onWebSocketConnect(org.eclipse.jetty.ee9.websocket.api.Session sess) {
        super.onWebSocketConnect(sess);
        app.getSocketManager().register(this);
        dispatch("_open", new Object[] { new SocketBean(this, app, session) });
    }

    @Override
    public void onWebSocketText(String message) {
        dispatch("_message", new Object[] { new SocketBean(this, app, session), message });
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        try {
            dispatch("_close", new Object[] {
                    new SocketBean(this, app, session),
                    Integer.valueOf(statusCode),
                    reason });
        } finally {
            app.getSocketManager().unregister(this);
            super.onWebSocketClose(statusCode, reason);
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        dispatch("_error", new Object[] {
                new SocketBean(this, app, session),
                cause == null ? null : cause.toString() });
        super.onWebSocketError(cause);
    }

    /**
     * Invoke a {@code <name>_socket<suffix>} handler, logging (but swallowing)
     * any error so a faulty handler cannot tear down the transport.
     */
    private void dispatch(String suffix, Object[] args) {
        try {
            app.invokeSocket(path, suffix, args, session);
        } catch (Exception x) {
            app.logError("Error in socket handler " + suffix + " for " + path, x);
        }
    }

    // --- SocketConnection --------------------------------------------------

    public String getId() {
        return id;
    }

    public boolean isOpen() {
        return isConnected();
    }

    public void send(Object message) {
        org.eclipse.jetty.ee9.websocket.api.Session sess = getSession();
        if (sess == null || !sess.isOpen()) {
            return;
        }
        String text = (message instanceof String) ? (String) message
                : String.valueOf(message);
        try {
            sess.getRemote().sendString(text);
        } catch (Exception x) {
            app.logError("Error sending to socket " + id, x);
        }
    }

    public void close(int code, String reason) {
        org.eclipse.jetty.ee9.websocket.api.Session sess = getSession();
        if (sess != null) {
            sess.close(code, reason);
        }
    }

    public synchronized INode getData() {
        if (data == null) {
            data = new TransientNode(app, "socket");
        }
        return data;
    }
}
