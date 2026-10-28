package com.miranoserqu.miras_space;

import com.miranoserqu.miras_space.client.ClientLevel;
import com.miranoserqu.miras_space.client.ClientPlayer;
import com.miranoserqu.miras_space.game.Player;
import com.miranoserqu.miras_space.server.ServerLevel;
import com.miranoserqu.miras_space.server.ServerPlayer;

import java.util.UUID;

/**
 * Main class for the Miras Space game.
 * Entry point that starts the game client.
 */
public class Main {

    public static void main(String[] args) {
        // Start the game client which handles everything
        Game game = new Game();
        ClientLevel client = new ClientLevel(game);
        ServerLevel server = new ServerLevel(game);
        ClientPlayer clientPlayer = new ClientPlayer(UUID.randomUUID(), "miranoserqu", client);
        ServerPlayer serverPlayer = new ServerPlayer(clientPlayer.getUuid(), clientPlayer.getName(), server);
        server.addPlayer(serverPlayer);
        client.setLocalPlayer(clientPlayer);
        server.start();
        client.start();
    }
}