package com.pockyl.hextale.thevoid.boss;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Every number of the Prosector and its fight, a SYNCED config ({@code hextale-prosector.toml}): the server's values
 * reach the clients, so a pack can retune the fight without patches.
 *
 * <p>Damage against the mod's top armour: level 3 and 4 Bulwark reach the game's 80% armour cap, so a plain heavy hit would
 * cost little. {@link #PIERCE} is the share of every hit that goes through armour and enchantments, which keeps a heavy hit
 * near half of a player's health at the top of the ladder without one-shotting anyone in diamond.
 */
public final class ProsectorConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * One row of the attack table: timing in ticks, reach in blocks, weight in the pick, first phase, damage; {@code size}
     * is the move's width or radius, and {@code lock} how many ticks before the end of the startup it stops following.
     */
    public record AttackData(ModConfigSpec.IntValue startup, ModConfigSpec.IntValue active, ModConfigSpec.IntValue recovery,
                             ModConfigSpec.IntValue cooldown, ModConfigSpec.DoubleValue minRange, ModConfigSpec.DoubleValue maxRange,
                             ModConfigSpec.DoubleValue weight, ModConfigSpec.IntValue minPhase, ModConfigSpec.DoubleValue damage,
                             ModConfigSpec.DoubleValue size, ModConfigSpec.IntValue lock) {
    }

    static {
        BUILDER.comment("The Prosector itself").push("prosector");
    }
    public static final ModConfigSpec.DoubleValue HEALTH = BUILDER.comment("Max health. The game caps every creature at 1024.")
            .defineInRange("health", 1000.0, 1.0, 1024.0);
    public static final ModConfigSpec.DoubleValue ARMOUR = BUILDER.comment("Armour points.").defineInRange("armour", 14.0, 0.0, 30.0);
    public static final ModConfigSpec.DoubleValue PER_PLAYER = BUILDER
            .comment("How much tougher it is per player in the fight beyond the first, as a share (0.35 = hits count for 1/1.35). Max health cannot grow past the game's cap, so this divides the hits instead.")
            .defineInRange("healthPerExtraPlayer", 0.35, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue PIERCE = BUILDER
            .comment("Share of its hits' damage added on top of what armour leaves, going through armour and enchantments.")
            .defineInRange("pierce", 0.3, 0.0, 1.0);
    public static final ModConfigSpec.IntValue GAP_MIN = BUILDER.comment("Fewest ticks between one attack's end and the next choice.")
            .defineInRange("gapMin", 6, 0, 400);
    public static final ModConfigSpec.IntValue GAP_MAX = BUILDER.comment("Most ticks between one attack's end and the next choice.")
            .defineInRange("gapMax", 24, 0, 400);
    public static final ModConfigSpec.IntValue PITY = BUILDER
            .comment("Other moves after which Excision is forced, so its opening always comes.").defineInRange("excisionPity", 4, 1, 50);
    public static final ModConfigSpec.DoubleValue HEIGHT_ANSWER = BUILDER
            .comment("Blocks over the ground under it from which a target counts as standing too high: Excision's weight is multiplied.")
            .defineInRange("heightAnswer", 5.0, 1.0, 16.0);
    public static final ModConfigSpec.IntValue REEL_TICKS = BUILDER.comment("Ticks it reels when a raised shield snags the Retractor's hooks.")
            .defineInRange("reelTicks", 30, 1, 400);
    public static final ModConfigSpec.IntValue PROBE_LAG = BUILDER.comment("Ticks the Probe's beam lags behind its target.")
            .defineInRange("probeLag", 8, 0, 40);
    public static final ModConfigSpec.IntValue PROBE_BURN = BUILDER.comment("Ticks between two burns of the Probe's beam.")
            .defineInRange("probeBurnTicks", 10, 1, 200);
    public static final ModConfigSpec.DoubleValue KEEP_AWAY = BUILDER.comment("Blocks it keeps from its target between moves.")
            .defineInRange("keepAway", 9.0, 0.0, 15.0);
    public static final ModConfigSpec.DoubleValue HOVER_HEIGHT = BUILDER.comment("Blocks over the ground it hangs at between moves.")
            .defineInRange("hoverHeight", 4.0, 0.0, 11.0);
    static {
        BUILDER.pop();
        BUILDER.comment("What stands between a hit and its health, in this order").push("gate");
    }
    public static final ModConfigSpec.DoubleValue RESIST = BUILDER
            .comment("Damage factor of a kind while its Specimen lives.").defineInRange("writtenFactor", 0.15, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue FORGET_FACTOR = BUILDER.comment("Damage factor from anything during Forgetting.")
            .defineInRange("forgettingFactor", 1.5, 1.0, 10.0);
    public static final ModConfigSpec.DoubleValue PENDING_FACTOR = BUILDER
            .comment("Damage factor while a phase turn is queued, so one combo cannot run past it.")
            .defineInRange("pendingTurnFactor", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue HIT_CAP = BUILDER.comment("Most damage one hit can do.")
            .defineInRange("hitCap", 20.0, 0.1, 100000.0);
    public static final ModConfigSpec.DoubleValue DPS_CAP = BUILDER
            .comment("Damage per second the bucket lets through. Forgetting's factor widens it too, or the window would pay nothing.")
            .defineInRange("damagePerSecond", 12.0, 0.1, 100000.0);
    static {
        BUILDER.pop();
        BUILDER.comment("Study: what it learns from being hurt").push("study");
    }
    public static final ModConfigSpec.DoubleValue STUDY_SHARE = BUILDER
            .comment("Share of its max health one kind must deal to be written down.").defineInRange("share", 0.06, 0.001, 1.0);
    public static final ModConfigSpec.IntValue MAX_WRITTEN_1 = BUILDER.comment("Kinds written down at once in phase 1.")
            .defineInRange("maxWrittenPhase1", 2, 0, 4);
    public static final ModConfigSpec.IntValue MAX_WRITTEN_2 = BUILDER.comment("Kinds written down at once in phase 2.")
            .defineInRange("maxWrittenPhase2", 3, 0, 4);
    public static final ModConfigSpec.IntValue MAX_WRITTEN_3 = BUILDER.comment("Kinds written down at once in phase 3.")
            .defineInRange("maxWrittenPhase3", 4, 0, 4);
    public static final ModConfigSpec.DoubleValue SPECIMEN_HEALTH = BUILDER.comment("Health of a Specimen.")
            .defineInRange("specimenHealth", 50.0, 1.0, 10000.0);
    public static final ModConfigSpec.DoubleValue SPECIMEN_HEAL = BUILDER
            .comment("Health a second each living Specimen gives it back, never past the last milestone.")
            .defineInRange("specimenHeal", 2.0, 0.0, 1000.0);
    public static final ModConfigSpec.IntValue FORGET_TICKS = BUILDER.comment("Ticks of Forgetting after a Specimen breaks.")
            .defineInRange("forgettingTicks", 80, 1, 2000);
    static {
        BUILDER.pop();
        BUILDER.comment("Phases").push("phases");
    }
    public static final ModConfigSpec.DoubleValue PHASE2_AT = BUILDER.comment("Share of health at which Dissection turns.")
            .defineInRange("dissectionAt", 0.66, 0.01, 0.99);
    public static final ModConfigSpec.DoubleValue PHASE3_AT = BUILDER.comment("Share of health at which Proof turns.")
            .defineInRange("proofAt", 0.33, 0.01, 0.99);
    public static final ModConfigSpec.IntValue TURN2_TICKS = BUILDER.comment("Length of the turn into Dissection.")
            .defineInRange("dissectionTurnTicks", 80, 1, 2000);
    public static final ModConfigSpec.IntValue TURN3_TICKS = BUILDER.comment("Length of the turn into Proof.")
            .defineInRange("proofTurnTicks", 120, 1, 2000);
    public static final ModConfigSpec.DoubleValue PROOF_HEALTH = BUILDER.comment("Health of the Proof.")
            .defineInRange("proofHealth", 200.0, 1.0, 10000.0);
    public static final ModConfigSpec.DoubleValue PROOF_DAMAGE = BUILDER.comment("Damage of the Proof's own blow.")
            .defineInRange("proofDamage", 13.0, 0.0, 1000.0);
    public static final ModConfigSpec.IntValue FALLEN_TICKS = BUILDER.comment("Ticks it lies open after the Proof falls.")
            .defineInRange("fallenTicks", 140, 1, 2000);
    public static final ModConfigSpec.DoubleValue LAST_STAND_TEMPO = BUILDER.comment("All timings in the last stand are multiplied by this.")
            .defineInRange("lastStandTempo", 0.7, 0.1, 1.0);
    public static final ModConfigSpec.IntValue DEATH_TICKS = BUILDER.comment("Length of its death.").defineInRange("deathTicks", 130, 21, 2000);
    static {
        BUILDER.pop();
        BUILDER.comment("Where it fights: round the spot it was called to, on whatever ground is there").push("fight");
    }
    public static final ModConfigSpec.DoubleValue FIGHT_RADIUS = BUILDER
            .comment("Blocks from the spot it was called to that count as the fight. Hits from further out do nothing. A second Prosector needs twice this.")
            .defineInRange("fightRadius", 40.0, 16.0, 128.0);
    public static final ModConfigSpec.DoubleValue WAKE_RADIUS = BUILDER.comment("Blocks from which a player wakes it after it gave up.")
            .defineInRange("wakeRadius", 16.0, 1.0, 64.0);
    public static final ModConfigSpec.IntValue EMPTY_TICKS = BUILDER
            .comment("Ticks with nobody in the fight before it gives up, keeping the last milestone.")
            .defineInRange("emptyTicks", 600, 20, 72000);
    public static final ModConfigSpec.IntValue CELL_RETURN_TICKS = BUILDER.comment("Ticks a cell cut by Section stays open.")
            .defineInRange("cellReturnTicks", 240, 1, 2000);
    public static final ModConfigSpec.DoubleValue CELL_DAMAGE = BUILDER.comment("Damage of stepping on an open cell.")
            .defineInRange("cellDamage", 6.0, 0.0, 1000.0);
    public static final ModConfigSpec.IntValue SUMMON_TICKS = BUILDER.comment("Ticks from a Seer's Eye touching the ground to it being out.")
            .defineInRange("summonTicks", 100, 6, 1200);
    static {
        BUILDER.pop();
        BUILDER.comment("Reward").push("reward");
    }
    public static final ModConfigSpec.IntValue FIRST_XP = BUILDER.comment("Experience of the first kill in a world.")
            .defineInRange("firstKillXp", 1500, 0, 100000);
    public static final ModConfigSpec.IntValue REPEAT_XP = BUILDER.comment("Experience of every later kill.")
            .defineInRange("repeatKillXp", 300, 0, 100000);
    public static final ModConfigSpec.IntValue MIN_SCROLLS = BUILDER.comment("Fewest rune scrolls it drops.").defineInRange("minScrolls", 2, 0, 27);
    public static final ModConfigSpec.IntValue MAX_SCROLLS = BUILDER.comment("Most rune scrolls it drops.").defineInRange("maxScrolls", 3, 0, 27);
    public static final ModConfigSpec.IntValue SCROLL_STARS = BUILDER.comment("Stars of those scrolls.").defineInRange("scrollStars", 3, 1, 3);
    public static final ModConfigSpec.DoubleValue ANATOMY_CHANCE = BUILDER.comment("Chance a kill leaves a scroll of the rune Anatomy.")
            .defineInRange("anatomyChance", 0.15, 0.0, 1.0);
    static {
        BUILDER.pop();
        BUILDER.comment("Attack table: startup, active, recovery and cooldown in ticks, reach in blocks, pick weight, first phase, damage, size (width, radius or pull speed), lock (ticks before the blow it stops following)")
                .push("attacks");
    }
    public static final AttackData INCISION = attack("incision", 25, 2, 20, 30, 0, 14, 1.0, 1, 20, 1.3, 8);
    public static final AttackData PROBE = attack("probe", 22, 70, 15, 120, 6, 40, 0.8, 1, 5, 0.15, 0);
    public static final AttackData RETRACTOR = attack("retractor", 20, 10, 15, 90, 6, 20, 0.8, 1, 6, 1.1, 0);
    public static final AttackData EXCISION = attack("excision", 60, 5, 70, 240, 0, 64, 0.4, 1, 26, 3.6, 20);
    public static final AttackData SECTION = attack("section", 30, 12, 10, 180, 0, 64, 1.0, 2, 12, 3, 0);
    public static final AttackData RIPOSTE = attack("riposte", 25, 6, 18, 60, 0, 10, 1.4, 1, 17, 8, 15);
    public static final AttackData VOLLEY = attack("returnVolley", 22, 24, 15, 80, 6, 64, 1.4, 1, 7, 0.09, 0);
    public static final AttackData CHARGE = attack("returnedCharge", 18, 35, 15, 80, 4, 64, 1.2, 1, 17, 4, 0);
    public static final AttackData CIRCLE = attack("invertedCircle", 35, 8, 15, 80, 0, 64, 1.4, 1, 14, 4, 0);
    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ProsectorConfig() {
    }

    /** How many kinds it may hold written down in a phase. */
    public static int maxWritten(int phase) {
        return (phase <= 1 ? MAX_WRITTEN_1 : phase == 2 ? MAX_WRITTEN_2 : MAX_WRITTEN_3).get();
    }

    private static AttackData attack(String name, int startup, int active, int recovery, int cooldown, double minRange, double maxRange,
                                     double weight, int minPhase, double damage, double size, int lock) {
        BUILDER.push(name);
        AttackData data = new AttackData(
                BUILDER.defineInRange("startup", startup, 0, 2000), BUILDER.defineInRange("active", active, 0, 2000),
                BUILDER.defineInRange("recovery", recovery, 0, 2000), BUILDER.defineInRange("cooldown", cooldown, 0, 72000),
                BUILDER.defineInRange("minRange", minRange, 0.0, 256.0), BUILDER.defineInRange("maxRange", maxRange, 0.0, 256.0),
                BUILDER.defineInRange("weight", weight, 0.0, 100.0), BUILDER.defineInRange("minPhase", minPhase, 1, 3),
                BUILDER.defineInRange("damage", damage, 0.0, 10000.0), BUILDER.defineInRange("size", size, 0.0, 64.0),
                BUILDER.defineInRange("lock", lock, 0, 2000));
        BUILDER.pop();
        return data;
    }
}
