package com.pockyl.hextale.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ChalkFigure;
import com.pockyl.hextale.entity.GrappleHook;
import com.pockyl.hextale.runework.ClothesEffects;
import com.pockyl.hextale.thevoid.realm.VoidPassage;

/**
 * Server-side memories that belong to one player's session or one world: let go when the player leaves or the server
 * stops, so a singleplayer world closed and another opened keeps neither the old level alive nor its game times.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class LeftBehind {
    private LeftBehind() {
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        VoidPassage.forgetPulls(event.getEntity().getUUID());
        ClothesEffects.forgetCloak(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        GrappleHook.forget(false);
        VoidPassage.forgetPulls(null);
        ClothesEffects.forgetCloak(null);
        ChalkFigure.forgetRoots();
    }
}
