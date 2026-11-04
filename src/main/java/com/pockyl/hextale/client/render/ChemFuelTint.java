package com.pockyl.hextale.client.render;

import com.mojang.serialization.MapCodec;
import com.pockyl.hextale.port.client.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.pockyl.hextale.chem.ChemGear;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.TankFuel;

/**
 * Item tint {@code hextale:chem_fuel}: the sight glass of a chem-steel tool or armour piece in the colour of the fuel
 * it runs on - its own tank when it is one, else the tank its holder wears (the local player when nobody holds it).
 * An empty tank leaves the glass dark.
 */
public record ChemFuelTint() implements ItemTintSource {
    public static final MapCodec<ChemFuelTint> MAP_CODEC = MapCodec.unit(new ChemFuelTint());
    private static final int EMPTY = 0x2E3530;

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        ItemStack tank = ChemTank.is(stack) ? stack : owner != null ? ChemTank.worn(owner) : ChemGear.clientTank.get();
        TankFuel fuel = ChemTank.contents(tank);
        return FastColor.ARGB32.opaque(fuel.isEmpty() ? EMPTY : fuel.color());
    }

    @Override
    public MapCodec<ChemFuelTint> type() {
        return MAP_CODEC;
    }
}
