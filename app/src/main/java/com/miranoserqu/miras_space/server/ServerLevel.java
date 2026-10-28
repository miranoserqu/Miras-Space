package com.miranoserqu.miras_space.server;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.Option;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.game.Player;

import java.util.*;

/**
 * Server-side game logic handler.
 * Processes player input and updates game state.
 */
public class ServerLevel extends Level {

    private static final int TICKS_PER_SECOND = 60;
    private static final int TICK_INTERVAL_MS = 1000 / TICKS_PER_SECOND;

    private long lastTick;
    private double tickAccumulator = 0;

    private Map<UUID, ServerPlayer> players;
    private boolean running;
    private Thread gameThread;

    /**
     * Creates a new game server.
     */
    public ServerLevel(Game game) {
        super(Option.WEIGHT_RESOLUTION.getValue(), Option.HEIGHT_RESOLUTION.getValue(), game);
        this.players = new HashMap<>();
        this.running = false;
    }

    /**
     * Adds a player to the game.
     *
     * @param serverPlayer the player to add
     */
    public void addPlayer(ServerPlayer serverPlayer) {
        players.put(serverPlayer.getUuid(), serverPlayer);
        this.game.getServerBus().register(serverPlayer);
    }

    public ServerPlayer getPlayer(UUID uuid){
        return this.players.get(uuid);
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
        for(ServerPlayer player : this.players.values()){
            player.tick();
        }
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