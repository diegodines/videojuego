package org.lwjglb.entities;

import org.joml.Vector3f;

/**
 * Cámara en primera persona: se ubica en los ojos del jugador y usa su misma orientación.
 * Maths.createViewMatrix aplica rotación en X (pitch), luego en Y (yaw) y luego la traslación.
 */
public class Camera {

    private final Vector3f position = new Vector3f(0, 8f, 0);
    private float pitch = 0;
    private float yaw = 0;
    private float roll;

    private final Player player;

    public Camera(Player player) {
        this.player = player;
    }

    public void move() {
        position.set(player.getEyePosition());
        pitch = player.getPitch();
        yaw = player.getYaw();
    }

    public void invertPitch() {
        this.pitch = -pitch;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPositionY(float posY) {
        position.y = posY;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getRoll() {
        return roll;
    }
}
