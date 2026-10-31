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

    /** What wore a chunk thin, by {@link com.pockyl.hextale.thevoid.Thinning.Source} name (mod units); saved while any. */
    public static final Supplier<AttachmentType<java.util.Map<String, Float>>> THINNING_SOURCES = ATTACHMENT_TYPES.register("thinning_sources",
            () -> AttachmentType.<java.util.Map<String, Float>>builder(() -> java.util.Map.of())
                    .serialize(com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING, com.mojang.serialization.Codec.FLOAT), sources -> !sources.isEmpty()).build());

    /** A chunk whose ground the blight took somewhere (see {@link com.pockyl.hextale.thevoid.Healing}); saved while set. */
    public static final Supplier<AttachmentType<Boolean>> SCARRED = ATTACHMENT_TYPES.register("scarred",
            () -> AttachmentType.builder(() -> false).serialize(com.mojang.serialization.Codec.BOOL, value -> value).build());

    /** Void Mites latched onto a player (see {@link com.pockyl.hextale.thevoid.VoidMites}); not kept through death. */
    public static final Supplier<AttachmentType<Integer>> VOID_MITES = com.pockyl.hextale.port.PortSync.synced(ATTACHMENT_TYPES.register("void_mites",
            () -> AttachmentType.builder(() -> 0).serialize(com.mojang.serialization.Codec.INT, count -> count > 0).build()),
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT);

    /** Shimmer in a player's veins (see {@link com.pockyl.hextale.chem.ShimmerVeins}); kept through death, synced. */
    public static final Supplier<AttachmentType<com.pockyl.hextale.chem.ShimmerVeins.Veins>> SHIMMER_VEINS = com.pockyl.hextale.port.PortSync.synced(ATTACHMENT_TYPES.register("shimmer_veins",
            () -> AttachmentType.builder(() -> com.pockyl.hextale.chem.ShimmerVeins.Veins.NONE)
                    .serialize(com.pockyl.hextale.chem.ShimmerVeins.Veins.CODEC, veins -> veins.count() > 0)
                    .copyOnDeath().build()),
            com.pockyl.hextale.chem.ShimmerVeins.Veins.STREAM_CODEC);

    /** The tick a creature put to sleep by a Slumber rune wakes (see {@link com.pockyl.hextale.runework.Slumber}); saved while set. */
    public static final Supplier<AttachmentType<Long>> SLUMBER = ATTACHMENT_TYPES.register("slumber",
            () -> AttachmentType.builder(() -> 0L).serialize(com.mojang.serialization.Codec.LONG, until -> until > 0).build());

    /** Placeholder for "nobody": attachments need a default value. */
    public static final java.util.UUID NOBODY = new java.util.UUID(0L, 0L);

    /**
     * The player who placed a device of the mod (any block entity of it): the device changes the world in that player's
     * name (see {@link com.pockyl.hextale.world.WorldEdits}). Saved while set.
     */
    public static final Supplier<AttachmentType<java.util.UUID>> PLACED_BY = ATTACHMENT_TYPES.register("placed_by",
            () -> AttachmentType.builder(() -> NOBODY).serialize(net.minecraft.core.UUIDUtil.CODEC,
                    uuid -> !NOBODY.equals(uuid)).build());

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
