package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.runework.BulwarkHeart;
import com.pockyl.hextale.runework.ClothesRune;
import com.pockyl.hextale.runework.RuneHolder;

/** The Arsenal: Bulwark armour (runes of the clothes, places by level, the crystal heart). */
public final class ArsenalGameTests {
    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private ArsenalGameTests() {
    }

    /** Bulwark takes the runes of the clothes piece of its slot, as many as its level holds. */
    @HexTest
    public static void bulwarkTakesTheClothesRunes(GameTestHelper helper) {
        ItemStack helmet = new ItemStack(ModItems.BULWARK_HELMET.get());
        helper.assertTrue(RuneHolder.of(helmet) == RuneHolder.HOOD && RuneHolder.HOOD.slots(helmet) == 1, "a first helmet takes one hood rune");
        ItemStack boots = new ItemStack(ModItems.RIFTFORGED_BULWARK_BOOTS.get());
        helper.assertTrue(RuneHolder.of(boots) == RuneHolder.BOOTS && RuneHolder.BOOTS.slots(boots) == 4, "riftforged boots take four");
        helper.assertTrue(RuneHolder.ROBE.slots(new ItemStack(ModItems.REFINED_BULWARK_CHESTPLATE.get())) == 2
                && RuneHolder.TROUSERS.slots(new ItemStack(ModItems.RADIANT_BULWARK_LEGGINGS.get())) == 3, "and the levels between");
        helper.assertTrue(RuneHolder.of(boots).holderRunes().contains(ClothesRune.BOOTS_WAVEWALK), "the boots' own runes");
        helper.succeed();
    }

