package com.pockyl.hextale.crafting;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.Arrays;

/**
 * The lapidary's mini-game, as pure rules shared by the server and the screen. A crystal splits cleanly only along its
 * hidden cleavage planes (angles 0-180 degrees, seeded per crystal). The worker turns the crystal over the disc and
 * grinds: grinding with a plane lined up with the disc (within the tolerance) cuts that facet, grinding across the planes
 * cracks the crystal. Grinding heats the crystal, resting cools it; overheating cracks it too. Every facet cut with fewer
 * than {@link Grade#maxCracks} cracks makes the next grade; too many cracks shatter it into hextech dust.
 */
public final class Cleavage {
    /** Heat at which the crystal cracks. */
    public static final float MAX_HEAT = 40;
    /** Heat lost per tick of rest. */
    public static final float COOLING = 1.5F;
    /** Misaligned grinding cracks the crystal once per this many ticks. */
    public static final int CRACK_TICKS = 8;

    /**
     * What is being cut: the first cut (unstable to crystal), the finish (crystal to stabilised gemstone) or the
     * engraving (a diamond cut to a brilliant with a groove for hextech: the level 2 gem; forgiving - a cracked diamond
     * comes back whole).
     */
    public enum Grade {
        FIRST(3, 10, 3, 24, 9),
        FINISH(5, 5, 2, 30, 6),
        ENGRAVE(4, 8, 3, 26, 8);

        public final int facets;
        /** Degrees a plane may be off the disc line and still cut. */
        public final float tolerance;
        public final int maxCracks;
        /** Ticks of aligned grinding a facet takes. */
        public final int grindTicks;
        /** Random offset of each plane from the even spread, degrees. */
        public final float jitter;

        Grade(int facets, float tolerance, int maxCracks, int grindTicks, float jitter) {
            this.facets = facets;
            this.tolerance = tolerance;
            this.maxCracks = maxCracks;
            this.grindTicks = grindTicks;
            this.jitter = jitter;
        }
    }

    private Cleavage() {
    }

    /** The crystal's planes, evenly spread over 180 degrees with a random turn and jitter. */
    public static float[] planes(Grade grade, long seed) {
        RandomSource random = RandomSource.create(seed);
        float step = 180.0F / grade.facets;
        float turn = random.nextFloat() * step;
        float[] planes = new float[grade.facets];
        for (int i = 0; i < grade.facets; i++) {
            planes[i] = Mth.positiveModulo(turn + i * step + (random.nextFloat() * 2 - 1) * grade.jitter, 180);
        }
        return planes;
    }

    /** Smallest difference between two plane angles (planes repeat every 180 degrees). */
    public static float difference(float a, float b) {
        float d = Mth.positiveModulo(a - b, 180);
        return Math.min(d, 180 - d);
    }

    /** The state of one cut, advanced stroke by stroke. */
    public static final class Cut {
        public final Grade grade;
        public final float[] planes;
        /** Aligned grinding ticks per plane; a plane is cut at {@link Grade#grindTicks}. */
        public final int[] ground;
        public int cracks;
        public float heat;
        /** Misaligned ticks not yet turned into a crack. */
        public int scratch;

        public Cut(Grade grade, long seed) {
            this.grade = grade;
            this.planes = planes(grade, seed);
            this.ground = new int[grade.facets];
        }

        public Cut(Grade grade, long seed, int[] ground, int cracks, float heat) {
            this(grade, seed);
            System.arraycopy(ground, 0, this.ground, 0, Math.min(ground.length, this.ground.length));
            this.cracks = cracks;
            this.heat = heat;
        }

        /** The uncut plane lined up with the disc at this angle, or -1. */
        public int aligned(float angle, float tolerance) {
            int best = -1;
            float bestDiff = tolerance;
            for (int i = 0; i < planes.length; i++) {
                float diff = difference(angle, planes[i]);
                if (!cut(i) && diff <= bestDiff) {
                    best = i;
                    bestDiff = diff;
                }
            }
            return best;
        }

        public boolean cut(int plane) {
            return ground[plane] >= grade.grindTicks;
        }

        public int facetsCut() {
            int n = 0;
            for (int i = 0; i < planes.length; i++) {
                n += cut(i) ? 1 : 0;
            }
            return n;
        }

        public boolean finished() {
            return facetsCut() == planes.length;
        }

        public boolean shattered() {
            return cracks >= grade.maxCracks;
        }

        public boolean over() {
            return finished() || shattered();
        }

        /**
         * One stroke: {@code rest} ticks of cooling, then {@code ticks} ticks of grinding at {@code angle} (the crystal does
         * not turn while it touches the disc). {@code speed} &gt; 1 grinds faster (a master disc on a first cut).
         */
        public void stroke(int rest, float angle, int ticks, float speed) {
            heat = Math.max(0, heat - COOLING * Math.max(0, rest));
            int plane = aligned(angle, grade.tolerance);
            for (int t = 0; t < ticks && !over(); t++) {
                heat += 1;
                if (heat >= MAX_HEAT) {
                    cracks++;
                    heat = MAX_HEAT * 0.5F;
                    continue;
                }
                if (plane >= 0) {
                    ground[plane] = Math.min(grade.grindTicks, ground[plane] + Math.max(1, Math.round(speed)));
                    if (cut(plane)) {
                        // The facet snapped off: grinding on at this angle only scratches.
                        plane = -1;
                    }
                } else if (++scratch >= CRACK_TICKS) {
                    scratch = 0;
                    cracks++;
                }
            }
        }

        public int[] groundCopy() {
            return Arrays.copyOf(ground, ground.length);
        }
    }
}
