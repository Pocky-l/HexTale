package com.pockyl.hextale.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.infusion.InfusionRecipes;
import com.pockyl.hextale.network.InfusionRecipesPayload;
import com.pockyl.hextale.network.ModNetwork;

/** Loads the altar recipes with the data packs and sends them to the clients. */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class InfusionEvents {
    private InfusionEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Hextale.id("infusion"), new InfusionRecipes.Loader(event.getServerResources().getRegistryLookup()));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(InfusionEvents::send);
    }

    private static void send(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, new InfusionRecipesPayload(InfusionRecipes.get(false)));
    }
}
