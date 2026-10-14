package com.pockyl.hextale;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MIN_ORBS = BUILDER
            .comment("Fewest unstable hextech orbs a lightning-struck amethyst block shatters into.")
            .translation("hextale.configuration.minOrbs")
            .defineInRange("minOrbs", 1, 1, 16);
    public static final ModConfigSpec.IntValue MAX_ORBS = BUILDER
            .comment("Most unstable hextech orbs a lightning-struck amethyst block shatters into.")
            .translation("hextale.configuration.maxOrbs")
            .defineInRange("maxOrbs", 3, 1, 16);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
