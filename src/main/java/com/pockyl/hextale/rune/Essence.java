package com.pockyl.hextale.rune;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The essences: what everything is made of. The first nine are the PRIMAL essences (fire, water, earth, air, light,
 * shadow, life, metal, lightning) - the grammar of the runes: each primal is a simple rune, a small hieroglyph (its
 * radical: two to four brush strokes in a unit box, y pointing down), and every complex rune pairs two of them. The
 * others are COMPOUNDS of two primals (fire + water = steam): few and plain, so that an item carries one to three
 * essences a researcher can tell apart. Each essence's make-up is a weight per primal ({@link #composition()}), from
 * which research picks the essences that belong to a rune's theme. Order matters: it indexes arrays, icon sheets and
 * stickers (tools/krita/hextale_essences.py draws the icons of the same essences in the same order). Essences of
 * older worlds ({@link #LEGACY}) are read as the essence they were folded into.
 */
public enum Essence {
    FIRE("fire", 0xFF8A4C, strokes(
            line(0.5, 0.08, 0.5, 0.55),
            line(0.12, 0.95, 0.5, 0.55, 0.88, 0.95),
            line(0.18, 0.28, 0.3, 0.44),
            line(0.82, 0.28, 0.7, 0.44))),
    WATER("water", 0x4FB3FF, strokes(
            line(0.5, 0.04, 0.5, 0.9, 0.38, 0.8),
            line(0.12, 0.32, 0.38, 0.42, 0.1, 0.78),
            line(0.9, 0.22, 0.62, 0.5, 0.92, 0.9))),
    EARTH("earth", 0xC9A26B, strokes(
            line(0.18, 0.38, 0.82, 0.38),
            line(0.5, 0.08, 0.5, 0.86),
            line(0.04, 0.88, 0.96, 0.88))),
    AIR("air", 0xCFE9F2, strokes(
            line(0.12, 0.16, 0.88, 0.16),
            line(0.22, 0.44, 0.78, 0.44),
            line(0.08, 0.7, 0.8, 0.7, 0.94, 0.96))),
    LIGHT("light", 0xFFE08A, strokes(
            line(0.2, 0.08, 0.8, 0.08, 0.8, 0.92, 0.2, 0.92, 0.2, 0.08),
            line(0.2, 0.5, 0.8, 0.5))),
    SHADOW("shadow", 0xA48AD6, strokes(
            line(0.1, 0.28, 0.1, 0.1, 0.9, 0.1, 0.9, 0.28),
            line(0.62, 0.3, 0.28, 0.94),
            line(0.56, 0.6, 0.7, 0.74))),
    LIFE("life", 0x8FE07A, strokes(
            line(0.08, 0.38, 0.92, 0.38),
            line(0.5, 0.04, 0.5, 0.96),
            line(0.5, 0.44, 0.12, 0.86),
            line(0.5, 0.44, 0.88, 0.86))),
    METAL("metal", 0xB8C4CC, strokes(
            line(0.06, 0.42, 0.5, 0.04, 0.94, 0.42),
            line(0.24, 0.58, 0.76, 0.58),
            line(0.5, 0.42, 0.5, 0.9),
            line(0.12, 0.92, 0.88, 0.92))),
    SPARK("spark", 0x7EF0FF, strokes(
            line(0.14, 0.18, 0.74, 0.18, 0.74, 0.64, 0.14, 0.64, 0.14, 0.18),
            line(0.14, 0.41, 0.74, 0.41),
            line(0.44, 0.04, 0.44, 0.86, 0.92, 0.86, 0.92, 0.7))),
    STEAM("steam", 0xD9E2E8, "fire", "water"),
    MAGMA("magma", 0xE8572A, "fire", "earth"),
    ASH("ash", 0x6E6A66, "fire", "shadow"),
    PLANT("plant", 0x60C050, "earth", "life"),
    CRYSTAL("crystal", 0xB0E0FF, "earth", "light"),
    ORE("ore", 0x9A8070, "earth", "metal"),
    COLD("cold", 0x9AD0F0, "water", "shadow"),
    SAP("sap", 0xA8C850, "water", "life"),
    BEAST("beast", 0xB07848, "air", "life"),
    DECAY("decay", 0x7A8A50, "shadow", "life"),
    MAGIC("magic", 0xC070F0, "light", "spark"),
    RIFT("rift", 0x9050E0, "shadow", "spark"),
    CIRCUIT("circuit", 0xE04848, "metal", "spark"),
    ;

    /** The nine primal essences, in rune order. */
    public static final List<Essence> PRIMALS = List.of(FIRE, WATER, EARTH, AIR, LIGHT, SHADOW, LIFE, METAL, SPARK);
    public static final int COUNT = values().length;

    private final String id;
    private final int color;
    private final List<float[]> radical;
    @Nullable
    private final String parentA;
    @Nullable
    private final String parentB;
    private Essence @Nullable [] parents;
    private double @Nullable [] composition;

    Essence(String id, int color, List<float[]> radical) {
        this.id = id;
        this.color = color;
        this.radical = radical;
        this.parentA = null;
        this.parentB = null;
    }

    Essence(String id, int color, String parentA, String parentB) {
        this.id = id;
        this.color = color;
        this.radical = List.of();
        this.parentA = parentA;
        this.parentB = parentB;
    }

    public String id() {
        return id;
    }

    /** Essence colour, RGB. */
    public int color() {
        return color;
    }

    public boolean isPrimal() {
        return parentA == null;
    }

    /** The two essences this one is made of (empty for a primal). */
    public Essence[] parents() {
        if (parents == null) {
            parents = isPrimal() ? new Essence[0] : new Essence[]{byId(parentA), byId(parentB)};
        }
        return parents;
    }

    /** 0 for primals, 1 for compounds of two primals, 2 and up for compounds of compounds. */
    public int tier() {
        if (isPrimal()) {
            return 0;
        }
        Essence[] p = parents();
        return 1 + Math.max(p[0].tier(), p[1].tier());
    }

    /** How much of each primal (by {@link #PRIMALS} index) this essence holds; the weights add up to 1. */
    public double[] composition() {
        if (composition == null) {
            double[] out = new double[PRIMALS.size()];
            if (isPrimal()) {
                out[ordinal()] = 1;
            } else {
                for (Essence parent : parents()) {
                    double[] part = parent.composition();
                    for (int i = 0; i < out.length; i++) {
                        out[i] += part[i] / 2;
                    }
                }
            }
            composition = out;
        }
        return composition;
    }

    /** How much of a primal is in this essence (0-1). */
    public double share(Essence primal) {
        return primal.isPrimal() ? composition()[primal.ordinal()] : 0;
    }

    /** Radical strokes as polylines {@code [x0, y0, x1, y1, ...]} in the unit box (primals only); never modify them. */
    public List<float[]> radical() {
        return radical;
    }

    public int strokeCount() {
        return radical.size();
    }

    public String translationKey() {
        return "rune.hextale.element." + id;
    }

    /** The compound of exactly these two primals (fire + earth = magma), in either order; null when a or b is not a primal. */
    @Nullable
    public static Essence pair(Essence a, Essence b) {
        for (Essence essence : values()) {
            if (essence.tier() == 1) {
                Essence[] p = essence.parents();
                if (p[0] == a && p[1] == b || p[0] == b && p[1] == a) {
                    return essence;
                }
            }
        }
        return null;
    }

    /** An essence by its id ("fire", "steam"...), as data components save it. */
    public static final com.mojang.serialization.Codec<Essence> ID_CODEC = com.mojang.serialization.Codec.STRING.comapFlatMap(id -> {
        Essence essence = byId(id);
        return essence == null ? com.mojang.serialization.DataResult.error(() -> "Unknown essence " + id)
                : com.mojang.serialization.DataResult.success(essence);
    }, Essence::id);
    public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, Essence> STREAM_CODEC =
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT.map(i -> values()[i], Essence::ordinal);

    /**
     * The essences of older versions (there were eighty) and the essence each one was folded into, so saved jars,
     * stickers and essence items keep working.
     */
    public static final java.util.Map<String, Essence> LEGACY = java.util.Map.ofEntries(
            java.util.Map.entry("smoke", ASH), java.util.Map.entry("sun", LIGHT), java.util.Map.entry("fury", FIRE),
            java.util.Map.entry("forge", METAL), java.util.Map.entry("blast", FIRE), java.util.Map.entry("clay", EARTH),
            java.util.Map.entry("mist", WATER), java.util.Map.entry("purity", WATER), java.util.Map.entry("rust", ORE),
            java.util.Map.entry("storm", SPARK), java.util.Map.entry("dust", EARTH), java.util.Map.entry("depths", SHADOW),
            java.util.Map.entry("soil", PLANT), java.util.Map.entry("quake", EARTH), java.util.Map.entry("sky", AIR),
            java.util.Map.entry("silence", SHADOW), java.util.Map.entry("breath", BEAST), java.util.Map.entry("motion", AIR),
            java.util.Map.entry("thunder", SPARK), java.util.Map.entry("illusion", RIFT), java.util.Map.entry("spirit", MAGIC),
            java.util.Map.entry("gleam", LIGHT), java.util.Map.entry("energy", MAGIC), java.util.Map.entry("corruption", DECAY),
            java.util.Map.entry("craft", METAL), java.util.Map.entry("vigor", LIFE), java.util.Map.entry("ice", COLD),
            java.util.Map.entry("glass", CRYSTAL), java.util.Map.entry("gem", CRYSTAL), java.util.Map.entry("gold", METAL),
            java.util.Map.entry("wood", PLANT), java.util.Map.entry("seed", PLANT), java.util.Map.entry("flesh", BEAST),
            java.util.Map.entry("bone", DECAY), java.util.Map.entry("fiber", PLANT), java.util.Map.entry("slime", SAP),
            java.util.Map.entry("venom", DECAY), java.util.Map.entry("sweet", SAP), java.util.Map.entry("knowledge", MAGIC),
            java.util.Map.entry("ender", RIFT), java.util.Map.entry("hell", MAGMA), java.util.Map.entry("echo", SHADOW),
            java.util.Map.entry("food", LIFE), java.util.Map.entry("cloth", BEAST), java.util.Map.entry("mechanism", CIRCUIT),
            java.util.Map.entry("weapon", METAL), java.util.Map.entry("protection", METAL), java.util.Map.entry("time", MAGIC),
            java.util.Map.entry("chaos", RIFT), java.util.Map.entry("order", CRYSTAL), java.util.Map.entry("ocean", WATER),
            java.util.Map.entry("dream", RIFT), java.util.Map.entry("soul", SHADOW), java.util.Map.entry("wind", AIR),
            java.util.Map.entry("tool", METAL), java.util.Map.entry("dye", PLANT), java.util.Map.entry("star", MAGIC),
            java.util.Map.entry("blood", BEAST));

    /** An essence by its id; the id of an essence of older versions gives the essence it was folded into. */
    @Nullable
    public static Essence byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String key = id.toLowerCase(Locale.ROOT);
        for (Essence essence : values()) {
            if (essence.id.equals(key)) {
                return essence;
            }
        }
        return LEGACY.get(key);
    }

    private static List<float[]> strokes(float[]... strokes) {
        return Arrays.asList(strokes);
    }

    private static float[] line(double... xy) {
        float[] points = new float[xy.length];
        for (int i = 0; i < xy.length; i++) {
            points[i] = (float) xy[i];
        }
        return points;
    }
}
