package com.pockyl.hextale.storage;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.pockyl.hextale.Hextale;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Everything held in Hex Vaults, by vault id, kept with the world in the overworld's data. The items live here and not
 * in the block, so a broken vault keeps them and they can be reached from any dimension.
 */
public final class VaultStore extends SavedData {
    public static final Codec<VaultStore> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, VaultContents.CODEC)
            .xmap(VaultStore::new, store -> store.vaults);
    public static final SavedDataType<VaultStore> TYPE = new SavedDataType<>(Hextale.id("vaults"), VaultStore::new, CODEC);
    private final Map<UUID, VaultContents> vaults = new HashMap<>();
    private final Map<UUID, VaultToll> tolls = new HashMap<>();

    public VaultStore() {
    }

    private VaultStore(Map<UUID, VaultContents> saved) {
        vaults.putAll(saved);
        vaults.values().forEach(contents -> contents.attach(this));
    }

    public static VaultStore of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The contents of vault {@code id}, new and empty the first time. */
    public VaultContents contents(UUID id) {
        VaultContents contents = vaults.get(id);
        if (contents == null) {
            contents = new VaultContents();
            contents.attach(this);
            vaults.put(id, contents);
            setDirty();
        }
        return contents;
    }

    /** The contents of vault {@code id}, or null when there never was such a vault. */
    @Nullable
    public VaultContents find(UUID id) {
        return vaults.get(id);
    }

    /** Where a player stands with the toll of distant takes; not saved, a restart forgives it. */
    public VaultToll toll(UUID player) {
        return tolls.computeIfAbsent(player, key -> new VaultToll());
    }
}
