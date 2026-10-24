package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.item.HextechSkypackItem;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.RuneHolder;
import com.pockyl.hextale.runework.SkypackRune;

/** The Hextech Skypack glides only with air in its tanks, spends it on launches and drinks only the air out of a jar. */
public final class SkypackGameTests {
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    private SkypackGameTests() {
    }

    @HexTest
    public static void theSkypackGlidesOnlyWithAir(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.HEXTECH_SKYPACK.get());
        helper.assertFalse(LivingEntity.canGlideUsing(pack, EquipmentSlot.CHEST), "an empty pack does not glide");
        HextechSkypackItem.setAir(pack, 10);
        helper.assertTrue(LivingEntity.canGlideUsing(pack, EquipmentSlot.CHEST), "a pack with air glides");
        helper.assertFalse(LivingEntity.canGlideUsing(pack, EquipmentSlot.HEAD), "only on the back");
        helper.assertTrue(RuneHolder.of(pack) == RuneHolder.SKYPACK, "the pack takes runes");
        helper.succeed();
    }

    @HexTest
    public static void aLaunchSpendsAirAndTheGlideIsFree(GameTestHelper helper) {
        Zombie wearer = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, AT);
        ItemStack pack = HextechSkypackItem.full();
        HextechSkypackItem.launch(helper.getLevel(), wearer, pack);
        helper.assertTrue(HextechSkypackItem.air(pack) == HextechSkypackItem.CAPACITY - HextechSkypackItem.LAUNCH_AIR,
                "a launch costs air, left " + HextechSkypackItem.air(pack));
        pack.onGlideDamage(wearer, EquipmentSlot.CHEST);
        helper.assertTrue(HextechSkypackItem.air(pack) == HextechSkypackItem.CAPACITY - HextechSkypackItem.LAUNCH_AIR,
                "gliding is free");
        HextechSkypackItem.setAir(pack, HextechSkypackItem.LAUNCH_AIR);
        helper.assertFalse(HextechSkypackItem.canLaunch(pack), "the last of the air is kept for gliding, not launched");
        helper.assertTrue(LivingEntity.canGlideUsing(pack, EquipmentSlot.CHEST), "with that little air it still glides");
        RuneHolder.SKYPACK.set(pack, PickaxeRunes.EMPTY.with(SkypackRune.DAM.rune(), 3));
        helper.assertTrue(HextechSkypackItem.strength(pack, SkypackRune.DAM) == 3, "the pack reads its runes");
        helper.succeed();
    }

    @HexTest
    public static void theSkypackDrinksOnlyAirFromAJar(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity jar = helper.getBlockEntity(AT, EssenceTankBlockEntity.class);
        jar.tank().set(EssenceMix.pure(Essence.AIR, 100).plus(EssenceMix.pure(Essence.FIRE, 50)));
        ItemStack pack = new ItemStack(ModItems.HEXTECH_SKYPACK.get());
        ServerLevel level = helper.getLevel();
        helper.assertTrue(HextechSkypackItem.fillFrom(level, jar, pack, Vec3.atCenterOf(helper.absolutePos(AT)).add(0, 1, 0),
                HextechSkypackItem.CAPACITY), "the pack drinks");
        helper.assertTrue(HextechSkypackItem.air(pack) == 100, "all the air came, got " + HextechSkypackItem.air(pack));
        helper.assertTrue(jar.tank().contents().get(Essence.AIR) == 0 && jar.tank().contents().get(Essence.FIRE) == 50,
                "the fire stays in the jar: " + jar.tank().contents());
        helper.assertFalse(HextechSkypackItem.fillFrom(level, jar, pack, Vec3.ZERO, 10), "no more air, nothing comes");
        helper.succeed();
    }
}
