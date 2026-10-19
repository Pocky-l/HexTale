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
import com.pockyl.hextale.runework.PickaxeRunes;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Hextale.MOD_ID);

    /** The complex rune written on a rune scroll. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ComplexRune>> RUNE =
            COMPONENTS.registerComponentType("rune", builder -> builder
                    .persistent(ComplexRunes.CODEC).networkSynchronized(ComplexRunes.STREAM_CODEC));
    /** The research topic written in a researcher's notes. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.pockyl.hextale.research.ResearchTopic>> TOPIC =
            COMPONENTS.registerComponentType("topic", builder -> builder
                    .persistent(com.pockyl.hextale.research.ResearchTopics.CODEC)
                    .networkSynchronized(com.pockyl.hextale.research.ResearchTopics.STREAM_CODEC));
    /** The finished drawing written into research notes, shown again when they lie on a Research Table. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.pockyl.hextale.item.NotesDrawing>> NOTES_DRAWING =
            COMPONENTS.registerComponentType("notes_drawing", builder -> builder
                    .persistent(com.pockyl.hextale.item.NotesDrawing.CODEC)
                    .networkSynchronized(com.pockyl.hextale.item.NotesDrawing.STREAM_CODEC));
    /** Stars the rune on a scroll was studied with (1-3). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STARS =
            COMPONENTS.registerComponentType("stars", builder -> builder
                    .persistent(Codec.intRange(1, 3)).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DiaryOwner>> DIARY_OWNER =
            COMPONENTS.registerComponentType("diary_owner", builder -> builder
                    .persistent(DiaryOwner.CODEC).networkSynchronized(DiaryOwner.STREAM_CODEC));

    /** The runes burned into a hextech pickaxe. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> PICKAXE_RUNES =
            COMPONENTS.registerComponentType("pickaxe_runes", builder -> builder
                    .persistent(PickaxeRunes.CODEC).networkSynchronized(PickaxeRunes.STREAM_CODEC));

    /** The runes burned into a hex pylon (on its item; the block entity keeps them while placed). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> PYLON_RUNES =
            COMPONENTS.registerComponentType("pylon_runes", builder -> builder
                    .persistent(PickaxeRunes.CODEC).networkSynchronized(PickaxeRunes.STREAM_CODEC));

    /** The runes burned into a Whirl Reaper (on its item; the block entity keeps them while placed). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> REAPER_RUNES =
            COMPONENTS.registerComponentType("reaper_runes", builder -> builder
                    .persistent(PickaxeRunes.CODEC).networkSynchronized(PickaxeRunes.STREAM_CODEC));

    /** The settings of a Pylon Filter. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.pockyl.hextale.transport.ItemFilter>> ITEM_FILTER =
            COMPONENTS.registerComponentType("item_filter", builder -> builder
                    .persistent(com.pockyl.hextale.transport.ItemFilter.CODEC).networkSynchronized(com.pockyl.hextale.transport.ItemFilter.STREAM_CODEC));

    /** The essence written on an essence sticker. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.pockyl.hextale.rune.Essence>> STICKER =
            COMPONENTS.registerComponentType("sticker", builder -> builder
                    .persistent(com.pockyl.hextale.rune.Essence.ID_CODEC).networkSynchronized(com.pockyl.hextale.rune.Essence.STREAM_CODEC));

    /** Which essence an essence item is. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.pockyl.hextale.rune.Essence>> ESSENCE_KIND =
            COMPONENTS.registerComponentType("essence_kind", builder -> builder
                    .persistent(com.pockyl.hextale.rune.Essence.ID_CODEC).networkSynchronized(com.pockyl.hextale.rune.Essence.STREAM_CODEC));

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
