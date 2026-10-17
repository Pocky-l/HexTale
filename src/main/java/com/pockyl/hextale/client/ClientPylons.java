package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import com.pockyl.hextale.block.HexPylonBlockEntity;
import com.pockyl.hextale.network.PylonCancelPayload;
import com.pockyl.hextale.transport.PylonLink;
import com.pockyl.hextale.transport.PylonLinking;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Client: the pylons around a block, read from the block entities the client already has (their links are synced).
 * Used by the goggles ("which pylons send here") and to untie a block from its pylons. Looks through the block entity
 * maps of the chunks within {@value #REACH} blocks only, and remembers the answer for the block it was asked about last
 * for a few ticks.
 */
public final class ClientPylons {
    /** No link reaches farther than the longest pylon range. */
    public static final int REACH = 33;
    private static final int CACHE_TICKS = 10;
    @Nullable
    private static BlockPos cachedFor;
    private static long cachedAt;
    private static List<HexPylonBlockEntity> cached = List.of();

    private ClientPylons() {
    }

    public static void register() {
        PylonLinking.clientLinked = (level, pos) -> !linkingTo(level, pos).isEmpty();
        // A click into the air gives a going link up (only the client sees such clicks).
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickEmpty event) -> {
            if (PylonLinking.clientFrom != null) {
                PylonLinking.clientFrom = null;
                ClientPacketDistributor.sendToServer(PylonCancelPayload.INSTANCE);
            }
        });
    }

    /** The pylons within reach whose links point at {@code target}. */
    public static List<HexPylonBlockEntity> linkingTo(Level level, BlockPos target) {
        long now = level.getGameTime();
        if (target.equals(cachedFor) && now - cachedAt < CACHE_TICKS) {
            return cached;
        }
        List<HexPylonBlockEntity> out = new ArrayList<>();
        for (HexPylonBlockEntity pylon : around(level, target)) {
            for (PylonLink link : pylon.links()) {
                if (link.pos().equals(target)) {
                    out.add(pylon);
                    break;
                }
            }
        }
        cachedFor = target.immutable();
        cachedAt = now;
        cached = out;
        return out;
    }

    /** Every pylon in the loaded chunks within {@value #REACH} blocks of {@code centre}. */
    public static List<HexPylonBlockEntity> around(Level level, BlockPos centre) {
        List<HexPylonBlockEntity> out = new ArrayList<>();
        int minX = (centre.getX() - REACH) >> 4;
        int maxX = (centre.getX() + REACH) >> 4;
        int minZ = (centre.getZ() - REACH) >> 4;
        int maxZ = (centre.getZ() + REACH) >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof HexPylonBlockEntity pylon && pylon.getBlockPos().closerThan(centre, REACH)) {
                        out.add(pylon);
                    }
                }
            }
        }
        return out;
    }

    public static boolean goggles() {
        return GogglesView.wearing(Minecraft.getInstance().player);
    }
}
