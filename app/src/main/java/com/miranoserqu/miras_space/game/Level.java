package com.miranoserqu.miras_space.game;

import com.miranoserqu.miras_space.Game;

public abstract class Level {

    protected final Game game;

    public Level(Game game){
        this.game = game;
    }

    public double getBoundaryX(){
        return 0;
    }

    public double getBoundaryY(){
        return 0;
    }

    public Game getGame(){
        return this.game;
    }

}
