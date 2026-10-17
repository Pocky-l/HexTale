package com.pockyl.hextale.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.item.DiaryOwner;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Hextale.MOD_ID);

    /** The complex rune written on a rune scroll. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ComplexRune>> RUNE =
            COMPONENTS.registerComponentType("rune", builder -> builder
                    .persistent(ComplexRunes.CODEC).networkSynchronized(ComplexRunes.STREAM_CODEC));
    /** Stars the rune on a scroll was studied with, or a hextech pickaxe's strength (1-3). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STARS =
            COMPONENTS.registerComponentType("stars", builder -> builder
                    .persistent(Codec.intRange(1, 3)).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DiaryOwner>> DIARY_OWNER =
            COMPONENTS.registerComponentType("diary_owner", builder -> builder
                    .persistent(DiaryOwner.CODEC).networkSynchronized(DiaryOwner.STREAM_CODEC));

    /** The essence in a flask or a tank item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EssenceMix>> ESSENCE =
            COMPONENTS.registerComponentType("essence", builder -> builder
                    .persistent(EssenceMix.CODEC).networkSynchronized(EssenceMix.STREAM_CODEC));

    private ModDataComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
