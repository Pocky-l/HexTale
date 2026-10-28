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
            () -> AttachmentType.builder(() -> new RuneKnowledge()).serialize(RuneKnowledge.MAP_CODEC.codec()).copyOnDeath().build());

    /** A creature held by Rune Shackles (not saved: after a reload the plate simply takes hold again). */
    public static final Supplier<AttachmentType<com.pockyl.hextale.farm.ShackleEvents.Shackle>> SHACKLE = ATTACHMENT_TYPES.register("shackle",
            () -> AttachmentType.builder(() -> new com.pockyl.hextale.farm.ShackleEvents.Shackle(0, -1)).build());

    /** The Thinning of a chunk (see {@link com.pockyl.hextale.thevoid.Thinning}); saved only while above zero. */
    public static final Supplier<AttachmentType<Float>> THINNING = ATTACHMENT_TYPES.register("thinning",
            () -> AttachmentType.builder(() -> 0F).serialize(com.mojang.serialization.Codec.FLOAT, value -> value > 0).build());

    /** A chunk whose ground the blight took somewhere (see {@link com.pockyl.hextale.thevoid.Healing}); saved while set. */
    public static final Supplier<AttachmentType<Boolean>> SCARRED = ATTACHMENT_TYPES.register("scarred",
            () -> AttachmentType.builder(() -> false).serialize(com.mojang.serialization.Codec.BOOL, value -> value).build());

    /** Void Mites latched onto a player (see {@link com.pockyl.hextale.thevoid.VoidMites}); not kept through death. */
    public static final Supplier<AttachmentType<Integer>> VOID_MITES = com.pockyl.hextale.port.PortSync.synced(ATTACHMENT_TYPES.register("void_mites",
            () -> AttachmentType.builder(() -> 0).serialize(com.mojang.serialization.Codec.INT, count -> count > 0).build()),
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT);

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
