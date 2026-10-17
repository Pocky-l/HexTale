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
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.DrainValveBlock;
import com.pockyl.hextale.block.DrainValveBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.item.UnstableHextechItem;
import com.pockyl.hextale.registry.ModBlocks;
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
            helper.succeed();
        });
    }

    /** The crafted way: eight stone around a bottle of essence; the bottle comes back. */
    @HexTest
    public static void magicStoneIsCraftedAroundABottle(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,
                Hextale.id("magic_stone"))).isPresent(), "the magic stone recipe is loaded");
        ItemStack bottle = new ItemStack(ModItems.ESSENCE_FLASK.get());
        helper.assertTrue(bottle.getItem().getCraftingRemainder(bottle).is(Items.GLASS_BOTTLE), "the bottle comes back as a glass bottle");
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
