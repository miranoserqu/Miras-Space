package com.miranoserqu.miras_space.server;

import com.miranoserqu.miras_space.event.SubscribeEvent;
import com.miranoserqu.miras_space.game.Game;
import com.miranoserqu.miras_space.game.Player;
import com.miranoserqu.miras_space.network.GamepadInputPayload;
import com.miranoserqu.miras_space.network.InputPayload;
import com.miranoserqu.miras_space.network.InputRequestPayload;
import com.miranoserqu.miras_space.network.PlayerUpdatePayload;
import com.miranoserqu.miras_space.tag.AttributeModifier;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.miranoserqu.miras_space.registries.Attributes.*;
import static com.miranoserqu.miras_space.tag.AttributeModifier.ModifierType.*;

/**
 * Represents the player's spaceship in the game.
 * Contains all relevant information about the player's state including orientation.
 */
public class ServerPlayer extends Player {

    public static final double BASE_MAX_SPEED = (double) 20 / 50;
    public static final double BASE_FORWARD_ACCELERATION = 0.5 / 50;
    public static final double BASE_REVERSE_ACCELERATION = 0.2 / 50;
    public static final double BASE_LATERAL_SPEED = 0.04 / 50;
    public static final double BASE_ROTATION_SPEED = 10;

    private final ServerLevel level;

    // Guardar solo el estado del último input recibido
    private InputPayload lastKeyboardInput = null;
    private GamepadInputPayload lastGamepadInput = null;
    private boolean turbo = false;

    private final List<AttributeModifier> attributeModifiers = new ArrayList<>();

    public ServerPlayer(UUID uuid, String name, ServerLevel level){
        super(uuid, name);
        this.level = level;
        this.attributeModifiers.add(new AttributeModifier(MAX_SPEED.get(), UUID.randomUUID(), "player:base_max_speed", ADD_BASE, BASE_MAX_SPEED));
        this.attributeModifiers.add(new AttributeModifier(FORWARD_ACCELERATION.get(), UUID.randomUUID(), "player:base_forward_acceleration", ADD_BASE, BASE_FORWARD_ACCELERATION));
        this.attributeModifiers.add(new AttributeModifier(REVERSE_ACCELERATION.get(), UUID.randomUUID(), "player:base_reverse_acceleration", ADD_BASE, BASE_REVERSE_ACCELERATION));
        this.attributeModifiers.add(new AttributeModifier(LATERAL_SPEED.get(), UUID.randomUUID(), "player:base_lateral_speed", ADD_BASE, BASE_LATERAL_SPEED));
        this.attributeModifiers.add(new AttributeModifier(ROTATION_SPEED.get(), UUID.randomUUID(), "player:base_rotation_speed", ADD_BASE, BASE_ROTATION_SPEED));
    }

    // El evento SOLO guarda el payload en una variable, NO mueve al jugador
    @SubscribeEvent
    public void onPlayerInput(InputPayload payload) {
        if (payload.playerUUID.equals(this.uuid)) {
            this.lastKeyboardInput = payload;
            this.lastGamepadInput = null; // Priorizar teclado si se recibe teclado
        }
    }

    @SubscribeEvent
    public void onPlayerGamepadInput(GamepadInputPayload payload) {
        if (payload.playerUUID.equals(this.uuid)) {
            this.lastGamepadInput = payload;
            this.lastKeyboardInput = null;
        }
    }

    // El tick del servidor (60 FPS regulados) es el ÚNICO lugar donde se calcula la física
    public void tick() {
        if (lastKeyboardInput != null) {
            processKeyboardPhysics(lastKeyboardInput);
        } else if (lastGamepadInput != null) {
            processGamepadPhysics(lastGamepadInput);
        }

        // Notificar a los clientes la nueva posición regulada a 60 FPS
        PlayerUpdatePayload updatePayload = new PlayerUpdatePayload(
                this.uuid, this.x, this.y, this.orientation,
                this.velocityX, this.velocityY, this.accelerationX, this.accelerationY
        );
        Game.getInstance().getClientBus().post(updatePayload);
    }

