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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.GreedyFunnelBlockEntity;
import com.pockyl.hextale.block.XpCondenserBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.rune.Essence;

/** The Experience Condenser: orbs around it never appear, a funnel pours into it, a bottle costs its share. */
public final class CondenserGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private CondenserGameTests() {
    }

    /**
     * An orb awarded next to the condenser is drunk before it exists. A sheep a player kills by a Greedy Funnel in the
     * condenser's reach leaves its experience in the condenser, not in the funnel's crystal. A bottle takes eleven points.
     */
    @HexTest(maxTicks = 40)
    public static void theCondenserDrinksExperience(GameTestHelper helper) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                helper.setBlock(BASE.offset(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= 3; y++) {
                    helper.setBlock(BASE.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        AABB area = new AABB(helper.absolutePos(BASE)).inflate(5, 4, 5);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
        helper.setBlock(BASE, ModBlocks.XP_CONDENSER.get());
        XpCondenserBlockEntity condenser = helper.getBlockEntity(BASE, XpCondenserBlockEntity.class);
        helper.setBlock(BASE.offset(3, 0, 0), Blocks.CHEST);
        helper.setBlock(BASE.offset(3, 1, 0), ModBlocks.GREEDY_FUNNEL.get());
        // Freshly placed devices join their index on the next tick.
        helper.runAfterDelay(1, () -> drink(helper, condenser));
    }

    private static void drink(GameTestHelper helper, XpCondenserBlockEntity condenser) {
        Vec3 spot = Vec3.atCenterOf(helper.absolutePos(BASE.offset(2, 0, 0)));
        ExperienceOrb.award(helper.getLevel(), spot, 12);
        // Only right at the spot: neighbouring tests make orbs of their own.
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, AABB.ofSize(spot, 1.5, 1.5, 1.5)).isEmpty(),
                "no orb appeared");
        helper.assertValueEqual(condenser.points(), 12, "points in the condenser");

        BlockPos jarPos = BASE.offset(-2, 0, -2);
        helper.setBlock(jarPos, ModBlocks.ESSENCE_TANK.get());
        helper.getBlockEntity(jarPos, EssenceTankBlockEntity.class).tank().set(EssenceMix.pure(Essence.DECAY, 8));
        GreedyFunnelBlockEntity funnel = helper.getBlockEntity(BASE.offset(3, 1, 0), GreedyFunnelBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Sheep sheep = helper.spawnWithNoFreeWill(EntityType.SHEEP, BASE.offset(3, 0, 2));
        sheep.hurt(helper.getLevel().damageSources().playerAttack(player), 1000);
        helper.assertTrue(condenser.points() > 12, "the funnel poured the sheep's experience into the condenser");
        helper.assertValueEqual(funnel.experience(), 0, "experience left in the funnel's crystal");

        int before = condenser.points();
        helper.assertTrue(condenser.bottle(), "enough for a bottle");
        helper.assertValueEqual(condenser.points(), before - XpCondenserBlockEntity.BOTTLE_COST, "points after a bottle");
        // Its reach would take the experience of neighbouring tests' deaths.
        helper.setBlock(BASE, Blocks.AIR);
        helper.succeed();
    }
}
