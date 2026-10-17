package com.pockyl.hextale.rune;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The essences: what everything is made of. The first nine are the PRIMAL essences (fire, water, earth, air, light,
 * shadow, life, metal, lightning) - the grammar of the runes: each primal is a simple rune, a small hieroglyph (its
 * radical: two to four brush strokes in a unit box, y pointing down), and every complex rune pairs two of them. All
 * the others are COMPOUNDS of two parents: 36 of two primals (fire + water = steam), then compounds of compounds
 * (dust + fire = glass). Each essence's make-up is a weight per primal ({@link #composition()}), from which research
 * picks the essences that belong to a rune's theme. Order matters: it indexes arrays, icon sheets and stickers
 * (tools/texture-gen/hextale_essence_icons.py lists the same essences in the same order).
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
    SMOKE("smoke", 0x8E8A86, "fire", "air"),
    SUN("sun", 0xFFC93C, "fire", "light"),
    ASH("ash", 0x6E6A66, "fire", "shadow"),
    FURY("fury", 0xD8323C, "fire", "life"),
    FORGE("forge", 0xE0884A, "fire", "metal"),
    BLAST("blast", 0xFF6A20, "fire", "spark"),
    CLAY("clay", 0xA88B7A, "water", "earth"),
    MIST("mist", 0xB8C8D8, "water", "air"),
    PURITY("purity", 0xE6F7FF, "water", "light"),
    COLD("cold", 0x9AD0F0, "water", "shadow"),
    SAP("sap", 0xA8C850, "water", "life"),
    RUST("rust", 0xB0603A, "water", "metal"),
    STORM("storm", 0x5A7AB8, "water", "spark"),
    DUST("dust", 0xD8C890, "earth", "air"),
    CRYSTAL("crystal", 0xB0E0FF, "earth", "light"),
    DEPTHS("depths", 0x6A5A50, "earth", "shadow"),
    SOIL("soil", 0x8A6A3A, "earth", "life"),
    ORE("ore", 0x9A8070, "earth", "metal"),
    QUAKE("quake", 0xA07850, "earth", "spark"),
    SKY("sky", 0x8EC8F8, "air", "light"),
    SILENCE("silence", 0x7A80A0, "air", "shadow"),
    BREATH("breath", 0xC0F0E0, "air", "life"),
    MOTION("motion", 0xB8D0E0, "air", "metal"),
    THUNDER("thunder", 0x6890D0, "air", "spark"),
    ILLUSION("illusion", 0xD8A0E8, "light", "shadow"),
    SPIRIT("spirit", 0xE0F8F0, "light", "life"),
    GLEAM("gleam", 0xF8E8A0, "light", "metal"),
    ENERGY("energy", 0x60F0D8, "light", "spark"),
    DECAY("decay", 0x7A8A50, "shadow", "life"),
    CORRUPTION("corruption", 0x7A3A70, "shadow", "metal"),
    RIFT("rift", 0x9050E0, "shadow", "spark"),
    CRAFT("craft", 0xC8A070, "life", "metal"),
    VIGOR("vigor", 0xF0B060, "life", "spark"),
    CIRCUIT("circuit", 0xE04848, "metal", "spark"),
    ICE("ice", 0xA8E0FF, "cold", "water"),
    GLASS("glass", 0xD8F0F0, "dust", "fire"),
    GEM("gem", 0x40E8B0, "crystal", "gleam"),
    GOLD("gold", 0xF8D040, "ore", "gleam"),
    PLANT("plant", 0x60C050, "soil", "light"),
    WOOD("wood", 0xA0784A, "plant", "earth"),
    SEED("seed", 0xC8B060, "plant", "life"),
    FLESH("flesh", 0xE07878, "breath", "earth"),
    BONE("bone", 0xF0E8D0, "decay", "earth"),
    BEAST("beast", 0xB07848, "flesh", "breath"),
    FIBER("fiber", 0xE8E0C0, "plant", "air"),
    SLIME("slime", 0x80E060, "clay", "life"),
    VENOM("venom", 0x90D040, "decay", "water"),
    SWEET("sweet", 0xF8B0D0, "plant", "sap"),
    KNOWLEDGE("knowledge", 0x70A0F0, "spirit", "light"),
    ENDER("ender", 0x308878, "rift", "spirit"),
    HELL("hell", 0xA03020, "magma", "shadow"),
    ECHO("echo", 0x407090, "silence", "air"),
    FOOD("food", 0xE8A050, "plant", "flesh"),
    CLOTH("cloth", 0xD8C0E0, "fiber", "craft"),
    MECHANISM("mechanism", 0x909CA8, "circuit", "motion"),
    WEAPON("weapon", 0xC0C8D0, "metal", "fury"),
    PROTECTION("protection", 0x7090B0, "craft", "earth"),
    TIME("time", 0xE0D0A0, "spirit", "motion"),
    MAGIC("magic", 0xC070F0, "spirit", "energy"),
    CHAOS("chaos", 0xF04898, "rift", "blast"),
    ORDER("order", 0x90A8F0, "crystal", "circuit"),
    OCEAN("ocean", 0x2070C0, "water", "crystal"),
    DREAM("dream", 0xB0A0F0, "illusion", "spirit"),
    SOUL("soul", 0x60D0E0, "spirit", "depths"),
    WIND("wind", 0xD8F0F8, "air", "motion"),
    TOOL("tool", 0x9AA0A8, "craft", "ore"),
    DYE("dye", 0xE878C0, "plant", "illusion"),
    STAR("star", 0xFFF0B0, "sky", "energy"),
    BLOOD("blood", 0xB01828, "flesh", "water"),
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

    private static float[] line(double... xy) {
        float[] points = new float[xy.length];
        for (int i = 0; i < xy.length; i++) {
            points[i] = (float) xy[i];
        }
        return points;
    }
}
