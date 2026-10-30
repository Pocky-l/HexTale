package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.IrisCompat;

/**
 * An aura round the WHOLE of a high-level tool, from its pommel to its tip, with no middle it starts from: a field along
 * the tool's axis ({@code from} - {@code to}, model pixels), as wide at each height as {@code profile} says (radii
 * spread evenly from bottom to top) and {@code depth} times as thick across the tool's face. Drawn wherever the item is
 * (first and third person, the ground, the inventory), everything from one clock:
 * <ul>
 * <li>{@code lightning} (radiant, level 3): short arcs snap here and there all along the tool, a few sparks wind round
 * it at different heights; every {@link #PULSE} ticks a discharge runs the whole length, pommel to tip. Kept restrained
 * so the tool does not shout.</li>
 * <li>{@code void} (riftforged, level 4): a slow vortex of violet light winds round the tool bottom to top, the
 * strands turning; stars rise along it and fade, dark motes sink; every {@link #INHALE} ticks the Void breathes in -
 * the vortex draws tighter and brightens.</li>
 * </ul>
 * {@code {"type": "hextale:tool_aura", "kind": "void", "from": [8, -8, 8], "to": [8, 28, 8], "profile": [3, 3, 4, 14, 17,
 * 10, 5]}} in a {@code minecraft:special} model laid over the tool's model, the whole wrapped in {@code hextale:animated}.
 */
public final class ToolAuraRenderer implements NoDataSpecialModelRenderer {
    public static final Identifier ID = Hextale.id("tool_aura");
    private static final float PX = 1 / 16F;

    public enum Kind implements StringRepresentable {
        LIGHTNING("lightning"),
        VOID("void");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private final Kind kind;
    private final Vec3 from;
    private final Vec3 axis;
    /** Across the tool (in its face) and through it, unit vectors. */
    private final Vec3 side;
    private final Vec3 through;
    private final float[] profile;
    private final float depth;

