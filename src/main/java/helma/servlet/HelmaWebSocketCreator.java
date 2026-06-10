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
import helma.framework.core.RequestEvaluator;
import helma.framework.core.Session;

import org.eclipse.jetty.ee9.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee9.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.ee9.websocket.server.JettyWebSocketCreator;

import java.net.HttpCookie;
import java.util.List;
import java.util.UUID;

/**
 * Decides, per WebSocket upgrade request, whether to accept the connection and
 * which {@link HelmaWebSocket} to create. One creator is mapped on the whole app
 * context ({@code /*}); the endpoint is resolved dynamically from the request
 * path, mirroring how actions are resolved for HTTP requests.
 *
 * <p>Handshake: the session is resolved from the {@code HopSession} cookie, then
 * the {@code <name>_socket} gate function runs with that session. The upgrade is
 * accepted unless the gate returns {@code false} (403) or the path has no socket
 * endpoint (404).</p>
 */
public class HelmaWebSocketCreator implements JettyWebSocketCreator {

    private final Application app;
    private final String sessionCookieName;
    private final String contextPath;

    public HelmaWebSocketCreator(Application app, String sessionCookieName, String contextPath) {
        this.app = app;
        this.sessionCookieName = sessionCookieName == null ? "HopSession" : sessionCookieName;
        this.contextPath = contextPath;
    }

    public Object createWebSocket(JettyServerUpgradeRequest request,
                                  JettyServerUpgradeResponse response) throws Exception {
        String path = appRelativePath(request.getRequestPath());

        // resolve the session from the cookie; fall back to an anonymous,
        // per-connection session so the gate and handlers always have one
        Session session = resolveSession(request);

        // run the handshake gate
        Object result;
        try {
            result = app.invokeSocket(path, "", RequestEvaluator.EMPTY_ARGS, session);
        } catch (Exception notFound) {
            // no socket endpoint at this path (or it threw): reject the upgrade
            response.sendError(404, "No socket endpoint at /" + path);
            return null;
        }

        if (Boolean.FALSE.equals(result)) {
            response.sendError(403, "Socket upgrade rejected");
            return null;
        }

        String id = "ws-" + UUID.randomUUID();
        return new HelmaWebSocket(app, path, session, id);
    }

    /**
     * Strip the context (mount) path and any leading slash, yielding the path as
     * the request evaluator expects it (e.g. "room/chat").
     */
    private String appRelativePath(String requestPath) {
        String p = requestPath == null ? "" : requestPath;
        if (contextPath != null && !"/".equals(contextPath) && p.startsWith(contextPath)) {
            p = p.substring(contextPath.length());
        }
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    private Session resolveSession(JettyServerUpgradeRequest request) {
        String sessionId = null;
        List<HttpCookie> cookies = request.getCookies();
        if (cookies != null) {
            for (HttpCookie cookie : cookies) {
                if (sessionCookieName.equals(cookie.getName())) {
                    sessionId = cookie.getValue();
                    break;
                }
            }
        }

        Session session = sessionId == null ? null : app.getSession(sessionId);
        if (session == null) {
            // anonymous connection: a transient, unregistered session keyed by a
            // fresh id so socket.data and session.data work per connection
            session = app.createSession(sessionId != null ? sessionId
                    : ("ws-" + UUID.randomUUID()));
        }
        return session;
    }
}
