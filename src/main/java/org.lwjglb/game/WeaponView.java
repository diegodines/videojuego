package org.lwjglb.game;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjglb.entities.Camera;
import org.lwjglb.glb.GlbModel;
import org.lwjglb.util.Maths;

/**
 * Escopeta en primera persona (del FBX de brazos + escopeta solo se dibuja la escopeta). Se dibujan al final, "pegados" a la cámara, después de
 * limpiar el buffer de profundidad para que el arma nunca atraviese paredes ni gallinas.
 * Elige la animación según lo que hace el jugador: disparar, recargar, caminar o quieto.
 */
public class WeaponView {

    // Ubicación de la escopeta respecto de la cámara (espacio de vista: X derecha, Y arriba, -Z adelante)
    private static final float OFFSET_X = 0.15f;
    private static final float OFFSET_Y = 0.12f;
    private static final float OFFSET_Z = -0.05f;
    private static final float SIZE = -0.015f;       // negativo = escala directa (el FBX viene en centímetros)
    private static final float ROT_X = -90f;         // el FBX es Z-arriba: se pasa a Y-arriba
    private static final float ROT_Y = 180f;         // giro sobre el eje del cañón: sin esto queda de cabeza

    private final GlbModel model;
    private final int idleAnim;
    private final float fireDuration, reloadDuration;
    private final Matrix4f normalize;

    private float time = 0;
    private float actionTime = 0;
    private static final int NONE = 0, FIRE = 1, RELOAD = 2;
    private int action = NONE;   // acción en curso

    public WeaponView(GlbModel model) {
        this.model = model;
        this.idleAnim = Math.max(0, model.findAnimation("idle"));
        // Las animaciones de disparo y recarga del FBX mueven la escopeta junto a los brazos (que no
        // se dibujan) y la levantan fuera de cuadro; por eso se reemplazan por un retroceso simple.
        this.fireDuration = 0.55f;
        this.reloadDuration = 1.4f;

        // El FBX trae la escopeta 100 veces más grande que los brazos: se corrige esa malla
        // y solo se muestra la escopeta (los brazos no se dibujan)
        if (model.fixOversizedSkinnedMeshes(idleAnim) > 0) model.showOnlyCorrectedMeshes();

        // Se mide en la pose de la animación "idle" (la pose de reposo del FBX no sirve de referencia)
        org.joml.Vector3f[] b = model.getAnimationBounds(idleAnim);
        float sx = b[1].x - b[0].x, sy = b[1].y - b[0].y, sz = b[1].z - b[0].z;
        float[] t = tuning();
        // Tamaño positivo: se centra el modelo y su lado mayor mide 'tamaño'.
        // Tamaño negativo: se usa como escala directa y sin centrar (el origen del FBX queda en la cámara).
        boolean center = t[6] > 0;
        float s = center ? t[6] / Math.max(sx, Math.max(sy, sz)) : -t[6];
        normalize = new Matrix4f()
                .translate(t[3], t[4], t[5])
                .rotateX((float) Math.toRadians(t[0]))
                .rotateY((float) Math.toRadians(t[1]))
                .rotateZ((float) Math.toRadians(t[2]))
                .scale(s);
        if (center) {
            normalize.translate(-(b[0].x + b[1].x) * 0.5f, -(b[0].y + b[1].y) * 0.5f, -(b[0].z + b[1].z) * 0.5f);
        }

        String[] names = model.getAnimationNames();
        System.out.println("Animaciones de la escopeta: " + String.join(", ", names));
        System.out.printf("Escopeta (pose idle): min %s max %s%n", b[0], b[1]);
        if (System.getenv("GRANJA_DIAG") != null) model.printMeshBounds(idleAnim);
    }

    /** Giro (X,Y,Z en grados), posición (X,Y,Z) y tamaño de los brazos. Se pueden ajustar con
     *  la variable de entorno GRANJA_ARMAS="rx ry rz ox oy oz tamaño" sin recompilar. */
    private static float[] tuning() {
        float[] t = {ROT_X, ROT_Y, 0, OFFSET_X, OFFSET_Y, OFFSET_Z, SIZE};
        String env = System.getenv("GRANJA_ARMAS");
        if (env != null) {
            String[] p = env.trim().split("\\s+");
            for (int i = 0; i < Math.min(p.length, t.length); i++) t[i] = Float.parseFloat(p[i]);
        }
        return t;
    }

    public void update(float dt) {
        time += dt;
        if (action != NONE) {
            actionTime += dt;
            float duration = action == FIRE ? fireDuration : reloadDuration;
            if (actionTime >= duration) action = NONE;
        }
    }

    public void playFire() {
        action = FIRE;
        actionTime = 0;
    }

    public void playReload() {
        action = RELOAD;
        actionTime = 0;
    }

    public float getFireDuration() { return fireDuration; }
    public float getReloadDuration() { return reloadDuration; }
    public boolean isBusy() { return action != NONE; }

    public void render(Matrix4f projection, Camera camera, boolean walking) {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        Matrix4f view = Maths.createViewMatrix(camera);
        Matrix4f vp = new Matrix4f(projection).mul(view);
        // Movimiento simple del arma (en espacio de la cámara): balanceo al caminar,
        // golpe hacia atrás y arriba al disparar, y bajar/subir al recargar.
        Matrix4f motion = new Matrix4f();
        if (walking) {
            motion.translate((float) Math.sin(time * 9) * 0.012f, (float) Math.abs(Math.cos(time * 9)) * 0.015f, 0);
        }
        if (action == FIRE) {
            float t = actionTime / fireDuration;
            float kick = t < 0.15f ? t / 0.15f : (1 - t) / 0.85f;   // sube rápido y vuelve lento
            motion.translate(0, kick * 0.03f, kick * 0.12f).rotateX((float) Math.toRadians(kick * 9));
        } else if (action == RELOAD) {
            float k = (float) Math.sin(Math.PI * actionTime / reloadDuration);
            motion.translate(0, -k * 0.35f, 0).rotateZ((float) Math.toRadians(k * 25));
        }
        Matrix4f modelMatrix = new Matrix4f(view).invert().mul(motion).mul(normalize);

        // La animación de reposo del FBX se mantiene siempre (mueve levemente la palanca y el cañón)
        model.setAnimationState(idleAnim, time);
        model.begin();
        model.render(vp, modelMatrix);
        model.end();
    }
}
