package com.chess.client.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Camera for 3D chess board viewing.
 * Supports orbit, pan, zoom controls.
 */
public class Camera {

    private Vector3f position;
    private Vector3f target;
    private Vector3f up;

    private float yaw;   // Horizontal rotation
    private float pitch; // Vertical rotation
    private float distance; // Distance from target

    private static final float MIN_DISTANCE = 3.0f;
    private static final float MAX_DISTANCE = 25.0f;
    private static final float MIN_PITCH = -89.0f;
    private static final float MAX_PITCH = 89.0f;

    public Camera() {
        this.position = new Vector3f(0, 8, 12);
        this.target = new Vector3f(0, 0, 0);
        this.up = new Vector3f(0, 1, 0);

        this.yaw = 0;
        this.pitch = 30;
        this.distance = 12;
    }

    /**
     * Update camera position based on orbit angles.
     */
    public void update() {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        float x = (float) (distance * Math.cos(pitchRad) * Math.sin(yawRad));
        float y = (float) (distance * Math.sin(pitchRad));
        float z = (float) (distance * Math.cos(pitchRad) * Math.cos(yawRad));

        position.set(target).add(x, y, z);
    }

    /**
     * Rotate camera (orbit).
     */
    public void rotate(float deltaYaw, float deltaPitch) {
        yaw += Math.toDegrees(deltaYaw);
        pitch -= Math.toDegrees(deltaPitch);

        // Clamp pitch to avoid gimbal lock
        pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch));

        update();
    }

    /**
     * Zoom camera (move closer/farther from target).
     */
    public void zoom(float delta) {
        distance -= delta;
        distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance));
        update();
    }

    /**
     * Pan camera (move target position).
     */
    public void pan(float deltaX, float deltaY) {
        Vector3f right = new Vector3f();
        new Vector3f(position).sub(target).normalize().cross(up, right);

        Vector3f panOffset = new Vector3f(right).mul(deltaX);
        panOffset.add(new Vector3f(up).mul(deltaY));

        target.add(panOffset);
        update();
    }

    /**
     * Look at a specific point.
     */
    public void lookAt(float x, float y, float z) {
        target.set(x, y, z);
        update();
    }

    /**
     * Get view matrix.
     */
    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position, target, up);
    }

    /**
     * Get projection matrix.
     */
    public Matrix4f getProjectionMatrix(int width, int height) {
        float aspectRatio = (float) width / height;
        float fov = (float) Math.toRadians(45.0f);
        float near = 0.1f;
        float far = 100.0f;

        return new Matrix4f().perspective(fov, aspectRatio, near, far);
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);

        // Recalculate orbit parameters
        Vector3f offset = new Vector3f(position).sub(target);
        this.distance = offset.length();

        float xz = (float) Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        this.pitch = (float) Math.toDegrees(Math.atan2(offset.y, xz));
        this.yaw = (float) Math.toDegrees(Math.atan2(offset.x, offset.z));
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Vector3f getTarget() {
        return new Vector3f(target);
    }
}
