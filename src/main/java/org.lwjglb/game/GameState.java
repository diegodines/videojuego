package org.lwjglb.game;

/** Estados de la partida. */
public enum GameState {
    START,    // pantalla de inicio
    PLAYING,  // corriendo el reloj
    WON,      // terminó el tiempo y se alcanzó la meta
    LOST      // terminó el tiempo sin alcanzar la meta
}
