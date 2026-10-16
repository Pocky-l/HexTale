package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

import com.pockyl.hextale.client.screen.ResearchTableScreen;
import com.pockyl.hextale.client.screen.book.StarBookScreen;
import com.pockyl.hextale.network.ItemEssencesPayload;
import com.pockyl.hextale.network.KnowledgeSyncPayload;
import com.pockyl.hextale.network.OpenDiaryPayload;
import com.pockyl.hextale.network.ResearchMessagePayload;
import com.pockyl.hextale.research.ItemEssenceTable;

/** Handlers of the client-bound payloads; run on the client thread. */
public final class ClientPayloads {
    private ClientPayloads() {
    }

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(KnowledgeSyncPayload.TYPE, (payload, context) -> knowledge(payload));
        event.register(ItemEssencesPayload.TYPE, (payload, context) -> itemElements(payload));
        event.register(ResearchMessagePayload.TYPE, (payload, context) -> researchMessage(payload));
        event.register(OpenDiaryPayload.TYPE, (payload, context) -> openDiary(payload));
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
