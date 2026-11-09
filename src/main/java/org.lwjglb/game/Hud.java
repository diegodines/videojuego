package org.lwjglb.game;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjglb.fontMeshCreator.FontType;
import org.lwjglb.fontMeshCreator.GUIText;
import org.lwjglb.guis.GuiRenderer;
import org.lwjglb.guis.GuiTexture;
import org.lwjglb.renderEngine.Loader;

import java.util.ArrayList;
import java.util.List;

/**
 * Interfaz del jugador: contador de gallinas, reloj, cartuchos, mira y las pantallas de
 * inicio, victoria y derrota. Las imágenes se ubican en coordenadas de OpenGL (-1..1) y los
 * textos en coordenadas de pantalla (0..1, origen arriba a la izquierda).
 */
public class Hud {

    private static final float ASPECT = 9f / 16f;   // para que los íconos cuadrados no se deformen

    private final GuiRenderer guiRenderer;
    private final FontType font;

    private final List<GuiTexture> playingGuis = new ArrayList<>();
    private final List<GuiTexture> winGuis = new ArrayList<>();
    private final List<GuiTexture> loseGuis = new ArrayList<>();
    private final List<GuiTexture> startGuis = new ArrayList<>();

    private final TextLine counter, timer, ammo, title, subtitle, hint, story;

    public Hud(Loader loader, FontType font) {
        this.guiRenderer = new GuiRenderer(loader);
        this.font = font;

        int panel = loader.loadTexture("hud_panel");
        GuiTexture counterPanel = new GuiTexture(panel, new Vector2f(-0.80f, 0.86f), new Vector2f(0.17f, 0.10f));
        GuiTexture chickenIcon = new GuiTexture(loader.loadTexture("hud_gallina"),
                new Vector2f(-0.92f, 0.86f), new Vector2f(0.075f * ASPECT, 0.075f));
        GuiTexture timerPanel = new GuiTexture(panel, new Vector2f(0.80f, 0.86f), new Vector2f(0.17f, 0.10f));
        GuiTexture clockIcon = new GuiTexture(loader.loadTexture("hud_reloj"),
                new Vector2f(0.68f, 0.86f), new Vector2f(0.06f * ASPECT, 0.06f));
        GuiTexture crosshair = new GuiTexture(loader.loadTexture("mira"),
                new Vector2f(0, 0), new Vector2f(0.045f * ASPECT, 0.045f));
        GuiTexture ammoPanel = new GuiTexture(panel, new Vector2f(0.80f, -0.88f), new Vector2f(0.17f, 0.08f));
        playingGuis.add(counterPanel);
        playingGuis.add(chickenIcon);
        playingGuis.add(timerPanel);
        playingGuis.add(clockIcon);
        playingGuis.add(crosshair);
        playingGuis.add(ammoPanel);

        Vector2f msgPos = new Vector2f(0, 0.08f), msgScale = new Vector2f(0.42f, 0.30f);
        winGuis.add(new GuiTexture(loader.loadTexture("panel_victoria"), msgPos, msgScale));
        loseGuis.add(new GuiTexture(loader.loadTexture("panel_derrota"), msgPos, msgScale));
        startGuis.add(new GuiTexture(panel, new Vector2f(0, 0.05f), new Vector2f(0.55f, 0.45f)));

        counter = new TextLine(new Vector2f(0.08f, 0.045f), 2.2f, 0.12f, new Vector3f(1, 1, 1));
        timer = new TextLine(new Vector2f(0.85f, 0.045f), 2.2f, 0.12f, new Vector3f(1, 1, 1));
        ammo = new TextLine(new Vector2f(0.83f, 0.915f), 1.5f, 0.14f, new Vector3f(1, 0.9f, 0.6f));
        title = new TextLine(new Vector2f(0, 0.33f), 3.2f, 1f, new Vector3f(1, 1, 1));
        subtitle = new TextLine(new Vector2f(0, 0.44f), 1.8f, 1f, new Vector3f(1, 1, 1));
        hint = new TextLine(new Vector2f(0, 0.53f), 1.3f, 1f, new Vector3f(1, 1, 0.8f));
        story = new TextLine(new Vector2f(0, 0.62f), 1.1f, 1f, new Vector3f(0.95f, 0.95f, 0.95f));
    }

    public void showStart(int goal, int seconds) {
        hidePlaying();
        title.set("LA GRANJA DE DON RAMIRO");
        subtitle.set("Abate " + goal + " gallinas en " + seconds / 60 + " minutos");
        hint.set("Presiona START (o ENTER) para comenzar");
        story.set("La gallina dorada vale 3.   Gamepad: sticks, RT dispara, X recarga, A salta.   Teclado: WASD, mouse, clic dispara, R recarga");
    }

    public void showPlaying(int score, int goal, float timeLeft, int shells, int maxShells, boolean reloading) {
        title.hide(); subtitle.hide(); hint.hide(); story.hide();
        counter.set(score + " / " + goal);
        int secs = (int) Math.ceil(Math.max(0, timeLeft));
        timer.set(String.format("%d:%02d", secs / 60, secs % 60));
        timer.setColour(secs <= 15 ? new Vector3f(1, 0.35f, 0.3f) : new Vector3f(1, 1, 1));
        ammo.set(reloading ? "Recargando..." : "Cartuchos " + shells + "/" + maxShells);
    }

    public void showResult(boolean won, int score, int goal) {
        hidePlaying();
        title.set(won ? "VICTORIA!" : "DERROTA");
        subtitle.set(won ? "Abatiste " + score + " gallinas. La granja esta a salvo"
                         : "Solo abatiste " + score + " de " + goal + " gallinas");
        hint.set("Presiona START (o ENTER) para jugar otra vez");
        story.hide();
    }

    private void hidePlaying() {
        counter.hide(); timer.hide(); ammo.hide();
    }

    public void render(GameState state) {
        switch (state) {
            case START: guiRenderer.render(startGuis); break;
            case PLAYING: guiRenderer.render(playingGuis); break;
            case WON: guiRenderer.render(winGuis); break;
            case LOST: guiRenderer.render(loseGuis); break;
        }
    }

    public void cleanUp() {
        guiRenderer.cleanUp();
    }

    /** Texto que solo se vuelve a generar cuando cambia su contenido. */
    private class TextLine {
        private final Vector2f position;
        private final float size, maxLength;
        private Vector3f colour;
        private GUIText text;
        private String value;

        TextLine(Vector2f position, float size, float maxLength, Vector3f colour) {
            this.position = position;
            this.size = size;
            this.maxLength = maxLength;
            this.colour = colour;
        }

        void set(String newValue) {
            if (newValue.equals(value) && text != null) return;
            hide();
            value = newValue;
            text = new GUIText(newValue, size, font, position, maxLength, true);
            text.setColour(colour.x, colour.y, colour.z);
        }

        void setColour(Vector3f c) {
            if (c.equals(colour)) return;
            colour = c;
            if (text != null) text.setColour(c.x, c.y, c.z);
        }

        void hide() {
            if (text != null) {
                text.remove();
                text = null;
            }
            value = null;
        }
    }
}
