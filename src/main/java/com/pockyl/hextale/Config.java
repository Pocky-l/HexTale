package com.pockyl.hextale;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MIN_ORBS = BUILDER
            .comment("Fewest unstable hextech orbs each amethyst block or cluster shatters into when lightning runs through it.")
            .translation("hextale.configuration.minOrbs")
            .defineInRange("minOrbs", 1, 0, 16);
    public static final ModConfigSpec.IntValue MAX_ORBS = BUILDER
            .comment("Most unstable hextech orbs each amethyst block or cluster shatters into when lightning runs through it.")
            .translation("hextale.configuration.maxOrbs")
            .defineInRange("maxOrbs", 1, 0, 16);
    public static final ModConfigSpec.IntValue CHAIN_RADIUS = BUILDER
            .comment("How far (in blocks, every way from the struck block) lightning runs on through touching amethyst.")
            .translation("hextale.configuration.chainRadius")
            .defineInRange("chainRadius", 5, 0, 16);
    public static final ModConfigSpec.IntValue MAX_CHAIN = BUILDER
            .comment("Most amethyst blocks and clusters one strike shatters.")
            .translation("hextale.configuration.maxChain")
            .defineInRange("maxChain", 125, 1, 1024);

    public static final ModConfigSpec.BooleanValue THINNING = BUILDER
            .comment("Whether careless magic wears the edge of the world thin: haze, sick land and at last breaches of the Void around a base.")
            .translation("hextale.configuration.thinning")
            .define("thinning", true);
    public static final ModConfigSpec.DoubleValue THINNING_RATE = BUILDER
            .comment("How fast careless magic wears the edge thin (1 = as designed).")
            .translation("hextale.configuration.thinningRate")
            .defineInRange("thinningRate", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.IntValue BREACH_COOLDOWN = BUILDER
            .comment("Minutes after a breach is gone (starved, folded or closed) before a new one may tear open within 1000 blocks of it.")
            .translation("hextale.configuration.breachCooldown")
            .defineInRange("breachCooldownMinutes", 20, 0, 1440);

    public static final ModConfigSpec.BooleanValue BLIGHT_CHANGES_BLOCKS = BUILDER
            .comment("Whether blighted land changes blocks: grass and stone turn to Void soil and rock, "
                    + "plants die back or become Void flora. "
                    + "Off: the Thinning still spreads, hazes and opens breaches, but the ground stays as it is.")
            .translation("hextale.configuration.blightChangesBlocks")
            .define("blightChangesBlocks", true);
    public static final ModConfigSpec.BooleanValue VOID_MITES_SPAWN = BUILDER
            .comment("Whether Void Mites crawl out of blighted ground near players.")
            .translation("hextale.configuration.voidMitesSpawn")
            .define("voidMitesSpawn", true);
    public static final ModConfigSpec.BooleanValue VOIDLINGS_SPAWN = BUILDER
            .comment("Whether breaches of the Void send out voidlings (wild ones and those born from feeding a held breach).")
            .translation("hextale.configuration.voidlingsSpawn")
            .define("voidlingsSpawn", true);
    public static final ModConfigSpec.BooleanValue BREACHES = BUILDER
            .comment("Whether land worn all the way thin tears open into a breach of the Void by itself. "
                    + "Breach Hearts and the /hextale command still work.")
            .translation("hextale.configuration.breaches")
            .define("breaches", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
