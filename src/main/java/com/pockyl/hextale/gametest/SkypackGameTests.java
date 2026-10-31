package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
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
        helper.assertFalse(com.pockyl.hextale.port.PortEntities.canGlideUsing(pack, EquipmentSlot.CHEST), "an empty pack does not glide");
        HextechSkypackItem.setAir(pack, 10);
        helper.assertTrue(com.pockyl.hextale.port.PortEntities.canGlideUsing(pack, EquipmentSlot.CHEST), "a pack with air glides");
        helper.assertFalse(com.pockyl.hextale.port.PortEntities.canGlideUsing(pack, EquipmentSlot.HEAD), "only on the back");
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
        ((HextechSkypackItem) pack.getItem()).onGlideDamage(pack, wearer, EquipmentSlot.CHEST);
        helper.assertTrue(HextechSkypackItem.air(pack) == HextechSkypackItem.CAPACITY - HextechSkypackItem.LAUNCH_AIR,
                "gliding is free");
        HextechSkypackItem.setAir(pack, HextechSkypackItem.LAUNCH_AIR);
        helper.assertFalse(HextechSkypackItem.canLaunch(pack), "the last of the air is kept for gliding, not launched");
        helper.assertTrue(com.pockyl.hextale.port.PortEntities.canGlideUsing(pack, EquipmentSlot.CHEST), "with that little air it still glides");
        RuneHolder.SKYPACK.set(pack, PickaxeRunes.EMPTY.with(SkypackRune.DAM.rune(), 3));
        helper.assertTrue(HextechSkypackItem.strength(pack, SkypackRune.DAM) == 3, "the pack reads its runes");
        helper.succeed();
    }

    @HexTest
    public static void theSkypackDrinksOnlyAirFromAJar(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity jar = helper.<EssenceTankBlockEntity>getBlockEntity(AT);
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

    /**
     * Flamethrower: a wearer launching into a glide (their own ticks; held in place so the cone stays put) scorches the
     * cow in the jets' wake behind and below them - it burns and is hurt - while the ground never catches fire.
     */
    @HexTest(maxTicks = 80)
    public static void theFlamethrowerScorchesTheWake(GameTestHelper helper) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -1; z <= 5; z++) {
                helper.setBlock(AT.offset(x, -1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
                for (int y = 0; y <= 4; y++) {
                    helper.setBlock(AT.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR);
                }
            }
        }
        net.minecraft.world.entity.Mob cow = WeaponRuneGameTests.still(helper, EntityType.COW, AT.offset(0, 0, 3));
        net.minecraft.world.entity.player.Player wearer = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pack = HextechSkypackItem.full();
        RuneHolder.SKYPACK.set(pack, PickaxeRunes.EMPTY.with(SkypackRune.FLAMETHROWER.rune(), 3));
        wearer.setItemSlot(EquipmentSlot.CHEST, pack);
        BlockPos abs = helper.absolutePos(AT);
        // Facing north (away from the cow), two blocks up: the wake runs south and down onto the cow.
        Vec3 hover = new Vec3(abs.getX() + 0.5, abs.getY() + 2, abs.getZ() + 0.5);
        helper.onEachTick(() -> {
            wearer.moveTo(hover.x, hover.y, hover.z, 180, 0);
            wearer.setDeltaMovement(Vec3.ZERO);
            if (!wearer.isFallFlying()) {
                wearer.startFallFlying();
            }
            wearer.tick();
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(cow.getRemainingFireTicks() > 0 && cow.getHealth() < cow.getMaxHealth(),
                    "the cow in the wake burns (fire " + cow.getRemainingFireTicks() + ", health " + cow.getHealth() + ")");
            for (int x = -2; x <= 2; x++) {
                for (int z = -1; z <= 5; z++) {
                    helper.assertBlockNotPresent(net.minecraft.world.level.block.Blocks.FIRE, AT.offset(x, 0, z));
                }
            }
            wearer.discard();
        });
    }
}
