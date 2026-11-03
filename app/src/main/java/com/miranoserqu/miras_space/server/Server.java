package com.miranoserqu.miras_space.server;

import com.miranoserqu.miras_space.game.Player;

import java.util.UUID;

public interface Server {

    ServerPlayer getPlayer(UUID uuid);
    void addPlayer(ServerPlayer player);
    void tick();
    void start();
    void stop();

}
