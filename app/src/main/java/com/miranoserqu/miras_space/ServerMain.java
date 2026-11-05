package com.miranoserqu.miras_space;

import com.miranoserqu.miras_space.client.ServerHostLevel;
import com.miranoserqu.miras_space.server.ServerLevel;

import java.io.IOException;
import java.util.Scanner;

public class ServerMain {

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        Game game = new Game();
        ServerLevel server = new ServerLevel(game);
        System.out.print("Insert port: ");
        int port = scanner.nextInt();
        ServerHostLevel clientHandler = new ServerHostLevel(game, port, server);

        game.registerServer(server);
        game.registerClient(clientHandler);

        server.start();
        clientHandler.start();
    }
}
