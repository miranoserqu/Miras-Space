package com.miranoserqu.miras_space.render;

import com.miranoserqu.miras_space.Option;
import com.miranoserqu.miras_space.client.ClientPlayer;
import com.miranoserqu.miras_space.entity.RemotePlayer;
import org.lwjgl.glfw.GLFW;
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
    private final Camera2D camera = new Camera2D();

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

        if (localPlayer != null) {
            camera.setPosition((float) localPlayer.getX(), (float) localPlayer.getY());
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        window = glfwCreateWindow(width, height, "Miras Space - LWJGL", Option.WINDOW_TYPE.get().getValue().monitor(), 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Callback para mantener el Viewport de OpenGL y la matriz de la cámara sincronizados al cambiar el tamaño de ventana
        glfwSetFramebufferSizeCallback(window, (win, newWidth, newHeight) -> {
            if (newWidth > 0 && newHeight > 0) {
                this.width = newWidth;
                this.height = newHeight;
                glViewport(0, 0, newWidth, newHeight);
            }
        });

        GLFW.glfwSetWindowPos(window, 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwShowWindow(window);

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
                mouseY = getWindowHeight() - ypos;
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
        glViewport(0, 0, width, height);
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

        // --- SNAPSHOT DE COORDENADAS ---
        final double localX = localPlayer.getX();
        final double localY = localPlayer.getY();
        final double localVelX = localPlayer.getVelocityX();
        final double localVelY = localPlayer.getVelocityY();
        final double localAccelX = localPlayer.getAccelerationX();
        final double localAccelY = localPlayer.getAccelerationY();
        final double localOrient = localPlayer.getOrientation();
        final UUID localUuid = localPlayer.getUuid();

        // --- ACTUALIZAR LA CÁMARA ---
        // Se posiciona en las coordenadas del mundo sumando el temblor (shake) y el avance por velocidad
        double targetCamX = localX + localVelX * 0.05 + cameraShakeX;
        double targetCamY = localY + localVelY * 0.05 + cameraShakeY;
        camera.setPosition((float) targetCamX, (float) targetCamY);
        camera.updateMatrices(width, height);

        // APLICAR MATRIZ DE LA CÁMARA (Proyección + Vista combinadas en el pipeline de OpenGL)
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        float[] matrixBuffer = new float[16];
        camera.getCombinedMatrix().get(matrixBuffer);
        glLoadMatrixf(matrixBuffer);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // --- 1. FONDO (TILES EN COORDENADAS DEL MUNDO) ---
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        // Convertir el tamaño de textura en píxeles a unidades del mundo
        double tileW = textureWidth / Camera2D.PIXELS_PER_UNIT;
        double tileH = textureHeight / Camera2D.PIXELS_PER_UNIT;

        // Calcular el rango de teselas visibles en el área que abarca la cámara
        double camX = camera.getCombinedMatrix().m30();
        int startCol = (int) Math.floor((localX - 25.0) / tileW);
        int endCol = (int) Math.ceil((localX + 25.0) / tileW);
        int startRow = (int) Math.floor((localY - 25.0) / tileH);
        int endRow = (int) Math.ceil((localY + 25.0) / tileH);

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                float x1 = (float) (col * tileW);
                float y1 = (float) (row * tileH);
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

        // Tamaño de las naves expresado en unidades del mundo (ej. 0.35 unidades = 35px a zoom 1.0)
        float entitySize = 0.35f;

        if(debugOverlay){
            renderDebugArrows();
        }

        // --- 2. JUGADOR LOCAL ---
        glPushMatrix();
        glTranslated(localX, localY, 0);
        glRotated(Math.toDegrees(localOrient), 0, 0, 1);

        glBegin(GL_TRIANGLES);
        glColor3f(0.0f, 1.0f, 0.0f); // Verde
        glVertex2f(-entitySize, -entitySize);
        glVertex2f(-entitySize, entitySize);
        glVertex2f(entitySize, 0);
        glEnd();

        glPopMatrix();

        // --- 3. JUGADORES REMOTOS ---
        for (RemotePlayer remotePlayer : this.remotePlayers.values()) {
            if (remotePlayer.getUuid().equals(localUuid)) {
                continue;
            }

            glPushMatrix();
            glTranslated(remotePlayer.getX(), remotePlayer.getY(), 0);
            glRotated(Math.toDegrees(remotePlayer.getOrientation()), 0, 0, 1);

            glBegin(GL_TRIANGLES);
            glColor3f(1.0f, 0.0f, 0.0f); // Rojo
            glVertex2f(-entitySize, -entitySize);
            glVertex2f(-entitySize, entitySize);
            glVertex2f(entitySize, 0);
            glEnd();

            glPopMatrix();
        }

        // --- 4. MARCADOR DE APUNTADO ---
        if (aimTargetSet) {
            // Dibujar interfaz/cursor regresando temporalmente a coordenadas fijas de pantalla (píxeles)
            glMatrixMode(GL_PROJECTION);
            glPushMatrix();
            glLoadIdentity();
            glOrtho(0, width, height, 0, -1, 1);

            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glLoadIdentity();

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

            // Restaurar matrices del espacio del mundo
            glPopMatrix();
            glMatrixMode(GL_PROJECTION);
            glPopMatrix();
            glMatrixMode(GL_MODELVIEW);
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

            double camShakeMag = Math.min(accelMagnitude * 0.02, 0.15); // Escalado al espacio del mundo
            double shakeFreq = Math.max(0.3, accelMagnitude * 0.15);
            double shakePhase = (System.currentTimeMillis() / 16.0) % (2 * Math.PI);
            cameraShakeX = (float) (Math.sin(shakePhase) * camShakeMag);
            cameraShakeY = (float) (Math.cos(shakePhase * 0.7) * camShakeMag);

            worldOffsetX = localPlayer.getX() + localPlayer.getAccelerationX() * 0.01;
            worldOffsetY = localPlayer.getY() + localPlayer.getAccelerationY() * 0.01;
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
        // Guardar estado y pasar a proyección fija ortográfica en píxeles para el HUD
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glColor4f(0.0f, 0.0f, 0.0f, 0.7f);
        glBegin(GL_QUADS);
        glVertex2f(10, 10);
        glVertex2f(270, 10);
        glVertex2f(270, 150);
        glVertex2f(10, 150);
        glEnd();

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    public void renderDebugArrows(){
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        float[] matrixBuffer = new float[16];
        camera.getCombinedMatrix().get(matrixBuffer);
        glLoadMatrixf(matrixBuffer);

        double accel = Math.hypot(localPlayer.getAccelerationX(), localPlayer.getAccelerationY());

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        glColor4f(1.0f, 0.0f, 0.0f, 1);
        glPushMatrix();
        glTranslated(localPlayer.getX(), localPlayer.getY(), 0);
        glRotated(Math.toDegrees(Math.atan2(localPlayer.getAccelerationY(), localPlayer.getAccelerationX())) - 90, 0, 0, 1);
        glBegin(GL_QUADS);
        glVertex2f(-0.1f, 0);
        glVertex2f(-0.1f, (float) accel * 60);
        glVertex2f(0.1f, (float) accel * 60);
        glVertex2f(0.1f, 0);
        glEnd();
        glPopMatrix();

        glColor4f(0.0f, 0.0f, 1.0f, 1);
        glPushMatrix();
        glTranslated(localPlayer.getX(), localPlayer.getY(), 0);
        glRotated(Math.toDegrees(Math.atan2(localPlayer.getVelocityY(), localPlayer.getVelocityX())) - 90, 0, 0, 1);
        glBegin(GL_QUADS);
        glVertex2f(-0.05f, 0);
        glVertex2f(-0.05f, (float) localPlayer.getVelocityMagnitude() * 60);
        glVertex2f(0.05f, (float) localPlayer.getVelocityMagnitude() * 60);
        glVertex2f(0.05f, 0);
        glEnd();
        glPopMatrix();

        glMatrixMode(GL_MODELVIEW);
        glMatrixMode(GL_PROJECTION);
    }

    public Camera2D getCamera() {
        return camera;
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
