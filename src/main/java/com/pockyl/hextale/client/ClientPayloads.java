package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
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
        event.register(com.pockyl.hextale.network.GuidePayload.TYPE, (payload, context) -> com.pockyl.hextale.quest.Guide.setClient(payload.steps()));
        event.register(com.pockyl.hextale.network.CraftingRecipesPayload.TYPE, (payload, context) -> {
            com.pockyl.hextale.crafting.HexCraftingRecipe.ALL.setClient(payload.crafting());
            com.pockyl.hextale.crafting.AlloyRecipe.ALL.setClient(payload.alloying());
            com.pockyl.hextale.crafting.CrucibleRecipe.ALL.setClient(payload.crucible());
            com.pockyl.hextale.crafting.BrewRecipe.ALL.setClient(payload.brewing());
            com.pockyl.hextale.research.ExperimentPlan.ALL.setClient(payload.experiments());
        });
        event.register(com.pockyl.hextale.network.ThinningPayload.TYPE, (payload, context) -> ClientThinning.accept(payload));
        event.register(OpenStelePayload.TYPE, (payload, context) -> Minecraft.getInstance().gui.setScreen(new SteleScreen(payload.pos())));
        event.register(com.pockyl.hextale.network.OpenStickerPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().gui.setScreen(new com.pockyl.hextale.client.screen.StickerScreen(payload.hand())));
        event.register(PylonFlightsPayload.TYPE, (payload, context) -> pylonFlights(payload));
        event.register(com.pockyl.hextale.network.SawPayload.TYPE, (payload, context) -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(payload.mill()) instanceof com.pockyl.hextale.block.HexSawmillBlockEntity mill) {
                mill.show(payload, level.getGameTime());
            }
        });
        event.register(com.pockyl.hextale.network.WhirlPayload.TYPE, (payload, context) -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(payload.reaper()) instanceof com.pockyl.hextale.block.WhirlReaperBlockEntity reaper) {
                reaper.show(payload, level.getGameTime());
            }
        });
        event.register(com.pockyl.hextale.network.FunnelPayload.TYPE, (payload, context) -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(payload.funnel()) instanceof com.pockyl.hextale.block.GreedyFunnelBlockEntity funnel) {
                funnel.show(payload, level.getGameTime());
            }
        });
        event.register(com.pockyl.hextale.network.ArcPayload.TYPE, (payload, context) -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(payload.spire()) instanceof com.pockyl.hextale.block.ArcSpireBlockEntity spire) {
                spire.show(payload, level.getGameTime());
            }
        });
        event.register(com.pockyl.hextale.network.CondensePayload.TYPE, (payload, context) -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(payload.condenser()) instanceof com.pockyl.hextale.block.XpCondenserBlockEntity condenser) {
                condenser.show(payload, level.getGameTime());
            }
        });
        event.register(com.pockyl.hextale.network.HexcoreStrikePayload.TYPE, (payload, context) -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(payload.core()) instanceof com.pockyl.hextale.block.HexcoreBlockEntity core) {
                core.strike(payload, level.getGameTime());
            }
        });
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
        } else if (Minecraft.getInstance().gui.screen() instanceof com.pockyl.hextale.client.screen.machine.LabBenchScreen screen) {
            screen.onMessage(payload.text(), payload.tone());
        }
    }

    public static void openDiary(OpenDiaryPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.setScreen(new StarBookScreen(payload.owner()));
        // The leather cover opens, then the pages riffle.
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.2F, 0.5F));
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.9F, 1.0F));
    }
}
