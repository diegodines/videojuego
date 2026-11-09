package org.lwjglb.game;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImageWrite;
import org.lwjglb.renderEngine.DisplayManager;

import java.nio.ByteBuffer;

/**
 * Herramienta de prueba (no afecta el juego normal). Si se define la variable de entorno
 * GRANJA_CAPTURA=ruta/base, el juego arranca solo, apunta a la gallina más cercana, dispara,
 * fuerza el fin de la partida y guarda capturas PNG; luego se cierra.
 * Sirve para revisar el juego sin tener que jugarlo.
 */
public class ScreenCapture {

    private static final boolean QUICK = System.getenv("GRANJA_RAPIDO") != null;
    private static final float[] SHOT_TIMES = QUICK ? new float[] {1.0f, 7.5f} : new float[] {1.5f, 4.6f, 6.3f, 9.5f};

    private static final float FPS_END = QUICK ? 7f : 4f;

    private final Game game;
    private final String basePath;
    private float elapsed = 0;
    private int nextShot = 0;
    private boolean endForced = false;
    private int frames = 0;
    private float hitShotTimer = -1;
    private boolean hitShotDone = false, fpsPrinted = false;
    private int fpsStartFrames = -1;

    private ScreenCapture(Game game, String basePath) {
        this.game = game;
        this.basePath = basePath;
    }

    public static ScreenCapture fromEnvironment(Game game) {
        String base = System.getenv("GRANJA_CAPTURA");
        if (base == null || base.isEmpty()) return null;
        System.out.println("Modo captura: " + base);
        return new ScreenCapture(game, base);
    }

    public void beforeRender(float dt) {
        elapsed += dt;
        if (game.getState() == GameState.START && nextShot > 0 && !endForced) {
            game.forceStart();
        }
        if (game.getState() == GameState.PLAYING && !QUICK) {
            boolean firing = elapsed > 3f && elapsed < 8.5f;
            if (firing) game.debugAimAtNearestChicken();
            game.getInput().setDemo(0f, firing);
            if (elapsed > 8.5f && !endForced) {
                endForced = true;
                game.debugEndRound();
            }
        }
    }

    /** Devuelve true cuando ya tomó todas las capturas. */
    public boolean afterRender() {
        frames++;
        // captura extra justo después del primer impacto, para ver las plumas
        if (!hitShotDone && hitShotTimer < 0 && game.getScore() > 0) hitShotTimer = 0.12f;
        if (!hitShotDone && hitShotTimer > 0) {
            hitShotTimer -= DisplayManager.getFrameTimeSeconds();
            if (hitShotTimer <= 0) { save(basePath + "_impacto.png"); hitShotDone = true; }
        }
        // FPS medidos entre 2 y 4 segundos (sin capturas de por medio)
        if (elapsed >= 2f && fpsStartFrames < 0) fpsStartFrames = frames;
        if (elapsed >= FPS_END && fpsStartFrames >= 0 && !fpsPrinted) {
            fpsPrinted = true;
            System.out.printf("FPS medidos: %.1f%n", (frames - fpsStartFrames) / (elapsed - 2f));
        }
        if (nextShot < SHOT_TIMES.length && elapsed >= SHOT_TIMES[nextShot]) {
            save(basePath + "_" + nextShot + ".png");
            System.out.println("Puntaje en la captura " + nextShot + ": " + game.getScore());
            nextShot++;
        }
        return nextShot >= SHOT_TIMES.length;
    }

    private void save(String path) {
        int w = DisplayManager.getWindowWidth(), h = DisplayManager.getWindowHeight();
        ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
        GL11.glReadBuffer(GL11.GL_BACK);
        GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        STBImageWrite.stbi_flip_vertically_on_write(true);
        STBImageWrite.stbi_write_png(path, w, h, 4, pixels, w * 4);
        System.out.println("Captura guardada: " + path);
    }
}
