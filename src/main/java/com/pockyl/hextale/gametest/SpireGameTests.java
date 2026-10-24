package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import com.pockyl.hextale.block.ArcSpireBlockEntity;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.GreedyFunnelBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.rune.Essence;

/** The Hex Arc Spire: no spark - no arcs; charged, it fells the hostile creatures as a player would and spares the rest. */
public final class SpireGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private SpireGameTests() {
    }

    private static void clear(GameTestHelper helper) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                helper.setBlock(BASE.offset(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= 4; y++) {
                    helper.setBlock(BASE.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        AABB area = new AABB(helper.absolutePos(BASE)).inflate(6, 5, 6);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
    }

    private static void jar(GameTestHelper helper, BlockPos at, Essence essence, int amount) {
        helper.setBlock(at, ModBlocks.ESSENCE_TANK.get());
        helper.getBlockEntity(at, EssenceTankBlockEntity.class).tank().set(EssenceMix.pure(essence, amount));
    }

    /**
     * A spire beside a funnel on a chest, a spider and a cow in reach. Without spark it stands uncharged and the spider
     * lives; with a jar of spark the arcs fell the spider - its experience lands in the funnel's crystal, so the kill
     * counted as a player's - and the cow is spared.
     */
    @HexTest(maxTicks = 300)
    public static void theSpireStrikesTheHostile(GameTestHelper helper) {
        clear(helper);
        helper.setBlock(BASE, ModBlocks.ARC_SPIRE.get());
        helper.setBlock(BASE.offset(3, 0, 0), Blocks.CHEST);
        helper.setBlock(BASE.offset(3, 1, 0), ModBlocks.GREEDY_FUNNEL.get());
        jar(helper, BASE.offset(-2, 0, 2), Essence.DECAY, 8);
        ArcSpireBlockEntity spire = helper.getBlockEntity(BASE, ArcSpireBlockEntity.class);
        GreedyFunnelBlockEntity funnel = helper.getBlockEntity(BASE.offset(3, 1, 0), GreedyFunnelBlockEntity.class);
        Spider spider = helper.spawnWithNoFreeWill(EntityType.SPIDER, BASE.offset(2, 0, 2));
        // One arc fells it, whatever else the arcs find around (neighbouring tests stand close by).
        spider.setHealth(ArcSpireBlockEntity.DAMAGE - 2);
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, BASE.offset(-2, 0, 0));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(spire.uncharged(), "with no spark nearby the spire is uncharged");
            helper.assertTrue(spider.isAlive(), "no arcs without spark");
            jar(helper, BASE.offset(-2, 0, -2), Essence.SPARK, 8);
            spire.wake();
        });
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(!spider.isAlive(), "the arcs felled the spider");
            helper.assertTrue(cow.isAlive(), "the cow is spared");
            // A neighbouring test's Experience Condenser may reach the funnel and take the experience first.
            boolean condenser = com.pockyl.hextale.block.XpCondenserBlockEntity.INDEX.covering(helper.getLevel(),
                    GreedyFunnelBlockEntity.crystal(funnel.getBlockPos())) != null;
            helper.assertTrue(funnel.experience() > 0 || condenser, "the spider's experience was a player's kill's, in the funnel");
            helper.succeed();
        });
    }
}
