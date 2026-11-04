package com.miranoserqu.miras_space;

import com.miranoserqu.miras_space.client.ClientLevel;
import com.miranoserqu.miras_space.client.ClientPlayer;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.server.HttpsServerLevel;
import com.miranoserqu.miras_space.server.ServerLevel;
import com.miranoserqu.miras_space.server.ServerPlayer;
import com.sun.net.httpserver.HttpsServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.UUID;

public class ClientMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        // 1. Pedir IP
        System.out.print("Insert server address: ");
        String host = scanner.next();

        // 2. Pedir Puerto (imprime el mensaje explícitamente antes de leer)
        System.out.print("Insert port: ");
        int port = scanner.nextInt();

        // 3. Crear el juego y las instancias
        Game game = new Game();
        ClientLevel client = new ClientLevel(game);
        ClientPlayer clientPlayer = new ClientPlayer(UUID.randomUUID(), "miranoserqu", client);

        // 4. Instanciar la red con los valores recopilados
        InetSocketAddress address = new InetSocketAddress(host, 0);
        HttpsServer httpServer = HttpsServer.create(address, port);

        Level server = new HttpsServerLevel(httpServer, clientPlayer, game);

        client.setLocalPlayer(clientPlayer);
        client.start();
    }

}