    private void processKeyboardPhysics(InputPayload payload) {
        List<Integer> inputs = payload.inputs;
        int inputY = getForwardInput(inputs, payload.invertControls);
        int inputX = getLateralInput(inputs);

        this.turbo = false;

        for (int key : inputs) {
            if (key == KeyEvent.VK_CONTROL) {
                if (this.velocityX == 0 && this.velocityY == 0) {
                    updateOrientation(payload.targetAngle);
                    return;
                }
                double velocity = Math.hypot(velocityX, velocityY);
                double movementX = this.velocityX / velocity;
                double movementY = this.velocityY / velocity;
                double deaccel = FORWARD_ACCELERATION.get().getValue(this.attributeModifiers);

                if (velocity <= deaccel) {
                    this.velocityX = 0;
                    this.velocityY = 0;
                    this.accelerationX = 0;
                    this.accelerationY = 0;
                } else {
                    this.accelerationX = -movementX * deaccel;
                    this.accelerationY = -movementY * deaccel;
                    this.velocityX -= movementX * deaccel;
                    this.velocityY -= movementY * deaccel;
                }

                this.x += this.velocityX;
                this.y += this.velocityY;
                updateOrientation(Math.atan2(-movementY, -movementX));
                return;
            }

            if (key == KeyEvent.VK_SHIFT) {
                this.turbo = true;
            }
        }

        double speedBeforeThrust = Math.hypot(this.velocityX, this.velocityY);
        double accelX = 0;
        double accelY = 0;

        if (inputY != 0) {
            double accel = (inputY == 1 ? FORWARD_ACCELERATION.get().getValue(this.attributeModifiers)
                    : REVERSE_ACCELERATION.get().getValue(this.attributeModifiers)) * (turbo ? 2 : 1);
            accelX += Math.cos(orientation) * accel * inputY;
            accelY += Math.sin(orientation) * accel * inputY;
        }

        if (inputX != 0) {
            double lateralSpeed = LATERAL_SPEED.get().getValue(this.attributeModifiers);
            velocityX += -Math.sin(orientation) * lateralSpeed * inputX;
            velocityY += Math.cos(orientation) * lateralSpeed * inputX;
        }

        this.accelerationX = accelX;
        this.accelerationY = accelY;
        this.velocityX += this.accelerationX;
        this.velocityY += this.accelerationY;

        applySpeedLimit(speedBeforeThrust);

        this.x += velocityX;
        this.y += velocityY;
        updateOrientation(payload.targetAngle);
    }

    private void processGamepadPhysics(GamepadInputPayload payload) {
        this.turbo = payload.lt;

        if (Math.abs(payload.xAxis) < 0.1 && Math.abs(payload.yAxis) < 0.1) {
            if (this.velocityX == 0 && this.velocityY == 0) {
                updateOrientation(0);
                return;
            }
            double velocity = Math.hypot(velocityX, velocityY);
            double movementX = this.velocityX / velocity;
            double movementY = this.velocityY / velocity;
            double deaccel = FORWARD_ACCELERATION.get().getValue(this.attributeModifiers);

            if (velocity <= deaccel) {
                this.velocityX = 0;
                this.velocityY = 0;
                this.accelerationX = 0;
                this.accelerationY = 0;
            } else {
                this.accelerationX = -movementX * deaccel;
                this.accelerationY = -movementY * deaccel;
                this.velocityX -= movementX * deaccel;
                this.velocityY -= movementY * deaccel;
            }

            this.x += this.velocityX;
            this.y += this.velocityY;
            updateOrientation(Math.atan2(-movementY, -movementX));
            return;
        }

        double speedBeforeThrust = Math.hypot(this.velocityX, this.velocityY);
        double accelAngle = Math.atan2(payload.yAxis, payload.xAxis);

        this.accelerationX = FORWARD_ACCELERATION.get().getValue(this.attributeModifiers) * (turbo ? 2 : 1) * payload.xAxis;
        this.accelerationY = FORWARD_ACCELERATION.get().getValue(this.attributeModifiers) * (turbo ? 2 : 1) * payload.yAxis;

        this.velocityX += this.accelerationX;
        this.velocityY += this.accelerationY;

        applySpeedLimit(speedBeforeThrust);

        this.x += velocityX;
        this.y += velocityY;
        updateOrientation(accelAngle);
    }

