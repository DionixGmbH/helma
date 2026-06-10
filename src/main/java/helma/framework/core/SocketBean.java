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

package helma.framework.core;

import helma.objectmodel.INode;

/**
 * The {@code socket} object passed to WebSocket lifecycle handlers
 * ({@code <name>_socket_open} / {@code _message} / {@code _close} / {@code _error}).
 * It is a thin script-facing wrapper around a {@link SocketConnection}; a fresh
 * instance is created for each dispatch, while the underlying connection (and its
 * {@link #getData data} node) lives for the duration of the connection.
 */
public class SocketBean {

    private final SocketConnection connection;
    private final Application app;
    private final Session session;

    /**
     * @param connection the underlying connection
     * @param app the application
     * @param session the session this connection is bound to (may be null)
     */
    public SocketBean(SocketConnection connection, Application app, Session session) {
        this.connection = connection;
        this.app = app;
        this.session = session;
    }

    /**
     * Send a message to this client. A string is sent verbatim; for structured
     * data, stringify it first (e.g. {@code socket.send(JSON.stringify(obj))}).
     * @param message the message to send
     */
    public void send(Object message) {
        connection.send(message);
    }

    /**
     * Close this connection normally (status code 1000).
     */
    public void close() {
        connection.close(1000, null);
    }

    /**
     * Close this connection with the given WebSocket status code and reason.
     * @param code the close status code
     * @param reason an optional reason, or null
     */
    public void close(int code, String reason) {
        connection.close(code, reason);
    }

    /**
     * Subscribe this connection to a channel, so it receives messages sent with
     * {@code app.publish(channel, ...)}.
     * @param channel the channel name
     */
    public void subscribe(String channel) {
        app.getSocketManager().subscribe(connection, channel);
    }

    /**
     * Unsubscribe this connection from a channel.
     * @param channel the channel name
     */
    public void unsubscribe(String channel) {
        app.getSocketManager().unsubscribe(connection, channel);
    }

    /**
     * @return this connection's stable unique id
     */
    public String getId() {
        return connection.getId();
    }

    /**
     * @return true if the connection is still open
     */
    public boolean isOpen() {
        return connection.isOpen();
    }

    /**
     * @return a transient node scoped to this connection, for per-connection state
     */
    public INode getData() {
        return connection.getData();
    }

    /**
     * @return the session this connection is bound to, or null
     */
    public SessionBean getSession() {
        return session == null ? null : new SessionBean(session);
    }

    public String toString() {
        return "[Socket " + connection.getId() + "]";
    }
}
