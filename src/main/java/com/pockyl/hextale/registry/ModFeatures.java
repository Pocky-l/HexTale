package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.world.MagicTreeFeature;

/** World features of the mod; their configurations are data ({@code data/hextale/worldgen/feature}). */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE, Hextale.MOD_ID);

    public static final DeferredHolder<Feature<?>, MagicTreeFeature> MAGIC_TREE =
            FEATURE_TYPES.register("magic_tree", MagicTreeFeature::new);
    public static final DeferredHolder<Feature<?>, com.pockyl.hextale.world.BloomCaveFeature> BLOOM_CAVES =
            FEATURE_TYPES.register("bloom_caves", com.pockyl.hextale.world.BloomCaveFeature::new);

    private ModFeatures() {
    }

    public static void register(IEventBus modBus) {
        FEATURE_TYPES.register(modBus);
    }
}
