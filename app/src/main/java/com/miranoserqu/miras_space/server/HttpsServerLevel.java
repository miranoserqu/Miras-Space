package com.miranoserqu.miras_space.server;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.client.ClientPlayer;
import com.miranoserqu.miras_space.event.SubscribeEvent;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.network.GamepadInputPayload;
import com.miranoserqu.miras_space.network.InputPayload;
import com.miranoserqu.miras_space.network.InputRequestPayload;
import com.miranoserqu.miras_space.network.Payload;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HttpsServerLevel extends Level {

    private final HttpServer server;
    private final ClientPlayer player;
    private final Queue<Payload> pendingPayloads;
    private final ScheduledExecutorService scheduler;
    private String remoteServerUrl;

    public HttpsServerLevel(HttpServer server, ClientPlayer player, Game game, String remoteServerUrl) throws IOException, InterruptedException {
        super(game);
        this.player = player;
        this.server = server;
        this.remoteServerUrl = remoteServerUrl;
        this.pendingPayloads = new ConcurrentLinkedQueue<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        if (this.server != null) {
            this.server.createContext("/" + player.getUuid().toString() + "/output", this::handleOutputRequest);

            this.registerOntoServer(remoteServerUrl);

            this.scheduler.scheduleAtFixedRate(this::flushPendingPayloads, 0, 16, TimeUnit.MILLISECONDS);
        }
    }

    public HttpsServerLevel(HttpServer server, ClientPlayer player, Game game) throws IOException {
        super(game);
        this.player = player;
        this.server = server;
        this.pendingPayloads = new ConcurrentLinkedQueue<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public ServerPlayer getPlayer(UUID uuid) {
        return null;
    }

    public void tick() {}

    public void start() {}

    public void stop() {}

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

    private void registerOntoServer(String remoteServerUrl) throws IOException, InterruptedException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(buffer);
        UUID uuid = this.player.getUuid();
        dos.writeLong(uuid.getMostSignificantBits());
        dos.writeLong(uuid.getLeastSignificantBits());
        dos.writeUTF(this.player.getName());
        dos.writeUTF(this.getServerUrl());

        dos.flush();

        byte[] payload = buffer.toByteArray();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(remoteServerUrl + "/register"))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        if (response.statusCode() == 200) {
            System.out.println("[CLIENTE] Registrado con éxito en el servidor con UUID: " + uuid);
        } else {
            throw new RuntimeException("Error registrando cliente. Código HTTP: " + response.statusCode());
        }
    }

    private void flushPendingPayloads() {
        Payload payload;
        while ((payload = pendingPayloads.poll()) != null) {
            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(buffer);
                payload.serialize(dos);
                dos.flush();

                byte[] bytes = buffer.toByteArray();
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(remoteServerUrl + "/" + player.getUuid().toString() + "/input"))
                        .header("Content-Type", "application/octet-stream")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                System.err.println("[CLIENTE] Error enviando input al servidor: " + e.getMessage());
            }
        }
    }

    private void handleOutputRequest(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        InputStream is = exchange.getRequestBody();
        DataInputStream dis = new DataInputStream(is);
        int type = dis.readInt();
        switch (type) {
            case InputRequestPayload.ID -> {
                InputRequestPayload payload = new InputRequestPayload();
                payload.deserialize(dis);
                game.getServerBus().post(payload);
            }
        }
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    @SubscribeEvent
    public void onPlayerInput(InputPayload payload){
        this.pendingPayloads.add(payload);
    }

    @SubscribeEvent
    public void onPlayerGamepadInput(GamepadInputPayload payload){
        this.pendingPayloads.add(payload);
    }
}
