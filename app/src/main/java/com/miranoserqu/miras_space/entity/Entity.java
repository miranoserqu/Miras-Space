package com.miranoserqu.miras_space.entity;

import java.util.UUID;

public class Entity {

    protected double x;
    protected double y;
    protected double orientation;
    protected double velocityX;
    protected double velocityY;
    protected double accelerationX;
    protected double accelerationY;
    protected double mass = 1;
    protected final UUID uuid;

    public Entity(double x, double y, double orientation, double velocityX, double velocityY, UUID uuid){
        this.uuid = uuid;
        this.orientation = orientation;
        this.x = x; this.y = y; this.velocityX = velocityX; this.velocityY = velocityY;
    }

    public Entity(UUID uuid){
        this(0, 0, 0, 0, 0, uuid);
    }

    public Entity(){
        this(UUID.randomUUID());
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

    public double getAccelerationX(){
        return this.accelerationX;
    }

    public double getAccelerationY(){
        return this.accelerationY;
    }

    public double getMass(){
        return this.mass;
    }

}
