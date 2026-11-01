package com.pockyl.hextale.port;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/**
 * 26.x saved data is written by the codec of its {@link SavedDataType}; 1.21.1 saved data writes itself. The codec is
 * handed over when the data is made or loaded through its type.
 *
 * <p>port-of: net.minecraft.world.level.saveddata.SavedData
 */
public abstract class SavedData extends net.minecraft.world.level.saveddata.SavedData {
    /** Where data that could not be read at all is kept in the file. */
    static final String UNREADABLE = "unreadable_data";
    private Codec<SavedData> codec;
    private Tag unreadable;

    void unreadable(Tag tag) {
        this.unreadable = tag;
    }

    @SuppressWarnings("unchecked")
    void codec(Codec<? extends SavedData> codec) {
        this.codec = (Codec<SavedData>) codec;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (codec != null) {
            Tag data = codec.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow();
            tag.put("data", data);
        }
        if (unreadable != null) {
            tag.put(UNREADABLE, unreadable);
        }
        return tag;
    }
}
