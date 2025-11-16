package com.chess.client.ui;

import com.chess.client.net.GameClient;
import com.chess.client.renderer.OpenGLRenderer;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main UI controller for JavaFX interface.
 * Displays the OpenGL-rendered chess board and controls.
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private BorderPane root;

    @FXML
    private ImageView renderView;

    @FXML
    private VBox controlPanel;

    @FXML
    private Button newGameButton;

    @FXML
    private Button connectButton;

    @FXML
    private RadioButton localModeRadio;

    @FXML
    private RadioButton aiModeRadio;

    @FXML
    private RadioButton onlineModeRadio;

    @FXML
    private TextArea moveHistoryArea;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField serverUrlField;

    private OpenGLRenderer renderer;
    private GameClient gameClient;
    private AnimationTimer uiUpdateTimer;

    @FXML
    public void initialize() {
        logger.info("Initializing MainController...");

        // Setup toggle group for game modes
        ToggleGroup modeGroup = new ToggleGroup();
        localModeRadio.setToggleGroup(modeGroup);
        aiModeRadio.setToggleGroup(modeGroup);
        onlineModeRadio.setToggleGroup(modeGroup);
        localModeRadio.setSelected(true);

        // Setup button actions
        newGameButton.setOnAction(e -> startNewGame());
        connectButton.setOnAction(e -> connectToServer());

        // Initialize game client
        gameClient = new GameClient();

        // Start UI update timer
        startUIUpdateTimer();

        statusLabel.setText("Ready");
        logger.info("MainController initialized");
    }

    /**
     * Set the OpenGL renderer and start updating the ImageView.
     */
    public void setRenderer(OpenGLRenderer renderer) {
        this.renderer = renderer;

        // Bind renderer output to ImageView
        if (renderer != null) {
            renderView.setImage(renderer.getFrameImage());
            renderView.setPreserveRatio(true);
            renderView.setSmooth(true);

            logger.info("Renderer bound to ImageView");
        }
    }

    /**
     * Start a timer to update UI elements from game state.
     */
    private void startUIUpdateTimer() {
        uiUpdateTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateUI();
            }
        };
        uiUpdateTimer.start();
    }

    /**
     * Update UI based on current game state.
     */
    private void updateUI() {
        if (renderer == null || renderer.getBoard() == null) {
            return;
        }

        // Update move history
        String moves = renderer.getBoard().getMoveHistory();
        if (!moveHistoryArea.getText().equals(moves)) {
            moveHistoryArea.setText(moves);
            moveHistoryArea.setScrollTop(Double.MAX_VALUE);
        }

        // Update status
        String status = getGameStatus();
        if (!statusLabel.getText().equals(status)) {
            statusLabel.setText(status);
        }
    }

    /**
     * Get current game status.
     */
    private String getGameStatus() {
        if (renderer == null || renderer.getBoard() == null) {
            return "Initializing...";
        }

        if (renderer.getBoard().isCheckmate()) {
            return "Checkmate! " + (renderer.getBoard().isWhiteTurn() ? "Black" : "White") + " wins!";
        }

        if (renderer.getBoard().isStalemate()) {
            return "Stalemate - Draw";
        }

        if (renderer.getBoard().isCheck()) {
            return "Check! " + (renderer.getBoard().isWhiteTurn() ? "White" : "Black") + " to move";
        }

        return (renderer.getBoard().isWhiteTurn() ? "White" : "Black") + " to move";
    }

    /**
     * Start a new game.
     */
    @FXML
    private void startNewGame() {
        logger.info("Starting new game...");

        if (renderer != null && renderer.getBoard() != null) {
            renderer.getBoard().reset();
            moveHistoryArea.clear();
            statusLabel.setText("New game started");

            // Determine game mode
            if (aiModeRadio.isSelected()) {
                startAIGame();
            } else if (onlineModeRadio.isSelected()) {
                startOnlineGame();
            } else {
                statusLabel.setText("Local game - White to move");
            }
        }
    }

    /**
     * Start AI game mode.
     */
    private void startAIGame() {
        logger.info("Starting AI game mode...");
        statusLabel.setText("AI game - White (Human) to move");

        // TODO: Implement AI move handling
        // When player makes a move, call gameClient.requestAIMove()
    }

    /**
     * Start online multiplayer game.
     */
    private void startOnlineGame() {
        logger.info("Starting online game mode...");

        if (!gameClient.isConnected()) {
            statusLabel.setText("Not connected to server");
            showAlert("Connection Required", "Please connect to a server first.");
            return;
        }

        // Create game via API
        gameClient.createGame((success, gameId) -> {
            if (success) {
                statusLabel.setText("Online game created: " + gameId);
                logger.info("Game created with ID: {}", gameId);
            } else {
                statusLabel.setText("Failed to create game");
                showAlert("Error", "Failed to create online game.");
            }
        });
    }

    /**
     * Connect to game server.
     */
    @FXML
    private void connectToServer() {
        String serverUrl = serverUrlField.getText().trim();
        if (serverUrl.isEmpty()) {
            serverUrl = "http://localhost:8080";
        }

        logger.info("Connecting to server: {}", serverUrl);
        statusLabel.setText("Connecting to " + serverUrl + "...");

        gameClient.connect(serverUrl, success -> {
            if (success) {
                statusLabel.setText("Connected to server");
                connectButton.setText("Disconnect");
                logger.info("Successfully connected to server");
            } else {
                statusLabel.setText("Connection failed");
                showAlert("Connection Error", "Failed to connect to server at " + serverUrl);
            }
        });
    }

    /**
     * Show alert dialog.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Cleanup resources.
     */
    public void cleanup() {
        logger.info("Cleaning up MainController...");

        if (uiUpdateTimer != null) {
            uiUpdateTimer.stop();
        }

        if (gameClient != null) {
            gameClient.disconnect();
        }
    }
}
