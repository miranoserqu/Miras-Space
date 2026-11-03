package com.miranoserqu.miras_space.server;

import com.miranoserqu.miras_space.Game;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpsServerLevelTest {

    @Test
    void testHttpsServerLevelBridgeMethods() {
        Game game = new Game();
        HttpsServerLevel level = new HttpsServerLevel(null, game);

        // Verify bridge methods
        assertNull(level.getPlayer(null));
        assertDoesNotThrow(() -> level.tick());
        assertDoesNotThrow(() -> level.start());
        assertDoesNotThrow(() -> level.stop());
    }
}
