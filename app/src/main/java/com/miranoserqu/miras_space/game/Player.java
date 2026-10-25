package com.miranoserqu.miras_space.game;

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
public class Player {

    public static final double BASE_MAX_SPEED_SQUARED = Math.pow(20, 2); // 8 px/s max
    public static final double BASE_FORWARD_ACCELERATION = 0.5;        // ~2.4 px/s², reaches max in ~3.3s
    public static final double BASE_REVERSE_ACCELERATION = 0.2;
    public static final double BASE_LATERAL_SPEED = 0.04;
    public static final double BASE_ROTATION_SPEED = 5;

    // Position of the spaceship
    private double x;
    private double y;

    // Velocity of the spaceship (pixels per tick)
    private double velocityX;
    private double velocityY;

    // Acceleration applied on the last tick (pixels per tick^2)
    private double accelerationX;
    private double accelerationY;

    // Orientation of the spaceship (in radians, 0 = pointing right)
    private double orientation;

    // Size of the spaceship
    private static final int WIDTH = 50;
    private static final int HEIGHT = 50;

    // Game boundaries
    private final double maxX;
    private final double maxY;

    private boolean turbo = false;

    // Ship attributes (loaded from JSON)
    private final List<AttributeModifier> attributeModifiers = new ArrayList<>();

    /**
     * Creates a new player at the specified position.
     *
     * @param startX initial x position
     * @param startY initial y position
     * @param maxX maximum x boundary (game width)
     * @param maxY maximum y boundary (game height)
     */
    public Player(double startX, double startY, double maxX, double maxY) {
        this.x = startX;
        this.y = startY;
        this.maxX = maxX - WIDTH;  // Adjust for ship width
        this.maxY = maxY - HEIGHT; // Adjust for ship height
        this.orientation = 0;      // Start facing right (0 radians)

        // Initialize velocity
        this.velocityX = 0;
        this.velocityY = 0;

        this.attributeModifiers.add(new AttributeModifier(MAX_SPEED_SQUARED.get(), UUID.randomUUID(), "player:base_max_speed_squared", ADD_BASE, BASE_MAX_SPEED_SQUARED));
        this.attributeModifiers.add(new AttributeModifier(FORWARD_ACCELERATION.get(), UUID.randomUUID(), "player:base_forward_acceleration", ADD_BASE, BASE_FORWARD_ACCELERATION));
        this.attributeModifiers.add(new AttributeModifier(REVERSE_ACCELERATION.get(), UUID.randomUUID(), "player:base_reverse_acceleration", ADD_BASE, BASE_REVERSE_ACCELERATION));
        this.attributeModifiers.add(new AttributeModifier(LATERAL_SPEED.get(), UUID.randomUUID(), "player:base_lateral_speed", ADD_BASE, BASE_LATERAL_SPEED));
        this.attributeModifiers.add(new AttributeModifier(ROTATION_SPEED.get(), UUID.randomUUID(), "player:base_rotation_speed", ADD_BASE, BASE_ROTATION_SPEED));
    }

