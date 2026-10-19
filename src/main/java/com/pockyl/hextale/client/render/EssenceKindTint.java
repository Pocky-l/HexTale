package com.pockyl.hextale.client.render;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.item.EssenceItem;
import com.pockyl.hextale.rune.Essence;

/** Item tint {@code hextale:essence_kind}: an essence item's pictogram in the essence's colour, lifted a little. */
public record EssenceKindTint() implements ItemTintSource {
    public static final MapCodec<EssenceKindTint> MAP_CODEC = MapCodec.unit(new EssenceKindTint());

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        Essence essence = EssenceItem.essence(stack);
        return ARGB.opaque(essence == null ? 0xB8B0A0 : RuneDrawing.lighten(essence.color(), 0.2F));
    }

    @Override
    public MapCodec<EssenceKindTint> type() {
        return MAP_CODEC;
    }
}
