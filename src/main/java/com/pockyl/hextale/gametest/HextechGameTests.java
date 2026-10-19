package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.DrainValveBlock;
import com.pockyl.hextale.block.DrainValveBlockEntity;
import com.pockyl.hextale.Config;
import com.pockyl.hextale.entity.ThrownHextech;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.event.LightningEvents;
import com.pockyl.hextale.item.UnstableHextechItem;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

/**
 * Raw hextech is volatile: it bursts after a long fall or in a blast; refined gemstones and short drops are safe. Magic
 * stone is stone that drank essence: crafted around a bottle, or soaked where a drain valve pours essence.
 */
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
            AABB box = new AABB(helper.absoluteVec(new Vec3(-2, 0, -2)), helper.absoluteVec(new Vec3(3, 4, 3)));
            helper.assertTrue(count(helper, ModItems.HEXTECH_DUST.get(), box) == 3 * UnstableHextechItem.BURST_DUST, "the burst left dust");
            helper.succeed();
        });
    }

    private static int count(GameTestHelper helper, net.minecraft.world.item.Item item, AABB box) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, entity -> entity.getItem().is(item)).stream()
                .mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    /**
     * Lightning on one block of a 3x3x3 amethyst cube runs through all of it, ring by ring: every amethyst block turns
     * into orbs, the budding amethyst in the middle carries the wave and stays.
     */
    @HexTest(maxTicks = 120)
    public static void lightningRunsThroughTouchingAmethyst(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int y = 1; y < 4; y++) {
                for (int z = 0; z < 3; z++) {
                    helper.setBlock(new BlockPos(x, y, z), x == 1 && y == 2 && z == 1 ? Blocks.BUDDING_AMETHYST : Blocks.AMETHYST_BLOCK);
                }
            }
        }
        LightningEvents.strike(helper.getLevel(), helper.absolutePos(new BlockPos(1, 3, 1)));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 3, 1));
        helper.assertBlockPresent(Blocks.AMETHYST_BLOCK, new BlockPos(0, 1, 0));
        AABB box = new AABB(helper.absoluteVec(new Vec3(-4, -1, -4)), helper.absoluteVec(new Vec3(7, 8, 7)));
        helper.succeedWhen(() -> {
            helper.assertFalse(LightningEvents.waving(), "the wave ran out");
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(0, 1, 0));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 3, 2));
            helper.assertBlockPresent(Blocks.BUDDING_AMETHYST, new BlockPos(1, 2, 1));
            int orbs = count(helper, ModItems.UNSTABLE_HEXTECH.get(), box);
            helper.assertTrue(orbs >= 26 * Config.MIN_ORBS.get() && orbs <= 26 * Math.max(Config.MIN_ORBS.get(), Config.MAX_ORBS.get()),
                    "26 blocks gave their orbs, got " + orbs);
            helper.setBlock(new BlockPos(1, 2, 1), Blocks.AIR);
        });
    }

    /** A thrown orb shatters on the ground into hextech dust. */
    @HexTest(maxTicks = 80)
    public static void aThrownOrbShattersIntoDust(GameTestHelper helper) {
        floor(helper);
        Vec3 at = helper.absoluteVec(new Vec3(0.5, 4, 0.5));
        ThrownHextech orb = new ThrownHextech(ModEntities.THROWN_HEXTECH.get(), helper.getLevel());
        orb.setPos(at.x, at.y, at.z);
        orb.setItem(new ItemStack(ModItems.UNSTABLE_HEXTECH.get()));
        orb.setDeltaMovement(0, -0.5, 0);
        helper.getLevel().addFreshEntity(orb);
        AABB box = new AABB(helper.absoluteVec(new Vec3(-2, 0, -2)), helper.absoluteVec(new Vec3(3, 6, 3)));
        helper.succeedWhen(() -> {
            helper.assertTrue(orb.isRemoved(), "the orb shattered");
            int dust = count(helper, ModItems.HEXTECH_DUST.get(), box);
            helper.assertTrue(dust >= ThrownHextech.MIN_DUST && dust <= ThrownHextech.MAX_DUST, "the orb left its dust, got " + dust);
        });
    }

    /** Magic stone is no longer crafted: it is soaked in a crucible (see CrucibleGameTests) or under a drain valve. */
    @HexTest
    public static void magicStoneIsNotCrafted(GameTestHelper helper) {
        helper.assertFalse(helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,
                Hextale.id("magic_stone"))).isPresent(), "there is no magic stone crafting recipe");
        helper.succeed();
    }

    /** The poured way: an open drain valve over stone soaks the block under its stream into magic stone. */
    @HexTest(maxTicks = 80)
    public static void essencePouredOnStoneSoaksIntoMagicStone(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        BlockPos valve = new BlockPos(1, 4, 1);
        helper.setBlock(valve, ModBlocks.DRAIN_VALVE.get().defaultBlockState().setValue(DrainValveBlock.OPEN, true));
        DrainValveBlockEntity drain = helper.getBlockEntity(valve, DrainValveBlockEntity.class);
        helper.onEachTick(() -> drain.receive(Direction.EAST, EssenceMix.pure(Essence.FIRE, DrainValveBlockEntity.DROPS_PER_TICK), false));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(ModBlocks.MAGIC_STONE.get(), new BlockPos(1, 1, 1));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(0, 1, 0));
        });
    }
}
