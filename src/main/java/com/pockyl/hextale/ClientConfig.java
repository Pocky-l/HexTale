package com.pockyl.hextale;

import net.neoforged.neoforge.common.ModConfigSpec;

/** What each player sets for themselves. */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue BREACH_CINEMATIC = BUILDER
            .comment("Whether the camera flies to a breach of the Void that tears open nearby and shows it opening.")
            .translation("hextale.configuration.breachCinematic")
            .define("breachCinematic", true);

    public static final ModConfigSpec.BooleanValue SHOW_WIP_STEPS = BUILDER
            .comment("Whether the researcher's diary shows the devices still being worked out (WIP steps).")
            .translation("hextale.configuration.showWipSteps")
            .define("showWipSteps", false);

    /** Which rune signs (the hexagon and glyph when a rune works) are drawn. */
    public enum RuneSigns {
        /** Every rune's sign, your own drawn smaller. */
        ALL,
        /** Everyone's but your own (runes on your tools and clothes only glint). */
        NOT_MINE,
        /** None: runes only glint. */
        NONE
    }

    public static final ModConfigSpec.EnumValue<RuneSigns> RUNE_SIGNS = BUILDER
            .comment("Which rune signs are drawn when a rune works: ALL, NOT_MINE (not on your own tools and clothes) or NONE.")
            .translation("hextale.configuration.runeSigns")
            .defineEnum("runeSigns", RuneSigns.ALL);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
