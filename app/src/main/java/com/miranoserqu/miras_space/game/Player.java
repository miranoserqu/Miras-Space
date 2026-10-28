package com.miranoserqu.miras_space.game;

import com.miranoserqu.miras_space.Game;

import java.util.UUID;

public class Player {

    protected double x;
    protected double y;
    protected double orientation;
    protected double velocityX;
    protected double velocityY;
    protected final UUID uuid;
    protected final String name;

    public Player(double x, double y, double orientation, double velocityX, double velocityY, UUID uuid, String name){
        this.uuid = uuid;
        this.name = name;
        this.orientation = orientation;
        this.x = x; this.y = y; this.velocityX = velocityX; this.velocityY = velocityY;
    }

    public Player(UUID uuid, String name){
        this.uuid = uuid;
        this.name = name;
        this.orientation = 0;
        this.x = 0; this.y = 0; this.velocityX = 0; this.velocityY = 0;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getOrientation(){
        return orientation;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

}
