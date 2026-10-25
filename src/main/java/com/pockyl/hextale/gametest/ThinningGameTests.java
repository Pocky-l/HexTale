package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.thevoid.Thinning;

/** The Thinning: careless magic wears a chunk thin, the sickness flows into the land around, and weak haze fades. */
public final class ThinningGameTests {
    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private ThinningGameTests() {
    }

    @HexTest
    public static void stagesFollowTheValue(GameTestHelper helper) {
        helper.assertTrue(Thinning.Stage.of(0) == Thinning.Stage.CLEAN, "nothing is clean");
        helper.assertTrue(Thinning.Stage.of(99) == Thinning.Stage.CLEAN, "just under the haze is clean");
        helper.assertTrue(Thinning.Stage.of(100) == Thinning.Stage.HAZE, "100 is haze");
        helper.assertTrue(Thinning.Stage.of(450) == Thinning.Stage.MIASMA, "450 is miasma");
        helper.assertTrue(Thinning.Stage.of(700) == Thinning.Stage.BLIGHT, "700 is blight");
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
    public static void theSicknessFlowsAndWeakHazeFades(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        LevelChunk centre = level.getChunkAt(pos);
        LevelChunk east = level.getChunkAt(pos.east(16));
        Thinning.setAround(level, pos, 1, 0);
        Thinning.set(level, centre, 500);
        Thinning.flow(level);
        float left = Thinning.get(centre);
        float given = Thinning.get(east);
        helper.assertTrue(given > 0, "the land beside gets some of it");
        helper.assertTrue(left < 500 && left > 300, "the source keeps most of it, got " + left);
        helper.assertTrue(Math.abs(left + 4 * given - 500) < 0.5F, "nothing is lost while it flows above the haze, got " + left + " + 4 x " + given);
        Thinning.setAround(level, pos, 1, 0);
        Thinning.set(level, centre, 50);
        Thinning.flow(level);
        helper.assertTrue(Thinning.get(centre) < 50 - Thinning.FADE * 0.5F, "weak haze fades by itself");
        Thinning.setAround(level, pos, 1, 0);
        helper.succeed();
    }
}
