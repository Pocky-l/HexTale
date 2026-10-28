package com.pockyl.hextale.port;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.function.Supplier;

/**
 * Names saved data and how to make and read it (26.x); on 1.21.1 it gives the storage a factory and a file name
 * ({@code <namespace>_<path>}).
 *
 * <p>port-of: net.minecraft.world.level.saveddata.SavedDataType
 */
public record SavedDataType<T extends SavedData>(ResourceLocation id, Supplier<T> constructor, Codec<T> codec) {
    public net.minecraft.world.level.saveddata.SavedData.Factory<T> factory() {
        return new net.minecraft.world.level.saveddata.SavedData.Factory<>(() -> {
            T data = constructor.get();
            data.codec(codec);
            return data;
        }, (tag, registries) -> {
            T data = codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("data"))
                    .resultOrPartial().orElseGet(constructor);
            data.codec(codec);
            return data;
        }, null);
    }

    public String name() {
        return id.getNamespace() + "_" + id.getPath().replace('/', '_');
    }

    /** The 26.x {@code storage.computeIfAbsent(type)}. */
    public T get(DimensionDataStorage storage) {
        return storage.computeIfAbsent(factory(), name());
    }
}
