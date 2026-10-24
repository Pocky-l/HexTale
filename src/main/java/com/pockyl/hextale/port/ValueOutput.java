package com.pockyl.hextale.port;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The Minecraft 26.x save view over a 1.21.1 {@link CompoundTag}: the mod's block entities write their data through
 * the same calls on every version. Values that fail to encode are logged and left out.
 *
 * <p>port-of: net.minecraft.world.level.storage.ValueOutput
 */
public final class ValueOutput {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final CompoundTag tag;
    private final HolderLookup.Provider registries;
    private final DynamicOps<Tag> ops;

    private ValueOutput(CompoundTag tag, HolderLookup.Provider registries) {
        this.tag = tag;
        this.registries = registries;
        this.ops = RegistryOps.create(NbtOps.INSTANCE, registries);
    }

    public static ValueOutput of(CompoundTag tag, HolderLookup.Provider registries) {
        return new ValueOutput(tag, registries);
    }

    public CompoundTag tag() {
        return tag;
    }

    public HolderLookup.Provider registries() {
        return registries;
    }

    public <T> void store(String name, Codec<T> codec, T value) {
        codec.encodeStart(ops, value).resultOrPartial(error -> LOGGER.error("Cannot save {}: {}", name, error))
                .ifPresent(encoded -> tag.put(name, encoded));
    }

    public <T> void storeNullable(String name, Codec<T> codec, @Nullable T value) {
        if (value != null) {
            store(name, codec, value);
        }
    }

    public <T> void store(MapCodec<T> codec, T value) {
        codec.codec().encodeStart(ops, value).resultOrPartial(error -> LOGGER.error("Cannot save: {}", error))
                .ifPresent(encoded -> {
                    if (encoded instanceof CompoundTag compound) {
                        tag.merge(compound);
                    }
                });
    }

    public void putBoolean(String name, boolean value) {
        tag.putBoolean(name, value);
    }

    public void putByte(String name, byte value) {
        tag.putByte(name, value);
    }

    public void putShort(String name, short value) {
        tag.putShort(name, value);
    }

    public void putInt(String name, int value) {
        tag.putInt(name, value);
    }

    public void putLong(String name, long value) {
        tag.putLong(name, value);
    }

    public void putFloat(String name, float value) {
        tag.putFloat(name, value);
    }

    public void putDouble(String name, double value) {
        tag.putDouble(name, value);
    }

    public void putString(String name, String value) {
        tag.putString(name, value);
    }

    public void putIntArray(String name, int[] value) {
        tag.putIntArray(name, value);
    }

    public ValueOutput child(String name) {
        CompoundTag child = new CompoundTag();
        tag.put(name, child);
        return new ValueOutput(child, registries);
    }

    public ValueOutputList childrenList(String name) {
        ListTag list = new ListTag();
        tag.put(name, list);
        return new ValueOutputList() {
            @Override
            public ValueOutput addChild() {
                CompoundTag child = new CompoundTag();
                list.add(child);
                return new ValueOutput(child, registries);
            }

            @Override
            public void discardLast() {
                list.removeLast();
            }

            @Override
            public boolean isEmpty() {
                return list.isEmpty();
            }
        };
    }

    public <T> TypedOutputList<T> list(String name, Codec<T> codec) {
        ListTag list = new ListTag();
        tag.put(name, list);
        return new TypedOutputList<>() {
            @Override
            public void add(T value) {
                codec.encodeStart(ops, value).resultOrPartial(error -> LOGGER.error("Cannot save {}: {}", name, error))
                        .ifPresent(list::add);
            }

            @Override
            public boolean isEmpty() {
                return list.isEmpty();
            }
        };
    }

    public void discard(String name) {
        tag.remove(name);
    }

    public boolean isEmpty() {
        return tag.isEmpty();
    }

    public interface TypedOutputList<T> {
        void add(T value);

        boolean isEmpty();
    }

    public interface ValueOutputList {
        ValueOutput addChild();

        void discardLast();

        boolean isEmpty();
    }
}
