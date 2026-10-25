package com.miranoserqu.miras_space.server;

import com.miranoserqu.miras_space.game.Player;
import com.miranoserqu.miras_space.network.Payload;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side game logic handler.
 * Processes player input and updates game state.
 */
public class GameServer {
    private static final int TICKS_PER_SECOND = 60;
    private static final int TICK_INTERVAL_MS = 1000 / TICKS_PER_SECOND;

    private List<Player> players;
    private boolean running;
    private Thread gameThread;

    /**
     * Creates a new game server.
     */
    public GameServer() {
        this.players = new ArrayList<>();
        this.running = false;
    }

    /**
     * Adds a player to the game.
     *
     * @param player the player to add
     */
    public void addPlayer(Player player) {
        players.add(player);
    }

    /**
     * Processes a player input payload and updates the corresponding player.
     *
     * @param payload the input payload from client
     */
    public void processPlayerInput(Payload payload) {
        if (payload.getType() == Payload.PLAYER_INPUT) {
            int playerId = payload.getPlayerId();
            List<Integer> pressedKeys = payload.getPressedKeys();
            double aimAngle = payload.getAimAngle();

            if (playerId >= 0 && playerId < players.size()) {
                Player player = players.get(playerId);
                // Orientation is authoritative on the server: it turns towards the aim
                // angle at the rotation speed, then the thrusters use that orientation.
                player.updateOrientation(aimAngle);
                player.update(pressedKeys, payload.isAimTargetSet(), payload.getAimTargetX(), payload.getAimTargetY(), payload.isInvertControls());
            }
        }
    }

    /**
     * Updates all players (called once per tick).
     * This method is now a no-op since Player.update() handles the logic directly.
     * Kept for interface compatibility.
     */
    public void update() {
        // Update logic is handled in processPlayerInput now
    }

    /**
     * Gets player update payloads for all players.
     *
     * @return list of player update payloads
     */
    public List<Payload> getPlayerUpdates() {
        List<Payload> updates = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Payload payload = Payload.createPlayerUpdate(i, player.getX(), player.getY(), player.getOrientation(), player.getVelocityX(), player.getVelocityY(), player.getAccelerationX(), player.getAccelerationY());
            updates.add(payload);
        }
        return updates;
    }

    /**
     * Starts the game server loop.
     */
    public void start() {
        if (running) {
            return;
        }

        running = true;
        gameThread = new Thread(() -> {
            long lastTick = System.currentTimeMillis();

            while (running) {
                long now = System.currentTimeMillis();
                long elapsed = now - lastTick;

                if (elapsed >= TICK_INTERVAL_MS) {
                    tick();
                    lastTick = now;
                }

                // Sleep to avoid consuming too much CPU
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        gameThread.start();
    }

    /**
     * Stops the game server loop.
     */
    public void stop() {
        running = false;
        if (gameThread != null) {
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Performs one game tick: update logic and prepare state for clients.
     * Note: Actual game logic is handled in processPlayerInput which is called from the client tick.
     */
    private void tick() {
        // In a real implementation with multiple clients, we would:
        // 1. Process any received inputs
        // 2. Update game logic
        // 3. Send updates to connected clients
        // For now, the client drives the tick process
    }

    /**
     * Gets the number of ticks per second.
     *
     * @return ticks per second
     */
    public static int getTicksPerSecond() {
        return TICKS_PER_SECOND;
    }
}