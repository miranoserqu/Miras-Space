package com.miranoserqu.miras_space.network;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.client.ClientPlayer;
import com.miranoserqu.miras_space.client.HttpsClientLevel;
import com.miranoserqu.miras_space.server.HttpsServerLevel;
import com.miranoserqu.miras_space.server.ServerLevel;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NetworkIntegrationTest {

    @Test
    void testClientServerRegistrationAndCommunication() throws IOException, InterruptedException {
        Game serverGame = new Game();
        ServerLevel serverLevel = new ServerLevel(serverGame);

        // 1. Iniciar servidor central en puerto aleatorio (0)
        HttpsClientLevel centralServer = new HttpsClientLevel(serverGame, 0, serverLevel);
        centralServer.start();

        int serverPort = centralServer.getServerUrl().replaceAll("[^0-9]", "").isEmpty() ? 0 : Integer.parseInt(centralServer.getServerUrl().substring(centralServer.getServerUrl().lastIndexOf(':') + 1));
        String serverUrl = centralServer.getServerUrl();

        // 2. Iniciar cliente con su propio HttpServer para callbacks en puerto aleatorio (0)
        Game clientGame = new Game();
        HttpServer clientHttpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ClientPlayer clientPlayer = new ClientPlayer(UUID.randomUUID(), "test-player", null);

        HttpsServerLevel clientBridge = new HttpsServerLevel(clientHttpServer, clientPlayer, clientGame, serverUrl);
        clientHttpServer.start();

        // Dar tiempo para completar el registro HTTP POST a /register
        Thread.sleep(500);

        // Verificar que el servidor central registró al cliente exitosamente
        assertNotNull(serverLevel.getPlayer(clientPlayer.getUuid()), "El jugador debería estar registrado en el servidor");

        // Detener servidores
        clientHttpServer.stop(0);
        centralServer.stop(0);
    }
}
