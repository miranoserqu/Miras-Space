package com.miranoserqu.miras_space.game;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.entity.Entity;

import java.util.UUID;

public class Player extends Entity {

    protected final String name;
    protected Level level;

    public Player(double x, double y, double orientation, double velocityX, double velocityY, UUID uuid, String name){
        super(x, y, orientation, velocityX, velocityY, uuid);
        this.name = name;
    }

    public Player(UUID uuid, String name){
        super(uuid);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setLevel(Level level){
        this.level = level;
    }

    public Level getLevel(){
        return this.level;
    }

}
