package com.pockyl.hextale.chem;

import com.pockyl.hextale.GearConfig;
import com.pockyl.hextale.compat.curios.CuriosCompat;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The Chem Tank's rules in one place: what a tank holds ({@link TankFuel}), filling it with vials, pumping from one tank
 * into another, and finding the tank someone wears (chest slot first, then the Curios back slot). A tank holds one fuel
 * at a time: another fuel goes only into an empty tank. Using fuel never adds Thinning (owner, 2026-11-02).
 */
public final class ChemTank {
    private ChemTank() {
    }

    /** Whether the stack carries a tank: the Chem Tank or the chem-steel chestplate with a built-in tank. */
    public static boolean is(ItemStack stack) {
        return stack.is(ModItems.CHEM_TANK.get()) || stack.is(ModItems.CHEM_STEEL_TANK_CHESTPLATE.get());
    }

    /** Pours a carried stack of fuel vials into a tank item, as many as fit; returns whether any went in. */
    public static boolean pourOnto(ItemStack tank, ItemStack vials, net.minecraft.world.entity.player.Player player) {
        Brew brew = ChemVialItem.brew(vials);
        if (brew == null || ChemFuel.of(brew) == null) {
            return false;
        }
        TankFuel contents = contents(tank);
        int count = Math.min(vials.getCount(), room(contents, brew));
        if (count <= 0) {
            return false;
        }
        setContents(tank, poured(contents, brew, count));
        vials.shrink(count);
        player.playSound(net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY, 0.6F, 0.9F);
        return true;
    }

    public static TankFuel contents(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CHEM_TANK.get(), TankFuel.EMPTY);
    }

    public static void setContents(ItemStack stack, TankFuel fuel) {
        if (fuel.isEmpty()) {
            stack.remove(ModDataComponents.CHEM_TANK.get());
        } else {
            stack.set(ModDataComponents.CHEM_TANK.get(), fuel);
        }
    }

    public static int capacity() {
        return ChemFuel.tankActions();
    }

    /** Vials of this brew the contents could still take (0 for a brew that is no tank fuel or does not match). */
    public static int room(TankFuel contents, Brew brew) {
        ChemFuel fuel = ChemFuel.of(brew);
        if (fuel == null || !contents.isEmpty() && contents.fuel() != fuel) {
            return 0;
        }
        return (capacity() - contents.units()) / GearConfig.ACTIONS_PER_VIAL.get();
    }

    /** The contents after pouring in {@code vials} vials of the brew (call {@link #room} first). */
    public static TankFuel poured(TankFuel contents, Brew brew, int vials) {
        ChemFuel fuel = ChemFuel.of(brew);
        int units = (contents.isEmpty() ? 0 : contents.units()) + vials * GearConfig.ACTIONS_PER_VIAL.get();
        return new TankFuel(fuel, Math.min(capacity(), units));
    }

    /**
     * Pumps from one tank into another: as much as fits, only into an empty tank or one of the same fuel. Returns the two
     * tanks after the pump as {@code [from, into]}.
     */
    public static TankFuel[] pump(TankFuel from, TankFuel into) {
        if (from.isEmpty() || !into.isEmpty() && into.fuel() != from.fuel()) {
            return new TankFuel[]{from, into};
        }
        int moved = Math.min(from.units(), capacity() - (into.isEmpty() ? 0 : into.units()));
        TankFuel left = new TankFuel(from.fuel(), from.units() - moved);
        TankFuel got = new TankFuel(from.fuel(), (into.isEmpty() ? 0 : into.units()) + moved);
        return new TankFuel[]{left, got};
    }

    /** The tank the entity wears: in the chest slot, else in a Curios slot; empty when none. */
    public static ItemStack worn(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (is(chest)) {
            return chest;
        }
        return CuriosCompat.find(entity, ChemTank::is);
    }
}
