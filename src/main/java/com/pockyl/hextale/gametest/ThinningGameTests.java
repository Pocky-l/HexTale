package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.thevoid.Thinning;

/** The Thinning: magic wears a chunk thin, the land around catches a share of it, and nothing heals by itself. */
public final class ThinningGameTests {
    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private ThinningGameTests() {
    }

    @HexTest
    public static void stagesFollowTheValue(GameTestHelper helper) {
        float safe = Thinning.SAFE;
        helper.assertTrue(Thinning.Stage.of(0) == Thinning.Stage.CLEAN, "nothing is clean");
        helper.assertTrue(Thinning.Stage.of(safe + 99) == Thinning.Stage.CLEAN, "the safe part and a little more are clean");
        helper.assertTrue(Thinning.felt(safe - 1) == 0 && Thinning.felt(safe + 10) == 10, "only what lies past the safe part is felt");
        helper.assertTrue(Thinning.Stage.of(safe + 100) == Thinning.Stage.HAZE, "100 past it is haze");
        helper.assertTrue(Thinning.Stage.of(safe + 450) == Thinning.Stage.MIASMA, "450 past it is miasma");
        helper.assertTrue(Thinning.Stage.of(safe + 700) == Thinning.Stage.BLIGHT, "700 past it is blight");
        helper.assertTrue(Thinning.Stage.of(Thinning.MAX) == Thinning.Stage.BREACH, "the most is a breach");
        helper.succeed();
    }

    @HexTest
    public static void essencePouredAwayWearsTheWorldThin(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        LevelChunk chunk = level.getChunkAt(pos);
        Thinning.set(level, chunk, 0);
        Thinning.poured(level, pos, EssenceMix.unknown(40), 40, false);
        float lost = Thinning.get(chunk);
        helper.assertTrue(Math.abs(lost - 40 * Thinning.POURED_LOST * 2) < 0.01F, "unknown essence lost on the ground counts double, got " + lost);
        Thinning.set(level, chunk, 0);
        Thinning.poured(level, pos, EssenceMix.unknown(40), 40, true);
        float soaked = Thinning.get(chunk);
        helper.assertTrue(soaked > 0 && soaked < lost / 4, "essence the stone drinks wears the world much less, got " + soaked);
        Thinning.set(level, chunk, 0);
        helper.succeed();
    }

    @HexTest
    public static void theLandAroundCatchesItAndNothingHeals(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        LevelChunk centre = level.getChunkAt(pos);
        LevelChunk east = level.getChunkAt(pos.east(16));
        Thinning.setAround(level, pos, 2, 0);
        Thinning.set(level, centre, 500);
        for (int i = 0; i < 400; i++) {
            Thinning.flow(level);
        }
        float near = Thinning.get(east);
        helper.assertTrue(Thinning.get(centre) == 500, "the sick chunk keeps all of it, got " + Thinning.get(centre));
        helper.assertTrue(near > 500 * Thinning.FALLOFF - 10 && near <= 500 * Thinning.FALLOFF, "the land beside settles at its share, got " + near);
        float far = Thinning.get(level.getChunkAt(pos.east(32)));
        helper.assertTrue(far > 0 && far < near, "and the ring after it at less, got " + far);
        Thinning.set(level, centre, 50);
        Thinning.flow(level);
        helper.assertTrue(Thinning.get(centre) >= 50, "weak haze does not heal by itself");
        Thinning.setAround(level, pos, 8, 0);
        helper.succeed();
    }
}
