package com.pockyl.hextale.infusion;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Every altar recipe ({@code data/<namespace>/hextale_infusion/*.json}). The server keeps what it loaded; clients keep
 * what the server sent them (quests and JEI show them). The two live apart so an integrated server and its client
 * never share a half-reloaded set.
 */
public final class InfusionRecipes {
    public static final FileToIdConverter LISTER = FileToIdConverter.json("hextale_infusion");

    private static volatile Map<ResourceLocation, InfusionRecipe> server = Map.of();
    private static volatile Map<ResourceLocation, InfusionRecipe> client = Map.of();

    private InfusionRecipes() {
    }

    public static Map<ResourceLocation, InfusionRecipe> get(boolean clientSide) {
        return clientSide ? client : server;
    }

    public static void setServer(Map<ResourceLocation, InfusionRecipe> recipes) {
        server = Collections.unmodifiableMap(new LinkedHashMap<>(recipes));
    }

    public static void setClient(Map<ResourceLocation, InfusionRecipe> recipes) {
        client = Collections.unmodifiableMap(new LinkedHashMap<>(recipes));
    }

    /** Loads the recipes on every data reload; tags in ingredients resolve through the reload's registries. */
    public static final class Loader extends SimplePreparableReloadListener<Map<ResourceLocation, InfusionRecipe>> {
        private final HolderLookup.Provider registries;

        public Loader(HolderLookup.Provider registries) {
            this.registries = registries;
        }

        @Override
        protected Map<ResourceLocation, InfusionRecipe> prepare(ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, InfusionRecipe> found = new TreeMap<>();
            SimpleJsonResourceReloadListener.scanDirectory(manager, LISTER, RegistryOps.create(JsonOps.INSTANCE, registries),
                    InfusionRecipe.CODEC, found);
            return found;
        }

        @Override
        protected void apply(Map<ResourceLocation, InfusionRecipe> recipes, ResourceManager manager, ProfilerFiller profiler) {
            setServer(recipes);
        }
    }
}
