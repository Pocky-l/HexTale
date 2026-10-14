package com.pockyl.hextale.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
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
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DiaryOwner>> DIARY_OWNER =
            COMPONENTS.registerComponentType("diary_owner", builder -> builder
                    .persistent(DiaryOwner.CODEC).networkSynchronized(DiaryOwner.STREAM_CODEC));

    private ModDataComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
