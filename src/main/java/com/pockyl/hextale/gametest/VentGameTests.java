package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;

import com.pockyl.hextale.block.MiasmaVentBlock;
import com.pockyl.hextale.block.MiasmaVentBlockEntity;
import com.pockyl.hextale.registry.ModBlocks;

/** The Miasma Vent: free fumes that eat the hostile (the undead too) and spare the rest; redstone shuts it. */
public final class VentGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private VentGameTests() {
    }

    private static void floor(GameTestHelper helper) {
        int reach = MiasmaVentBlockEntity.RADIUS + 1;
        for (int x = -reach; x <= reach; x++) {
            for (int z = -reach; z <= reach; z++) {
                helper.setBlock(BASE.offset(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= 4; y++) {
                    helper.setBlock(BASE.offset(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    /**
     * With no fuel anywhere, the first breath poisons and hurts a creeper at the edge of the cloud and kills a weak zombie
     * (poison alone could not touch it); a cow in the cloud is left whole. (Only the first breath is checked: in some test
     * runs the creatures do not tick, and an untouched hurt cooldown would turn the next blows away.)
     */
    @HexTest(maxTicks = 120, padding = 6)
    public static void theVentChokesTheHostileForFree(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(BASE, ModBlocks.MIASMA_VENT.get());
        int edge = MiasmaVentBlockEntity.RADIUS;
        Creeper creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, BASE.offset(edge, 0, 0));
        float creeperHealth = creeper.getHealth();
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, BASE.offset(-edge, 0, edge));
        zombie.setHealth(MiasmaVentBlockEntity.DAMAGE - 1);
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, BASE.offset(0, 0, -2));
        float cowHealth = cow.getHealth();
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(!zombie.isAlive(), "the fumes ate the undead zombie");
            helper.assertTrue(creeper.hasEffect(MobEffects.POISON) && creeper.getHealth() < creeperHealth, "the creeper breathed poison");
            helper.assertTrue(cow.getHealth() == cowHealth, "the cow only coughs");
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(cow.isAlive() && cow.getHealth() == cowHealth, "the cow is still whole");
            helper.succeed();
        });
    }

    /** A redstone signal holds the stack shut: the weak spider over it lives. */
    @HexTest(maxTicks = 120, padding = 6)
    public static void redstoneShutsTheVent(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(BASE, ModBlocks.MIASMA_VENT.get());
        helper.setBlock(BASE.west(), Blocks.REDSTONE_BLOCK);
        Spider spider = helper.spawnWithNoFreeWill(EntityType.SPIDER, BASE.offset(2, 0, 0));
        spider.setHealth(1);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(helper.getBlockState(BASE).getValue(MiasmaVentBlock.POWERED), "the vent sees the signal");
            helper.assertTrue(spider.isAlive() && spider.getHealth() == 1, "a shut vent breathes nothing");
            helper.succeed();
        });
    }
}
