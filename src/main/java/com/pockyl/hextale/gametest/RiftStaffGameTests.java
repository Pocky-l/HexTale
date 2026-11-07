package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.runework.RiftStaff;
import com.pockyl.hextale.runework.RuneHolder;
import com.pockyl.hextale.runework.ToolRune;

/** The Rift Staff: bolts, the lance on every third cast, the ward that stops, breaks and heals, its runes. */
public final class RiftStaffGameTests {
    private RiftStaffGameTests() {
    }

    /** A player at (1, 2, 1) looking south (+z) with the staff in hand. */
    private static ServerPlayer caster(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        player.snapTo(at.x, at.y, at.z, 0, 0);
        player.setYHeadRot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RIFT_STAFF.get()));
        return player;
    }

    /** A cast sends a bolt that hurts the creature in its way, counts a step of the run, and the rhythm holds the next. */
    @HexTest(maxTicks = 60, setupTicks = 20)
    public static void aBoltHurtsWhatItMeets(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Mob target = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 6));
        helper.assertTrue(RiftStaff.cast(player), "the first cast goes");
        helper.assertFalse(RiftStaff.cast(player), "the next waits for the staff's rhythm");
        ItemStack staff = player.getMainHandItem();
        helper.assertTrue(staff.getOrDefault(ModDataComponents.STAFF_CHARGE.get(), 0) == 1, "one step of the run");
        helper.succeedWhen(() -> helper.assertTrue(target.getHealth() < target.getMaxHealth(), "the bolt reached the zombie"));
    }

    /** The cast after two is the lance: everything along the line is hurt at once, and the run starts over. */
    @HexTest(maxTicks = 40, setupTicks = 20)
    public static void theThirdCastIsALanceThroughTheLine(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        player.getMainHandItem().set(ModDataComponents.STAFF_CHARGE.get(), RiftStaff.LANCE_AFTER);
        Mob near = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 4));
        Mob far = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 7));
        helper.assertTrue(RiftStaff.cast(player), "the lance goes");
        helper.assertTrue(near.getMaxHealth() - near.getHealth() >= 10 && far.getMaxHealth() - far.getHealth() >= 10,
                "both zombies in the line are hurt hard: " + near.getHealth() + ", " + far.getHealth());
        helper.assertTrue(player.getMainHandItem().getOrDefault(ModDataComponents.STAFF_CHARGE.get(), 0) == 0, "the run starts over");
        helper.succeed();
    }

    /** A raised ward stops a blow from the front, spends what it stopped and counts the blow towards the lance. */
    @HexTest(maxTicks = 40)
    public static void theWardStopsAndSpends(GameTestHelper helper) {
        Mob holder = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        Mob striker = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 3));
        raise(holder);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(holder.isBlocking(), "the ward is up");
            float before = holder.getHealth();
            holder.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(striker), 8);
            ItemStack staff = holder.getUseItem();
            helper.assertTrue(holder.getHealth() == before, "nothing got through: " + before + " -> " + holder.getHealth());
            helper.assertTrue(staff.getOrDefault(ModDataComponents.STAFF_WARD.get(), 0F) == 8, "the ward spent the blow");
            helper.assertTrue(staff.getOrDefault(ModDataComponents.STAFF_CHARGE.get(), 0) == 1, "and counted it towards the lance");
            helper.succeed();
        });
    }

    /** A blow bigger than what is left gets the rest through and breaks the ward; lowered and left alone, it heals. */
    @HexTest(maxTicks = 200)
    public static void theWardBreaksAndHeals(GameTestHelper helper) {
        Mob holder = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        Mob striker = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 3));
        ItemStack staff = raise(holder);
        staff.set(ModDataComponents.STAFF_WARD.get(), RiftStaff.WARD_MAX - 3);
        helper.runAfterDelay(5, () -> {
            float before = holder.getHealth();
            holder.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(striker), 8);
            helper.assertTrue(before - holder.getHealth() > 3, "the rest got through: " + before + " -> " + holder.getHealth());
            helper.assertFalse(holder.isUsingItem(), "the broken ward is lowered");
            ItemStack held = holder.getMainHandItem();
            helper.assertTrue(RiftStaff.wardLeft(held) == 0, "and holds nothing");
            helper.succeedWhen(() -> helper.assertTrue(RiftStaff.wardLeft(held) > 0, "left alone, it heals"));
        });
    }

    /** The staff takes the sword's runes that work from afar and its own ward runes, not the sword's powers. */
    @HexTest
    public static void theStaffTakesItsRunes(GameTestHelper helper) {
        ItemStack staff = new ItemStack(ModItems.RIFT_STAFF.get());
        RuneHolder holder = RuneHolder.of(staff);
        helper.assertTrue(holder == RuneHolder.STAFF && holder.slots(staff) == 4, "four places, like riftforged gear");
        helper.assertTrue(holder.holderRunes().contains(ToolRune.CHAIN_LIGHTNING) && holder.holderRunes().contains(ToolRune.MIRROR)
                && holder.holderRunes().contains(ToolRune.REINFORCEMENT), "sword runes and the ward's own");
        helper.assertFalse(holder.holderRunes().contains(ToolRune.SHOCKWAVE) || holder.holderRunes().contains(ToolRune.BLINK)
                || holder.holderRunes().contains(ToolRune.WHIRLWIND), "no sword powers: its clicks are its own");
        RuneHolder.STAFF.set(staff, com.pockyl.hextale.runework.PickaxeRunes.EMPTY.with(ToolRune.REINFORCEMENT.rune(), 3));
        helper.assertTrue(RiftStaff.wardMax(staff) > RiftStaff.WARD_MAX, "Reinforcement makes the ward hold more");
        helper.succeed();
    }

    private static ItemStack raise(Mob holder) {
        ItemStack staff = new ItemStack(ModItems.RIFT_STAFF.get());
        holder.setItemInHand(InteractionHand.MAIN_HAND, staff);
        holder.setYRot(0);
        holder.setYHeadRot(0);
        holder.startUsingItem(InteractionHand.MAIN_HAND);
        return holder.getUseItem();
    }
}
