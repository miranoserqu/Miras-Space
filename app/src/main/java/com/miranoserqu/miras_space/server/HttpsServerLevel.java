package com.miranoserqu.miras_space.server;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.Option;
import com.miranoserqu.miras_space.entity.Entity;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.network.GamepadInputPayload;
import com.miranoserqu.miras_space.network.InputPayload;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HTTPS server-side game level handler.
 * Listens for payloads from a remote server/client via HTTPS and processes them.
 */
public class HttpsServerLevel extends Level implements Server {

    protected final double boundaryX;
    protected final double boundaryY;

    private final HttpsServer server;
    private final Map<UUID, ServerPlayer> players;
    private final Map<UUID, Entity> entities;
    private boolean running;

    public HttpsServerLevel(HttpsServer server, Game game) {
        super(game);
        this.server = server;
        this.boundaryX = Option.WEIGHT_RESOLUTION.getValue();
        this.boundaryY = Option.HEIGHT_RESOLUTION.getValue();
        this.players = new HashMap<>();
        this.entities = new HashMap<>();
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
                                game.getServerBus().post(payload);
                            } else if (type == 1) {
                                GamepadInputPayload payload = new GamepadInputPayload(0, 0, false, false, false, false, false, false, false, false, false, false, false, false, false);
                                payload.deserialize(dis);
                                game.getServerBus().post(payload);
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
        return this.players.get(uuid);
    }

    @Override
    public void addPlayer(ServerPlayer serverPlayer) {
        players.put(serverPlayer.getUuid(), serverPlayer);
        this.game.getServerBus().register(serverPlayer);
        serverPlayer.setLevel(this);
    }

    @Override
    public void tick() {
        for (ServerPlayer player : this.players.values()) {
            player.tick();
        }
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

    @Override
    public double getBoundaryX() {
        return this.boundaryX;
    }

    @Override
    public double getBoundaryY() {
        return this.boundaryY;
    }
}
