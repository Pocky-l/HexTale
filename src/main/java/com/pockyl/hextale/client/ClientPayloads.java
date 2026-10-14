package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;

import com.pockyl.hextale.client.screen.ResearchTableScreen;
import com.pockyl.hextale.client.screen.diary.DiaryScreen;
import com.pockyl.hextale.network.FusionResultPayload;
import com.pockyl.hextale.network.KnowledgeSyncPayload;
import com.pockyl.hextale.network.OpenDiaryPayload;

/** Handlers of the client-bound payloads; run on the client thread. */
public final class ClientPayloads {
    private ClientPayloads() {
    }

    public static void knowledge(KnowledgeSyncPayload payload) {
        ClientKnowledge.set(payload.knowledge());
    }

    public static void fusionResult(FusionResultPayload payload) {
        if (Minecraft.getInstance().screen instanceof ResearchTableScreen screen) {
            screen.onFusionResult(payload.result());
        }
    }

    public static void openDiary(OpenDiaryPayload payload) {
        Minecraft.getInstance().setScreen(new DiaryScreen(payload.owner()));
    }
}
