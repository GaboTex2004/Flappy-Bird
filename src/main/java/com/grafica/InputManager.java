package com.grafica;

import org.lwjgl.glfw.GLFW;

public class InputManager {
    private final long window;
    private boolean prevSpace;
    private boolean prevW;
    private boolean prevR;

    public InputManager(long window) {
        this.window = window;
    }

    public boolean isEscapePressed() {
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
    }

    public boolean isSpacePressed() {
        boolean spaceAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        boolean result = spaceAhora && !prevSpace;
        prevSpace = spaceAhora;
        return result;
    }
    // Nuevo método para el Jugador 2
    public boolean isWOrUpPressed() {
        boolean wAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS || 
                         GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS;
        boolean result = wAhora && !prevW;
        prevW = wAhora;
        return result;
    }

    public boolean isRPressed() {
        boolean rAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        boolean result = rAhora && !prevR;
        prevR = rAhora;
        return result;
    }

    private boolean prevEnter;
    public boolean isEnterPressed() {
        boolean enterAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;
        boolean result = enterAhora && !prevEnter;
        prevEnter = enterAhora;
        return result;
    }

    private boolean prevDown;
    public boolean isDownPressed() {
        boolean downAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS ||
                            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
        boolean result = downAhora && !prevDown;
        prevDown = downAhora;
        return result;
    }

    public boolean isTPressed() {
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_T) == GLFW.GLFW_PRESS;
    }
}
