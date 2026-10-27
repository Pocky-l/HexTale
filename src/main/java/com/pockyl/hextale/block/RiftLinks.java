package com.pockyl.hextale.block;

import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.pockyl.hextale.Hextale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * The links between Lesser Rift Gates: each link has its own id and the places of the (at most two) gates it ties,
 * kept with the world in the overworld's data so gates in any dimension find each other. A gate that is broken leaves
 * its link with one end - its partner then knows it has been left alone.
 */
public final class RiftLinks extends SavedData {
    public static final Codec<RiftLinks> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, GlobalPos.CODEC.listOf())
            .xmap(RiftLinks::new, RiftLinks::ends);
    public static final SavedDataType<RiftLinks> TYPE = new SavedDataType<>(Hextale.id("rift_links"), RiftLinks::new, CODEC);
    private final Map<UUID, List<GlobalPos>> links = new HashMap<>();

    public RiftLinks() {
    }

    private RiftLinks(Map<UUID, List<GlobalPos>> saved) {
        saved.forEach((id, ends) -> links.put(id, new ArrayList<>(ends)));
    }

    private Map<UUID, List<GlobalPos>> ends() {
        return links;
    }

    public static RiftLinks of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Ties the gates at {@code a} and {@code b} with a new link. */
    public void tie(UUID id, GlobalPos a, GlobalPos b) {
        links.put(id, new ArrayList<>(List.of(a, b)));
        setDirty();
    }

    /** The other end of link {@code id} seen from {@code from}, or null when it has none (any more). */
    @Nullable
    public GlobalPos partner(UUID id, GlobalPos from) {
        List<GlobalPos> ends = links.get(id);
        if (ends == null || !ends.contains(from)) {
            return null;
        }
        for (GlobalPos end : ends) {
            if (!end.equals(from)) {
                return end;
            }
        }
        return null;
    }

    /** The gate at {@code at} is gone from link {@code id}; a link with no gate left is forgotten. */
    public void cut(UUID id, GlobalPos at) {
        List<GlobalPos> ends = links.get(id);
        if (ends != null && ends.remove(at)) {
            if (ends.isEmpty()) {
                links.remove(id);
            }
            setDirty();
        }
    }
}
