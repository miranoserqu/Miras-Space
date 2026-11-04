package com.miranoserqu.miras_space.server;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.client.ClientPlayer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HttpsServerLevelTest {

    @Test
    void testHttpsServerLevelBridgeMethods() throws IOException {
        Game game = new Game();
        HttpsServerLevel level = new HttpsServerLevel(null, null, game);

        // Verify bridge methods
        assertNull(level.getPlayer(null));
        assertDoesNotThrow(level::tick);
        assertDoesNotThrow(level::start);
        assertDoesNotThrow(level::stop);
    }
}
