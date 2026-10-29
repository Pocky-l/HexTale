package com.pockyl.hextale.rune;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.pockyl.hextale.rune.RuneGlyphs.arc;
import static com.pockyl.hextale.rune.RuneGlyphs.curve;
import static com.pockyl.hextale.rune.RuneGlyphs.dot;
import static com.pockyl.hextale.rune.RuneGlyphs.line;

import org.jspecify.annotations.Nullable;

/**
 * The essences: what everything is made of. The first nine are the PRIMAL essences (fire, water, earth, air, light,
 * shadow, life, metal, lightning) - the grammar of the runes: each primal is a simple rune, a sign of the arcane script
 * (its radical: two to four flowing strokes and dots in a unit box, y pointing down), and every complex rune pairs two of them. The
 * others are COMPOUNDS of two primals (fire + water = steam): few and plain, so that an item carries one to three
 * essences a researcher can tell apart. Each essence's make-up is a weight per primal ({@link #composition()}), from
 * which research picks the essences that belong to a rune's theme. Order matters: it indexes arrays, icon sheets and
 * stickers (tools/krita/hextale_essences.py draws the icons of the same essences in the same order).
 */
public enum Essence {
    FIRE("fire", 0xFF8A4C, strokes(
            curve(0.3, 0.92, 0.1, 0.6, 0.6, 0.5, 0.5, 0.06),
            curve(0.5, 0.06, 0.75, 0.4, 0.9, 0.7, 0.62, 0.9),
            dot(0.3, 0.92))),
    WATER("water", 0x4FB3FF, strokes(
            curve(0.08, 0.3, 0.3, 0.05, 0.45, 0.6, 0.62, 0.32),
            curve(0.62, 0.32, 0.8, 0.05, 0.95, 0.4, 0.8, 0.55),
            curve(0.2, 0.75, 0.4, 0.55, 0.55, 0.95, 0.85, 0.7))),
    EARTH("earth", 0xC9A26B, strokes(
            curve(0.15, 0.25, 0.4, 0.18, 0.6, 0.32, 0.85, 0.22),
            curve(0.5, 0.25, 0.42, 0.5, 0.58, 0.65, 0.48, 0.9),
            curve(0.12, 0.92, 0.35, 0.82, 0.65, 0.98, 0.9, 0.88),
            dot(0.85, 0.22))),
    AIR("air", 0xCFE9F2, strokes(
            curve(0.1, 0.35, 0.35, 0.1, 0.75, 0.15, 0.8, 0.35),
            arc(0.66, 0.42, 0.12, -30, 200),
            curve(0.15, 0.65, 0.4, 0.5, 0.7, 0.75, 0.92, 0.6),
            curve(0.3, 0.88, 0.5, 0.78, 0.65, 0.92, 0.78, 0.85))),
    LIGHT("light", 0xFFE08A, strokes(
            arc(0.5, 0.48, 0.24, 200, 520),
            curve(0.5, 0.0, 0.46, 0.12, 0.54, 0.16, 0.5, 0.24),
            curve(0.5, 0.72, 0.46, 0.84, 0.56, 0.9, 0.42, 1.0),
            dot(0.5, 0.48))),
    SHADOW("shadow", 0xA48AD6, strokes(
            curve(0.75, 0.1, 0.15, 0.1, 0.15, 0.9, 0.75, 0.9),
            curve(0.75, 0.9, 0.45, 0.7, 0.45, 0.3, 0.75, 0.1),
            dot(0.85, 0.5))),
    LIFE("life", 0x8FE07A, strokes(
            curve(0.5, 0.98, 0.45, 0.7, 0.55, 0.45, 0.5, 0.2),
            arc(0.5, 0.16, 0.1, 90, 450),
            curve(0.5, 0.62, 0.25, 0.6, 0.15, 0.45, 0.18, 0.35),
            curve(0.5, 0.48, 0.75, 0.46, 0.85, 0.32, 0.82, 0.22))),
    METAL("metal", 0xB8C4CC, strokes(
            curve(0.2, 0.15, 0.45, 0.3, 0.55, 0.3, 0.8, 0.15),
            line(0.5, 0.27, 0.5, 0.76),
            curve(0.25, 0.55, 0.4, 0.5, 0.6, 0.6, 0.75, 0.55),
            arc(0.5, 0.86, 0.1, -90, 230))),
    SPARK("spark", 0x7EF0FF, strokes(
            line(0.7, 0.02, 0.35, 0.45, 0.65, 0.5, 0.3, 0.98),
            dot(0.7, 0.02),
            curve(0.12, 0.3, 0.2, 0.2, 0.3, 0.2, 0.38, 0.28),
            curve(0.62, 0.72, 0.7, 0.8, 0.8, 0.8, 0.88, 0.7))),
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

    /** An essence by its id, or null. */
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
        return null;
    }

    private static List<float[]> strokes(float[]... strokes) {
        return Arrays.asList(strokes);
    }

}
