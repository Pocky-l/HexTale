package com.pockyl.hextale.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.knowledge.DiaryNote;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.RuneBoards;

/** Keeps the client's copy of the knowledge current and notices the first orb in a player's hands. */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class PlayerEvents {
    private PlayerEvents() {
    }

    /** Boards take a moment to generate: do it in the background before anyone opens a table. */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        RuneBoards.warmUp();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Research.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Research.sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Research.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0
                && !Research.of(player).hasNote(DiaryNote.FIRST_HEXTECH)
                && player.getInventory().contains(stack -> stack.is(ModItems.UNSTABLE_HEXTECH.get()))) {
            Research.unlockNote(player, DiaryNote.FIRST_HEXTECH);
        }
    }
}
