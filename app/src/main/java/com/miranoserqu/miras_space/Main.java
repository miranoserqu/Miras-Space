package com.miranoserqu.miras_space;

import com.miranoserqu.miras_space.client.ClientLevel;
import com.miranoserqu.miras_space.client.ClientPlayer;
import com.miranoserqu.miras_space.game.Game;
import com.miranoserqu.miras_space.server.ServerLevel;
import com.miranoserqu.miras_space.server.ServerPlayer;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

import static de.gurkenlabs.input4j.InputDevices.init;

/**
 * Main class for the Miras Space game.
 * Entry point that starts the game client.
 */
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Insert operation: ");
        String op = scanner.next();
        switch(op){
            case "runClient" -> ClientMain.main(args);
            case "runServer" -> ServerMain.main(args);
            case "runNormal" -> normal();
        }
    }

    public static void normal(){
        Game game = Game.getInstance();
        ClientLevel client = new ClientLevel();
        ServerLevel server = new ServerLevel();
        ClientPlayer clientPlayer = new ClientPlayer(UUID.randomUUID(), "miranoserqu", client);
        ServerPlayer serverPlayer = new ServerPlayer(clientPlayer.getUuid(), clientPlayer.getName(), server);
        server.addPlayer(serverPlayer);
        client.setLocalPlayer(clientPlayer);
        server.start();
        client.start();
    }

}