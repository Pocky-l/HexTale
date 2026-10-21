package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.WhirlReaperBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.rune.Essence;

/** The Whirl Reaper: crops reaped in place, fruits lifted off their stems, stalks cut above the root, and no wind - no whirls. */
public final class WhirlReaperGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private WhirlReaperGameTests() {
    }

    /** Clears the field (blocks and loose items outside the 1x1x1 structure outlive their tests) and lays a stone floor. */
    private static void clear(GameTestHelper helper) {
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                helper.setBlock(BASE.offset(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= 5; y++) {
                    helper.setBlock(BASE.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        AABB area = new AABB(helper.absolutePos(BASE)).inflate(9, 6, 9);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
    }

    private static WhirlReaperBlockEntity reaper(GameTestHelper helper, int wind) {
        if (!helper.getBlockState(BASE).is(ModBlocks.WHIRL_REAPER.get())) {
            helper.setBlock(BASE, ModBlocks.WHIRL_REAPER.get());
        }
        if (wind > 0) {
            BlockPos jarPos = BASE.offset(-2, 0, -2);
            helper.setBlock(jarPos, ModBlocks.ESSENCE_TANK.get());
            helper.getBlockEntity(jarPos, EssenceTankBlockEntity.class).tank().set(EssenceMix.pure(Essence.AIR, wind));
        }
        return helper.getBlockEntity(BASE, WhirlReaperBlockEntity.class);
    }

    private static int lying(GameTestHelper helper, Item item, BlockPos around) {
        AABB box = new AABB(helper.absolutePos(around)).inflate(1.5);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).stream().filter(e -> e.getItem().is(item))
                .mapToInt(e -> e.getItem().getCount()).sum();
    }

    /**
     * One field (neighbouring tests stand a few blocks away, so one clearing only): with no wind essence nearby the reaper
     * stands becalmed and nothing is touched; once a jar of wind stands by, ripe wheat is reaped where it stands and grows
     * again from its first age, a young crop is left alone, a melon is lifted off its stem and breaks into slices where it
     * lay, and sugar cane is cut above its root.
     */
    @HexTest(maxTicks = 400)
    public static void theReaperReapsItsField(GameTestHelper helper) {
        clear(helper);
        BlockPos crop = BASE.offset(3, 0, 0);
        BlockPos young = BASE.offset(0, 0, 3);
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        helper.setBlock(young.below(), Blocks.FARMLAND);
        helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        helper.setBlock(young, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3));
        BlockPos stem = BASE.offset(-3, 0, 2);
        BlockPos melon = stem.east();
        helper.setBlock(stem.below(), Blocks.FARMLAND);
        helper.setBlock(melon.below(), Blocks.DIRT);
        helper.setBlock(melon, Blocks.MELON);
        helper.setBlock(stem, Blocks.ATTACHED_MELON_STEM.defaultBlockState().setValue(AttachedStemBlock.FACING, Direction.EAST));
        BlockPos cane = BASE.offset(3, 0, 2);
        helper.setBlock(cane.below(), Blocks.SAND);
        helper.setBlock(cane.below(2), Blocks.STONE);
        helper.setBlock(cane.below(2).east(), Blocks.STONE);
        helper.setBlock(cane.below().east(), Blocks.WATER);
        for (int dy = 0; dy < 3; dy++) {
            helper.setBlock(cane.above(dy), Blocks.SUGAR_CANE);
        }
        WhirlReaperBlockEntity reaper = reaper(helper, 0);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(reaper.becalmed(), "with no wind it knows it is becalmed");
            helper.assertTrue(reaper.pending().isEmpty(), "no whirl left without wind");
            helper.assertBlockProperty(crop, CropBlock.AGE, 7);
            helper.assertBlockPresent(Blocks.SUGAR_CANE, cane.above(2));
            reaper(helper, 4);
            reaper.wake();
            helper.succeedWhen(() -> {
                helper.assertBlockProperty(crop, CropBlock.AGE, 0);
                helper.assertTrue(lying(helper, Items.WHEAT, crop) >= 1, "the wheat lies where it grew");
                // Still growing on its own (random ticks may age it), never reaped while young.
                helper.assertTrue(helper.getBlockState(young).getValue(CropBlock.AGE) >= 3, "the young wheat is left alone");
                helper.assertBlockPresent(Blocks.AIR, melon);
                helper.assertTrue(lying(helper, Items.MELON_SLICE, melon) >= 3, "the melon broke into slices where it lay");
                helper.assertBlockPresent(Blocks.SUGAR_CANE, cane);
                helper.assertBlockPresent(Blocks.AIR, cane.above());
                helper.assertTrue(lying(helper, Items.SUGAR_CANE, cane) >= 2, "the cut cane lies beside its root");
                EssenceTankBlockEntity jar = helper.getBlockEntity(BASE.offset(-2, 0, -2), EssenceTankBlockEntity.class);
                helper.assertTrue(jar.tank().amount() < 4, "the whirls drank wind from the jar");
            });
        });
    }
}
