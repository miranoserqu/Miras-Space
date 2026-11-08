package com.miranoserqu.miras_space.util;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

import java.awt.*;

public class ScreenUtils {

    public record ScreenResolution(int width, int height, int refreshRate) {}

    /**
     * Obtiene la resolución del monitor principal adaptándose al sistema operativo.
     */
    public static ScreenResolution getPrimaryScreenResolution() {
        String os = System.getProperty("os.name").toLowerCase();

        // En Linux es preferible GLFW/LWJGL para evitar HeadlessException o fallos con Wayland/X11
        if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            ScreenResolution res = getResolutionViaGLFW();
            if (res != null) {
                return res;
            }
        }

        // En Windows / macOS intentamos AWT primero
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                DisplayMode mode = GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDisplayMode();

                if (mode != null && mode.getHeight() > 0) {
                    return new ScreenResolution(mode.getWidth(), mode.getHeight(), mode.getRefreshRate());
                }
            }
        } catch (Throwable ignored) {
            // Si falla AWT en cualquier plataforma, hacemos fallback a GLFW
        }

        // Fallback universal a GLFW si AWT falla
        ScreenResolution res = getResolutionViaGLFW();
        if (res != null) {
            return res;
        }

        // Resolución por defecto de emergencia si ambos métodos fallan
        return new ScreenResolution(1920, 1080, 60);
    }

    /**
     * Obtiene las dimensiones mediante la API nativa de GLFW (LWJGL 3)
     */
    private static ScreenResolution getResolutionViaGLFW() {
        boolean initializedHere = false;

        // Si GLFW no está inicializado en el hilo actual, lo inicializamos temporalmente
        if (!GLFW.glfwInit()) {
            return null;
        }

        try {
            long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
            if (primaryMonitor == 0) {
                return null;
            }

            GLFWVidMode vidMode = GLFW.glfwGetVideoMode(primaryMonitor);
            if (vidMode != null) {
                return new ScreenResolution(vidMode.width(), vidMode.height(), vidMode.refreshRate());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