    private void applySpeedLimit(double speedBeforeThrust) {
        double maxSpeed = MAX_SPEED.get().getValue(this.attributeModifiers) * (turbo ? 2 : 1);
        double speed = Math.hypot(velocityX, velocityY);
        double threshold = maxSpeed * 1.15;

        if (speed > maxSpeed && maxSpeed > 0) {
            double velOrientation = Math.atan2(velocityY, velocityX);
            if (speed > threshold) {
                double frozenLimit = Math.min(speedBeforeThrust, speed);
                if (speed > frozenLimit) {
                    this.velocityX = Math.cos(velOrientation) * frozenLimit;
                    this.velocityY = Math.sin(velOrientation) * frozenLimit;
                }
            } else {
                this.velocityX = Math.cos(velOrientation) * maxSpeed;
                this.velocityY = Math.sin(velOrientation) * maxSpeed;
            }
        }
    }

    /**
     * Extracts the forward/backward input axis (-1, 0, 1) from the list of pressed keys.
     * Handles W/S and UP/DOWN keys.
     */
    private static int getForwardInput(List<Integer> pressedKeys, boolean invert) {
        for (int kc : pressedKeys) {
            if (kc == KeyEvent.VK_W || kc == KeyEvent.VK_UP) {
                return invert ? -1 : 1;
            }
            if (kc == KeyEvent.VK_S || kc == KeyEvent.VK_DOWN) {
                return invert ? 1 : -1;
            }
        }
        return 0;
    }

    /**
     * Extracts the lateral input axis (-1, 0, 1) from the list of pressed keys.
     * Handles Q/E keys.
     */
    private static int getLateralInput(List<Integer> pressedKeys) {
        for (int kc : pressedKeys) {
            if (kc == KeyEvent.VK_Q) {
                return -1;  // lateral left
            }
            if (kc == KeyEvent.VK_E) {
                return 1;  // lateral right
            }
        }
        return 0;
    }

    /**
     * Rotates the ship towards a target angle, limited by the rotation speed attribute.
     * This gives the smooth turn behaviour instead of snapping to the cursor.
     *
     * @param targetAngle desired orientation in radians (0 = pointing right)
     */
    public void updateOrientation(double targetAngle) {
        double diff = targetAngle - this.orientation;
        while (diff <= -Math.PI) diff += 2 * Math.PI;
        while (diff > Math.PI) diff -= 2 * Math.PI;

        double maxRadiansPerTick = Math.toRadians(ROTATION_SPEED.get().getValue(this.attributeModifiers));
        double step = Math.max(-maxRadiansPerTick, Math.min(maxRadiansPerTick, diff));
        setOrientation(this.orientation + step);
    }

    /**
     * Sets the ship's orientation directly (used for mouse-based aiming).
     *
     * @param orientation new orientation in radians (0 = pointing right)
     */
    public void setOrientation(double orientation) {
        this.orientation = orientation;
        // Normalize to [0, 2*PI)
        while (this.orientation < 0) this.orientation += 2 * Math.PI;
        while (this.orientation >= 2 * Math.PI) this.orientation -= 2 * Math.PI;
    }

    /**
     * Gets the player's x position.
     *
     * @return x position
     */
    public double getX() {
        return x;
    }

    /**
     * Sets the player's x position.
     *
     * @param x the x position to set
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Gets the player's y position.
     *
     * @return y position
     */
    public double getY() {
        return y;
    }

    /**
     * Sets the player's y position.
     *
     * @param y the y position to set
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Gets the player's orientation (in radians, 0 = pointing right).
     *
     * @return orientation in radians
     */
    public double getOrientation() {
        return orientation;
    }

    /**
     * Gets the player's velocity X.
     *
     * @return velocity X
     */
    public double getVelocityX() {
        return velocityX;
    }

    /**
     * Gets the player's velocity Y.
     *
     * @return velocity Y
     */
    public double getVelocityY() {
        return velocityY;
    }

    /**
     * Sets the player's velocity X.
     *
     * @param velocityX the velocity X to set
     */
    public void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }

    /**
     * Sets the player's velocity Y.
     *
     * @param velocityY the velocity Y to set
     */
    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    /**
     * Gets the acceleration applied on the last tick.
     *
     * @return acceleration X
     */
    public double getAccelerationX() {
        return accelerationX;
    }

    /**
     * Gets the acceleration applied on the last tick.
     *
     * @return acceleration Y
     */
    public double getAccelerationY() {
        return accelerationY;
    }

    /**
     * Gets the ship attributes.
     *
     * @return ship attributes
     */
    public List<AttributeModifier> getAttributeModifiers() {
        return this.attributeModifiers;
    }
}