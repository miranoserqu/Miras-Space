package com.miranoserqu.miras_space.client;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.event.SubscribeEvent;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.network.GamepadInputPayload;
import com.miranoserqu.miras_space.network.InputPayload;
import com.miranoserqu.miras_space.network.InputRequestPayload;
import com.miranoserqu.miras_space.server.ServerLevel;
import com.miranoserqu.miras_space.server.ServerPlayer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class HttpsClientLevel extends Level {

    private final HttpServer server;
    private final ThreadPoolExecutor executor;
    private final ServerLevel serverLevel;
    private final Map<UUID, String> clientCallbackUrls = new ConcurrentHashMap<>();

    public HttpsClientLevel(Game game, int port, ServerLevel serverLevel) throws IOException {
        super(game);
        this.serverLevel = serverLevel;
        this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        this.server.setExecutor(executor);

        this.server.createContext("/register", this::handleClientRegistration);
        game.getServerBus().register(this);
    }

    public String getServerUrl() {
        InetSocketAddress address = this.server.getAddress();
        if (address == null) {
            throw new IllegalStateException("El servidor aún no ha sido vinculado a una dirección.");
        }
        String host = address.getHostString();
        if ("0.0.0.0".equals(host) || "0:0:0:0:0:0:0:0".equals(host) || "::".equals(host)) {
            host = "127.0.0.1";
        }
        int port = address.getPort();
        return String.format("http://%s:%d", host, port);
    }

    private void handleClientRegistration(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        try (DataInputStream dis = new DataInputStream(exchange.getRequestBody())) {
            UUID clientId = new UUID(dis.readLong(), dis.readLong());
            String clientName = dis.readUTF();
            String clientCallbackUrl = dis.readUTF(); // El cliente envía su propia URL de callback

            clientCallbackUrls.put(clientId, clientCallbackUrl);

            ServerPlayer serverPlayer = new ServerPlayer(clientId, clientName, this.serverLevel);
            this.serverLevel.addPlayer(serverPlayer);

            System.out.println("[REGISTRO] Cliente registrado en servidor: " + clientName + " (" + clientId + ")");

            exchange.sendResponseHeaders(200, -1);
            exchange.close();

            // Registrar contexto para recibir inputs de este cliente por POST
            this.server.createContext("/" + clientId.toString() + "/input", this::handleInputRequest);

        } catch (Exception e) {
            System.err.println("[ERROR] Fallo al registrar cliente: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        }
    }

    private void handleInputRequest(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        try (DataInputStream dis = new DataInputStream(exchange.getRequestBody())) {
            int type = dis.readInt();
            switch(type) {
                case InputPayload.ID -> {
                    InputPayload payload = new InputPayload();
                    payload.deserialize(dis);
                    game.getServerBus().post(payload);
                }
                case GamepadInputPayload.ID -> {
                    GamepadInputPayload payload = new GamepadInputPayload();
                    payload.deserialize(dis);
                    game.getServerBus().post(payload);
                }
            }
        }
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    public void start() {
        this.server.start();
        System.out.println("Servidor central escuchando en http://0.0.0.0:" + server.getAddress().getPort());
    }

    public void stop(int delaySegundos) {
        this.server.stop(delaySegundos);
        this.executor.shutdown();
    }

    @SubscribeEvent
    public void onPlayerInputRequest(InputRequestPayload payload) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(buffer);
            payload.serialize(dos);
            dos.flush();
            byte[] payloadBytes = buffer.toByteArray();

            HttpClient httpClient = HttpClient.newHttpClient();
            for (Map.Entry<UUID, String> entry : clientCallbackUrls.entrySet()) {
                UUID playerUUID = entry.getKey();
                String callbackUrl = entry.getValue();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(callbackUrl + "/" + playerUUID.toString() + "/output"))
                        .header("Content-Type", "application/octet-stream")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(payloadBytes))
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Fallo al enviar solicitud de input a clientes: " + e.getMessage());
        }
    }
}
