package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.RuneShacklesBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.rune.Essence;

/** Rune Shackles: a creature on a fed plate hangs still, can still be hurt again and again, and walks off once it is gone. */
public final class ShackleGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private ShackleGameTests() {
    }

    /**
     * A husk on a plate with a jar of shadow by it is taken hold of and does not move for seconds. Two blows a little
     * apart both hurt it (the blink after a hit still wears off while it is held). With the plate broken the hold runs
     * out and it is free again.
     */
    @HexTest(maxTicks = 200)
    public static void theShacklesHoldACreature(GameTestHelper helper) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                helper.setBlock(BASE.offset(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= 3; y++) {
                    helper.setBlock(BASE.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        helper.setBlock(BASE, ModBlocks.RUNE_SHACKLES.get());
        BlockPos jarPos = BASE.offset(-2, 0, -2);
        helper.setBlock(jarPos, ModBlocks.ESSENCE_TANK.get());
        helper.getBlockEntity(jarPos, EssenceTankBlockEntity.class).tank().set(EssenceMix.pure(Essence.SHADOW, 8));
        RuneShacklesBlockEntity plate = helper.getBlockEntity(BASE, RuneShacklesBlockEntity.class);
        Husk husk = helper.spawn(EntityTypes.HUSK, new Vec3(1.5, 1.2, 1.5));
        Vec3[] at = new Vec3[1];
        float[] health = new float[1];
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(husk.hasData(ModAttachments.SHACKLE), "the plate took hold of the husk");
            helper.assertValueEqual(plate.held().length, 1, "creatures held");
            at[0] = husk.position();
            health[0] = husk.getHealth();
            husk.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 2);
        });
        helper.runAfterDelay(60, () -> {
            husk.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 2);
            helper.assertTrue(husk.getHealth() <= health[0] - 4 + 0.01F, "both blows hurt the held husk");
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(husk.position().distanceTo(at[0]) < 0.01, "a held husk does not move");
            helper.setBlock(BASE, Blocks.AIR);
        });
        helper.runAfterDelay(140, () -> {
            helper.assertTrue(!husk.hasData(ModAttachments.SHACKLE), "with the plate gone the hold ran out");
            helper.succeed();
        });
    }
}
