package com.pockyl.hextale.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.crafting.AlloyRecipe;
import com.pockyl.hextale.crafting.HexCraftingRecipe;
import com.pockyl.hextale.network.CraftingRecipesPayload;
import com.pockyl.hextale.network.GuidePayload;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.infusion.InfusionRecipes;
import com.pockyl.hextale.network.InfusionRecipesPayload;
import com.pockyl.hextale.network.ModNetwork;

/**
 * Loads the altar, Hex Workbench and Hex Furnace recipes and the researcher's guide with the data packs and sends them to
 * the clients.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class InfusionEvents {
    private InfusionEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new InfusionRecipes.Loader(event.getRegistryAccess()));
        event.addListener(HexCraftingRecipe.ALL.loader(event.getRegistryAccess()));
        event.addListener(AlloyRecipe.ALL.loader(event.getRegistryAccess()));
        event.addListener(com.pockyl.hextale.crafting.CrucibleRecipe.ALL.loader(event.getRegistryAccess()));
        event.addListener(com.pockyl.hextale.crafting.BrewRecipe.ALL.loader(event.getRegistryAccess()));
        event.addListener(com.pockyl.hextale.research.ExperimentPlan.ALL.loader(event.getRegistryAccess()));
        event.addListener(Guide.loader(event.getRegistryAccess()));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(InfusionEvents::send);
    }

    private static void send(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, new InfusionRecipesPayload(InfusionRecipes.get(false)));
        ModNetwork.sendToPlayer(player, new CraftingRecipesPayload(HexCraftingRecipe.ALL.get(false), AlloyRecipe.ALL.get(false),
                com.pockyl.hextale.crafting.CrucibleRecipe.ALL.get(false), com.pockyl.hextale.crafting.BrewRecipe.ALL.get(false),
                com.pockyl.hextale.research.ExperimentPlan.ALL.get(false)));
        ModNetwork.sendToPlayer(player, new GuidePayload(Guide.server().definitions()));
    }
}
