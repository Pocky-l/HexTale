package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.item.UnstableHextechItem;
import com.pockyl.hextale.registry.ModItems;

/** Raw hextech is volatile: it bursts after a long fall or in a blast; refined gemstones and short drops are safe. */
public final class HextechGameTests {
    private HextechGameTests() {
    }

    private static ItemEntity drop(GameTestHelper helper, ItemStack stack, double height) {
        Vec3 at = helper.absoluteVec(new Vec3(0.5, 1 + height, 0.5));
        ItemEntity entity = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, stack);
        entity.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static void floor(GameTestHelper helper) {
        helper.setBlock(BlockPos.ZERO, Blocks.STONE);
    }

    /** An orb dropped from well above the burst height is gone when it lands, and hurts what stands beside it. */
    @HexTest(maxTicks = 120)
    public static void unstableHextechBurstsAfterALongFall(GameTestHelper helper) {
        floor(helper);
        ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new Vec3(0.5, 1, 0.5));
        float health = stand.getHealth();
        ItemEntity orb = drop(helper, new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), UnstableHextechItem.BURST_FALL + 4);
        helper.succeedWhen(() -> {
            helper.assertTrue(orb.isRemoved(), "the orb burst on landing");
            helper.assertTrue(!stand.isAlive() || stand.getHealth() < health || stand.getDeltaMovement().lengthSqr() > 0
                    || !stand.position().equals(helper.absoluteVec(new Vec3(0.5, 1, 0.5))), "the burst reached what stood beside it");
        });
    }

    /** A short drop leaves the orb whole; so does any fall of a refined gemstone. */
    @HexTest(maxTicks = 80)
    public static void shortFallsAndGemstonesAreSafe(GameTestHelper helper) {
        floor(helper);
        ItemEntity orb = drop(helper, new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), 2);
        ItemEntity gem = drop(helper, new ItemStack(ModItems.HEXTECH_GEMSTONE.get()), UnstableHextechItem.BURST_FALL + 4);
        helper.runAfterDelay(60, () -> {
            helper.assertFalse(orb.isRemoved(), "the orb survived a short drop");
            helper.assertFalse(gem.isRemoved(), "the gemstone survived a long fall");
            helper.succeed();
        });
    }

    /** Orbs fresh from the storm are still settling: their first fall does not burst them. */
    @HexTest(maxTicks = 120)
    public static void stormBornOrbsSettleFirst(GameTestHelper helper) {
        floor(helper);
        ItemEntity orb = drop(helper, new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), UnstableHextechItem.BURST_FALL + 4);
        orb.addTag(UnstableHextechItem.BORN_TAG);
        helper.runAfterDelay(80, () -> {
            helper.assertFalse(orb.isRemoved(), "a storm-born orb survived its first fall");
            helper.succeed();
        });
    }

    /** An orb caught in an explosion bursts (and does not linger as a destroyed item). */
    @HexTest(maxTicks = 40)
    public static void unstableHextechBurstsInABlast(GameTestHelper helper) {
        floor(helper);
        ItemEntity orb = drop(helper, new ItemStack(ModItems.UNSTABLE_HEXTECH.get(), 3), 0);
        helper.runAfterDelay(5, () -> {
            Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 0.5));
            helper.getLevel().explode(null, at.x, at.y, at.z, 1.0F, Level.ExplosionInteraction.NONE);
            helper.assertTrue(orb.isRemoved(), "the orb burst in the blast");
            helper.succeed();
        });
    }
}
