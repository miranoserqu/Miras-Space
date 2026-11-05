package com.miranoserqu.miras_space;

import com.miranoserqu.miras_space.client.ClientLevel;
import com.miranoserqu.miras_space.client.ClientPlayer;
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

        // 3. Crear el juego y las instancias locales
        Game game = new Game();
        ClientLevel client = new ClientLevel(game);
        ClientPlayer clientPlayer = new ClientPlayer(UUID.randomUUID(), "miranoserqu", client);
        client.setLocalPlayer(clientPlayer);

        // 4. Instanciar la red local del cliente (escucha en un puerto aleatorio local para recibir callbacks)
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
        HttpServer httpServer = HttpServer.create(address, 0);

        // Pasamos la URL del servidor central remoto para que el cliente se registre allí
        String remoteServerUrl = String.format("http://%s:%d", host, port);
        ClientPeerLevel serverBridge = new ClientPeerLevel(clientPlayer, game, host, port);

        // ¡IMPORTANTE! Iniciamos el servidor HTTP del cliente para que pueda escuchar
        httpServer.start();
        System.out.println("[CLIENTE] Servidor local de callbacks iniciado en: " + remoteServerUrl);

        client.setLocalPlayer(clientPlayer);
        client.start();
    }
}
