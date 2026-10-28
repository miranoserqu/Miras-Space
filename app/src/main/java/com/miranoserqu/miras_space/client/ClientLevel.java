package com.miranoserqu.miras_space.client;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.Option;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.game.Player;
import com.miranoserqu.miras_space.network.InputPayload;
import com.miranoserqu.miras_space.server.ServerPlayer;
import com.miranoserqu.miras_space.network.Payload;
import com.miranoserqu.miras_space.resource.Resource;
import com.miranoserqu.miras_space.server.ServerLevel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private GamepadInput gamepad = new GamepadInput();
    private double mouseX = 0;
    private double mouseY = 0;
    private boolean aimTargetSet = false;
    private boolean useGamepad = false;
    private boolean invertControls = false;
    private double aimTargetX = 0;
    private double aimTargetY = 0;
    private double worldOffsetX = 0;
    private double worldOffsetY = 0;

    private boolean debugOverlay = false;
    private double lastAccelX = 0;
    private double lastAccelY = 0;
    private double tickRate = 0;
    private double cameraShakeX = 0;
    private double cameraShakeY = 0;
    private double accelMagnitude = 0;

    public ClientLevel(Game game) {
        super(Option.WEIGHT_RESOLUTION.getValue(), Option.HEIGHT_RESOLUTION.getValue(), game);
        initializeUI();
    }

    public void setLocalPlayer(ClientPlayer localPlayer){
        this.localPlayer = localPlayer;
        this.game.getClientBus().register(localPlayer);
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
        List<Integer> pressedKeys = new ArrayList<>();

        if (useGamepad) {
            // MODO MANDO FÍSICO: lee axes del mando real, bloquea teclado/ratón
            gamepad.poll();
            double axisX = gamepad.getAxisX(); // joystick izquierdo: -1..1
            double axisY = gamepad.getAxisY(); // joystick derecho / LT: -1..1

            double shipCenterX = gamePanel.getWidth() / 2.0;
            double shipCenterY = gamePanel.getHeight() / 2.0;

            // Joystick izquierdo (lateral / Q-E)
            if (axisX < -0.3) pressedKeys.add(KeyEvent.VK_Q);
            if (axisX > 0.3) pressedKeys.add(KeyEvent.VK_E);

            // LT / Aceleración adelante (W)
            // Usamos axisY del joystick izquierdo o botón A como LT
            if (axisY < -0.3 || gamepad.isButtonA()) pressedKeys.add(KeyEvent.VK_W);

            // Joystick derecho = cursor de apunte (simula posición de ratón)
            mouseX = shipCenterX + axisX * 100.0; // si el mando tiene 2 joysticks
            mouseY = shipCenterY + (gamepad.isButtonB() ? -100 : 0); // simplificado

            // Si hay mando físico conectado, usar sus axes para apunte
            double dx = mouseX - shipCenterX;
            double dy = mouseY - shipCenterY;
            double targetAngle = Math.atan2(dy, dx);

            Payload inputPayload = new InputPayload(mouseX, mouseY, pressedKeys, targetAngle, localPlayer.getUuid(), invertControls);
            this.game.getServerBus().post(inputPayload);

            // Actualiza posición / cámara igual que en modo normal
            double vx = localPlayer.getVelocityX();
            double vy = localPlayer.getVelocityY();

            double camShakeMag = Math.min(accelMagnitude * 0.6, 8.0);
            double shakeFreq = Math.max(0.3, accelMagnitude * 0.15);
            double shakePhase = (System.currentTimeMillis() / 16.0) % (2 * Math.PI);
            cameraShakeX = Math.sin(shakePhase) * camShakeMag;
            cameraShakeY = Math.cos(shakePhase * 0.7) * camShakeMag;

            worldOffsetX = vx;
            worldOffsetY = vy;

            gamePanel.repaint();
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

        double vx = localPlayer.getVelocityX();
        double vy = localPlayer.getVelocityY();

        double camShakeMag = Math.min(accelMagnitude * 0.6, 8.0);
        double shakeFreq = Math.max(0.3, accelMagnitude * 0.15);
        double shakePhase = (System.currentTimeMillis() / 16.0) % (2 * Math.PI);
        cameraShakeX = Math.sin(shakePhase) * camShakeMag;
        cameraShakeY = Math.cos(shakePhase * 0.7) * camShakeMag;

        worldOffsetX += vx;
        worldOffsetY += vy;

        gamePanel.repaint();
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

            // Draw tiled background
            if (spaceTexture != null) {
                int tileW = spaceTexture.getWidth();
                int tileH = spaceTexture.getHeight();
                double offsetX = worldOffsetX % tileW;
                double offsetY = worldOffsetY % tileH;
                if (offsetX < 0) offsetX += tileW;
                if (offsetY < 0) offsetY += tileH;

                // Camera offset + ship vibration when accelerating for speed sensation
                double camOffsetX = localPlayer.getVelocityX() * 0.15 + cameraShakeX;
                double camOffsetY = localPlayer.getVelocityY() * 0.15 + cameraShakeY;

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
            if (debugOverlay) {
                g2d.setColor(new Color(0, 150, 255, 220)); // blue velocity
                drawArrow(g2d, centerX, centerY, localPlayer.getVelocityX() * 8, localPlayer.getVelocityY() * 8);
                g2d.setColor(new Color(255, 50, 50, 220)); // red acceleration
                drawArrow(g2d, centerX, centerY, lastAccelX * 60, lastAccelY * 60);

                drawDebugText(g2d);
            }

        // Aim marker
        if (aimTargetSet) {
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
            g2d.rotate((float) localPlayer.getOrientation());
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
            double speed = Math.hypot(vx, vy);
            double accelMagnitude = Math.hypot(lastAccelX, lastAccelY);

            g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            String[] lines = {
                    String.format("Pos:    (%.2f, %.2f)", localPlayer.getX(), localPlayer.getY()),
                    String.format("Vel:    (%.3f, %.3f)  |v|=%.3f", vx, vy, speed),
                    String.format("Accel:  (%.3f, %.3f)  |a|=%.3f", lastAccelX, lastAccelY, accelMagnitude),
                    String.format("Angle:  %.3f rad", localPlayer.getOrientation()),
                    String.format("FPS:    %.1f", tickRate),
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