    private ToolAuraRenderer(Kind kind, Vector3fc from, Vector3fc to, List<Float> profile, float depth) {
        this.kind = kind;
        this.from = new Vec3(from.x() * PX, from.y() * PX, from.z() * PX);
        this.axis = new Vec3(to.x() * PX, to.y() * PX, to.z() * PX).subtract(this.from);
        Vec3 across = axis.cross(new Vec3(0, 0, 1));
        this.side = across.lengthSqr() < 1.0E-10 ? new Vec3(1, 0, 0) : across.normalize();
        this.through = side.cross(axis).normalize();
        this.profile = new float[Math.max(1, profile.size())];
        for (int i = 0; i < this.profile.length; i++) {
            this.profile[i] = (profile.isEmpty() ? 4 : profile.get(i)) * PX;
        }
        this.depth = depth;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        Minecraft minecraft = Minecraft.getInstance();
        float time = HexFx.time(minecraft.level, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        if (kind == Kind.LIGHTNING) {
            lightning(poseStack, collector, time);
        } else {
            vortex(poseStack, collector, time);
        }
    }

    /** The field's radius at height {@code t} (0 = pommel, 1 = tip). */
    private float width(float t) {
        if (profile.length == 1) {
            return profile[0];
        }
        float at = Mth.clamp(t, 0, 1) * (profile.length - 1);
        int i = Math.min(profile.length - 2, (int) at);
        return Mth.lerp(at - i, profile[i], profile[i + 1]);
    }

    /** A point of the field: height {@code t}, angle round the axis, {@code shell} of the radius there. */
    private Vec3 at(float t, float angle, float shell) {
        float r = width(t) * shell;
        return from.add(axis.scale(t)).add(side.scale(Mth.cos(angle) * r)).add(through.scale(Mth.sin(angle) * r * depth));
    }

    // ---- Lightning ---------------------------------------------------------------------------------------------------

    /** Ticks between discharges along the whole tool. */
    public static final float PULSE = 60;
    private static final int CORE_WHITE = 0xEAFBFF;
    private static final int SHEATH = 0x4FC4FF;

    private void lightning(PoseStack poseStack, SubmitNodeCollector collector, float time) {
        float phase = (time % PULSE) / PULSE;
        int frame = (int) Math.floor(time * 24 / 20F);
        // Short arcs here and there along the whole tool, each living a couple of frames; often none.
        for (int arc = 0; arc < 3; arc++) {
            RandomSource random = RandomSource.create((frame / 2) * 7919L + arc * 104729L);
            if (random.nextFloat() < 0.55F) {
                continue;
            }
            float t = random.nextFloat();
            Vec3 a = at(t, random.nextFloat() * Mth.TWO_PI, 0.8F + 0.4F * random.nextFloat());
            Vec3 b = at(Mth.clamp(t + (random.nextFloat() - 0.5F) * 0.25F, 0, 1), random.nextFloat() * Mth.TWO_PI, 0.8F + 0.4F * random.nextFloat());
            bolt(poseStack, collector, jagged(a, b, random, 4, 0.3), 0.65F + 0.35F * random.nextFloat(), 1);
        }
        // The discharge: one arc running the whole length, pommel to tip, drawn as it runs and fading behind.
        if (phase < 0.25F) {
            float run = phase / 0.18F;
            RandomSource random = RandomSource.create((long) Math.floor(time / PULSE) * 31L);
            int steps = 18;
            Vec3[] path = new Vec3[steps + 1];
            float spin = random.nextFloat() * Mth.TWO_PI;
            for (int i = 0; i <= steps; i++) {
                float t = i / (float) steps;
                path[i] = at(t, spin + t * 5 + (random.nextFloat() - 0.5F) * 1.2F, 0.9F + 0.3F * random.nextFloat());
            }
            List<Vec3> points = new ArrayList<>();
            for (int i = 0; i < steps; i++) {
                Vec3[] piece = jagged(path[i], path[i + 1], random, 2, 0.35);
                points.addAll(Arrays.asList(piece).subList(0, piece.length - 1));
            }
            points.add(path[steps]);
            Vec3[] bolt = points.toArray(Vec3[]::new);
            float[] light = new float[bolt.length];
            for (int i = 0; i < bolt.length; i++) {
                float u = i / (float) (bolt.length - 1);
                // Lit up to the running head, fading behind it, the whole fading out at the end of the run.
                light[i] = u <= run ? Mth.clamp(1 - (run - u) * 1.2F, 0.15F, 1) * Mth.clamp(1.6F - run, 0, 1) : 0;
            }
            strip(poseStack, collector, bolt, light, 1.4F * PX, SHEATH, 0.5F);
            strip(poseStack, collector, bolt, light, 0.38F * PX, CORE_WHITE, 0.95F);
        }
        // A few sparks winding round the tool at different heights.
        for (int s = 0; s < 4; s++) {
            RandomSource random = RandomSource.create(s * 977L + 3);
            float t = 0.15F + 0.7F * random.nextFloat() + 0.08F * Mth.sin(time * 0.05F + s);
            float speed = (0.15F + random.nextFloat() * 0.1F) * (random.nextBoolean() ? 1 : -1);
            Vec3[] tail = new Vec3[6];
            float[] fade = new float[6];
            for (int i = 0; i < 6; i++) {
                tail[5 - i] = at(t, (time - i * 0.6F) * speed + s * 1.7F, 1.05F);
                fade[5 - i] = 1 - i / 6F;
            }
            strip(poseStack, collector, tail, fade, 0.45F * PX, SHEATH, 0.5F);
            puff(poseStack, collector, HexFx.MOTE, tail[5], 0.7F * PX, CORE_WHITE, 0.65F, 0);
        }
    }

    private void bolt(PoseStack poseStack, SubmitNodeCollector collector, Vec3[] bolt, float flicker, float scale) {
        float[] light = new float[bolt.length];
        Arrays.fill(light, flicker);
        strip(poseStack, collector, bolt, light, 1.2F * scale * PX, SHEATH, 0.45F);
        strip(poseStack, collector, bolt, light, 0.33F * scale * PX, CORE_WHITE, 0.9F);
        puff(poseStack, collector, HexFx.MOTE, bolt[bolt.length - 1], 0.7F * PX, CORE_WHITE, 0.55F * flicker, 0);
    }

    // ---- Void ------------------------------------------------------------------------------------------------------------

    /** Ticks between the Void's breaths. */
    public static final float INHALE = 80;
    private static final int SMOKE = 0x0B0414;
    private static final int RIM = 0x8F55FF;
    private static final int PALE = 0xD9C2FF;
    private static final int STAR = 0xE8DCFF;

    private void vortex(PoseStack poseStack, SubmitNodeCollector collector, float time) {
        float breath = (time % INHALE) / INHALE;
        float inhale = (float) Math.exp(-Math.pow((breath - 0.25F) / 0.1F, 2));
        float shell = 1 - 0.25F * inhale;
        // Strands of violet light winding round the tool from pommel to tip, turning slowly.
        int strands = 3;
        int points = 40;
        float turns = 2.2F;
        for (int strand = 0; strand < strands; strand++) {
            Vec3[] curve = new Vec3[points];
            float[] glow = new float[points];
            float offset = strand * Mth.TWO_PI / strands + time * 0.03F;
            for (int i = 0; i < points; i++) {
                float t = i / (float) (points - 1);
                curve[i] = at(t, offset + t * turns * Mth.TWO_PI, shell * (0.95F + 0.08F * Mth.sin(t * 11 + time * 0.07F + strand)));
                // Brighter bands travel up the strand; the ends fade in and out.
                float band = 0.5F + 0.5F * Mth.sin(t * 9 - time * 0.12F + strand * 2.1F);
                glow[i] = Mth.sin(t * Mth.PI) * (0.35F + 0.65F * band);
            }
            strip(poseStack, collector, curve, glow, (1.6F + 0.6F * inhale) * PX, RIM, 0.45F + 0.2F * inhale);
            strip(poseStack, collector, curve, glow, 0.4F * PX, PALE, 0.45F);
        }
        // Stars rising along the vortex and fading; dark motes sinking through it.
        for (int s = 0; s < 22; s++) {
            RandomSource random = RandomSource.create(s * 977L + 41);
            float period = 70 + random.nextFloat() * 50;
            float life = ((time + random.nextFloat() * period) % period) / period;
            boolean dark = s % 3 == 0;
            float t = dark ? 1 - life : life;
            float angle = random.nextFloat() * Mth.TWO_PI + t * turns * Mth.TWO_PI + time * 0.03F;
            Vec3 at = at(t, angle, shell * (0.6F + 0.5F * random.nextFloat()));
            float alpha = Mth.sin(life * Mth.PI);
            if (dark) {
                darkSpot(poseStack, collector, at, (0.6F + random.nextFloat() * 0.6F) * PX, 0.7F * alpha);
            } else {
                star(poseStack, collector, at, (0.8F + random.nextFloat() * 0.8F) * PX, STAR, alpha * 0.85F, s % 4, angle);
            }
        }
    }

    // ---- Drawing --------------------------------------------------------------------------------------------------------

    /** A line broken by midpoint displacement into 2^depth jagged segments. */
    private static Vec3[] jagged(Vec3 from, Vec3 to, RandomSource random, int depth, double roughness) {
        List<Vec3> points = new ArrayList<>(List.of(from, to));
        double scale = from.distanceTo(to) * roughness;
        for (int level = 0; level < depth; level++) {
            List<Vec3> next = new ArrayList<>();
            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 a = points.get(i);
                Vec3 b = points.get(i + 1);
                Vec3 offset = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).scale(scale * 0.6);
                next.add(a);
                next.add(a.lerp(b, 0.5).add(offset));
            }
            next.add(points.getLast());
            points = next;
            scale *= 0.55;
        }
        return points.toArray(Vec3[]::new);
    }

