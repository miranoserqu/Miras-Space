package com.miranoserqu.miras_space.client;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.Option;
import com.miranoserqu.miras_space.event.SubscribeEvent;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.network.*;
import com.miranoserqu.miras_space.render.LWJGLRenderer;
import de.gurkenlabs.input4j.*;
import de.gurkenlabs.input4j.components.XInput;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClientLevel extends Level {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int TICKS_PER_SECOND = 60;
    private static final double TICK_INTERVAL_NS = 1_000_000_000.0 / TICKS_PER_SECOND;

    private LWJGLRenderer renderer;
    private ClientPlayer localPlayer;
    private Collection<InputDevice> controllers;
    private long lastTime = System.currentTimeMillis();
    private long now = System.currentTimeMillis();
    private double accelMagnitude = 0;

    public ClientLevel(Game game) {
        super(game);
        this.controllers = InputDevices.init().getAll();
        this.renderer = new LWJGLRenderer(WINDOW_WIDTH, WINDOW_HEIGHT, null);
        this.renderer.init();
        game.registerServer(this);
    }

    public void setLocalPlayer(ClientPlayer localPlayer){
        this.localPlayer = localPlayer;
        this.renderer.setLocalPlayer(localPlayer);
        this.game.getClientBus().register(localPlayer);
        localPlayer.setLevel(this);
    }

    public void start(){
        long lastTimeNs = System.nanoTime();
        double delta = 0;

        while (!renderer.windowShouldClose()) {
            long nowNs = System.nanoTime();
            delta += (nowNs - lastTimeNs) / TICK_INTERVAL_NS;
            lastTimeNs = nowNs;

            while (delta >= 1) {
                update();
                delta--;
            }

            renderer.pollEvents();
            renderer.render();

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        renderer.cleanup();
    }

    public void update() {
        if (localPlayer == null) return;

        now = System.currentTimeMillis();
        renderer.update();
        lastTime = now;
    }

    @SubscribeEvent
    public void onPositionChange(PlayerUpdatePayload payload){
        if (this.localPlayer != null) {
            this.localPlayer.setX(payload.x);
            this.localPlayer.setY(payload.y);
            this.localPlayer.setVelocityX(payload.vX);
            this.localPlayer.setVelocityY(payload.vY);
            this.localPlayer.setOrientation(payload.angle);
            this.localPlayer.setAccelerationX(payload.aX);
            this.localPlayer.setAccelerationY(payload.aY);
            accelMagnitude = (float) Math.hypot(payload.aX, payload.aY);
        }
    }

    @SubscribeEvent
    public void onInputRequest(InputRequestPayload inputRequestPayload){
        if (localPlayer == null) return;

        boolean useGamepad = renderer.isUseGamepad();

        if (useGamepad) {
            InputDevice controller = null;
            for(InputDevice c : this.controllers){
                if(c.getControllerType().equals(ControllerType.XBOX) || c.getControllerType().equals(ControllerType.GENERIC)) {
                    controller = c;
                    break;
                }
            }

            if(controller == null) return;

            controller.poll();

            GamepadInputPayload payload = getGamepadInputPayload(controller);
            this.game.getServerBus().post(payload);
            return;
        }

        List<Integer> pressedKeys = getKeyboardInput();

        double shipCenterX = renderer.getWindowWidth() / 2.0;
        double shipCenterY = renderer.getWindowHeight() / 2.0;
        double mouseX = renderer.getMouseX();
        double mouseY = renderer.getMouseY();
        double dx = mouseX - shipCenterX;
        double dy = mouseY - shipCenterY;
        double targetAngle = Math.atan2(dy, dx);

        InputPayload inputPayload = new InputPayload(
                renderer.getAimTargetX(),
                renderer.getAimTargetY(),
                pressedKeys,
                targetAngle,
                this.localPlayer.getUuid(),
                renderer.isInvertControls()
        );
        this.game.getServerBus().post(inputPayload);
    }

    private static GamepadInputPayload getGamepadInputPayload(InputDevice controller) {
        float xAxis = 0;
        float yAxis = 0;
        boolean a = false, b = false, x = false, y = false;
        boolean lt = false, lb = false, rt = false, rb = false;
        boolean start = false, right = false, left = false, up = false, down = false;

        for(InputComponent component : controller.getComponents()){
            if(component.getId().equals(XInput.LEFT_THUMB_X)) xAxis = component.getData();
            else if(component.getId().equals(XInput.LEFT_THUMB_Y)) yAxis = -component.getData();
            else if(component.getId().equals(XInput.A)) a = component.getData() != 0;
            else if(component.getId().equals(XInput.B)) b = component.getData() != 0;
            else if(component.getId().equals(XInput.X)) x = component.getData() != 0;
            else if(component.getId().equals(XInput.Y)) y = component.getData() != 0;
            else if(component.getId().equals(XInput.LEFT_THUMB)) lt = component.getData() != 0;
            else if(component.getId().equals(XInput.LEFT_SHOULDER)) lb = component.getData() != 0;
            else if(component.getId().equals(XInput.RIGHT_THUMB)) rt = component.getData() != 0;
            else if(component.getId().equals(XInput.RIGHT_SHOULDER)) rb = component.getData() != 0;
            else if(component.getId().equals(XInput.START)) start = component.getData() != 0;
            else if(component.getId().equals(XInput.DPAD_RIGHT)) right = component.getData() != 0;
            else if(component.getId().equals(XInput.DPAD_LEFT)) left = component.getData() != 0;
            else if(component.getId().equals(XInput.DPAD_UP)) up = component.getData() != 0;
            else if(component.getId().equals(XInput.DPAD_DOWN)) down = component.getData() != 0;
        }

        return new GamepadInputPayload(xAxis, yAxis, a, b, x, y, lt, lb, rt, rb, start, right, left, up, down);
    }

    private List<Integer> getKeyboardInput() {
        List<Integer> pressedKeys = new ArrayList<>();
        boolean[] keyStates = renderer.getKeyStates();
        for (int kc = 0; kc < keyStates.length; kc++) {
            if (keyStates[kc]) {
                int awtKey = translateGlfwToAwtKey(kc);
                if (awtKey != 0 && !pressedKeys.contains(awtKey)) {
                    pressedKeys.add(awtKey);
                }
            }
        }
        return pressedKeys;
    }

    private int translateGlfwToAwtKey(int glfwKey) {
        switch (glfwKey) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_W: return KeyEvent.VK_W;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_S: return KeyEvent.VK_S;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_A: return KeyEvent.VK_A;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_D: return KeyEvent.VK_D;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_Q: return KeyEvent.VK_Q;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_E: return KeyEvent.VK_E;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_UP: return KeyEvent.VK_UP;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN: return KeyEvent.VK_DOWN;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT: return KeyEvent.VK_LEFT;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT: return KeyEvent.VK_RIGHT;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT:
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT: return KeyEvent.VK_SHIFT;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL:
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL: return KeyEvent.VK_CONTROL;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE: return KeyEvent.VK_SPACE;
            default: return 0;
        }
    }
}
