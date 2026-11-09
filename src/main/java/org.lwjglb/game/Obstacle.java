package org.lwjglb.game;

import org.joml.Vector3f;

/** Obstáculo del escenario representado como un cilindro vertical (círculo en X/Z + altura). */
public class Obstacle {

    public final float x, z, radius, height;

    public Obstacle(float x, float z, float radius, float height) {
        this.x = x;
        this.z = z;
        this.radius = radius;
        this.height = height;
    }

    /** Si el círculo (pos, r) se mete en el obstáculo, lo empuja hacia afuera. Devuelve true si chocó. */
    public boolean pushOut(Vector3f pos, float r) {
        float dx = pos.x - x, dz = pos.z - z;
        float min = radius + r;
        float d2 = dx * dx + dz * dz;
        if (d2 >= min * min) return false;
        float d = (float) Math.sqrt(d2);
        if (d < 0.0001f) { dx = 1; dz = 0; d = 1; }
        pos.x = x + dx / d * min;
        pos.z = z + dz / d * min;
        return true;
    }

    /** ¿El punto está dentro del cilindro? (para que las balas se detengan al pegar en algo) */
    public boolean contains(Vector3f p, float groundY) {
        float dx = p.x - x, dz = p.z - z;
        return dx * dx + dz * dz < radius * radius && p.y < groundY + height;
    }
}
