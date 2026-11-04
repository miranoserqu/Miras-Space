package com.miranoserqu.miras_space;

import com.miranoserqu.miras_space.client.HttpsClientLevel;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.server.ServerLevel;
import com.miranoserqu.miras_space.server.ServerPlayer;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

public class ServerMain {

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        Game game = new Game();
        ServerLevel server = new ServerLevel(game);
        System.out.print("Insert port: ");
        int port = scanner.nextInt();
        ServerPlayer serverPlayer = new ServerPlayer(UUID.randomUUID(), "miranoserqu", server);
        Level client = new HttpsClientLevel(game, port);
        server.addPlayer(serverPlayer);
        server.start();
    }
}
