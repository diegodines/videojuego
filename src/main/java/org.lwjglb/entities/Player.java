package org.lwjglb.entities;

import org.joml.Vector3f;
import org.lwjglb.game.Input;
import org.lwjglb.game.Obstacle;
import org.lwjglb.terrains.Terrain;

import java.util.List;

/**
 * Jugador en primera persona: no se dibuja, solo tiene posición (los pies), orientación y la
 * física básica (gravedad, salto, choque con obstáculos y bordes del mapa).
 */
public class Player {

    public static final float GRAVITY = -50;

    private static final float RUN_SPEED = 9f;        // unidades por segundo
    private static final float LOOK_SPEED_X = 150f;   // grados por segundo con el stick a fondo
    private static final float LOOK_SPEED_Y = 100f;
    private static final float JUMP_POWER = 16f;
    public static final float RADIUS = 0.8f;          // radio del cuerpo para colisiones
    public static final float EYE_HEIGHT = 2.4f;

    private final Vector3f position;
    private float yaw;     // grados; 0 = mirando hacia -Z
    private float pitch;   // grados; positivo = mirando hacia abajo
    private float upwardsSpeed = 0;
    private boolean isInAir = false;
    private boolean moving = false;

    public Player(Vector3f position, float yaw) {
        this.position = position;
        this.yaw = yaw;
    }

    public void reset(Vector3f start, float yaw) {
        position.set(start);
        this.yaw = yaw;
        this.pitch = 0;
        upwardsSpeed = 0;
        isInAir = false;
    }

    public void update(float dt, Input input, Terrain terrain, List<Obstacle> obstacles,
                       float minXZ, float maxX, float minZ, float maxZ) {
        // Mirar
        // Mirar: stick derecho (velocidad) + mouse (desplazamiento directo)
        yaw += input.getLookX() * LOOK_SPEED_X * dt + input.getMouseYaw();
        pitch += input.getLookY() * LOOK_SPEED_Y * dt + input.getMousePitch();
        pitch = Math.max(-80, Math.min(80, pitch));

        // Moverse relativo a la vista (avanzar + paso lateral)
        double yawRad = Math.toRadians(yaw);
        float fx = (float) Math.sin(yawRad), fz = (float) -Math.cos(yawRad);   // adelante
        float rx = (float) Math.cos(yawRad), rz = (float) Math.sin(yawRad);    // derecha
        float mx = fx * input.getMoveY() + rx * input.getMoveX();
        float mz = fz * input.getMoveY() + rz * input.getMoveX();
        float len = (float) Math.sqrt(mx * mx + mz * mz);
        if (len > 1) { mx /= len; mz /= len; }
        moving = len > 0.05f;
        position.x += mx * RUN_SPEED * dt;
        position.z += mz * RUN_SPEED * dt;

        // Colisión 2: jugador contra obstáculos (se le empuja fuera del círculo del obstáculo)
        for (Obstacle o : obstacles) {
            o.pushOut(position, RADIUS);
        }
        // Bordes del mapa (la cerca)
        position.x = Math.max(minXZ, Math.min(maxX, position.x));
        position.z = Math.max(minZ, Math.min(maxZ, position.z));

        // Salto y gravedad
        if (input.jumpPressed() && !isInAir) {
            upwardsSpeed = JUMP_POWER;
            isInAir = true;
        }
        upwardsSpeed += GRAVITY * dt;
        position.y += upwardsSpeed * dt;
        float ground = terrain.getHeightOfTerrain(position.x, position.z);
        if (position.y < ground) {
            position.y = ground;
            upwardsSpeed = 0;
            isInAir = false;
        }
    }

    /** Dirección en la que mira la cámara (vector unitario). */
    public Vector3f getLookDirection() {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vector3f(
                (float) (Math.sin(y) * Math.cos(p)),
                (float) -Math.sin(p),
                (float) (-Math.cos(y) * Math.cos(p)));
    }

    public Vector3f getEyePosition() {
        return new Vector3f(position.x, position.y + EYE_HEIGHT, position.z);
    }

    /** Orienta la vista (solo lo usa la prueba automática de ScreenCapture). */
    public void setLook(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = Math.max(-80, Math.min(80, pitch));
    }

    public Vector3f getPosition() { return position; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public boolean isMoving() { return moving; }
    public boolean isInAir() { return isInAir; }
}
