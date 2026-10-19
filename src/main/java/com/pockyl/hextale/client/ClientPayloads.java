package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

import com.pockyl.hextale.client.screen.ResearchTableScreen;
import com.pockyl.hextale.client.screen.book.StarBookScreen;
import com.pockyl.hextale.infusion.InfusionRecipes;
import com.pockyl.hextale.network.InfusionRecipesPayload;
import com.pockyl.hextale.network.ItemEssencesPayload;
import com.pockyl.hextale.network.OpenStelePayload;
import com.pockyl.hextale.client.screen.SteleScreen;
import com.pockyl.hextale.network.KnowledgeSyncPayload;
import com.pockyl.hextale.network.OpenDiaryPayload;
import com.pockyl.hextale.network.ResearchMessagePayload;
import com.pockyl.hextale.research.ItemEssenceTable;
import com.pockyl.hextale.block.HexPylonBlockEntity;
import com.pockyl.hextale.network.PylonFlightsPayload;
import com.pockyl.hextale.network.PylonLinkPayload;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.transport.PylonFlight;
import com.pockyl.hextale.transport.PylonLinking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Handlers of the client-bound payloads; run on the client thread. */
public final class ClientPayloads {
    private ClientPayloads() {
    }

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(KnowledgeSyncPayload.TYPE, (payload, context) -> knowledge(payload));
        event.register(ItemEssencesPayload.TYPE, (payload, context) -> itemElements(payload));
        event.register(ResearchMessagePayload.TYPE, (payload, context) -> researchMessage(payload));
        event.register(OpenDiaryPayload.TYPE, (payload, context) -> openDiary(payload));
        event.register(InfusionRecipesPayload.TYPE, (payload, context) -> InfusionRecipes.setClient(payload.recipes()));
        event.register(com.pockyl.hextale.network.CraftingRecipesPayload.TYPE, (payload, context) -> {
            com.pockyl.hextale.crafting.HexCraftingRecipe.ALL.setClient(payload.crafting());
            com.pockyl.hextale.crafting.AlloyRecipe.ALL.setClient(payload.alloying());
        });
        event.register(OpenStelePayload.TYPE, (payload, context) -> Minecraft.getInstance().gui.setScreen(new SteleScreen(payload.pos())));
        event.register(com.pockyl.hextale.network.OpenStickerPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().gui.setScreen(new com.pockyl.hextale.client.screen.StickerScreen(payload.hand())));
        event.register(PylonFlightsPayload.TYPE, (payload, context) -> pylonFlights(payload));
        event.register(PylonLinkPayload.TYPE, (payload, context) -> PylonLinking.clientFrom = payload.from().orElse(null));
    }

    /** Items a pylon moved: show them flying, with the pull or the throw heard at the gem. */
    public static void pylonFlights(PylonFlightsPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getBlockEntity(payload.pylon()) instanceof HexPylonBlockEntity pylon)) {
            return;
        }
        pylon.show(payload.flights(), level.getGameTime());
        Vec3 gem = pylon.gem();
        boolean gathered = payload.flights().stream().anyMatch(PylonFlight::gathered);
        boolean essence = payload.flights().stream().anyMatch(PylonFlight::isEssence);
        level.playLocalSound(gem.x, gem.y, gem.z, essence ? ModSounds.ESSENCE_DRAW.get() : gathered ? ModSounds.PYLON_GATHER.get()
                : ModSounds.PYLON_SEND.get(), SoundSource.BLOCKS, 0.4F, 0.9F + level.getRandom().nextFloat() * 0.2F, false);
    }

    public static void knowledge(KnowledgeSyncPayload payload) {
        ClientKnowledge.set(payload.knowledge());
    }

    public static void itemElements(ItemEssencesPayload payload) {
        ItemEssenceTable.setById(payload.essences());
    }

    public static void researchMessage(ResearchMessagePayload payload) {
        if (Minecraft.getInstance().gui.screen() instanceof ResearchTableScreen screen) {
            screen.onMessage(payload.text(), payload.tone());
        }
    }

    public static void openDiary(OpenDiaryPayload payload) {
        Minecraft.getInstance().gui.setScreen(new StarBookScreen(payload.owner()));
    }
}
