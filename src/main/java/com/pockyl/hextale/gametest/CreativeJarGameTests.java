package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.ArcSpireBlockEntity;
import com.pockyl.hextale.block.GreedyFunnelBlockEntity;
import com.pockyl.hextale.block.MiasmaVentBlockEntity;
import com.pockyl.hextale.block.RuneShacklesBlockEntity;
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
        helper.setBlock(BASE.offset(-3, 0, 3), ModBlocks.RUNE_SHACKLES.get());
        helper.setBlock(BASE.offset(-3, 0, -1), ModBlocks.MIASMA_VENT.get());
        MiasmaVentBlockEntity vent = helper.getBlockEntity(BASE.offset(-3, 0, -1), MiasmaVentBlockEntity.class);
        GreedyFunnelBlockEntity funnel = helper.getBlockEntity(BASE.above(), GreedyFunnelBlockEntity.class);
        ArcSpireBlockEntity spire = helper.getBlockEntity(BASE.offset(3, 0, -3), ArcSpireBlockEntity.class);
        RuneShacklesBlockEntity plate = helper.getBlockEntity(BASE.offset(-3, 0, 3), RuneShacklesBlockEntity.class);
        helper.runAfterDelay(5, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            Sheep sheep = helper.spawnWithNoFreeWill(EntityType.SHEEP, BASE.offset(2, 0, 2));
            sheep.hurt(helper.getLevel().damageSources().playerAttack(player), 1000);
            helper.assertTrue(!funnel.hungry() && funnel.charges() > 0, "the funnel fed on the creative jar (hungry " + funnel.hungry() + ")");
            Spider spider = helper.spawnWithNoFreeWill(EntityType.SPIDER, BASE.offset(3, 0, -1));
            spider.setHealth(ArcSpireBlockEntity.DAMAGE - 2);
            Cow held = helper.spawn(EntityType.COW, new Vec3(-1.5, 2.0, 4.5));
            Spider choked = helper.spawnWithNoFreeWill(EntityType.SPIDER, BASE.offset(-3, 1, -1));
            choked.setHealth(MiasmaVentBlockEntity.DAMAGE - 1);
            spire.wake();
            helper.runAfterDelay(110, () -> {
                helper.assertTrue(!spider.isAlive(), "the spire struck with spark from the creative jar (uncharged " + spire.uncharged() + ")");
                helper.assertTrue(held.hasData(ModAttachments.SHACKLE), "the shackles hold with shadow from the creative jar (dark "
                        + plate.dark() + ", held " + plate.held().length + ")");
                helper.assertTrue(!choked.isAlive() && !vent.dry(), "the vent breathes the creative jar's murk");
                helper.succeed();
            });
        });
    }
}
