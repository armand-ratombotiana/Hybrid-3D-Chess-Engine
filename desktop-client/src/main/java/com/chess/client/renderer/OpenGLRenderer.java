package com.chess.client.renderer;

import com.chess.client.model.Board;
import com.chess.client.model.Piece;
import com.chess.client.model.Square;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * LWJGL OpenGL renderer for 3D chess board.
 * Uses offscreen rendering with FBO, texture copied to JavaFX ImageView.
 */
public class OpenGLRenderer {

    private static final Logger logger = LoggerFactory.getLogger(OpenGLRenderer.class);

    private final int width;
    private final int height;

    // GLFW window (hidden, offscreen)
    private long window;

    // OpenGL objects
    private int shaderProgram;
    private int vao;
    private int vbo;
    private int fbo;
    private int textureId;

    // Camera
    private final Camera camera;

    // Chess board model
    private Board board;

    // Render state
    private volatile boolean shouldClose = false;
    private WritableImage frameImage;
    private ByteBuffer pixelBuffer;

    // Mouse input
    private double lastMouseX;
    private double lastMouseY;
    private boolean mousePressed = false;

    public OpenGLRenderer(int width, int height) {
        this.width = width;
        this.height = height;
        this.camera = new Camera();
        this.camera.setPosition(0, 8, 12);
        this.camera.lookAt(0, 0, 0);

        this.pixelBuffer = BufferUtils.createByteBuffer(width * height * 4);
        this.frameImage = new WritableImage(width, height);
    }

    /**
     * Initialize OpenGL context and resources.
     */
    public void initialize() {
        logger.info("Initializing GLFW and OpenGL...");

        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        // Create window
        window = glfwCreateWindow(width, height, "Chess Renderer", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        // Setup input callbacks
        setupInputCallbacks();

        // Make OpenGL context current
        glfwMakeContextCurrent(window);
        glfwSwapInterval(0); // No VSync for offscreen rendering

        // Initialize OpenGL capabilities
        GL.createCapabilities();

        logger.info("OpenGL Version: {}", glGetString(GL_VERSION));
        logger.info("GLSL Version: {}", glGetString(GL_SHADING_LANGUAGE_VERSION));

        // Initialize OpenGL state
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);

        // Create shader program
        createShaderProgram();

        // Create chess board geometry
        createBoardGeometry();

        // Create framebuffer for offscreen rendering
        createFramebuffer();

        // Initialize board model
        board = new Board();
        board.setupStartingPosition();

        logger.info("OpenGL initialization complete");
    }

    /**
     * Main render loop iteration.
     */
    public void render() {
        glfwPollEvents();

        // Update camera
        camera.update();

        // Render to framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Use shader program
        glUseProgram(shaderProgram);

        // Set uniforms
        Matrix4f projection = camera.getProjectionMatrix(width, height);
        Matrix4f view = camera.getViewMatrix();
        Matrix4f model = new Matrix4f().identity();

        setUniformMatrix4f("uProjection", projection);
        setUniformMatrix4f("uView", view);
        setUniformMatrix4f("uModel", model);

        // Render chess board
        renderBoard();

        // Render pieces
        renderPieces();

        // Read pixels from framebuffer
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);

        // Copy to JavaFX image (on JavaFX thread)
        Platform.runLater(this::updateJavaFXImage);

