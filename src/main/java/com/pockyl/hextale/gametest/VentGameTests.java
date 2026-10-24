package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.block.Blocks;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.MiasmaVentBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlocks;

/** The Miasma Vent: dry without unknown essence, choking with it. */
public final class VentGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private VentGameTests() {
    }

    /** A weak spider over a vent with no unknown essence stays alive; once a jar of the murk stands by, the fumes kill it. */
    @HexTest(maxTicks = 200)
    public static void theVentChokesTheHostile(GameTestHelper helper) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                helper.setBlock(BASE.offset(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= 4; y++) {
                    helper.setBlock(BASE.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        helper.setBlock(BASE, ModBlocks.MIASMA_VENT.get());
        MiasmaVentBlockEntity vent = helper.<MiasmaVentBlockEntity>getBlockEntity(BASE);
        Spider spider = helper.spawnWithNoFreeWill(EntityType.SPIDER, BASE.above());
        spider.setHealth(MiasmaVentBlockEntity.DAMAGE - 1);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(vent.dry(), "no unknown essence - the vent is dry");
            helper.assertTrue(spider.isAlive(), "no fumes while dry");
            BlockPos jarPos = BASE.offset(-2, 0, -2);
            helper.setBlock(jarPos, ModBlocks.ESSENCE_TANK.get());
            helper.<EssenceTankBlockEntity>getBlockEntity(jarPos).tank().set(EssenceMix.unknown(8));
        });
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(!spider.isAlive(), "the fumes killed the spider");
            helper.assertTrue(!vent.dry(), "fed with the murk");
            helper.succeed();
        });
    }
}
