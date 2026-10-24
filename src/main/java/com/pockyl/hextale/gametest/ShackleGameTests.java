package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
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
     * A cow on a plate with a jar of shadow by it is taken hold of and does not move for seconds. Two blows a little
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
        // A full block: monsters spawn on it like on any floor.
        helper.assertTrue(helper.getBlockState(BASE).isValidSpawn(helper.getLevel(), helper.absolutePos(BASE), EntityType.ZOMBIE),
                "a zombie may spawn on the shackles");
        BlockPos jarPos = BASE.offset(-2, 0, -2);
        helper.setBlock(jarPos, ModBlocks.ESSENCE_TANK.get());
        helper.<EssenceTankBlockEntity>getBlockEntity(jarPos).tank().set(EssenceMix.pure(Essence.SHADOW, 8));
        RuneShacklesBlockEntity plate = helper.<RuneShacklesBlockEntity>getBlockEntity(BASE);
        Cow cow = helper.spawn(EntityType.COW, new Vec3(1.5, 2.0, 1.5));
        Vec3[] at = new Vec3[1];
        float[] health = new float[1];
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(cow.hasData(ModAttachments.SHACKLE), "the plate took hold of the cow");
            helper.assertValueEqual(plate.held().length, 1, "creatures held");
            at[0] = cow.position();
            health[0] = cow.getHealth();
            cow.hurt(helper.getLevel().damageSources().generic(), 2);
        });
        helper.runAfterDelay(60, () -> {
            cow.hurt(helper.getLevel().damageSources().generic(), 2);
            helper.assertTrue(cow.getHealth() <= health[0] - 4 + 0.01F, "both blows hurt the held cow (" + health[0] + " -> " + cow.getHealth() + ", held " + cow.hasData(ModAttachments.SHACKLE) + ", at " + cow.position() + ")");
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(cow.position().distanceTo(at[0]) < 0.01, "a held cow does not move");
            helper.setBlock(BASE, Blocks.AIR);
        });
        helper.runAfterDelay(140, () -> {
            helper.assertTrue(!cow.hasData(ModAttachments.SHACKLE), "with the plate gone the hold ran out");
            helper.succeed();
        });
    }
}
