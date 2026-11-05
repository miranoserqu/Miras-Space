package com.miranoserqu.miras_space;

import com.miranoserqu.miras_space.client.ClientLevel;
import com.miranoserqu.miras_space.client.ClientPlayer;
import com.miranoserqu.miras_space.game.Game;
import com.miranoserqu.miras_space.server.ClientPeerLevel;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.UUID;

public class ClientMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        // 1. Pedir IP del servidor central
        System.out.print("Insert server address: ");
        String host = scanner.next();

        // 2. Pedir Puerto del servidor central
        System.out.print("Insert port: ");
        int port = scanner.nextInt();

        System.out.print("Insert name: ");
        String name = scanner.next();

        Game game = Game.getInstance();

        // 3. Crear el juego y las instancias locales
        ClientLevel client = new ClientLevel();
        ClientPlayer clientPlayer = new ClientPlayer(UUID.randomUUID(), name, client);
        client.setLocalPlayer(clientPlayer);

        // Pasamos la URL del servidor central remoto para que el cliente se registre allí
        String remoteServerUrl = String.format("http://%s:%d", host, port);
        ClientPeerLevel serverBridge = new ClientPeerLevel(clientPlayer, host, port);

        System.out.println("[CLIENTE] Servidor local de callbacks iniciado en: " + remoteServerUrl);

        game.registerClient(client);
        game.registerServer(serverBridge);

        client.setLocalPlayer(clientPlayer);
        client.start();
    }
}
