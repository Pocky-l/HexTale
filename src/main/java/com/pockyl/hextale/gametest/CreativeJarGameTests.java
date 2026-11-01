package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.ArcSpireBlockEntity;
import com.pockyl.hextale.block.GreedyFunnelBlockEntity;
import com.pockyl.hextale.block.RiftGateBlock;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModBlocks;

/** The mob farm devices draw from a Creative Essence Tank like from any jar. */
public final class CreativeJarGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private CreativeJarGameTests() {
    }

    @HexTest(maxTicks = 200)
    public static void creativeJarFeedsTheFarm(GameTestHelper helper) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                helper.setBlock(BASE.offset(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= 4; y++) {
                    helper.setBlock(BASE.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        helper.setBlock(BASE.offset(-2, 0, -2), ModBlocks.CREATIVE_ESSENCE_TANK.get());
        helper.setBlock(BASE, Blocks.CHEST);
        helper.setBlock(BASE.above(), ModBlocks.GREEDY_FUNNEL.get());
        helper.setBlock(BASE.offset(3, 0, -3), ModBlocks.ARC_SPIRE.get());
        for (BlockPos cell : ShackleGameTests.ring(BASE.offset(-3, 0, 3))) {
            helper.setBlock(cell, ModBlocks.CHALK_SIGIL.get());
        }
        GreedyFunnelBlockEntity funnel = helper.getBlockEntity(BASE.above(), GreedyFunnelBlockEntity.class);
        ArcSpireBlockEntity spire = helper.getBlockEntity(BASE.offset(3, 0, -3), ArcSpireBlockEntity.class);
        helper.runAfterDelay(5, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            Sheep sheep = helper.spawnWithNoFreeWill(EntityTypes.SHEEP, BASE.offset(2, 0, 2));
            sheep.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 1000);
            helper.assertTrue(!funnel.hungry() && funnel.charges() > 0, "the funnel fed on the creative jar (hungry " + funnel.hungry() + ")");
            Spider spider = helper.spawnWithNoFreeWill(EntityTypes.SPIDER, BASE.offset(3, 0, -1));
            spider.setHealth(ArcSpireBlockEntity.DAMAGE - 2);
            Cow held = helper.spawn(EntityTypes.COW, new Vec3(-1.5, 1.0, 4.5));
            spire.wake();
            helper.runAfterDelay(110, () -> {
                helper.assertTrue(!spider.isAlive(), "the spire struck with spark from the creative jar (uncharged " + spire.uncharged() + ")");
                helper.assertTrue(held.hasData(ModAttachments.SHACKLE), "a binding figure holds with shadow from the creative jar");
                helper.succeed();
            });
        });
    }

    /**
     * GitHub issue #5: a gate that went dry noticed filled jars only when something tried to pass, so it kept saying
     * "dry" beside a Creative Essence Jar; a hungry funnel likewise until the next death. Both look again by themselves.
     */
    @HexTest(maxTicks = 200)
    public static void dryDevicesWakeBesideACreativeJar(GameTestHelper helper) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                helper.setBlock(BASE.offset(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= 4; y++) {
                    helper.setBlock(BASE.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        BlockPos gatePos = BASE.offset(-3, 0, -3);
        helper.setBlock(gatePos, ModBlocks.RIFT_GATE.get());
        helper.setBlock(BASE, Blocks.CHEST);
        helper.setBlock(BASE.above(), ModBlocks.GREEDY_FUNNEL.get());
        RiftGateBlock.Gate gate = helper.getBlockEntity(gatePos, RiftGateBlock.Gate.class);
        GreedyFunnelBlockEntity funnel = helper.getBlockEntity(BASE.above(), GreedyFunnelBlockEntity.class);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!gate.afford(helper.getLevel(), RiftGateBlock.Gate.TOLL_PER_MB) && gate.dry(),
                    "with no rift essence the gate is dry");
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            Sheep sheep = helper.spawnWithNoFreeWill(EntityTypes.SHEEP, BASE.offset(2, 0, 2));
            sheep.hurt(helper.getLevel().damageSources().playerAttack(player), 1000);
            helper.assertTrue(funnel.hungry(), "with no decay the funnel is hungry");
            helper.setBlock(BASE.offset(-2, 0, 0), ModBlocks.CREATIVE_ESSENCE_TANK.get());
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(!gate.dry(), "the gate saw the creative jar by itself");
            helper.assertTrue(!funnel.hungry(), "the funnel saw the creative jar by itself");
            helper.succeed();
        });
    }
}
