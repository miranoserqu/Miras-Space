package com.miranoserqu.miras_space.server;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.Option;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpsServerLevelTest {

    @Test
    void testHttpsServerLevelBasicMethods() {
        Game game = new Game();
        HttpsServerLevel level = new HttpsServerLevel(null, game);

        // Verify boundaries with delta for floating point comparison
        assertEquals(Option.WEIGHT_RESOLUTION.getValue(), level.getBoundaryX(), 0.001);
        assertEquals(Option.HEIGHT_RESOLUTION.getValue(), level.getBoundaryY(), 0.001);

        // Test initial state
        assertNull(level.getPlayer(null));
        assertDoesNotThrow(() -> level.tick());
        assertDoesNotThrow(() -> level.start());
        assertDoesNotThrow(() -> level.stop());
    }
}
