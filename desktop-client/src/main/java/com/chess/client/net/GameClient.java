package com.chess.client.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Network client for communicating with Quarkus backend.
 * Handles REST API calls and WebSocket connections.
 */
public class GameClient {

    private static final Logger logger = LoggerFactory.getLogger(GameClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String serverUrl;
    private String authToken;
    private WebSocketClient wsClient;
    private boolean connected;

    public GameClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        this.objectMapper = new ObjectMapper();
        this.connected = false;
    }

    /**
     * Connect to game server.
     */
    public void connect(String serverUrl, Consumer<Boolean> callback) {
        this.serverUrl = serverUrl;

        // Test connection with health check
        CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/q/health/live"))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );

                return response.statusCode() == 200;
            } catch (Exception e) {
                logger.error("Connection failed", e);
                return false;
            }
        }).thenAccept(success -> {
            connected = success;
            callback.accept(success);
        });
    }

    /**
     * Authenticate user and obtain JWT token.
     */
    public CompletableFuture<Boolean> login(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> credentials = Map.of(
                    "username", username,
                    "password", password
                );

                String jsonBody = objectMapper.writeValueAsString(credentials);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 200) {
                    Map<String, Object> result = objectMapper.readValue(
                        response.body(),
                        Map.class
                    );
                    authToken = (String) result.get("token");
                    logger.info("Login successful");
                    return true;
                }

                logger.warn("Login failed: {}", response.statusCode());
                return false;

            } catch (Exception e) {
                logger.error("Login error", e);
                return false;
            }
        });
    }

    /**
     * Create a new game.
     */
    public void createGame(BiConsumer<Boolean, String> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> gameRequest = Map.of(
                    "mode", "ONLINE",
                    "timeControl", Map.of(
                        "minutes", 10,
                        "increment", 5
                    )
                );

                String jsonBody = objectMapper.writeValueAsString(gameRequest);

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/game/create"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

                if (authToken != null) {
                    requestBuilder.header("Authorization", "Bearer " + authToken);
                }

                HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    Map<String, Object> result = objectMapper.readValue(
                        response.body(),
                        Map.class
                    );
                    String gameId = (String) result.get("gameId");
                    return Map.of("success", true, "gameId", gameId);
                }

                return Map.of("success", false, "gameId", "");

            } catch (Exception e) {
                logger.error("Create game error", e);
                return Map.of("success", false, "gameId", "");
            }
        }).thenAccept(result -> {
            callback.accept((Boolean) result.get("success"), (String) result.get("gameId"));
        });
    }

    /**
     * Request AI move.
     */
    public CompletableFuture<String> requestAIMove(String gameId, String fen) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> moveRequest = Map.of(
                    "gameId", gameId,
                    "fen", fen,
                    "playerColor", "black",
                    "timeControl", Map.of(
                        "minutes", 10,
                        "increment", 5
                    )
                );

                String jsonBody = objectMapper.writeValueAsString(moveRequest);

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/game/ai-move"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30));

                if (authToken != null) {
                    requestBuilder.header("Authorization", "Bearer " + authToken);
                }

                HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 200) {
                    Map<String, Object> result = objectMapper.readValue(
                        response.body(),
                        Map.class
                    );

                    String from = (String) result.get("from");
                    String to = (String) result.get("to");

                    logger.info("AI move: {} -> {}", from, to);
                    return from + to;
                }

                logger.warn("AI move request failed: {}", response.statusCode());
                return null;

            } catch (Exception e) {
                logger.error("AI move request error", e);
                return null;
            }
        });
    }

    /**
     * Connect to WebSocket for live game updates.
     */
    public void connectWebSocket(String gameId, Consumer<String> messageHandler) {
        if (wsClient != null) {
            wsClient.close();
        }

        String wsUrl = serverUrl.replace("http://", "ws://").replace("https://", "wss://");
        wsUrl += "/ws/game/" + gameId;

        wsClient = new WebSocketClient(wsUrl, authToken);
        wsClient.setMessageHandler(messageHandler);
        wsClient.connect();
    }

    public void disconnect() {
        if (wsClient != null) {
            wsClient.close();
        }
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getAuthToken() {
        return authToken;
    }
}
