package com.pockyl.hextale.block;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import com.pockyl.hextale.port.SavedData;
import com.pockyl.hextale.port.SavedDataType;

import com.pockyl.hextale.Hextale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The pockets in the shadow that Shadow Coffers share: one 27-slot container per pair, kept with the world (in the
 * overworld's data, so every dimension reaches the same pocket).
 */
public final class ShadowPockets extends SavedData {
    public static final int SIZE = 27;
    public static final Codec<ShadowPockets> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, ItemStack.OPTIONAL_CODEC.listOf())
            .xmap(ShadowPockets::new, ShadowPockets::stacks);
    public static final SavedDataType<ShadowPockets> TYPE = new SavedDataType<>(Hextale.id("shadow_pockets"), ShadowPockets::new, CODEC);
    private final Map<UUID, SimpleContainer> pockets = new HashMap<>();

    public ShadowPockets() {
    }

    private ShadowPockets(Map<UUID, List<ItemStack>> saved) {
        saved.forEach((id, stacks) -> {
            SimpleContainer pocket = pocket(id);
            for (int i = 0; i < Math.min(SIZE, stacks.size()); i++) {
                pocket.setItem(i, stacks.get(i));
            }
        });
        setDirty(false);
    }

    private Map<UUID, List<ItemStack>> stacks() {
        Map<UUID, List<ItemStack>> out = new HashMap<>();
        pockets.forEach((id, pocket) -> {
            if (!pocket.isEmpty()) {
                List<ItemStack> list = new ArrayList<>(SIZE);
                for (int i = 0; i < SIZE; i++) {
                    list.add(pocket.getItem(i));
                }
                out.put(id, list);
            }
        });
        return out;
    }

    public static ShadowPockets of(MinecraftServer server) {
        return TYPE.get(server.overworld().getDataStorage());
    }

    /** The pocket a pair of coffers shares; made empty the first time it is asked for. */
    public SimpleContainer pocket(UUID pair) {
        return pockets.computeIfAbsent(pair, id -> new SimpleContainer(SIZE) {
            @Override
            public void setChanged() {
                super.setChanged();
                ShadowPockets.this.setDirty();
            }
        });
    }
}