        // Unbind framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Render the chess board (8x8 squares).
     */
    private void renderBoard() {
        glBindVertexArray(vao);

        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                // Determine square color
                boolean isLight = (file + rank) % 2 == 0;
                Vector3f color = isLight
                    ? new Vector3f(0.9f, 0.9f, 0.85f)  // Light squares
                    : new Vector3f(0.5f, 0.35f, 0.25f); // Dark squares

                setUniform3f("uColor", color);

                // Position square
                Matrix4f model = new Matrix4f()
                    .translate(file - 3.5f, 0, rank - 3.5f)
                    .scale(1.0f, 0.1f, 1.0f);

                setUniformMatrix4f("uModel", model);

                // Draw square
                glDrawArrays(GL_TRIANGLES, 0, 36);
            }
        }

        glBindVertexArray(0);
    }

    /**
     * Render chess pieces.
     */
    private void renderPieces() {
        glBindVertexArray(vao);

        for (Square square : board.getAllSquares()) {
            Piece piece = square.getPiece();
            if (piece == null) continue;

            // Determine piece color
            Vector3f color = piece.isWhite()
                ? new Vector3f(0.95f, 0.95f, 0.95f) // White pieces
                : new Vector3f(0.15f, 0.15f, 0.15f); // Black pieces

            setUniform3f("uColor", color);

            // Position piece
            int file = square.getFile();
            int rank = square.getRank();

            Matrix4f model = new Matrix4f()
                .translate(file - 3.5f, 0.5f, rank - 3.5f)
                .scale(0.3f, 0.8f, 0.3f);

            setUniformMatrix4f("uModel", model);

            // Draw piece (simple cylinder for now)
            glDrawArrays(GL_TRIANGLES, 0, 36);
        }

        glBindVertexArray(0);
    }

    /**
     * Copy pixel buffer to JavaFX WritableImage.
     */
    private void updateJavaFXImage() {
        pixelBuffer.rewind();

        // Flip vertically (OpenGL origin is bottom-left, JavaFX is top-left)
        byte[] pixels = new byte[width * height * 4];
        for (int y = 0; y < height; y++) {
            int flippedY = height - 1 - y;
            pixelBuffer.position(flippedY * width * 4);
            pixelBuffer.get(pixels, y * width * 4, width * 4);
        }

        pixelBuffer.rewind();

        frameImage.getPixelWriter().setPixels(
            0, 0, width, height,
            PixelFormat.getByteBgraInstance(),
            pixels, 0, width * 4
        );
    }

    /**
     * Create shader program.
     */
    private void createShaderProgram() {
        // Vertex shader
        String vertexShaderSource = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aNormal;

            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;

            out vec3 FragPos;
            out vec3 Normal;

            void main() {
                FragPos = vec3(uModel * vec4(aPos, 1.0));
                Normal = mat3(transpose(inverse(uModel))) * aNormal;
                gl_Position = uProjection * uView * vec4(FragPos, 1.0);
            }
            """;

        // Fragment shader
        String fragmentShaderSource = """
            #version 330 core
            in vec3 FragPos;
            in vec3 Normal;

            uniform vec3 uColor;

            out vec4 FragColor;

            void main() {
                // Simple Blinn-Phong lighting
                vec3 lightPos = vec3(5.0, 10.0, 5.0);
                vec3 lightColor = vec3(1.0, 1.0, 1.0);

                // Ambient
                float ambientStrength = 0.3;
                vec3 ambient = ambientStrength * lightColor;

                // Diffuse
                vec3 norm = normalize(Normal);
                vec3 lightDir = normalize(lightPos - FragPos);
                float diff = max(dot(norm, lightDir), 0.0);
                vec3 diffuse = diff * lightColor;

                // Specular
                float specularStrength = 0.5;
                vec3 viewDir = normalize(-FragPos);
                vec3 halfwayDir = normalize(lightDir + viewDir);
                float spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);
                vec3 specular = specularStrength * spec * lightColor;

                vec3 result = (ambient + diffuse + specular) * uColor;
                FragColor = vec4(result, 1.0);
            }
            """;

        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentShaderSource);

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);

        // Check for linking errors
        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            String infoLog = glGetProgramInfoLog(shaderProgram);
            throw new RuntimeException("Shader program linking failed: " + infoLog);
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        logger.info("Shader program created successfully");
    }

    private int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String infoLog = glGetShaderInfoLog(shader);
            throw new RuntimeException("Shader compilation failed: " + infoLog);
        }

        return shader;
    }

    /**
     * Create cube geometry for board squares and pieces.
     */
    private void createBoardGeometry() {
        float[] vertices = {
            // Positions          // Normals
            // Front face
            -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
             0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
             0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
             0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
            -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,

            // Back face
            -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
             0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
             0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
             0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
            -0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
            -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,

            // Top face
            -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,
             0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,
             0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
             0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
            -0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
            -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,

            // Bottom face
            -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,
             0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,
             0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
             0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
            -0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
            -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,

            // Right face
             0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
             0.5f,  0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
             0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,
             0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,
             0.5f, -0.5f,  0.5f,  1.0f,  0.0f,  0.0f,
             0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,

            // Left face
            -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f,  0.5f, -0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f, -0.5f,  0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Normal attribute
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);

        logger.info("Board geometry created");
    }

    /**
     * Create framebuffer for offscreen rendering.
     */
    private void createFramebuffer() {
        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        // Create texture for color attachment
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);

        // Create renderbuffer for depth attachment
        int rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rbo);

        // Check framebuffer completeness
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer is not complete");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        logger.info("Framebuffer created");
    }

    /**
     * Setup GLFW input callbacks for mouse interaction.
     */
    private void setupInputCallbacks() {
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                mousePressed = action == GLFW_PRESS;

                if (action == GLFW_PRESS) {
                    // Perform ray picking to detect clicked square
                    double[] xpos = new double[1];
                    double[] ypos = new double[1];
                    glfwGetCursorPos(window, xpos, ypos);

                    performRayPicking(xpos[0], ypos[0]);
                }
            }
        });

        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (mousePressed) {
                double dx = xpos - lastMouseX;
                double dy = ypos - lastMouseY;

                camera.rotate((float) dx * 0.005f, (float) dy * 0.005f);
            }

            lastMouseX = xpos;
            lastMouseY = ypos;
        });

        glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
            camera.zoom((float) yoffset * 0.5f);
        });
    }

    /**
     * Perform ray-picking to determine which board square was clicked.
     */
    private void performRayPicking(double mouseX, double mouseY) {
        // Convert screen coordinates to normalized device coordinates
        float x = (float) (2.0 * mouseX / width - 1.0);
        float y = (float) (1.0 - 2.0 * mouseY / height);

        // Create ray in world space (simplified)
        // Full implementation would use inverse projection/view matrices

        logger.info("Ray picking at screen coords: ({}, {}) -> NDC: ({}, {})", mouseX, mouseY, x, y);

        // TODO: Implement full ray-board intersection
        // For now, log the click for demonstration
        System.out.println("CLICK DETECTED at screen position: " + mouseX + ", " + mouseY);
    }

    /**
     * Set shader uniform matrix.
     */
    private void setUniformMatrix4f(String name, Matrix4f matrix) {
        int location = glGetUniformLocation(shaderProgram, name);
        float[] buffer = new float[16];
        matrix.get(buffer);
        glUniformMatrix4fv(location, false, buffer);
    }

    /**
     * Set shader uniform vec3.
     */
    private void setUniform3f(String name, Vector3f vec) {
        int location = glGetUniformLocation(shaderProgram, name);
        glUniform3f(location, vec.x, vec.y, vec.z);
    }

    public WritableImage getFrameImage() {
        return frameImage;
    }

    public Camera getCamera() {
        return camera;
    }

    public Board getBoard() {
        return board;
    }

    public boolean shouldClose() {
        return shouldClose || glfwWindowShouldClose(window);
    }

    public void cleanup() {
        logger.info("Cleaning up OpenGL resources...");

        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteFramebuffers(fbo);
        glDeleteTextures(textureId);
        glDeleteProgram(shaderProgram);

        glfwDestroyWindow(window);
        glfwTerminate();

        logger.info("OpenGL cleanup complete");
    }
}
