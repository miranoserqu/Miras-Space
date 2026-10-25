package com.miranoserqu.miras_space.network;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a data packet sent between client and server.
 * Contains information about player actions and state updates.
 */
public class Payload {
    // Types of payloads
    public static final int PLAYER_INPUT = 0;
    public static final int PLAYER_UPDATE = 1;
    public static final int GAME_STATE = 2;

    private int type;
    private int playerId;
    private List<Integer> pressedKeys;  // List of currently pressed key codes
    private double x;       // Player position
    private double y;       // Player position
    private double orientation; // Player orientation (radians)
    private double aimAngle;    // Target orientation requested by the client (radians)
    private long timestamp;
    private double velocityX;  // Player velocity X
    private double velocityY;  // Player velocity Y
    private double accelerationX; // Player acceleration applied on the last tick
    private double accelerationY; // Player acceleration applied on the last tick
    private boolean aimTargetSet;
    private double aimTargetX;
    private double aimTargetY;
    private boolean invertControls;

    /**
     * Creates a new payload.
     *
     * @param type the type of payload
     */
    public Payload(int type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.pressedKeys = new ArrayList<>();
    }

    /**
     * Creates a player input payload.
     *
     * @param playerId the ID of the player
     * @param pressedKeys list of currently pressed key codes
     * @param aimAngle target orientation in radians (0 = pointing right)
     * @return the payload
     */
    public static Payload createPlayerInput(int playerId, List<Integer> pressedKeys, double aimAngle, boolean aimTargetSet, double aimTargetX, double aimTargetY, boolean invertControls) {
        Payload payload = new Payload(PLAYER_INPUT);
        payload.playerId = playerId;
        payload.pressedKeys = new ArrayList<>(pressedKeys);
        payload.aimAngle = aimAngle;
        payload.aimTargetSet = aimTargetSet;
        payload.aimTargetX = aimTargetX;
        payload.aimTargetY = aimTargetY;
        payload.invertControls = invertControls;
        return payload;
    }

    /**
     * Creates a player update payload.
     *
     * @param playerId the ID of the player
     * @param x the player's x position
     * @param y the player's y position
     * @param orientation the player's orientation in radians
     * @return the payload
     */
    public static Payload createPlayerUpdate(int playerId, double x, double y, double orientation, double velocityX, double velocityY, double accelerationX, double accelerationY) {
        Payload payload = new Payload(PLAYER_UPDATE);
        payload.playerId = playerId;
        payload.x = x;
        payload.y = y;
        payload.orientation = orientation;
        payload.velocityX = velocityX;
        payload.velocityY = velocityY;
        payload.accelerationX = accelerationX;
        payload.accelerationY = accelerationY;
        return payload;
    }

    // Getters and Setters
    public int getType() {
        return type;
    }

    public int getPlayerId() {
        return playerId;
    }

    public List<Integer> getPressedKeys() {
        return new ArrayList<>(pressedKeys);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getOrientation() {
        return orientation;
    }

    public double getAimAngle() {
        return aimAngle;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public double getAccelerationX() {
        return accelerationX;
    }

    public double getAccelerationY() {
        return accelerationY;
    }
    public boolean isAimTargetSet() { return aimTargetSet; }
    public double getAimTargetX() { return aimTargetX; }
    public double getAimTargetY() { return aimTargetY; }
    public boolean isInvertControls() { return invertControls; }

}