    /** An additive line of light, crossed in two planes so it keeps its width whichever way the tool turns. */
    private static void strip(PoseStack poseStack, SubmitNodeCollector collector, Vec3[] points, float[] light, float width, int color,
                              float alpha) {
        if (points.length < 2) {
            return;
        }
        Vec3 dir = points[points.length - 1].subtract(points[0]);
        Vector3f along = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z);
        Vector3f a = new Vector3f(along).cross(0, 0, 1);
        if (a.lengthSquared() < 1.0E-10F) {
            a.set(1, 0, 0);
        }
        a.normalize();
        Vector3f b = new Vector3f(along).cross(a);
        if (b.lengthSquared() < 1.0E-10F) {
            b.set(0, 0, 1);
        }
        b.normalize();
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            JarStreamFx.flatStrip(consumer, pose, points, light, a, width, color, alpha);
            JarStreamFx.flatStrip(consumer, pose, points, light, b, width, color, alpha);
        });
    }

    /** A soft additive sprite at a point, as three crossed quads (a small cloud of light from any side). */
    private static void puff(PoseStack poseStack, SubmitNodeCollector collector, Identifier texture, Vec3 at, float size, int color, float alpha,
                             float roll) {
        if (alpha <= 0.01F || texture == HexFx.HALO && IrisCompat.shadersOn()) {
            return;
        }
        collector.submitCustomGeometry(poseStack, HexGlow.textured(texture),
                (pose, consumer) -> crossed(consumer, pose, at, size, color, alpha, roll, 0, 1));
    }

    /** One frame of the four-star glint sheet, as crossed quads. */
    private static void star(PoseStack poseStack, SubmitNodeCollector collector, Vec3 at, float size, int color, float alpha, int frame,
                             float roll) {
        if (alpha <= 0.01F) {
            return;
        }
        collector.submitCustomGeometry(poseStack, HexGlow.textured(HexFx.GLINT),
                (pose, consumer) -> crossed(consumer, pose, at, size, color, alpha, roll, frame / 4F, (frame + 1) / 4F));
    }

    /** A small soft dark mote (translucent: additive light cannot darken what lies behind it). */
    private static void darkSpot(PoseStack poseStack, SubmitNodeCollector collector, Vec3 at, float size, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(HexFx.MOTE),
                (pose, consumer) -> crossed(consumer, pose, at, size, SMOKE, alpha, 0, 0, 1));
    }

    private static void crossed(VertexConsumer consumer, PoseStack.Pose pose, Vec3 at, float size, int color, float alpha, float roll,
                                float u0, float u1) {
        float h = size / 2;
        Quaternionf turn = new Quaternionf().rotateZ(roll);
        Vector3f[][] planes = {
                {new Vector3f(1, 0, 0), new Vector3f(0, 1, 0)},
                {new Vector3f(0, 0, 1), new Vector3f(0, 1, 0)},
                {new Vector3f(1, 0, 0), new Vector3f(0, 0, 1)}};
        float[][] corners = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
        for (Vector3f[] plane : planes) {
            Vector3f u = turn.transform(new Vector3f(plane[0])).mul(h);
            Vector3f v = turn.transform(new Vector3f(plane[1])).mul(h);
            for (int pass = 0; pass < 2; pass++) {
                for (int k = 0; k < 4; k++) {
                    float[] c = corners[pass == 0 ? k : 3 - k];
                    HexFx.vertex(consumer, pose, (float) (at.x + u.x * c[0] + v.x * c[1]), (float) (at.y + u.y * c[0] + v.y * c[1]),
                            (float) (at.z + u.z * c[0] + v.z * c[1]), Mth.lerp(c[2], u0, u1), c[3], color, alpha);
                }
            }
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        float widest = 0;
        for (float r : profile) {
            widest = Math.max(widest, r);
        }
        for (float t = 0; t <= 1.001F; t += 0.25F) {
            for (int k = 0; k < 4; k++) {
                Vec3 p = from.add(axis.scale(t)).add(side.scale((k % 2 == 0 ? 1 : -1) * widest * 1.3)).add(through.scale((k < 2 ? 1 : -1) * widest * depth));
                output.accept(new Vector3f((float) p.x, (float) p.y, (float) p.z));
            }
        }
    }

    /** The JSON side. */
    public record Unbaked(Kind kind, Vector3fc from, Vector3fc to, List<Float> profile, float depth) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Kind.CODEC.fieldOf("kind").forGetter(Unbaked::kind),
                ExtraCodecs.VECTOR3F.fieldOf("from").forGetter(Unbaked::from),
                ExtraCodecs.VECTOR3F.fieldOf("to").forGetter(Unbaked::to),
                Codec.FLOAT.listOf().fieldOf("profile").forGetter(Unbaked::profile),
                Codec.FLOAT.optionalFieldOf("depth", 0.5F).forGetter(Unbaked::depth)
        ).apply(instance, Unbaked::new));

        @Override
        public ToolAuraRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new ToolAuraRenderer(kind, from, to, profile, depth);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
