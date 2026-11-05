package com.miranoserqu.miras_space.entity;

import com.miranoserqu.miras_space.render.LWJGLRenderer;

import java.util.UUID;

public abstract class Entity {

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

    public abstract void render(org.lwjgl.opengl.GL11 gl, double screenX, double screenY);

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

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setOrientation(double orientation) {
        this.orientation = orientation;
    }

    public void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }

    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    public void setAccelerationX(double accelerationX) {
        this.accelerationX = accelerationX;
    }

    public void setAccelerationY(double accelerationY) {
        this.accelerationY = accelerationY;
    }
}
