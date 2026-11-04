package com.miranoserqu.miras_space.client;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.event.SubscribeEvent;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.network.GamepadInputPayload;
import com.miranoserqu.miras_space.network.InputPayload;
import com.miranoserqu.miras_space.network.InputRequestPayload;
import com.miranoserqu.miras_space.server.HttpsServerLevel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsServer;

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

    private HttpsServer server;
    private ThreadPoolExecutor executor;
    private final Map<UUID, HttpsServerLevel> clients = new ConcurrentHashMap<>();

    public HttpsClientLevel(Game game, int port) throws IOException {
        super(game);

        this.server = HttpsServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        this.server.setExecutor(executor);

        this.server.createContext("/register", this::handleClientRegistration);

        game.registerClient(this);
    }

    public String getServerUrl() {
        InetSocketAddress address = this.server.getAddress();

        // Si no está iniciado o socket no vinculado, retornamos vacío o fallback
        if (address == null) {
            throw new IllegalStateException("El servidor aún no ha sido vinculado a una dirección.");
        }

        String host = address.getHostString();

        // Si el servidor escucha en todas las interfaces ("0.0.0.0" o "::"),
        // usamos "127.0.0.1" para peticiones locales.
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
            // Leer el ID con el que se identifica el nuevo cliente
            UUID clientId = new UUID(dis.readLong(), dis.readLong());
            String clientName = dis.readUTF();

            // Registrar o recuperar cliente de forma atómica
            HttpsServerLevel client = clients.computeIfAbsent(clientId, id -> {
                System.out.println("[REGISTRO] Nuevo cliente vinculado: " + id);
                try {
                    return new HttpsServerLevel(this.server, new ClientPlayer(id, clientName, this), this.game);
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            // Confirmar registro exitoso (200 OK)
            exchange.sendResponseHeaders(200, -1);
            exchange.close();

            this.server.createContext("/" + clientId.toString() + "/output/");
            this.server.createContext("/" + clientId.toString() + "/input/", this::handleInputRequest);

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
    }

    public void start() {
        this.server.start();
        System.out.println("Servidor central escuchando en http://127.0.0.1:" + server.getAddress().getPort());
    }

    public void stop(int delaySegundos) {
        this.server.stop(delaySegundos);
        this.executor.shutdown();
    }

    public Map<UUID, HttpsServerLevel> getClients() {
        return this.clients;
    }

    public HttpsServerLevel getClient(UUID uuid) {
        return this.clients.get(uuid);
    }

    @SubscribeEvent
    public void onPlayerInputRequest(InputRequestPayload payload) throws IOException, InterruptedException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(buffer);
        payload.serialize(dos);
        dos.flush();

        byte[] payloadBytes = buffer.toByteArray();

        for(UUID playerUUID : this.clients.keySet()) {
            // 2. Enviar petición HTTP POST al endpoint /register
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.getServerUrl() + "/" + playerUUID.toString() + "/output"))
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payloadBytes))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 200) {
                System.out.println("[SERVER] Input request sent successfully to player with UUID: " + playerUUID);
            } else {
                throw new RuntimeException("Error when sending input request. HTTP code: " + response.statusCode());
            }
        }
    }

}
