package com.miranoserqu.miras_space.server;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.client.ClientLevel;
import com.miranoserqu.miras_space.client.ClientPlayer;
import com.miranoserqu.miras_space.event.SubscribeEvent;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.network.GamepadInputPayload;
import com.miranoserqu.miras_space.network.InputPayload;
import com.miranoserqu.miras_space.network.InputRequestPayload;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;

import java.io.*;
import java.util.UUID;

/**
 * HTTPS server-side game level handler.
 * Acts as a bridge between the remote server and the client, listening for payloads
 * and dispatching them to the event bus.
 */
public class HttpsServerLevel extends Level implements Server {

    private final HttpsServer server;
    private final ClientPlayer player;
    private boolean running;

    public HttpsServerLevel(HttpsServer server, ClientPlayer player, Game game) {
        super(game);
        this.player = player;
        this.server = server;
        this.running = false;

        if (this.server != null) {
            this.server.createContext("/" + player.getUuid().toString() + "/output/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        try (InputStream is = exchange.getRequestBody();
                             DataInputStream dis = new DataInputStream(is)) {

                            int type = dis.readInt();
                            switch(type){
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
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        exchange.sendResponseHeaders(200, -1);
                    } else {
                        exchange.sendResponseHeaders(405, -1);
                    }
                }
            });
        }
    }

    @Override
    public ServerPlayer getPlayer(UUID uuid) {
        // Not needed for a bridge
        return null;
    }

    @Override
    public void addPlayer(ServerPlayer serverPlayer) {
        // Not needed for a bridge
    }

    @Override
    public void tick() {
        // Bridge doesn't tick physics itself, it reacts to incoming payloads
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        if (this.server != null) {
            this.server.start();
        }
    }

    @Override
    public void stop() {
        running = false;
        if (this.server != null) {
            this.server.stop(0);
        }
    }

    @SubscribeEvent
    public void onPlayerInputRequest(InputRequestPayload payload){
        this.server.createContext("/input/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                DataOutputStream dos = new DataOutputStream(buffer);
                payload.serialize(dos);
                dos.flush();

                byte[] responseBytes = buffer.toByteArray();
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        });
    }

}
