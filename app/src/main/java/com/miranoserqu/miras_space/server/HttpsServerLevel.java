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

/**
 * HTTP server-side game level handler.
 * Acts as a bridge between the remote server and the client, listening for payloads
 * and dispatching them to the event bus.
 */
public class HttpsServerLevel extends Level {

    private final HttpServer server;
    private final ClientPlayer player;
    private final Queue<Payload> pendingPayloads;
    private boolean running;

    public HttpsServerLevel(HttpServer server, ClientPlayer player, Game game, String remoteServerUrl) throws IOException, InterruptedException {
        super(game);
        this.player = player;
        this.server = server;
        this.running = false;
        this.pendingPayloads = new ConcurrentLinkedQueue<>();

        if (this.server != null) {
            this.server.createContext("/" + player.getUuid().toString() + "/output", this::handleOutputRequest);
            this.server.createContext("/" + this.player.getUuid().toString() + "/input", this::handleInputRequest);

            this.registerOntoServer(remoteServerUrl);
        }

        game.registerServer(this);
    }

    public HttpsServerLevel(HttpServer server, ClientPlayer player, Game game) throws IOException {
        super(game);
        this.player = player;
        this.server = server;
        this.running = false;
        this.pendingPayloads = new ConcurrentLinkedQueue<>();
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

    private void handleInputRequest(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        Payload payload = pendingPayloads.poll();

        if (payload != null) {
            sendPayload(exchange, payload);
        } else {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        }
    }

    private void registerOntoServer(String remoteServerUrl) throws IOException, InterruptedException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(buffer);
        UUID uuid = this.player.getUuid();
        dos.writeLong(uuid.getMostSignificantBits());
        dos.writeLong(uuid.getLeastSignificantBits());
        dos.writeUTF(this.player.getName());
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
            System.out.println("[CLIENTE] Player registered successfully with UUID: " + uuid);
        } else {
            throw new RuntimeException("Error registering client. Código HTTP: " + response.statusCode());
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

    private void sendPayload(HttpExchange exchange, Payload payload) throws IOException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(buffer);

            payload.serialize(dos);
            dos.flush();

            byte[] responseBytes = buffer.toByteArray();
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e) {
            System.err.println("Error serializing payload: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        }
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
