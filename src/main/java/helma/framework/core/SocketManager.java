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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and fan-out hub for live client connections (WebSockets).
 *
 * <p>This default implementation is single-JVM: {@link #publish} delivers only
 * to connections held by this node. It is built to be subclassed &ndash; name an
 * alternative via the {@code socketManagerImpl} application property &ndash; so a
 * clustered implementation (e.g. Helma-Swarm) can override {@link #publish} to
 * additionally broadcast to peer nodes while reusing {@link #deliverLocal} for
 * node-local delivery. That split is the explicit clustering seam: a peer that
 * receives a broadcast calls {@code deliverLocal} (never {@code publish}), so no
 * re-broadcast loop forms.</p>
 *
 * <p>Connections are inherently node-local &ndash; an open socket lives on the JVM
 * that accepted it and cannot migrate &ndash; so only the channel
 * <em>subscriptions</em> of <em>this</em> node are tracked here. A clustered
 * publish therefore replicates the message, not the membership.</p>
 *
 * <p>Built in the image of {@link SessionManager}: created and initialised by
 * {@link Application}, and swappable by class name.</p>
 */
public class SocketManager {

    protected Application app;

    // open connections by id
    protected final Map<String, SocketConnection> sockets =
            new ConcurrentHashMap<String, SocketConnection>();

    // channel name -> set of subscribed connection ids
    protected final Map<String, Set<String>> channels =
            new ConcurrentHashMap<String, Set<String>>();

    public SocketManager() {
    }

    public void init(Application app) {
        this.app = app;
    }

    public void shutdown() {
        sockets.clear();
        channels.clear();
    }

    /**
     * Register a newly opened connection.
     *
     * @param socket the connection to register
     */
    public void register(SocketConnection socket) {
        sockets.put(socket.getId(), socket);
    }

    /**
     * Remove a closed connection and drop it from every channel.
     *
     * @param socket the connection to remove
     */
    public void unregister(SocketConnection socket) {
        String id = socket.getId();
        sockets.remove(id);
        for (Set<String> members : channels.values()) {
            members.remove(id);
        }
    }

    /**
     * Subscribe a connection to a channel.
     *
     * @param socket the connection
     * @param channel the channel name
     */
    public void subscribe(SocketConnection socket, String channel) {
        if (channel == null) {
            return;
        }
        channels.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet())
                .add(socket.getId());
    }

    /**
     * Unsubscribe a connection from a channel.
     *
     * @param socket the connection
     * @param channel the channel name
     */
    public void unsubscribe(SocketConnection socket, String channel) {
        if (channel == null) {
            return;
        }
        Set<String> members = channels.get(channel);
        if (members != null) {
            members.remove(socket.getId());
        }
    }

    /**
     * Publish a message to every connection subscribed to a channel.
     *
     * <p>The default implementation delivers to node-local subscribers only. A
     * clustered subclass overrides this to also broadcast to peers, and has each
     * peer call {@link #deliverLocal} on receipt.</p>
     *
     * @param channel the channel name
     * @param message the message; a string is sent verbatim, any other value is
     * encoded by the connection (typically as JSON)
     */
    public void publish(String channel, Object message) {
        deliverLocal(channel, message);
    }

    /**
     * Deliver a message to this node's subscribers of a channel. This is the
     * node-local half of {@link #publish}, and the entry point a clustered
     * subclass calls when a peer broadcast arrives.
     *
     * @param channel the channel name
     * @param message the message to deliver
     */
    protected void deliverLocal(String channel, Object message) {
        Set<String> members = channels.get(channel);
        if (members == null) {
            return;
        }
        for (String id : members) {
            SocketConnection socket = sockets.get(id);
            if (socket != null && socket.isOpen()) {
                try {
                    socket.send(message);
                } catch (Exception x) {
                    if (app != null) {
                        app.logError("Error sending to socket " + id, x);
                    }
                }
            }
        }
    }

    /**
     * Return all open connections held by this node.
     *
     * @return a snapshot collection of open connections
     */
    public Collection<SocketConnection> getSockets() {
        return new ArrayList<SocketConnection>(sockets.values());
    }

    /**
     * Return the open connections on this node subscribed to a channel.
     *
     * @param channel the channel name
     * @return a snapshot collection of subscribed connections
     */
    public Collection<SocketConnection> getSockets(String channel) {
        ArrayList<SocketConnection> result = new ArrayList<SocketConnection>();
        Set<String> members = channels.get(channel);
        if (members != null) {
            for (String id : members) {
                SocketConnection socket = sockets.get(id);
                if (socket != null) {
                    result.add(socket);
                }
            }
        }
        return result;
    }

    /**
     * Return the number of open connections held by this node.
     *
     * @return the current number of open sockets
     */
    public int countSockets() {
        return sockets.size();
    }
}
