package org.lwjglb.game;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjglb.terrains.Terrain;

import java.util.List;
import java.util.Random;

/**
 * Gallina del juego. Deambula por la granja, huye cuando el jugador se acerca y, al recibir un
 * disparo, cae y desaparece. La gallina dorada es más rápida y vale más puntos.
 */
public class Chicken {

    public enum State { WANDER, IDLE, FLEE, DYING, DEAD }

    public static final float HEIGHT = 1.6f;       // alto en unidades del mundo
    public static final float HIT_RADIUS = 0.95f;  // radio de la esfera de impacto
    private static final float FLEE_DISTANCE = 14f;
    private static final float DEATH_TIME = 0.9f;

    private final boolean golden;
    private final Vector3f position;
    private float heading;            // radianes; 0 = mirando hacia +Z
    private State state = State.WANDER;
    private float stateTimer;
    private float animTime;
    private float deathTimer;

    public Chicken(Vector3f position, boolean golden, Random rng) {
        this.position = position;
        this.golden = golden;
        this.heading = rng.nextFloat() * (float) (Math.PI * 2);
        this.stateTimer = 1 + rng.nextFloat() * 3;
        this.animTime = rng.nextFloat() * 5;   // desfase para que no se muevan todas igual
    }

    public void update(float dt, Terrain terrain, Vector3f playerPos, List<Obstacle> obstacles,
                       float min, float maxX, float minZ, float maxZ, Random rng) {
        animTime += dt;

        if (state == State.DYING) {
            deathTimer += dt;
            if (deathTimer >= DEATH_TIME) state = State.DEAD;
            return;
        }
        if (state == State.DEAD) return;

        float speedBoost = golden ? 1.5f : 1f;
        float dxp = position.x - playerPos.x, dzp = position.z - playerPos.z;
        float distPlayer = (float) Math.sqrt(dxp * dxp + dzp * dzp);

        if (distPlayer < FLEE_DISTANCE) {
            // Huye en dirección contraria al jugador, con un poco de zigzag
            if (state != State.FLEE) animTime = 0;
            state = State.FLEE;
            float away = (float) Math.atan2(dxp, dzp);
            heading = lerpAngle(heading, away + (float) Math.sin(animTime * 4) * 0.5f, dt * 6);
        } else {
            if (state == State.FLEE) { state = State.WANDER; stateTimer = 2; }
            stateTimer -= dt;
            if (stateTimer <= 0) {
                if (rng.nextFloat() < 0.3f) {
                    state = State.IDLE;
                    stateTimer = 1 + rng.nextFloat() * 2;
                } else {
                    state = State.WANDER;
                    heading += (rng.nextFloat() - 0.5f) * 2.5f;
                    stateTimer = 2 + rng.nextFloat() * 3;
                }
            }
        }

        float speed = state == State.FLEE ? 6.5f : state == State.WANDER ? 1.8f : 0f;
        speed *= speedBoost;
        position.x += (float) Math.sin(heading) * speed * dt;
        position.z += (float) Math.cos(heading) * speed * dt;

        // Colisión: la gallina rebota contra los obstáculos y la cerca
        for (Obstacle o : obstacles) {
            if (o.pushOut(position, 0.5f)) {
                heading += (float) Math.PI * 0.5f;
            }
        }
        if (position.x < min || position.x > maxX || position.z < minZ || position.z > maxZ) {
            position.x = Math.max(min, Math.min(maxX, position.x));
            position.z = Math.max(minZ, Math.min(maxZ, position.z));
            float cx = (min + maxX) * 0.5f, cz = (minZ + maxZ) * 0.5f;
            heading = (float) Math.atan2(cx - position.x, cz - position.z);   // vuelve al centro
        }
        position.y = terrain.getHeightOfTerrain(position.x, position.z);
    }

    /** Prueba si el segmento a->b (recorrido de una bala en este frame) atraviesa la gallina. */
    public boolean isHitBy(Vector3f a, Vector3f b) {
        if (!isAlive()) return false;
        Vector3f c = getCenter();
        Vector3f ab = new Vector3f(b).sub(a);
        float len2 = ab.lengthSquared();
        float t = len2 > 0 ? Math.max(0, Math.min(1, new Vector3f(c).sub(a).dot(ab) / len2)) : 0;
        Vector3f closest = new Vector3f(a).fma(t, ab);
        return closest.distanceSquared(c) <= HIT_RADIUS * HIT_RADIUS;
    }

    public void kill() {
        state = State.DYING;
        deathTimer = 0;
        animTime = 0;
    }

    /** Matriz de modelo: posición, orientación y, al morir, cae de lado y se encoge. */
    public Matrix4f modelMatrix(Matrix4f normalize) {
        Matrix4f m = new Matrix4f().translate(position).rotateY(heading);
        if (state == State.DYING) {
            float t = Math.min(1, deathTimer / DEATH_TIME);
            m.rotateZ(t * (float) Math.PI * 0.5f);
            m.scale(Math.max(0.05f, 1 - t * t));
        }
        return m.mul(normalize);
    }

    private static float lerpAngle(float a, float b, float t) {
        float diff = (float) Math.atan2(Math.sin(b - a), Math.cos(b - a));
        return a + diff * Math.min(1, t);
    }

    public Vector3f getCenter() {
        return new Vector3f(position.x, position.y + HEIGHT * 0.5f, position.z);
    }

    public boolean isAlive() { return state != State.DYING && state != State.DEAD; }
    public boolean isDead() { return state == State.DEAD; }
    public boolean isGolden() { return golden; }
    public State getState() { return state; }
    public float getAnimTime() { return animTime; }
    public Vector3f getPosition() { return position; }
}
