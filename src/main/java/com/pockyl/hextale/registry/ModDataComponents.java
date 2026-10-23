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
    /** The shape of the Void a Void Shard opens: 0 = any, else {@code VoidShape} ordinal + 1. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VOID_SHAPE =
            COMPONENTS.registerComponentType("void_shape", builder -> builder
                    .persistent(Codec.intRange(0, 32)).networkSynchronized(ByteBufCodecs.VAR_INT));
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

    /** The runes burned into a Hex Sawmill. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> SAWMILL_RUNES =
            COMPONENTS.registerComponentType("sawmill_runes", builder -> builder
                    .persistent(PickaxeRunes.CODEC).networkSynchronized(PickaxeRunes.STREAM_CODEC));

    /** The runes burned into a Greedy Funnel. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> FUNNEL_RUNES =
            COMPONENTS.registerComponentType("funnel_runes", builder -> builder
                    .persistent(PickaxeRunes.CODEC).networkSynchronized(PickaxeRunes.STREAM_CODEC));

    /** The runes burned into a Hex Furnace. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> FURNACE_RUNES =
            COMPONENTS.registerComponentType("furnace_runes", builder -> builder
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

    /** The brew in a Chem Vial. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.pockyl.hextale.chem.Brew>> BREW =
            COMPONENTS.registerComponentType("brew", builder -> builder
                    .persistent(com.pockyl.hextale.chem.Brew.CODEC).networkSynchronized(com.pockyl.hextale.chem.Brew.STREAM_CODEC));

    /** The runes burned into a Chem Burner. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> BURNER_RUNES =
            COMPONENTS.registerComponentType("burner_runes", builder -> builder
                    .persistent(PickaxeRunes.CODEC).networkSynchronized(PickaxeRunes.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> TANK_RUNES =
            COMPONENTS.registerComponentType("tank_runes", builder -> builder
                    .persistent(PickaxeRunes.CODEC).networkSynchronized(PickaxeRunes.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> LAB_RUNES =
            COMPONENTS.registerComponentType("lab_runes", builder -> builder
                    .persistent(PickaxeRunes.CODEC).networkSynchronized(PickaxeRunes.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> STILL_RUNES =
            COMPONENTS.registerComponentType("still_runes", builder -> builder
                    .persistent(PickaxeRunes.CODEC).networkSynchronized(PickaxeRunes.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> BRAZIER_RUNES =
            COMPONENTS.registerComponentType("brazier_runes", builder -> builder
                    .persistent(PickaxeRunes.CODEC).networkSynchronized(PickaxeRunes.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> SKYPACK_RUNES =
            COMPONENTS.registerComponentType("skypack_runes", builder -> builder
                    .persistent(PickaxeRunes.CODEC).networkSynchronized(PickaxeRunes.STREAM_CODEC));
    /** The rune on a piece of the researcher's clothes. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> CLOTHES_RUNES =
            COMPONENTS.registerComponentType("clothes_runes", builder -> builder
                    .persistent(PickaxeRunes.CODEC).networkSynchronized(PickaxeRunes.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PickaxeRunes>> BATTERY_RUNES =
            COMPONENTS.registerComponentType("battery_runes", builder -> builder
                    .persistent(PickaxeRunes.CODEC).networkSynchronized(PickaxeRunes.STREAM_CODEC));
    /** Charge kept in a Crystal Battery taken off the ground. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BATTERY_CHARGE =
            COMPONENTS.registerComponentType("battery_charge", builder -> builder
                    .persistent(Codec.intRange(0, 100000)).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** mB of air essence in a Hextech Skypack's tanks. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SKYPACK_AIR =
            COMPONENTS.registerComponentType("skypack_air", builder -> builder
                    .persistent(Codec.intRange(0, 100000)).networkSynchronized(ByteBufCodecs.VAR_INT));

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
