package org.lwjglb.engineTester;

import org.lwjglb.fontRendering.TextMaster;
import org.lwjglb.game.Game;
import org.lwjglb.game.ScreenCapture;
import org.lwjglb.renderEngine.DisplayManager;
import org.lwjglb.renderEngine.Loader;
import org.lwjglb.renderEngine.MasterRenderer;

import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

/**
 * Punto de entrada de "La granja de Don Ramiro".
 * Toda la lógica del juego está en org.lwjglb.game.Game.
 */
public class MainGameLoop {

    public static void main(String[] args) throws Exception {
        DisplayManager.createDisplay();
        Loader loader = new Loader();
        TextMaster.init(loader);
        MasterRenderer renderer = new MasterRenderer(loader);

        Game game = new Game(loader, renderer);
        ScreenCapture capture = ScreenCapture.fromEnvironment(game);   // solo para pruebas automáticas

        while (!glfwWindowShouldClose(DisplayManager.window)) {
            float dt = DisplayManager.getFrameTimeSeconds();
            game.update(dt);
            if (capture != null) capture.beforeRender(dt);
            game.render();
            if (capture != null && capture.afterRender()) break;
            DisplayManager.updateDisplay();
        }

        game.cleanUp();
        TextMaster.cleanUp();
        renderer.cleanUp();
        loader.cleanUp();
        DisplayManager.closeDisplay();
    }
}
