package com.pockyl.hextale.port;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The Minecraft 26.x load view over a 1.21.1 {@link CompoundTag}: the mod's block entities read their data through the
 * same calls on every version. Values that fail to decode read as missing.
 *
 * <p>port-of: net.minecraft.world.level.storage.ValueInput
 */
public final class ValueInput {
    private final CompoundTag tag;
    private final HolderLookup.Provider registries;
    private final DynamicOps<Tag> ops;

    private ValueInput(CompoundTag tag, HolderLookup.Provider registries) {
        this.tag = tag;
        this.registries = registries;
        this.ops = RegistryOps.create(NbtOps.INSTANCE, registries);
    }

    public static ValueInput of(CompoundTag tag, HolderLookup.Provider registries) {
        return new ValueInput(tag, registries);
    }

    public CompoundTag tag() {
        return tag;
    }

    public HolderLookup.Provider registries() {
        return registries;
    }

    @Deprecated
    public HolderLookup.Provider lookup() {
        return registries;
    }

    public <T> Optional<T> read(String name, Codec<T> codec) {
        Tag value = tag.get(name);
        return value == null ? Optional.empty() : codec.parse(ops, value).result();
    }

    public <T> Optional<T> read(MapCodec<T> codec) {
        return codec.codec().parse(ops, tag).result();
    }

    public Optional<ValueInput> child(String name) {
        return tag.contains(name, Tag.TAG_COMPOUND) ? Optional.of(new ValueInput(tag.getCompound(name), registries)) : Optional.empty();
    }

    public ValueInput childOrEmpty(String name) {
        return new ValueInput(tag.getCompound(name), registries);
    }

    public Optional<ValueInputList> childrenList(String name) {
        return tag.contains(name, Tag.TAG_LIST) ? Optional.of(childrenListOrEmpty(name)) : Optional.empty();
    }

    public ValueInputList childrenListOrEmpty(String name) {
        ListTag list = tag.getList(name, Tag.TAG_COMPOUND);
        List<ValueInput> children = list.stream().map(child -> new ValueInput((CompoundTag) child, registries)).toList();
        return new ValueInputList() {
            @Override
            public boolean isEmpty() {
                return children.isEmpty();
            }

            @Override
            public Stream<ValueInput> stream() {
                return children.stream();
            }

            @Override
            public Iterator<ValueInput> iterator() {
                return children.iterator();
            }
        };
    }

    public <T> Optional<TypedInputList<T>> list(String name, Codec<T> codec) {
        return tag.contains(name, Tag.TAG_LIST) ? Optional.of(listOrEmpty(name, codec)) : Optional.empty();
    }

    public <T> TypedInputList<T> listOrEmpty(String name, Codec<T> codec) {
        Tag value = tag.get(name);
        List<T> items = value instanceof ListTag list
                ? list.stream().flatMap(element -> codec.parse(ops, element).result().stream()).toList()
                : List.of();
        return new TypedInputList<>() {
            @Override
            public boolean isEmpty() {
                return items.isEmpty();
            }

            @Override
            public Stream<T> stream() {
                return items.stream();
            }

            @Override
            public Iterator<T> iterator() {
                return items.iterator();
            }
        };
    }

    public boolean getBooleanOr(String name, boolean defaultValue) {
        return tag.contains(name, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(name) : defaultValue;
    }

    public byte getByteOr(String name, byte defaultValue) {
        return tag.contains(name, Tag.TAG_ANY_NUMERIC) ? tag.getByte(name) : defaultValue;
    }

    public int getShortOr(String name, short defaultValue) {
        return tag.contains(name, Tag.TAG_ANY_NUMERIC) ? tag.getShort(name) : defaultValue;
    }

    public Optional<Integer> getInt(String name) {
        return tag.contains(name, Tag.TAG_ANY_NUMERIC) ? Optional.of(tag.getInt(name)) : Optional.empty();
    }

    public int getIntOr(String name, int defaultValue) {
        return tag.contains(name, Tag.TAG_ANY_NUMERIC) ? tag.getInt(name) : defaultValue;
    }

    public Optional<Long> getLong(String name) {
        return tag.contains(name, Tag.TAG_ANY_NUMERIC) ? Optional.of(tag.getLong(name)) : Optional.empty();
    }

    public long getLongOr(String name, long defaultValue) {
        return tag.contains(name, Tag.TAG_ANY_NUMERIC) ? tag.getLong(name) : defaultValue;
    }

    public float getFloatOr(String name, float defaultValue) {
        return tag.contains(name, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(name) : defaultValue;
    }

    public double getDoubleOr(String name, double defaultValue) {
        return tag.contains(name, Tag.TAG_ANY_NUMERIC) ? tag.getDouble(name) : defaultValue;
    }

    public Optional<String> getString(String name) {
        return tag.contains(name, Tag.TAG_STRING) ? Optional.of(tag.getString(name)) : Optional.empty();
    }

    public String getStringOr(String name, String defaultValue) {
        return tag.contains(name, Tag.TAG_STRING) ? tag.getString(name) : defaultValue;
    }

    public Optional<int[]> getIntArray(String name) {
        return tag.contains(name, Tag.TAG_INT_ARRAY) ? Optional.of(tag.getIntArray(name)) : Optional.empty();
    }

    public interface TypedInputList<T> extends Iterable<T> {
        boolean isEmpty();

        Stream<T> stream();
    }

    public interface ValueInputList extends Iterable<ValueInput> {
        boolean isEmpty();

        Stream<ValueInput> stream();
    }
}
