package com.pockyl.hextale.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.world.MagicTreeFeature;

/** World features of the mod; their configurations are data ({@code data/hextale/worldgen/configured_feature}). */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE, Hextale.MOD_ID);

    public static final DeferredHolder<Feature<?>, MagicTreeFeature.Type> MAGIC_TREE =
            FEATURE_TYPES.register("magic_tree", MagicTreeFeature.Type::new);
    public static final DeferredHolder<Feature<?>, com.pockyl.hextale.world.BloomCaveFeature.Type> BLOOM_CAVES =
            FEATURE_TYPES.register("bloom_caves", com.pockyl.hextale.world.BloomCaveFeature.Type::new);
    public static final DeferredHolder<Feature<?>, com.pockyl.hextale.world.StruckGeodeFeature.Type> STRUCK_GEODE =
            FEATURE_TYPES.register("struck_geode", com.pockyl.hextale.world.StruckGeodeFeature.Type::new);

    public static final DeferredRegister<MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Hextale.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Hextale.MOD_ID);

    /** The Void's own terrain (the dimension {@code hextale:the_void}). */
    public static final DeferredHolder<MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator>,
            MapCodec<com.pockyl.hextale.thevoid.realm.VoidChunkGenerator>> VOID_GENERATOR =
            CHUNK_GENERATORS.register("the_void", () -> com.pockyl.hextale.thevoid.realm.VoidChunkGenerator.CODEC);
    public static final DeferredHolder<MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>,
            MapCodec<com.pockyl.hextale.thevoid.realm.VoidBiomeSource>> VOID_ZONES =
            BIOME_SOURCES.register("void_zones", () -> com.pockyl.hextale.thevoid.realm.VoidBiomeSource.CODEC);

    private ModFeatures() {
    }

    public static void register(IEventBus modBus) {
        FEATURE_TYPES.register(modBus);
        CHUNK_GENERATORS.register(modBus);
        BIOME_SOURCES.register(modBus);
    }
}
