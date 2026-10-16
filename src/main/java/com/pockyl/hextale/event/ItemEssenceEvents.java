package com.pockyl.hextale.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.network.ItemEssencesPayload;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.research.ItemEssenceResolver;
import com.pockyl.hextale.research.ItemEssenceTable;

/** Resolves the elements of every item when the server starts and when data packs reload, and tells the clients. */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class ItemEssenceEvents {
    private ItemEssenceEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        resolve(event.getServer());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            // /reload: recipes and the data map may have changed.
            resolve(event.getPlayerList().getServer());
        } else if (ItemEssenceTable.isEmpty()) {
            resolve(event.getPlayerList().getServer());
        }
        event.getRelevantPlayers().forEach(ItemEssenceEvents::send);
    }

    private static void resolve(MinecraftServer server) {
        ItemEssenceTable.set(ItemEssenceResolver.resolve(server.getRecipeManager(), server.registryAccess()));
    }

    private static void send(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, new ItemEssencesPayload(ItemEssenceTable.byId()));
    }
}
