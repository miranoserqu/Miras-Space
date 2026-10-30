package com.miranoserqu.miras_space.client;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.Option;
import com.miranoserqu.miras_space.event.SubscribeEvent;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.network.GamepadInputPayload;
import com.miranoserqu.miras_space.network.InputPayload;
import com.miranoserqu.miras_space.network.InputRequestPayload;
import com.miranoserqu.miras_space.network.Payload;
import com.miranoserqu.miras_space.registries.Attributes;
import com.miranoserqu.miras_space.resource.Resource;
import de.gurkenlabs.input4j.*;
import de.gurkenlabs.input4j.components.XInput;
import de.gurkenlabs.input4j.foreign.windows.xinput.XInputButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.im.InputContext;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ClientLevel extends Level {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int TICKS_PER_SECOND = 60;
    private static final int TICK_INTERVAL_MS = 1000 / TICKS_PER_SECOND;
    private static final int TILE_SIZE = 256;

    private JFrame frame;
    private GamePanel gamePanel;
    private ClientPlayer localPlayer;

    private boolean[] keyStates = new boolean[256];
    private Collection<InputDevice> controllers;
    private double mouseX = 0;
    private double mouseY = 0;
    private float controllerX = 0;
    private float controllerY = 0;
    private boolean aimTargetSet = false;
    private boolean useGamepad = false;
    private boolean invertControls = false;
    private double aimTargetX = 0;
    private double aimTargetY = 0;
    private double worldOffsetX = 0;
    private double worldOffsetY = 0;
    private double lastOrientation = 0;

    private boolean debugOverlay = false;
    private int frameCount = 0;
    private int currentFPS = 0;
    private long now = System.nanoTime();
    private long lastTime = System.nanoTime();
    private long lastSecond = System.nanoTime();
    private double cameraShakeX = 0;
    private double cameraShakeY = 0;
    private double accelMagnitude = 0;

    public ClientLevel(Game game) {
        super(Option.WEIGHT_RESOLUTION.getValue(), Option.HEIGHT_RESOLUTION.getValue(), game);
        this.controllers = InputDevices.init().getAll();
        initializeUI();
    }

    public void setLocalPlayer(ClientPlayer localPlayer){
        this.localPlayer = localPlayer;
        this.game.getClientBus().register(localPlayer);
        localPlayer.setLevel(this);
    }

    private void initializeUI() {
        frame = new JFrame("Miras Space - Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setLocationRelativeTo(null);
        gamePanel = new GamePanel();
        frame.add(gamePanel);
        frame.setVisible(true);
        gamePanel.requestFocusInWindow();
        setupKeyboardInput();
        setupMouseTracking();
    }

    public void start(){
        Thread mainThread = new Thread(() -> {
            while(true){
                update();
            }
        });
        mainThread.start();
    }

    private void setupKeyboardInput() {
        gamePanel.setFocusable(true);

        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int kc = e.getKeyCode();
                if (kc == KeyEvent.VK_F3 || kc == KeyEvent.VK_F5) return;
                if (kc >= 0 && kc < keyStates.length) keyStates[kc] = true;
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int kc = e.getKeyCode();
                if (kc == KeyEvent.VK_F3 || kc == KeyEvent.VK_F5) return;
                if (kc >= 0 && kc < keyStates.length) keyStates[kc] = false;
            }
        });

        // G key toggles gamepad mode
        gamePanel.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "toggleGamepad");
        gamePanel.getActionMap().put("toggleGamepad", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                useGamepad = !useGamepad;
                System.out.println("Control mode: " + (useGamepad ? "GAMEPAD" : "MOUSE+KB"));
            }
        });

        // F3 toggles debug overlay
        gamePanel.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "toggleDebugOverlay");
        gamePanel.getActionMap().put("toggleDebugOverlay", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleDebugOverlay();
            }
        });

        // I key toggles invert controls
        gamePanel.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_I, 0), "toggleInvertControls");
        gamePanel.getActionMap().put("toggleInvertControls", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                invertControls = !invertControls;
                System.out.println("Invert controls: " + (invertControls ? "ON" : "OFF"));
            }
        });
    }

    private void toggleDebugOverlay() {
        debugOverlay = !debugOverlay;
        System.out.println("Debug overlay: " + (debugOverlay ? "ON" : "OFF"));
        gamePanel.repaint();
    }

    private void setupMouseTracking() {
        gamePanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!useGamepad) { mouseX = e.getX(); mouseY = e.getY(); }
            }
        });
        gamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (useGamepad) return;
                if (e.getButton() == MouseEvent.BUTTON3) {
                    if (aimTargetSet) {
                        aimTargetSet = false;
                        aimTargetX = 0;
                        aimTargetY = 0;
                    } else {
                        aimTargetSet = true;
                        aimTargetX = e.getX();
                        aimTargetY = e.getY();
                    }
                }
            }
        });
    }

    private List<Integer> getKeyboardInput() {
        List<Integer> pressedKeys = new ArrayList<>();
        for (int kc = 0; kc < keyStates.length; kc++) {
            if (keyStates[kc]) {
                pressedKeys.add(kc);
            }
        }
        return pressedKeys;
    }

    public void update() {

        double vx = localPlayer.getVelocityX();
        double vy = localPlayer.getVelocityY();

        double camShakeMag = Math.min(accelMagnitude * 0.6, 8.0);
        double shakeFreq = Math.max(0.3, accelMagnitude * 0.15);
        double shakePhase = (System.currentTimeMillis() / 16.0) % (2 * Math.PI);
        cameraShakeX = Math.sin(shakePhase) * camShakeMag;
        cameraShakeY = Math.cos(shakePhase * 0.7) * camShakeMag;

        this.now = System.nanoTime();

        worldOffsetX = localPlayer.getX() + vx * (now - lastTime) / 1000000;
        worldOffsetY = localPlayer.getY() + vy * (now - lastTime) / 1000000;

        gamePanel.repaint();

        this.lastTime = now;
    }

    @SubscribeEvent
    public void onInputRequest(InputRequestPayload inputRequestPayload){
        List<Integer> pressedKeys = new ArrayList<>();

        if (useGamepad) {
            // MODO MANDO FÍSICO: lee axes del mando real, bloquea teclado/ratón
            InputDevice controller = null;
            for(InputDevice c : this.controllers){
                if(c.getControllerType().equals(ControllerType.XBOX) || c.getControllerType().equals(ControllerType.GENERIC)) controller = c;
            }

            if(controller == null) return;

            controller.poll();

            GamepadInputPayload payload = getGamepadInputPayload(controller);
            this.game.getServerBus().post(payload);

            return;
        }

        // MODO MOUSE + TECLADO (normal)
        pressedKeys = getKeyboardInput();

        // Client sends aimAngle to server; server is authoritative for orientation.
        double shipCenterX = gamePanel.getWidth() / 2.0;
        double shipCenterY = gamePanel.getHeight() / 2.0;
        double dx = mouseX - shipCenterX;
        double dy = mouseY - shipCenterY;
        double targetAngle = Math.atan2(dy, dx);

        InputPayload inputPayload = new InputPayload(aimTargetX, aimTargetY, pressedKeys, targetAngle, this.localPlayer.getUuid(), invertControls);
        this.game.getServerBus().post(inputPayload);
    }

    private static GamepadInputPayload getGamepadInputPayload(InputDevice controller) {
        float xAxis = 0;
        float yAxis = 0;
        boolean a = false;
        boolean b = false;
        boolean x = false;
        boolean y = false;
        boolean lt = false;
        boolean lb = false;
        boolean rt = false;
        boolean rb = false;
        boolean start = false;
        boolean right = false;
        boolean left = false;
        boolean up = false;
        boolean down = false;

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

        GamepadInputPayload payload = new GamepadInputPayload(xAxis, yAxis, a, b, x, y, lt, lb, rt, rb, start, right, left, up, down);
        return payload;
    }

    private class GamePanel extends JPanel {
        private BufferedImage spaceTexture;

        public GamePanel() {
            loadTexture();
        }

        private void loadTexture() {
            try {
                spaceTexture = Resource.getInstance().getImage("/textures/space_tile.png");
            } catch (IllegalArgumentException e) {
                spaceTexture = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = spaceTexture.createGraphics();
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
                java.util.Random rand = new java.util.Random(42);
                for (int i = 0; i < 200; i++) {
                    int x = rand.nextInt(TILE_SIZE);
                    int y = rand.nextInt(TILE_SIZE);
                    int size = rand.nextInt(3) + 1;
                    int brightness = rand.nextInt(150) + 100;
                    g2d.setColor(new Color(brightness, brightness, brightness));
                    g2d.fillOval(x - size/2, y - size/2, size, size);
                }
                g2d.dispose();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Black background to prevent white flashes at edges when camera offset pushes tiles
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            Graphics2D g2d = (Graphics2D) g.create();

            // Return early if localPlayer is not yet initialized
            if (ClientLevel.this.localPlayer == null) {
                g2d.dispose();
                return;
            }

            // Draw tiled background
            if (spaceTexture != null) {
                int tileW = spaceTexture.getWidth();
                int tileH = spaceTexture.getHeight();
                double offsetX = worldOffsetX % tileW;
                double offsetY = worldOffsetY % tileH;
                if (offsetX < 0) offsetX += tileW;
                if (offsetY < 0) offsetY += tileH;

                // Camera offset + ship vibration when accelerating for speed sensation
                double camOffsetX = ClientLevel.this.localPlayer.getAccelerationX() * 0.5 + cameraShakeX;
                double camOffsetY = ClientLevel.this.localPlayer.getAccelerationY() * 0.5 + cameraShakeY;

                int startCol = (int) (Math.floor((-offsetX + camOffsetX) / tileW)) - 1;
                int startRow = (int) (Math.floor((-offsetY + camOffsetY) / tileH)) - 1;
                int endCol = startCol + (int) Math.ceil((getWidth() + tileW) / tileW) + 3;
                int endRow = startRow + (int) Math.ceil((getHeight() + tileH) / tileH) + 3;

                for (int row = startRow; row <= endRow; row++) {
                    for (int col = startCol; col <= endCol; col++) {
                        int x = (int) (col * tileW - offsetX + camOffsetX);
                        int y = (int) (row * tileH - offsetY + camOffsetY);
                        g2d.drawImage(spaceTexture, x, y, null);
                    }
                }
            }

            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            // Debug overlay: velocity (blue arrow) and acceleration (red arrow),
            // drawn in screen space so they are not rotated with the ship.
            if (debugOverlay && ClientLevel.this.localPlayer != null) {
                g2d.setColor(new Color(0, 150, 255, 220)); // blue velocity
                drawArrow(g2d, centerX, centerY, ClientLevel.this.localPlayer.getVelocityX() * 8, ClientLevel.this.localPlayer.getVelocityY() * 8);
                g2d.setColor(new Color(255, 50, 50, 220)); // red acceleration
                drawArrow(g2d, centerX, centerY, ClientLevel.this.localPlayer.getAccelerationX() * 60, ClientLevel.this.localPlayer.getAccelerationY() * 60);

                drawDebugText(g2d);
            }

        // Aim marker
        if (aimTargetSet && ClientLevel.this.localPlayer != null) {
            int cx = (int)(getWidth() / 2.0);
            int cy = (int)(getHeight() / 2.0);
            int mx = (int)(aimTargetX);
            int my = (int)(aimTargetY);
            g2d.setColor(new Color(255, 255, 0, 220));
            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8, 4}, 0f));
            g2d.drawLine(cx, cy, mx, my);
            g2d.setColor(new Color(255, 255, 0, 180));
            int r = 12;
            g2d.drawOval(mx - r, my - r, r * 2, r * 2);
            g2d.fillOval(mx - r, my - r, r * 2, r * 2);
        }

            // Draw ship centered with rotation + speed offset + vibration
            double speedCamX = localPlayer.getVelocityX() * 0.15 + cameraShakeX;
            double speedCamY = localPlayer.getVelocityY() * 0.15 + cameraShakeY;
            g2d.translate(centerX + speedCamX, centerY + speedCamY);
            g2d.rotate((float) localPlayer.getOrientation() + (localPlayer.getOrientation() - lastOrientation) * (now - lastTime) / 1000000);
            lastOrientation = localPlayer.getOrientation();
            // Ensure lastTime doesn't get reset to prevent potential overflow
            if (lastTime == 0) lastTime = System.nanoTime();
            g2d.setColor(new Color(0, 255, 0, 200));
            int[] xPts = {-25, 25, 0};
            int[] yPts = {0, 0, -25};
            g2d.fillPolygon(xPts, yPts, 3);
            g2d.dispose();
        }

        /**
         * Draws an arrow from (x1, y1) with the given delta.
         */
        private void drawArrow(Graphics2D g2d, double x1, double y1, double dx, double dy) {
            double length = Math.hypot(dx, dy);
            if (length < 0.5) return; // nothing to show

            double x2 = x1 + dx;
            double y2 = y1 + dy;
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);

            // Arrow head
            double angle = Math.atan2(dy, dx);
            double headLength = 12;
            double spread = Math.PI / 7;
            int[] headX = {
                    (int) x2,
                    (int) (x2 - headLength * Math.cos(angle - spread)),
                    (int) (x2 - headLength * Math.cos(angle + spread))
            };
            int[] headY = {
                    (int) y2,
                    (int) (y2 - headLength * Math.sin(angle - spread)),
                    (int) (y2 - headLength * Math.sin(angle + spread))
            };
            g2d.fillPolygon(headX, headY, 3);
        }

        /**
         * Draws the telemetry text block (position, velocity, acceleration).
         */
        private void drawDebugText(Graphics2D g2d) {
            double vx = localPlayer.getVelocityX();
            double vy = localPlayer.getVelocityY();
            double accelerationX = localPlayer.getAccelerationX();
            double accelerationY = localPlayer.getAccelerationY();
            double speed = Math.hypot(vx, vy);
            double accelMagnitude = Math.hypot(accelerationX, accelerationY);

            frameCount++;

            // Si ha pasado 1 segundo (1,000,000,000 nanosegundos)
            if (now - lastSecond >= 1_000_000_000L) {
                currentFPS = frameCount; // Actualiza el valor a mostrar en pantalla
                frameCount = 0;          // Reinicia el contador
                lastSecond = now;          // Adelanta el reloj
            }

            g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            String[] lines = {
                    String.format("Pos:    (%.2f, %.2f)", localPlayer.getX(), localPlayer.getY()),
                    String.format("Vel:    (%.3f, %.3f)  |v|=%.3f", vx, vy, speed),
                    String.format("Accel:  (%.3f, %.3f)  |a|=%.3f", accelerationX, accelerationY, accelMagnitude),
                    String.format("Angle:  %.3f rad", localPlayer.getOrientation()),
                    "FPS:    " + currentFPS,
                    String.format("Mode:   %s", useGamepad ? "GAMEPAD" : "MOUSE+KB")
            };

            int boxWidth = 260;
            int boxHeight = lines.length * 17 + 12;
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.fillRect(10, 10, boxWidth, boxHeight);
            g2d.setColor(new Color(120, 220, 120));
            for (int i = 0; i < lines.length; i++) {
                g2d.drawString(lines[i], 20, 30 + i * 17);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT);
        }
    }

}