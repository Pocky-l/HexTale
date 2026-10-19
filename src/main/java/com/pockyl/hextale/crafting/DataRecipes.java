package com.pockyl.hextale.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
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
 * One kind of the mod's own recipes, read from {@code data/<namespace>/<folder>/*.json} like the altar's. The server keeps
 * what it loaded; clients keep what the server sent them (the guide and JEI show them). The two live apart so an
 * integrated server and its client never share a half-reloaded set.
 */
public final class DataRecipes<T> {
    private final FileToIdConverter lister;
    private final Codec<T> codec;
    private volatile Map<Identifier, T> server = Map.of();
    private volatile Map<Identifier, T> client = Map.of();

    public DataRecipes(String folder, Codec<T> codec) {
        this.lister = FileToIdConverter.json(folder);
        this.codec = codec;
    }

    public Map<Identifier, T> get(boolean clientSide) {
        return clientSide ? client : server;
    }

    public void setServer(Map<Identifier, T> recipes) {
        server = Collections.unmodifiableMap(new LinkedHashMap<>(recipes));
    }

    public void setClient(Map<Identifier, T> recipes) {
        client = Collections.unmodifiableMap(new LinkedHashMap<>(recipes));
    }

    /** Loads the recipes on every data reload; tags in ingredients resolve through the reload's registries. */
    public SimplePreparableReloadListener<Map<Identifier, T>> loader(HolderLookup.Provider registries) {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Map<Identifier, T> prepare(ResourceManager manager, ProfilerFiller profiler) {
                Map<Identifier, T> found = new TreeMap<>();
                SimpleJsonResourceReloadListener.scanDirectory(manager, lister, RegistryOps.create(JsonOps.INSTANCE, registries), codec, found);
                return found;
            }

            @Override
            protected void apply(Map<Identifier, T> recipes, ResourceManager manager, ProfilerFiller profiler) {
                setServer(recipes);
            }
        };
    }
}
