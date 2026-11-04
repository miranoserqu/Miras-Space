package com.miranoserqu.miras_space.client;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.event.SubscribeEvent;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.network.GamepadInputPayload;
import com.miranoserqu.miras_space.network.InputPayload;
import com.miranoserqu.miras_space.network.InputRequestPayload;
import com.miranoserqu.miras_space.server.HttpsServerLevel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class HttpsClientLevel extends Level {

    private HttpServer server;
    private ThreadPoolExecutor executor;
    private final Map<UUID, HttpsServerLevel> clients = new ConcurrentHashMap<>();

    public HttpsClientLevel(Game game, int port) throws IOException {
        super(game);

        this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        this.server.setExecutor(executor);

        this.server.createContext("/register", this::handleClientRegistration);

        game.registerClient(this);
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

            HttpsServerLevel client = clients.computeIfAbsent(clientId, id -> {
                System.out.println("[REGISTRO] Nuevo cliente vinculado: " + id);
                try {
                    return new HttpsServerLevel(this.server, new ClientPlayer(id, clientName, this), this.game);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            exchange.sendResponseHeaders(200, -1);
            exchange.close();

            this.server.createContext("/" + clientId.toString() + "/output", this::handleInputRequest);
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

        InputStream is = exchange.getRequestBody();
        DataInputStream dis = new DataInputStream(is);
        int type = dis.readInt();
        switch(type) {
            case InputPayload.ID -> {
                InputPayload payload = new InputPayload();
                payload.deserialize(dis);
                game.getClientBus().post(payload);
            }
            case GamepadInputPayload.ID -> {
                GamepadInputPayload payload = new GamepadInputPayload();
                payload.deserialize(dis);
                game.getClientBus().post(payload);
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
}
