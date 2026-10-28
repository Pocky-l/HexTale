package com.pockyl.hextale;

import net.neoforged.neoforge.common.ModConfigSpec;

/** What each player sets for themselves. */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue BREACH_CINEMATIC = BUILDER
            .comment("Whether the camera flies to a breach of the Void that tears open nearby and shows it opening.")
            .translation("hextale.configuration.breachCinematic")
            .define("breachCinematic", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
