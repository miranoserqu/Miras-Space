package com.miranoserqu.miras_space.game;

import com.miranoserqu.miras_space.Game;

public class Level {

    protected final double boundaryX;
    protected final double boundaryY;
    protected final Game game;

    public Level(double boundaryX, double boundaryY, Game game){
        this.boundaryX = boundaryX;
        this.boundaryY = boundaryY;
        this.game = game;
    }

    public double getBoundaryX() {
        return boundaryX;
    }

    public double getBoundaryY() {
        return boundaryY;
    }

    public Game getGame(){
        return this.game;
    }

}
