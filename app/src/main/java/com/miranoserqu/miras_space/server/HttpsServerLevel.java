package com.miranoserqu.miras_space.server;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.network.GamepadInputPayload;
import com.miranoserqu.miras_space.network.InputPayload;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * HTTPS server-side game level handler.
 * Acts as a bridge between the remote server and the client, listening for payloads
 * and dispatching them to the event bus.
 */
public class HttpsServerLevel extends Level implements Server {

    private final HttpsServer server;
    private boolean running;

    public HttpsServerLevel(HttpsServer server, Game game) {
        super(game);
        this.server = server;
        this.running = false;

        if (this.server != null) {
            this.server.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        try (InputStream is = exchange.getRequestBody();
                             DataInputStream dis = new DataInputStream(is)) {

                            int type = dis.readByte();
                            if (type == 0) {
                                InputPayload payload = new InputPayload(0, 0, null, 0, null, false);
                                payload.deserialize(dis);
                                // Relay to client bus so the client reacts to server state/input updates
                                game.getClientBus().post(payload);
                            } else if (type == 1) {
                                GamepadInputPayload payload = new GamepadInputPayload(0, 0, false, false, false, false, false, false, false, false, false, false, false, false, false);
                                payload.deserialize(dis);
                                // Relay to client bus
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
}
