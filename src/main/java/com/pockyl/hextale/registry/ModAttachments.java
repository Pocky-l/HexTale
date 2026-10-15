package com.pockyl.hextale.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.knowledge.RuneKnowledge;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Hextale.MOD_ID);

    /** A researcher's runes and diary notes; survives death. */
    public static final Supplier<AttachmentType<RuneKnowledge>> KNOWLEDGE = ATTACHMENT_TYPES.register("knowledge",
            () -> AttachmentType.builder(() -> new RuneKnowledge()).serialize(RuneKnowledge.MAP_CODEC).copyOnDeath().build());

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
