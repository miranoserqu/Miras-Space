package com.miranoserqu.miras_space.server;

import com.miranoserqu.miras_space.Game;
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
import java.util.ArrayList;
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
    private volatile boolean hasPendingInputRequest = false;

    public HttpsServerLevel(HttpsServer server, ClientPlayer player, Game game) {
        super(game);
        this.player = player;
        this.server = server;
        this.running = false;

        if (this.server != null) {
            // Contexto para recibir payloads del servidor/cliente
            this.server.createContext("/" + player.getUuid().toString() + "/output/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        try (InputStream is = exchange.getRequestBody();
                             DataInputStream dis = new DataInputStream(is)) {

                            int type = dis.readByte();
                            if (type == 0) {
                                InputPayload payload = new InputPayload(0, 0, new ArrayList<>(), 0, new UUID(0, 0), false);
                                payload.deserialize(dis);
                                game.getClientBus().post(payload);
                            } else if (type == 1) {
                                GamepadInputPayload payload = new GamepadInputPayload(0, 0, false, false, false, false, false, false, false, false, false, false, false, false, false);
                                payload.deserialize(dis);
                                game.getClientBus().post(payload);
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

            // Contexto para notificar si hay solicitud de input pendiente (se envía una vez y se limpia)
            this.server.createContext("/input/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (hasPendingInputRequest) {
                        hasPendingInputRequest = false; // Se marca como enviado para que solo se envíe una vez
                        exchange.sendResponseHeaders(200, 0);
                        exchange.getResponseBody().close();
                    } else {
                        exchange.sendResponseHeaders(204, -1); // No content
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public void onPlayerInputRequest(InputRequestPayload payload){
        this.hasPendingInputRequest = true;
    }

    @Override
    public ServerPlayer getPlayer(UUID uuid) {
        return null;
    }

    @Override
    public void addPlayer(ServerPlayer serverPlayer) {
    }

    @Override
    public void tick() {
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
}
