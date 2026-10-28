package com.pockyl.hextale.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.crafting.DataRecipes;
import com.pockyl.hextale.network.GuidePayload;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.RecipesPayload;

/**
 * Loads the researcher's guide with the data packs and sends it and the mod's recipes (read by the vanilla recipe manager,
 * see {@code DataRecipes}) to the clients.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class InfusionEvents {
    private InfusionEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(Guide.loader(event.getRegistryAccess()));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(InfusionEvents::send);
    }

    private static void send(ServerPlayer player) {
        for (DataRecipes<?> kind : DataRecipes.kinds()) {
            ModNetwork.sendToPlayer(player, RecipesPayload.of(kind));
        }
        ModNetwork.sendToPlayer(player, new GuidePayload(Guide.server().definitions()));
    }
}
