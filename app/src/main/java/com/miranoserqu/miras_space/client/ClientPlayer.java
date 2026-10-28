package com.miranoserqu.miras_space.client;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.event.SubscribeEvent;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.game.Player;
import com.miranoserqu.miras_space.network.PlayerUpdatePayload;

import java.util.UUID;

public class ClientPlayer extends Player {

    private ClientLevel level;

    public ClientPlayer(double x, double y, double orientation, double velocityX, double velocityY, UUID uuid, String name, ClientLevel level) {
        super(x, y, orientation, velocityX, velocityY, uuid, name);
        this.level = level;
    }

    public ClientPlayer(UUID uuid, String name, ClientLevel level){
        super(uuid, name);
        this.level = level;
    }

    @SubscribeEvent
    public void onPositionChange(PlayerUpdatePayload payload){
        this.x = payload.x;
        this.y = payload.y;
        this.velocityX = payload.vX;
        this.velocityY = payload.vY;
        this.orientation = payload.angle;
        this.accelerationX = payload.aX;
        this.accelerationY = payload.aY;
    }

}
