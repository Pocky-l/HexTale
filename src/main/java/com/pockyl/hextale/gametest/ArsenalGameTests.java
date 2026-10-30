package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
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
        Mob wearer = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, AT);
        ItemStack chest = new ItemStack(ModItems.BULWARK_CHESTPLATE.get());
        BulwarkHeart.set(chest, new BulwarkHeart.Charge(BulwarkHeart.MAX, false));
        wearer.setItemSlot(EquipmentSlot.CHEST, chest);
        Mob near = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, AT.east(2));
        helper.runAfterDelay(3, () -> {
            float before = wearer.getHealth();
            wearer.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(near), 10);
            ItemStack worn = wearer.getItemBySlot(EquipmentSlot.CHEST);
            int left = BulwarkHeart.of(worn).charge();
            helper.assertTrue(before - wearer.getHealth() > 0, "the blow landed, health " + before + " -> " + wearer.getHealth());
            helper.assertTrue(left < BulwarkHeart.MAX && left >= BulwarkHeart.MAX - 4, "the heart paid for what it soaked, left " + left);
            helper.assertTrue(before - wearer.getHealth() < 10, "the blow was softened");
            BulwarkHeart.set(worn, new BulwarkHeart.Charge(1, false));
            wearer.setInvulnerableTime(0);
            wearer.damageCooldownTime = 0;
            wearer.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(near), 6);
            helper.assertTrue(BulwarkHeart.of(worn).cracked(), "run dry, the heart cracks");
            Vec3 start = near.position();
            helper.succeedWhen(() -> helper.assertTrue(near.getDeltaMovement().horizontalDistance() > 0.05 || near.position().distanceTo(start) > 0.3,
                    "and the flash throws the zombie back"));
        });
    }

    /** A thrown dagger bites the creature it meets and turns back to its thrower at once. */
    @HexTest(maxTicks = 80)
    @SuppressWarnings("removal")
    public static void aThrownDaggerBitesAndComesBack(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer thrower = helper.makeMockServerPlayerInLevel();
        Vec3 from = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        thrower.snapTo(from.x, from.y, from.z, 0, 0);
        Mob target = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 5));
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

    /** A dagger in the off hand strikes along with the main hand's blow. */
    @HexTest
    @SuppressWarnings("removal")
    public static void anOffHandDaggerStrikesAlong(GameTestHelper helper) {
        Mob plain = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        Mob paired = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 2, 1));
        net.minecraft.server.level.ServerPlayer one = helper.makeMockServerPlayerInLevel();
        one.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        net.minecraft.server.level.ServerPlayer two = helper.makeMockServerPlayerInLevel();
        two.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        two.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(ModItems.TENDRIL_DAGGER.get()));
        plain.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(one), 6);
        paired.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(two), 6);
        float plainLost = plain.getMaxHealth() - plain.getHealth();
        float pairedLost = paired.getMaxHealth() - paired.getHealth();
        helper.assertTrue(pairedLost >= plainLost + 1.5F, "the off-hand dagger adds its blow, " + plainLost + " vs " + pairedLost);
        helper.succeed();
    }
}
