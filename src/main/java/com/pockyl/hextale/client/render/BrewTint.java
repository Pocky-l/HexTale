package com.pockyl.hextale.client.render;

import com.mojang.serialization.MapCodec;
import com.pockyl.hextale.port.client.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;

/** Item tint {@code hextale:brew}: the liquid in a Chem Vial in its brew's colour. */
public record BrewTint() implements ItemTintSource {
    public static final MapCodec<BrewTint> MAP_CODEC = MapCodec.unit(new BrewTint());

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        Brew brew = ChemVialItem.brew(stack);
        return FastColor.ARGB32.opaque(brew == null ? 0x9AA89A : brew.color());
    }

    @Override
    public MapCodec<BrewTint> type() {
        return MAP_CODEC;
    }
}
