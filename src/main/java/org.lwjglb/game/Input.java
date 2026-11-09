package org.lwjglb.game;

import org.lwjgl.glfw.GLFWGamepadState;
import org.lwjglb.renderEngine.DisplayManager;
import org.lwjglb.renderEngine.Keyboard;
import org.lwjglb.renderEngine.Mouse;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Entrada del jugador. Lee el primer gamepad conectado (API de gamepads de GLFW, mapeo tipo Xbox)
 * y, además, mouse y teclado. Ambos funcionan a la vez: se usa el que el jugador tenga.
 *
 * Gamepad:        stick izq = moverse | stick der = mirar | RT = disparar | A = saltar
 *                 X = recargar | START = comenzar / reintentar
 * Mouse/teclado:  mouse = mirar | clic izq o ESPACIO = disparar | WASD = moverse
 *                 SHIFT = saltar | R = recargar | ENTER = comenzar / reintentar
 *                 ESC = soltar el cursor (un clic lo vuelve a capturar)
 */
public class Input {

    private static final float DEAD_ZONE = 0.18f;
    private static final float MOUSE_SENSITIVITY = 0.12f;   // grados por píxel

    // Mouse: el cursor se oculta y queda "capturado" mientras se juega
    private final double[] cursorX = new double[1], cursorY = new double[1];
    private double lastCursorX, lastCursorY;
    private boolean wantCapture = false, captured = false, releasedByEsc = false;
    private float mouseYaw, mousePitch;

    private final GLFWGamepadState state = GLFWGamepadState.create();
    private boolean gamepadConnected = false;

    // Valores del frame actual
    private float moveX, moveY, lookX, lookY;
    private boolean fire, jump, reload, start;
    // Valores del frame anterior, para detectar "recién presionado"
    private boolean prevFire, prevJump, prevReload, prevStart;
    private boolean fireDown, jumpDown, reloadDown, startDown;

    public void update() {
        prevFire = fireDown;
        prevJump = jumpDown;
        prevReload = reloadDown;
        prevStart = startDown;

        moveX = moveY = lookX = lookY = 0;
        fireDown = jumpDown = reloadDown = startDown = false;

        gamepadConnected = false;
        for (int jid = GLFW_JOYSTICK_1; jid <= GLFW_JOYSTICK_LAST; jid++) {
            if (glfwJoystickIsGamepad(jid) && glfwGetGamepadState(jid, state)) {
                gamepadConnected = true;
                moveX = deadZone(state.axes(GLFW_GAMEPAD_AXIS_LEFT_X));
                moveY = -deadZone(state.axes(GLFW_GAMEPAD_AXIS_LEFT_Y));   // arriba = avanzar
                lookX = deadZone(state.axes(GLFW_GAMEPAD_AXIS_RIGHT_X));
                lookY = deadZone(state.axes(GLFW_GAMEPAD_AXIS_RIGHT_Y));
                fireDown = state.axes(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER) > 0.3f
                        || state.buttons(GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER) == GLFW_PRESS;
                jumpDown = state.buttons(GLFW_GAMEPAD_BUTTON_A) == GLFW_PRESS;
                reloadDown = state.buttons(GLFW_GAMEPAD_BUTTON_X) == GLFW_PRESS;
                startDown = state.buttons(GLFW_GAMEPAD_BUTTON_START) == GLFW_PRESS;
                break;
            }
        }

        // Teclado (se suma al gamepad)
        if (Keyboard.isKeyDown(GLFW_KEY_W)) moveY += 1;
        if (Keyboard.isKeyDown(GLFW_KEY_S)) moveY -= 1;
        if (Keyboard.isKeyDown(GLFW_KEY_D)) moveX += 1;
        if (Keyboard.isKeyDown(GLFW_KEY_A)) moveX -= 1;
        updateMouse();
        fireDown |= Keyboard.isKeyDown(GLFW_KEY_SPACE) || (captured && Mouse.isLeftButtonPressed());
        jumpDown |= Keyboard.isKeyDown(GLFW_KEY_LEFT_SHIFT);
        reloadDown |= Keyboard.isKeyDown(GLFW_KEY_R);
        startDown |= Keyboard.isKeyDown(GLFW_KEY_ENTER);

        // Entrada simulada (solo la usa ScreenCapture en las pruebas automáticas)
        lookX += demoLookX;
        fireDown |= demoFire;

        moveX = clamp(moveX);
        moveY = clamp(moveY);
        lookX = clamp(lookX);
        lookY = clamp(lookY);
    }

    /** Mira con el mouse: mide cuánto se movió el cursor desde el frame anterior. */
    private void updateMouse() {
        long window = DisplayManager.window;
        mouseYaw = mousePitch = 0;

        if (Keyboard.isKeyDown(GLFW_KEY_ESCAPE)) releasedByEsc = true;
        if (releasedByEsc && Mouse.isLeftButtonPressed()) releasedByEsc = false;   // clic: volver a capturar
        boolean capture = wantCapture && !releasedByEsc && glfwGetWindowAttrib(window, GLFW_FOCUSED) == GLFW_TRUE;

        if (capture != captured) {
            glfwSetInputMode(window, GLFW_CURSOR, capture ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
            if (capture && glfwRawMouseMotionSupported()) glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
            captured = capture;
            glfwGetCursorPos(window, cursorX, cursorY);
            lastCursorX = cursorX[0];
            lastCursorY = cursorY[0];
            return;   // sin salto de cámara al capturar
        }
        if (!captured) return;

        glfwGetCursorPos(window, cursorX, cursorY);
        mouseYaw = (float) (cursorX[0] - lastCursorX) * MOUSE_SENSITIVITY;
        mousePitch = (float) (cursorY[0] - lastCursorY) * MOUSE_SENSITIVITY;   // mouse hacia abajo = mirar abajo
        lastCursorX = cursorX[0];
        lastCursorY = cursorY[0];
    }

    /** El juego pide capturar el mouse solo mientras se está jugando. */
    public void setMouseCapture(boolean capture) {
        wantCapture = capture;
    }

    /** Grados a girar este frame por el movimiento del mouse. */
    public float getMouseYaw() { return mouseYaw; }
    public float getMousePitch() { return mousePitch; }

    private static float deadZone(float v) {
        if (Math.abs(v) < DEAD_ZONE) return 0;
        // re-escala para que el movimiento empiece suave justo después de la zona muerta
        return Math.signum(v) * (Math.abs(v) - DEAD_ZONE) / (1 - DEAD_ZONE);
    }

    private static float clamp(float v) {
        return Math.max(-1, Math.min(1, v));
    }

    public float getMoveX() { return moveX; }
    public float getMoveY() { return moveY; }
    public float getLookX() { return lookX; }
    public float getLookY() { return lookY; }

    /** Gatillo mantenido (la escopeta controla su propia cadencia). */
    public boolean isFireHeld() { return fireDown; }
    public boolean jumpPressed() { return jumpDown && !prevJump; }
    public boolean reloadPressed() { return reloadDown && !prevReload; }
    public boolean startPressed() { return startDown && !prevStart; }
    public boolean firePressed() { return fireDown && !prevFire; }
    public boolean isGamepadConnected() { return gamepadConnected; }

    private float demoLookX = 0;
    private boolean demoFire = false;

    /** Para pruebas automáticas: simula girar la vista y apretar el gatillo. */
    public void setDemo(float lookX, boolean fire) {
        demoLookX = lookX;
        demoFire = fire;
    }
}
