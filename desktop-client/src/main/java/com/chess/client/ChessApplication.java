package com.chess.client;

import com.chess.client.renderer.OpenGLRenderer;
import com.chess.client.ui.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main JavaFX Application entry point.
 * Integrates JavaFX UI with LWJGL OpenGL 3D chess renderer.
 */
public class ChessApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(ChessApplication.class);
    private static final String TITLE = "Chess Game - 3D OpenGL";
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private OpenGLRenderer renderer;
    private MainController mainController;

    public static void main(String[] args) {
        // Set system properties for better rendering
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.verbose", "false");
        System.setProperty("javafx.animation.fullspeed", "true");

        logger.info("Starting Chess Application...");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Initializing JavaFX stage...");

            // Load FXML layout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            BorderPane root = loader.load();
            mainController = loader.getController();

            // Create scene
            Scene scene = new Scene(root, WIDTH, HEIGHT);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            // Configure stage
            primaryStage.setTitle(TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            // Initialize OpenGL renderer after stage is shown
            primaryStage.setOnShown(event -> {
                try {
                    initializeRenderer();
                    mainController.setRenderer(renderer);
                } catch (Exception e) {
                    logger.error("Failed to initialize OpenGL renderer", e);
                    showError("OpenGL Initialization Failed",
                             "Could not initialize 3D renderer. Please check your graphics drivers.");
                }
            });

            // Cleanup on close
            primaryStage.setOnCloseRequest(event -> cleanup());

            primaryStage.show();
            logger.info("Application started successfully");

        } catch (IOException e) {
            logger.error("Failed to load FXML", e);
            showError("Application Error", "Failed to load user interface.");
        }
    }

    /**
     * Initialize the LWJGL OpenGL renderer.
     * This creates an offscreen OpenGL context that renders to a texture,
     * which is then displayed in a JavaFX ImageView.
     */
    private void initializeRenderer() {
        logger.info("Initializing OpenGL renderer...");
        renderer = new OpenGLRenderer(800, 800); // Square aspect ratio for chess board
        renderer.initialize();

        // Start render loop
        Thread renderThread = new Thread(() -> {
            try {
                while (!renderer.shouldClose()) {
                    renderer.render();

                    // Cap at ~60 FPS
                    Thread.sleep(16);
                }
            } catch (InterruptedException e) {
                logger.warn("Render thread interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Render loop error", e);
            }
        }, "OpenGL-Render-Thread");

        renderThread.setDaemon(true);
        renderThread.start();

        logger.info("OpenGL renderer initialized successfully");
    }

    /**
     * Cleanup resources on application exit.
     */
    private void cleanup() {
        logger.info("Cleaning up resources...");

        if (renderer != null) {
            renderer.cleanup();
        }

        if (mainController != null) {
            mainController.cleanup();
        }

        Platform.exit();
        System.exit(0);
    }

    /**
     * Show error dialog.
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @Override
    public void stop() {
        cleanup();
    }
}
