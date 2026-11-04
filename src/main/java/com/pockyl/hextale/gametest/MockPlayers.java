package com.pockyl.hextale.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * A creative server player joined to the test level, as the tests need a real {@link ServerPlayer} (knowledge, packets,
 * inventory).
 *
 * <p>1.21.1: the vanilla helper is not deprecated here, so this is a plain delegate.
 */
final class MockPlayers {
    private MockPlayers() {
    }

    static ServerPlayer inLevel(GameTestHelper helper) {
        return helper.makeMockServerPlayerInLevel();
    }
}
