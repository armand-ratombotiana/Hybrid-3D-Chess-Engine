package com.chess.client.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.websocket.*;
import java.net.URI;
import java.util.function.Consumer;

/**
 * WebSocket client for real-time game updates.
 */
@ClientEndpoint
public class WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketClient.class);

    private final String url;
    private final String authToken;
    private Session session;
    private Consumer<String> messageHandler;

    public WebSocketClient(String url, String authToken) {
        this.url = url;
        this.authToken = authToken;
    }

    public void connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            URI uri = new URI(url);

            logger.info("Connecting to WebSocket: {}", url);
            session = container.connectToServer(this, uri);

            logger.info("WebSocket connected");
        } catch (Exception e) {
            logger.error("WebSocket connection failed", e);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        logger.info("WebSocket session opened: {}", session.getId());

        // Send authentication if token available
        if (authToken != null) {
            try {
                session.getBasicRemote().sendText("{\"type\":\"AUTH\",\"token\":\"" + authToken + "\"}");
            } catch (Exception e) {
                logger.error("Failed to send auth message", e);
            }
        }
    }

    @OnMessage
    public void onMessage(String message) {
        logger.debug("WebSocket message received: {}", message);

        if (messageHandler != null) {
            messageHandler.accept(message);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("WebSocket closed: {}", closeReason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("WebSocket error", throwable);
    }

    public void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                logger.error("Failed to send message", e);
            }
        }
    }

    public void close() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                logger.error("Error closing WebSocket", e);
            }
        }
    }

    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }
}