    /** A charged heart soaks part of a blow and pays for it; run dry, it cracks and throws the creatures round back. */
    @HexTest(maxTicks = 60)
    public static void theBulwarkHeartSoaksAndCracks(GameTestHelper helper) {
        // Any creature can wear it; a fresh mock player would be invulnerable for its first seconds.
        Mob wearer = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, AT);
        ItemStack chest = new ItemStack(ModItems.BULWARK_CHESTPLATE.get());
        BulwarkHeart.set(chest, new BulwarkHeart.Charge(BulwarkHeart.MAX, false));
        wearer.setItemSlot(EquipmentSlot.CHEST, chest);
        Mob near = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, AT.east(2));
        helper.runAfterDelay(3, () -> {
            float before = wearer.getHealth();
            wearer.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(near), 10);
            ItemStack worn = wearer.getItemBySlot(EquipmentSlot.CHEST);
            int left = BulwarkHeart.of(worn).charge();
            helper.assertTrue(before - wearer.getHealth() > 0, "the blow landed, health " + before + " -> " + wearer.getHealth());
            helper.assertTrue(left < BulwarkHeart.MAX && left >= BulwarkHeart.MAX - 4, "the heart paid for what it soaked, left " + left);
            helper.assertTrue(before - wearer.getHealth() < 10, "the blow was softened");
            BulwarkHeart.set(worn, new BulwarkHeart.Charge(1, false));
            wearer.invulnerableTime = 0;
            wearer.invulnerableTime = 0;
            wearer.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(near), 6);
            helper.assertTrue(BulwarkHeart.of(worn).cracked(), "run dry, the heart cracks");
            Vec3 start = near.position();
            helper.succeedWhen(() -> helper.assertTrue(near.getDeltaMovement().horizontalDistance() > 0.05 || near.position().distanceTo(start) > 0.3,
                    "and the flash throws the zombie back"));
        });
    }

    /** A thrown dagger bites the creature it meets and turns back to its thrower at once. */
    // Entities in a fresh spot may not tick for a while: give the flight time to start.
    @HexTest(maxTicks = 200, setupTicks = 40)
    @SuppressWarnings("removal")
    public static void aThrownDaggerBitesAndComesBack(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer thrower = helper.makeMockServerPlayerInLevel();
        Vec3 from = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        thrower.snapTo(from.x, from.y, from.z, 0, 0);
        Mob target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 5));
        com.pockyl.hextale.entity.ThrownDagger dagger = new com.pockyl.hextale.entity.ThrownDagger(helper.getLevel(), thrower,
                new ItemStack(ModItems.TENDRIL_DAGGER.get()));
        dagger.setPos(from.x, from.y + 1.2, from.z + 1);
        dagger.setDeltaMovement(new Vec3(0, 0, 1.2));
        dagger.setNoGravity(true);
        helper.getLevel().addFreshEntity(dagger);
        helper.succeedWhen(() -> {
            helper.assertTrue(target.getHealth() < target.getMaxHealth(), "the dagger bit the zombie");
            helper.assertTrue(dagger.returning() || dagger.isRemoved(), "and turns back to its thrower");
        });
    }

    /** A dagger in the off hand sometimes finds a gap and strikes along with the main hand's blow - never on every blow. */
    @HexTest
    @SuppressWarnings("removal")
    public static void anOffHandDaggerStrikesAlong(GameTestHelper helper) {
        Mob plain = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        Mob paired = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(5, 2, 1));
        net.minecraft.server.level.ServerPlayer one = helper.makeMockServerPlayerInLevel();
        one.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        net.minecraft.server.level.ServerPlayer two = helper.makeMockServerPlayerInLevel();
        two.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        two.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(ModItems.TENDRIL_DAGGER.get()));
        ItemStack dagger = new ItemStack(ModItems.TENDRIL_DAGGER.get());
        helper.assertTrue(com.pockyl.hextale.runework.TendrilDaggerItem.offHandExtra(dagger, 0) > 0
                && com.pockyl.hextale.runework.TendrilDaggerItem.offHandExtra(dagger, 0.99F) == 0, "a lucky roll bites, an unlucky one not");
        plain.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(one), 6);
        float plainLost = plain.getMaxHealth() - plain.getHealth();
        // Thirty blows: the dagger finds a gap in some of them (missing all thirty at 30% is a 1 in 40000 chance).
        boolean bit = false;
        for (int blow = 0; blow < 30 && !bit; blow++) {
            paired.setHealth(paired.getMaxHealth());
            paired.invulnerableTime = 0;
            paired.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(two), 6);
            bit = paired.getMaxHealth() - paired.getHealth() >= plainLost + 1.5F;
        }
        helper.assertTrue(bit, "the off-hand dagger adds its blow now and then");
        helper.succeed();
    }

    /** A full Strike Gauntlet punch at stone breaks a 3 x 3 face two deep; into the air it throws a zombie back. */
    @HexTest(padding = 4)
    @SuppressWarnings("removal")
    public static void theStrikeGauntletBreaksRockAndThrows(GameTestHelper helper) {
        for (int x = 0; x <= 2; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 4; z <= 6; z++) {
                    helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.STONE);
                }
            }
        }
        net.minecraft.server.level.ServerPlayer smith = helper.makeMockServerPlayerInLevel();
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1.4, 1.5));
        smith.snapTo(at.x, at.y, at.z, 0, 0);
        ItemStack fist = new ItemStack(ModItems.STRIKE_GAUNTLET.get());
        smith.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, fist);
        int held = com.pockyl.hextale.runework.StrikeGauntletItem.STAGES * com.pockyl.hextale.runework.StrikeGauntletItem.STAGE_TICKS;
        fist.releaseUsing(helper.getLevel(), smith, fist.getUseDuration(smith) - held);
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR, new BlockPos(1, 2, 4));
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR, new BlockPos(0, 2, 5));
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR, new BlockPos(2, 2, 4));
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.STONE, new BlockPos(1, 2, 6));
        Mob target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 3));
        smith.getCooldowns().removeCooldown(smith.getCooldowns().getCooldownGroup(fist));
        smith.snapTo(at.x, at.y, at.z, 180, 0);
        net.minecraft.world.phys.Vec3 behind = helper.absoluteVec(new Vec3(1.5, 1, 0.5));
        target.snapTo(behind.x, behind.y, behind.z, 0, 0);
        fist.releaseUsing(helper.getLevel(), smith, fist.getUseDuration(smith) - held);
        helper.assertTrue(target.getHealth() < target.getMaxHealth(), "the shockwave hurt the zombie");
        helper.succeed();
    }

    /** A Crystal Battery feeds the heart of a Bulwark cuirass worn within its reach. */
    @HexTest(maxTicks = 60)
    @SuppressWarnings("removal")
    public static void aBatteryFeedsTheBulwarkHeart(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), com.pockyl.hextale.registry.ModBlocks.CRYSTAL_BATTERY.get());
        var battery = helper.getBlockEntity(new BlockPos(1, 1, 1), com.pockyl.hextale.block.CrystalBatteryBlockEntity.class);
        battery.soak(new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), 8);
        net.minecraft.server.level.ServerPlayer wearer = helper.makeMockServerPlayerInLevel();
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 1, 1.5));
        wearer.snapTo(at.x, at.y, at.z, 0, 0);
        wearer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.BULWARK_CHESTPLATE.get()));
        helper.succeedWhen(() -> helper.assertTrue(BulwarkHeart.of(wearer.getItemBySlot(EquipmentSlot.CHEST)).charge() > 0,
                "the battery fed the heart"));
    }

    /** Backstab: the same blow lands harder on a creature that turns its back. */
    @HexTest
    @SuppressWarnings("removal")
    public static void backstabStrikesHarderFromBehind(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer striker = helper.makeMockServerPlayerInLevel();
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 1, 3.5));
        striker.snapTo(at.x, at.y, at.z, 0, 0);
        ItemStack blade = new ItemStack(ModItems.TENDRIL_DAGGER.get());
        RuneHolder.SWORD.set(blade, com.pockyl.hextale.runework.PickaxeRunes.EMPTY.with(com.pockyl.hextale.runework.ToolRune.BACKSTAB.rune(), 3));
        striker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, blade);
        // Both face south (+Z): the first stands south of the striker (its back to them), the second north (face to face).
        Mob away = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 5));
        Mob facing = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
        for (Mob mob : java.util.List.of(away, facing)) {
            mob.setYRot(0);
            mob.setYHeadRot(0);
            mob.setYBodyRot(0);
        }
        away.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(striker), 4);
        facing.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(striker), 4);
        float back = away.getMaxHealth() - away.getHealth();
        float front = facing.getMaxHealth() - facing.getHealth();
        helper.assertTrue(back > front + 1.5F, "the blow from behind lands harder, " + back + " vs " + front);
        helper.succeed();
    }
}
