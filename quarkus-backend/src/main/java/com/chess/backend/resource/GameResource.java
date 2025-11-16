package com.chess.backend.resource;

import com.chess.backend.model.Game;
import com.chess.backend.service.GameService;
import com.chess.backend.client.AIServiceClient;
import com.chess.backend.dto.AIMoveRequest;
import com.chess.backend.dto.AIMoveResponse;
import com.chess.backend.dto.CreateGameRequest;
import com.chess.backend.dto.GameResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * REST endpoints for game management.
 */
@Path("/api/game")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameResource {

    private static final Logger LOG = Logger.getLogger(GameResource.class);

    @Inject
    GameService gameService;

    @Inject
    @RestClient
    AIServiceClient aiServiceClient;

    /**
     * Create a new game.
     */
    @POST
    @Path("/create")
    @RolesAllowed({"PLAYER", "ADMIN"})
    public Response createGame(@Valid CreateGameRequest request) {
        LOG.infof("Creating new game: mode=%s", request.mode);

        try {
            Game game = gameService.createGame(request);
            GameResponse response = GameResponse.from(game);

            return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();

        } catch (Exception e) {
            LOG.error("Failed to create game", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"Failed to create game\"}")
                .build();
        }
    }

    /**
     * Get game by ID.
     */
    @GET
    @Path("/{gameId}")
    public Response getGame(@PathParam("gameId") String gameId) {
        LOG.infof("Getting game: %s", gameId);

        try {
            UUID uuid = UUID.fromString(gameId);
            Game game = gameService.getGame(uuid);

            if (game == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Game not found\"}")
                    .build();
            }

            GameResponse response = GameResponse.from(game);
            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Invalid game ID\"}")
                .build();
        }
    }

    /**
     * Request AI move for a game.
     */
    @POST
    @Path("/ai-move")
    public Response requestAIMove(@Valid AIMoveRequest request) {
        LOG.infof("Requesting AI move for game: %s, FEN: %s", request.gameId, request.fen);

        try {
            // Call AI service
            AIMoveResponse aiResponse = aiServiceClient.predict(request);

            LOG.infof("AI move: %s -> %s (score: %.2f)",
                aiResponse.from, aiResponse.to, aiResponse.score);

            // Validate and apply move to game state (if gameId provided)
            if (request.gameId != null && !request.gameId.isEmpty()) {
                UUID gameId = UUID.fromString(request.gameId);
                gameService.applyMove(gameId, aiResponse.from, aiResponse.to);
            }

            return Response.ok(aiResponse).build();

        } catch (Exception e) {
            LOG.error("AI move request failed", e);

            // Fallback to minimax if AI service fails
            LOG.warn("Falling back to built-in engine");
            AIMoveResponse fallback = gameService.getFallbackMove(request.fen);

            return Response.ok(fallback).build();
        }
    }

    /**
     * List active games.
     */
    @GET
    @Path("/list")
    public Response listGames(
        @QueryParam("status") @DefaultValue("IN_PROGRESS") String status,
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size
    ) {
        LOG.infof("Listing games: status=%s, page=%d, size=%d", status, page, size);

        try {
            var games = gameService.listGames(status, page, size);
            return Response.ok(games).build();

        } catch (Exception e) {
            LOG.error("Failed to list games", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"Failed to list games\"}")
                .build();
        }
    }

    /**
     * Make a move in a game.
     */
    @POST
    @Path("/{gameId}/move")
    @RolesAllowed({"PLAYER", "ADMIN"})
    public Response makeMove(
        @PathParam("gameId") String gameId,
        @Valid MoveRequest moveRequest
    ) {
        LOG.infof("Making move in game %s: %s -> %s",
            gameId, moveRequest.from, moveRequest.to);

        try {
            UUID uuid = UUID.fromString(gameId);
            Game game = gameService.applyMove(uuid, moveRequest.from, moveRequest.to);

            GameResponse response = GameResponse.from(game);
            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        } catch (Exception e) {
            LOG.error("Failed to make move", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"Failed to make move\"}")
                .build();
        }
    }

    public static class MoveRequest {
        public String from;
        public String to;
        public String promotion;
    }
}
