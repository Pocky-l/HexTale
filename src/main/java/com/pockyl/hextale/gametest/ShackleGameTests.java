package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.ChalkSigilBlock;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

/** Binding Chalk: a closed figure holds a creature standing in it (still, but it can be hurt), an open one does not. */
public final class ShackleGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private ShackleGameTests() {
    }

    /** The eight cells round {@code centre}, a 3x3 square line. */
    static BlockPos[] ring(BlockPos centre) {
        BlockPos[] cells = new BlockPos[8];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    cells[i++] = centre.offset(dx, 0, dz);
                }
            }
        }
        return cells;
    }

    /**
     * A ring drawn with Binding Chalk (through the real click path) round a cow, a jar of shadow by it: the closed figure
     * wakes and the cow is held still; two blows a little apart both hurt it. With one cell wiped the figure opens and
     * the cow is free again.
     */
    @HexTest(maxTicks = 200, padding = 2)
    public static void aClosedFigureHoldsACreature(GameTestHelper helper) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                helper.setBlock(BASE.offset(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= 3; y++) {
                    helper.setBlock(BASE.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        BlockPos jarPos = BASE.offset(-3, 0, -3);
        helper.setBlock(jarPos, ModBlocks.ESSENCE_TANK.get());
        helper.<EssenceTankBlockEntity>getBlockEntity(jarPos).tank().set(EssenceMix.pure(Essence.SHADOW, 8));
        net.minecraft.server.level.ServerPlayer player = GameTests.watcher(helper, helper.absolutePos(BASE));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BINDING_CHALK.get()));
        BlockPos[] cells = ring(BASE);
        for (BlockPos cell : cells) {
            BlockPos floor = helper.absolutePos(cell.below());
            player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), net.minecraft.core.Direction.UP, floor, false));
        }
        for (BlockPos cell : cells) {
            helper.assertBlockPresent(ModBlocks.CHALK_SIGIL.get(), cell);
        }
        Cow cow = helper.spawn(EntityType.COW, new Vec3(1.5, 1.0, 1.5));
        Vec3[] at = new Vec3[1];
        float[] health = new float[1];
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(helper.getBlockState(cells[0]).getValue(ChalkSigilBlock.ACTIVE), "the closed figure woke");
            helper.assertTrue(cow.hasData(ModAttachments.SHACKLE), "the figure took hold of the cow");
            at[0] = cow.position();
            health[0] = cow.getHealth();
            cow.hurt(helper.getLevel().damageSources().generic(), 2);
        });
        helper.runAfterDelay(60, () -> {
            cow.hurt(helper.getLevel().damageSources().generic(), 2);
            helper.assertTrue(cow.getHealth() <= health[0] - 4 + 0.01F, "both blows hurt the held cow (" + health[0] + " -> " + cow.getHealth() + ")");
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(cow.position().distanceTo(at[0]) < 0.01, "a held cow does not move");
            helper.setBlock(cells[3], Blocks.AIR);
        });
        helper.runAfterDelay(140, () -> {
            helper.assertTrue(!helper.getBlockState(cells[0]).getValue(ChalkSigilBlock.ACTIVE), "the opened figure sleeps");
            helper.assertTrue(!cow.hasData(ModAttachments.SHACKLE), "with the figure open the hold ran out");
            cow.discard();
            helper.succeed();
        });
    }
}
