package com.miranoserqu.miras_space.client;

import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.game.Player;

import java.util.UUID;

public class ClientPlayer extends Player {

    public ClientPlayer(double x, double y, double orientation, double velocityX, double velocityY, UUID uuid, String name) {
        super(x, y, orientation, velocityX, velocityY, uuid, name);
    }

    public ClientPlayer(UUID uuid, String name){
        super(uuid, name);
    }

}