    /**
     * Updates the player's physics state based on the keys currently held down.
     * Called once per tick. Rotation is handled externally (mouse-based).
     *
     * @param pressedKeys key codes of every key held down at this instant
     */
    public void update(List<Integer> pressedKeys, boolean aimTargetSet, double aimTargetX, double aimTargetY, boolean invertControls) {
        // Translate the held keys into thrust axes
        int inputY = getForwardInput(pressedKeys, invertControls);
        int inputX = getLateralInput(pressedKeys);

        this.turbo = false;

        for(int key : pressedKeys){
            if(key == KeyEvent.VK_CONTROL && this.velocityX != 0 && this.velocityY != 0){
                double velocity = Math.sqrt(Math.pow(this.velocityX, 2) + Math.pow(this.velocityY, 2));
                double movementX = this.velocityX / velocity;
                double movementY = this.velocityY / velocity;

                double deaccel = REVERSE_ACCELERATION.get().getValue(this.attributeModifiers);

                if(velocity <= deaccel){
                    this.velocityX = 0;
                    this.velocityY = 0;
                }

                this.accelerationX = -movementX * deaccel;
                this.accelerationY = -movementY * deaccel;

                this.velocityX -= movementX * deaccel;
                this.velocityY -= movementY * deaccel;

                return;
            }

            if(key == KeyEvent.VK_SHIFT) {
                this.turbo = true;
            }
        }

        // Speed at the start of the tick, before this tick's thrust is applied.
        // Used as the "frozen" limit while the ship is above the max speed.
        double speedBeforeThrust = Math.hypot(this.velocityX, this.velocityY);

        // Calculate acceleration based on current orientation and input
        double accelX = 0;
        double accelY = 0;

        // Forward/backward thrusters (W/S/UP/DOWN keys) - along ship's orientation
        if (inputY != 0) {
            double accel;
            if (inputY == 1) {
                // W/UP = forward
                accel = FORWARD_ACCELERATION.get().getValue(this.attributeModifiers) * (turbo ? 2 : 1);
            } else {
                // S/DOWN = reverse (slower)
                accel = REVERSE_ACCELERATION.get().getValue(this.attributeModifiers) * (turbo ? 2 : 1);
            }
            accelX += Math.cos(orientation) * accel * inputY;
            accelY += Math.sin(orientation) * accel * inputY;
        }

        // Lateral thrusters (Q/E keys) - perpendicular to ship's orientation
        if (inputX != 0) {
            double lateralSpeed = LATERAL_SPEED.get().getValue(this.attributeModifiers);
            velocityX += -Math.sin(orientation) * lateralSpeed * inputX;
            velocityY += Math.cos(orientation) * lateralSpeed * inputX;
        }

        // Aim target mode: if target is set and accelerating forward, steer toward target
        if (aimTargetSet && inputY == 1) {
            double dx = aimTargetX - x;
            double dy = aimTargetY - y;
            double targetAngle = Math.atan2(dy, dx);
            // Adjust orientation smoothly toward target
            updateOrientation(targetAngle);
            // Re-align acceleration to target direction
            double accel = FORWARD_ACCELERATION.get().getValue(this.attributeModifiers) * (turbo ? 2 : 1);
            accelX = Math.cos(orientation) * accel;
            accelY = Math.sin(orientation) * accel;
        }

        // Apply acceleration to velocity with gradual build-up (in-space physics)
        this.accelerationX = accelX;
        this.accelerationY = accelY;
        this.velocityX = this.velocityX + this.accelerationX;
        this.velocityY = this.velocityY + this.accelerationY;

        // Speed management
        double maxSpeedSquared = MAX_SPEED_SQUARED.get().getValue(this.attributeModifiers) * (turbo ? 4 : 1);
        double speedSq = velocityX * velocityX + velocityY * velocityY;
        double speed = Math.sqrt(speedSq);
        double maxSpeed = Math.sqrt(maxSpeedSquared);
        double threshold = maxSpeed * 1.15;

        if (speedSq > maxSpeedSquared && maxSpeedSquared > 0) {
            double velOrientation = Math.atan2(velocityY, velocityX);

            if (speed > threshold) {
                // Way above max: freeze speed at its current value (do not reduce)
                // It must NOT grow further either: cap at speedBeforeThrust
                double frozenLimit = Math.min(speedBeforeThrust, speed);
                // Prevent acceleration from pushing it over frozenLimit this tick
                // By projecting velocity to frozenLimit along direction if it grew
                if (speed > frozenLimit) {
                    this.velocityX = Math.cos(velOrientation) * frozenLimit;
                    this.velocityY = Math.sin(velOrientation) * frozenLimit;
                }
                // If it stayed under frozenLimit naturally, leave it unchanged
            } else {
                // Between max and 15% over: standard clamp
                this.velocityX = Math.cos(velOrientation) * maxSpeed;
                this.velocityY = Math.sin(velOrientation) * maxSpeed;
            }
        }

        // Update position
        x += velocityX;
        y += velocityY;

        // Keep within bounds (world bounds, not screen bounds - world is infinite)
        // Bounds are effectively disabled for infinite space; kept for safety
        x = Math.max(-1e6, Math.min(x, 1e6));
        y = Math.max(-1e6, Math.min(y, 1e6));
    }

    /**
     * Extracts the forward/backward input axis (-1, 0, 1) from the list of pressed keys.
     * Handles W/S and UP/DOWN keys.
     */
    private static int getForwardInput(List<Integer> pressedKeys) {
        return getForwardInput(pressedKeys, false);
    }
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
     * Gets the player's width.
     *
     * @return width
     */
    public int getWidth() {
        return WIDTH;
    }

    /**
     * Gets the player's height.
     *
     * @return height
     */
    public int getHeight() {
        return HEIGHT;
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