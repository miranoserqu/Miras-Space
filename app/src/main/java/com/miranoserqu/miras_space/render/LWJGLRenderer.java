package com.miranoserqu.miras_space.render;

import com.miranoserqu.miras_space.client.ClientPlayer;
import com.miranoserqu.miras_space.entity.RemotePlayer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class LWJGLRenderer {

    private long window;
    private int width, height;
    private double worldOffsetX = 0;
    private double worldOffsetY = 0;
    private float cameraShakeX = 0;
    private float cameraShakeY = 0;
    private double accelMagnitude = 0;
    private boolean debugOverlay = false;
    private int frameCount = 0;
    private int currentFPS = 0;
    private long lastSecond = System.nanoTime();
    private ClientPlayer localPlayer;
    private Map<UUID, RemotePlayer> remotePlayers;

    private boolean[] keyStates = new boolean[GLFW_KEY_LAST + 1];
    private double mouseX = 0;
    private double mouseY = 0;
    private double aimTargetX = 0;
    private double aimTargetY = 0;
    private boolean aimTargetSet = false;
    private boolean useGamepad = false;
    private boolean invertControls = false;

    private int textureId;
    private int textureWidth = 256;
    private int textureHeight = 256;

    public LWJGLRenderer(int width, int height, ClientPlayer player, Map<UUID, RemotePlayer> remotePlayers) {
        this.width = width;
        this.height = height;
        this.localPlayer = player;
        this.remotePlayers = remotePlayers;
    }

    public void setLocalPlayer(ClientPlayer localPlayer){
        this.localPlayer = localPlayer;
    }

    public boolean init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwWindowHint(GLFW_RESIZABLE, GL_FALSE);

        window = glfwCreateWindow(width, height, "Miras Space - LWJGL", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_F3) {
                if (action == GLFW_PRESS) toggleDebugOverlay();
            } else if (key == GLFW_KEY_F5) {
                if (action == GLFW_PRESS) {
                    useGamepad = !useGamepad;
                    System.out.println("Control mode: " + (useGamepad ? "GAMEPAD" : "MOUSE+KB"));
                }
            } else if (key == GLFW_KEY_I) {
                if (action == GLFW_PRESS) {
                    invertControls = !invertControls;
                    System.out.println("Invert controls: " + (invertControls ? "ON" : "OFF"));
                }
            }
            if (key >= 0 && key < keyStates.length) {
                if (action == GLFW_PRESS) keyStates[key] = true;
                else if (action == GLFW_RELEASE) keyStates[key] = false;
            }
        });

        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (!useGamepad && button == GLFW_MOUSE_BUTTON_RIGHT) {
                if (action == GLFW_PRESS) {
                    if (aimTargetSet) {
                        aimTargetSet = false;
                        aimTargetX = 0;
                        aimTargetY = 0;
                    } else {
                        aimTargetSet = true;
                        aimTargetX = mouseX;
                        aimTargetY = mouseY;
                    }
                }
            }
        });

        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (!useGamepad) {
                mouseX = xpos;
                mouseY = ypos;
            }
        });

        try (MemoryStack stack = stackPush()) {
            var pWidth = stack.mallocInt(1);
            var pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);
            width = pWidth.get(0);
            height = pHeight.get(0);
        }

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidmode != null) {
            glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        initOpenGL();
        loadTexture();

        return true;
    }

    private void initOpenGL() {
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
    }

    private void loadTexture() {
        int[] pixels = new int[textureWidth * textureHeight];
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < 200; i++) {
            int x = rand.nextInt(textureWidth);
            int y = rand.nextInt(textureHeight);
            int size = rand.nextInt(3) + 1;
            int brightness = rand.nextInt(150) + 100;
            int color = (brightness) | (brightness << 8) | (brightness << 16) | (0xFF << 24);
            for (int px = x - size / 2; px < x + size / 2 && px < textureWidth && px >= 0; px++) {
                for (int py = y - size / 2; py < y + size / 2 && py < textureHeight && py >= 0; py++) {
                    pixels[py * textureWidth + px] = color;
                }
            }
        }

        IntBuffer buffer = MemoryUtil.memAllocInt(pixels.length);
        buffer.put(pixels).flip();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textureWidth, textureHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        MemoryUtil.memFree(buffer);
    }

    public void cleanup() {
        glDeleteTextures(textureId);
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (localPlayer == null) {
            glfwSwapBuffers(window);
            return;
        }

        // --- SNAPSHOT DE COORDENADAS (Evita desincronización por multithreading) ---
        final double localX = localPlayer.getX();
        final double localY = localPlayer.getY();
        final double localVelX = localPlayer.getVelocityX();
        final double localVelY = localPlayer.getVelocityY();
        final double localAccelX = localPlayer.getAccelerationX();
        final double localAccelY = localPlayer.getAccelerationY();
        final double localOrient = localPlayer.getOrientation();
        final UUID localUuid = localPlayer.getUuid();

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // --- 1. FONDO (TILES) ---
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        double tileW = textureWidth;
        double tileH = textureHeight;

        // Usamos el snapshot localX/localY si worldOffset depende del jugador
        double offsetX = worldOffsetX % tileW;
        double offsetY = worldOffsetY % tileH;
        if (offsetX < 0) offsetX += tileW;
        if (offsetY < 0) offsetY += tileH;

        double camOffsetX = localAccelX * 0.5 + cameraShakeX;
        double camOffsetY = localAccelY * 0.5 + cameraShakeY;

        int startCol = (int) Math.floor((-offsetX + camOffsetX) / tileW) - 1;
        int startRow = (int) Math.floor((-offsetY + camOffsetY) / tileH) - 1;
        int endCol = startCol + (int) Math.ceil((width + tileW) / tileW) + 3;
        int endRow = startRow + (int) Math.ceil((height + tileH) / tileH) + 3;

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                float x1 = (float) (col * tileW - offsetX + camOffsetX);
                float y1 = (float) (row * tileH - offsetY + camOffsetY);
                float x2 = (float) (x1 + tileW);
                float y2 = (float) (y1 + tileH);

                glBegin(GL_QUADS);
                glTexCoord2f(0, 0); glVertex2f(x1, y1);
                glTexCoord2f(1, 0); glVertex2f(x2, y1);
                glTexCoord2f(1, 1); glVertex2f(x2, y2);
                glTexCoord2f(0, 1); glVertex2f(x1, y2);
                glEnd();
            }
        }
        glDisable(GL_TEXTURE_2D);

        // --- CENTRO DE CÁMARA (Basado en el snapshot) ---
        double speedCamX = localVelX * 0.15 + cameraShakeX;
        double speedCamY = localVelY * 0.15 + cameraShakeY;
        double camCenterX = width / 2.0 + speedCamX;
        double camCenterY = height / 2.0 + speedCamY;
        double size = 25.0;

        // --- 2. JUGADOR LOCAL ---
        glPushMatrix();
        glTranslated(camCenterX, camCenterY, 0);
        glRotated(Math.toDegrees(localOrient), 0, 0, 1);

        glBegin(GL_TRIANGLES);
        glColor3f(0.0f, 1.0f, 0.0f); // Verde
        glVertex2f((float) -size, (float) -size);
        glVertex2f((float) -size, (float) size);
        glVertex2f((float) size, 0);
        glEnd();

        // --- 3. JUGADORES REMOTOS ---
        for (RemotePlayer remotePlayer : this.remotePlayers.values()) {
            // Ignorar si el mapa contiene accidentalmente al propio jugador local
            if (remotePlayer.getUuid().equals(localUuid)) {
                continue;
            }

            // Posición relativa calculada ESTRICTAMENTE contra el snapshot inmutable
            double relX = remotePlayer.getX() - localX;
            double relY = remotePlayer.getY() - localY;

            glBegin(GL_TRIANGLES);
            glColor3f(1.0f, 0.0f, 0.0f); // Rojo
            glVertex2f((float) (relX - size), (float) (relY - size));
            glVertex2f((float) (relX - size), (float) (relY + size));
            glVertex2f((float) (relX + size), (float) relY);
            glEnd();

        }

        glPopMatrix();

        // --- 4. MARCADOR DE APUNTADO ---
        if (aimTargetSet) {
            glLineWidth(2.0f);
            glColor3f(1.0f, 1.0f, 0.0f);
            glBegin(GL_LINES);
            glVertex2f((float) (width / 2.0), (float) (height / 2.0));
            glVertex2f((float) aimTargetX, (float) aimTargetY);
            glEnd();

            glColor3f(1.0f, 1.0f, 0.0f);
            int r = 12;
            glBegin(GL_POLYGON);
            for (int i = 0; i < 360; i += 10) {
                double angle = Math.toRadians(i);
                glVertex2f((float) (aimTargetX + r * Math.cos(angle)), (float) (aimTargetY + r * Math.sin(angle)));
            }
            glEnd();
        }

        if (debugOverlay) {
            renderDebugOverlay();
        }

        glfwSwapBuffers(window);
    }

    public void update() {
        updateFPS();

        if (localPlayer != null) {
            accelMagnitude = (float) Math.hypot(localPlayer.getAccelerationX(), localPlayer.getAccelerationY());

            double camShakeMag = Math.min(accelMagnitude * 0.6, 8.0);
            double shakeFreq = Math.max(0.3, accelMagnitude * 0.15);
            double shakePhase = (System.currentTimeMillis() / 16.0) % (2 * Math.PI);
            cameraShakeX = (float) (Math.sin(shakePhase) * camShakeMag);
            cameraShakeY = (float) (Math.cos(shakePhase * 0.7) * camShakeMag);

            worldOffsetX = localPlayer.getX() + localPlayer.getVelocityX() * 0.001;
            worldOffsetY = localPlayer.getY() + localPlayer.getVelocityY() * 0.001;
        }
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public boolean windowShouldClose() {
        return glfwWindowShouldClose(window);
    }

    private void toggleDebugOverlay() {
        debugOverlay = !debugOverlay;
        System.out.println("Debug overlay: " + (debugOverlay ? "ON" : "OFF"));
    }

    private void updateFPS() {
        frameCount++;

        if (System.nanoTime() - lastSecond >= 1_000_000_000L) {
            currentFPS = frameCount;
            frameCount = 0;
            lastSecond = System.nanoTime();
        }
    }

    private void renderDebugOverlay() {
        glLoadIdentity();

        double vx = localPlayer.getVelocityX();
        double vy = localPlayer.getVelocityY();
        double accelerationX = localPlayer.getAccelerationX();
        double accelerationY = localPlayer.getAccelerationY();
        double speed = Math.hypot(vx, vy);
        double accelMag = Math.hypot(accelerationX, accelerationY);

        glColor4f(0.0f, 0.0f, 0.0f, 0.7f);
        glBegin(GL_QUADS);
        glVertex2f(10, 10);
        glVertex2f(270, 10);
        glVertex2f(270, 150);
        glVertex2f(10, 150);
        glEnd();
    }

    public long getWindow() {
        return window;
    }

    public int getWindowWidth() {
        return width;
    }

    public int getWindowHeight() {
        return height;
    }

    public boolean[] getKeyStates() {
        return keyStates;
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public boolean isUseGamepad() {
        return useGamepad;
    }

    public boolean isInvertControls() {
        return invertControls;
    }

    public double getAimTargetX() {
        return aimTargetX;
    }

    public double getAimTargetY() {
        return aimTargetY;
    }

    public boolean isAimTargetSet() {
        return aimTargetSet;
    }

    public void setWindowShouldClose(boolean shouldClose) {
        if (shouldClose && window != 0) {
            glfwSetWindowShouldClose(window, true);
        }
    }
}
