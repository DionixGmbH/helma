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
 * A transport-agnostic handle to a single live client connection (a WebSocket)
 * managed by {@link SocketManager}.
 *
 * <p>The Jetty WebSocket endpoint implements this interface so the manager can
 * address connections without depending on the servlet / WebSocket layer, and
 * so a clustered {@code SocketManager} (e.g. Helma-Swarm) can fan messages out
 * without knowing anything about the underlying transport. It is also the object
 * a {@link SocketBean} wraps to expose the connection to scripts.</p>
 */
public interface SocketConnection {

    /**
     * A stable, unique id for this connection.
     *
     * @return the connection id
     */
    String getId();

    /**
     * Whether the connection is still open and able to receive messages.
     *
     * @return true if the connection is open
     */
    boolean isOpen();

    /**
     * Send a message to the client. A {@code String} is sent verbatim; any
     * other value is coerced to a string by the implementation.
     *
     * @param message the message to send
     */
    void send(Object message);

    /**
     * Close the connection with the given WebSocket status code and reason.
     *
     * @param code the WebSocket close status code (e.g. 1000 for normal closure)
     * @param reason an optional human-readable reason, or null
     */
    void close(int code, String reason);

    /**
     * A transient scratch node scoped to this connection, surviving across the
     * connection's lifecycle events. Exposed to scripts as {@code socket.data}.
     *
     * @return the connection's transient data node
     */
    INode getData();
}
