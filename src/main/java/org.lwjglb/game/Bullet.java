package org.lwjglb.game;

import org.joml.Vector3f;
import org.lwjglb.entities.Entity;
import org.lwjglb.models.TexturedModel;

/** Proyectil disparado por la escopeta. Viaja en línea recta y se dibuja con el modelo bala.obj. */
public class Bullet {

    public static final float SPEED = 70f;
    private static final float LIFE = 1.2f;

    private final Vector3f position;
    private final Vector3f previous;
    private final Vector3f velocity;
    private final Entity entity;
    private float age = 0;

    public Bullet(TexturedModel model, Vector3f start, Vector3f direction) {
        this.position = new Vector3f(start);
        this.previous = new Vector3f(start);
        this.velocity = new Vector3f(direction).normalize().mul(SPEED);
        // bala.obj apunta hacia +Z: se gira en Y hacia la dirección del disparo
        float rotY = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        float rotX = (float) Math.toDegrees(-Math.asin(Math.max(-1, Math.min(1, direction.y))));
        this.entity = new Entity(model, position, rotX, rotY, 0, 0.35f);
    }

    public void update(float dt) {
        previous.set(position);
        position.fma(dt, velocity);
        age += dt;
    }

    public boolean isExpired() { return age >= LIFE; }
    public void expire() { age = LIFE; }
    public Vector3f getPosition() { return position; }
    public Vector3f getPrevious() { return previous; }
    public Entity getEntity() { return entity; }
}
