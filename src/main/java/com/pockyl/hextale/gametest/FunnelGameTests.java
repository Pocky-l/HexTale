package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.GreedyFunnelBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.rune.Essence;

/** The Greedy Funnel: deaths around it go straight into the chest below and the crystal, and without decay it lets them be. */
public final class FunnelGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private FunnelGameTests() {
    }

    /** Clears the ground around (blocks and loose items outside the 1x1x1 structure outlive their tests). */
    private static void clear(GameTestHelper helper) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                helper.setBlock(BASE.offset(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= 4; y++) {
                    helper.setBlock(BASE.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        loose(helper).forEach(ItemEntity::discard);
    }

    private static java.util.List<ItemEntity> loose(GameTestHelper helper) {
        AABB area = new AABB(helper.absolutePos(BASE)).inflate(6, 5, 6);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area);
    }

    private static int stored(ChestBlockEntity chest) {
        int count = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            count += chest.getItem(i).getCount();
        }
        return count;
    }

    private static Sheep sheep(GameTestHelper helper) {
        return helper.spawnWithNoFreeWill(EntityType.SHEEP, BASE.offset(3, 0, 1));
    }

    /**
     * One funnel on a chest. With no decay nearby it goes hungry: a sheep killed beside it drops its wool and mutton on the
     * ground as always. With a jar of decay by it, the next sheep - killed by a player - leaves nothing on the ground and
     * no orbs: the drops are in the chest and the experience in the crystal.
     */
    @HexTest(maxTicks = 200)
    public static void theFunnelSwallowsDeaths(GameTestHelper helper) {
        clear(helper);
        helper.setBlock(BASE, Blocks.CHEST);
        helper.setBlock(BASE.above(), ModBlocks.GREEDY_FUNNEL.get());
        GreedyFunnelBlockEntity funnel = helper.getBlockEntity(BASE.above(), GreedyFunnelBlockEntity.class);
        ChestBlockEntity chest = helper.getBlockEntity(BASE, ChestBlockEntity.class);
        helper.runAfterDelay(2, () -> {
            sheep(helper).kill(helper.getLevel());
            helper.assertTrue(funnel.hungry(), "with no decay nearby the funnel is hungry");
            helper.assertTrue(!loose(helper).isEmpty(), "a hungry funnel lets the drops fall");
            helper.assertValueEqual(stored(chest), 0, "items in the chest");
            loose(helper).forEach(ItemEntity::discard);

            BlockPos jarPos = BASE.offset(-2, 0, -2);
            helper.setBlock(jarPos, ModBlocks.ESSENCE_TANK.get());
            helper.getBlockEntity(jarPos, EssenceTankBlockEntity.class).tank().set(EssenceMix.pure(Essence.DECAY, 8));
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            Sheep second = sheep(helper);
            second.hurt(helper.getLevel().damageSources().playerAttack(player), 1000);
            helper.assertTrue(second.isDeadOrDying(), "the sheep is dead");
            helper.assertTrue(!funnel.hungry(), "fed with decay");
            helper.assertTrue(loose(helper).isEmpty(), "nothing lies on the ground");
            helper.assertTrue(stored(chest) >= 2, "wool and mutton went into the chest");
            // A neighbouring test's Experience Condenser may reach this funnel and take the experience first.
            helper.assertTrue(funnel.experience() > 0 || com.pockyl.hextale.block.XpCondenserBlockEntity.INDEX.covering(helper.getLevel(),
                    GreedyFunnelBlockEntity.crystal(funnel.getBlockPos())) != null, "the experience went into the crystal");
            AABB area = new AABB(helper.absolutePos(BASE)).inflate(6, 5, 6);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, area).isEmpty(), "no orbs were spawned");
            helper.succeed();
        });
    }
}
