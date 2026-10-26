package com.pockyl.hextale.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.world.MagicTreeFeature;

/** World features of the mod; their configurations are data ({@code data/hextale/worldgen/feature}). */
public final class ModFeatures {
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, Hextale.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<MagicTreeFeature>> MAGIC_TREE =
            FEATURE_TYPES.register("magic_tree", () -> MagicTreeFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<com.pockyl.hextale.world.BloomCaveFeature>> BLOOM_CAVES =
            FEATURE_TYPES.register("bloom_caves", () -> com.pockyl.hextale.world.BloomCaveFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<com.pockyl.hextale.world.StruckGeodeFeature>> STRUCK_GEODE =
            FEATURE_TYPES.register("struck_geode", () -> com.pockyl.hextale.world.StruckGeodeFeature.CODEC);

    private ModFeatures() {
    }

    public static void register(IEventBus modBus) {
        FEATURE_TYPES.register(modBus);
    }
}
