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
 * <li>{@code void} (riftforged, level 4): the abyss seeping out of the tool, never the same twice - small clouds of
 * the Void well up at random places along it, spread, sink towards the pommel and thin out; violet tendrils creep out
 * and twist away; now and then a short tear into the abyss flares for a few frames; a few faint stars glimmer in the
 * dark. Kept close to the tool.</li>
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
            abyss(poseStack, collector, time);
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

    private static final int SMOKE = 0x0B0414;
    private static final int ABYSS = 0x5A22C8;
    private static final int RIM = 0x8F55FF;
    private static final int PALE = 0xD9C2FF;
    private static final int STAR = 0xE8DCFF;

    private void abyss(PoseStack poseStack, SubmitNodeCollector collector, float time) {
        // Darkness: small clouds of the Void well up at random places along the tool, spread a little, sink and thin out.
        // Every cloud is born somewhere new each time it comes back, so nothing repeats.
        for (int c = 0; c < 26; c++) {
            float period = 40 + (c * 37 % 41);
            float clock = time + c * 13.7F;
            int cycle = (int) Math.floor(clock / period);
            float life = (clock - cycle * period) / period;
            RandomSource random = RandomSource.create(cycle * 7919L + c * 104729L);
            float t = random.nextFloat();
            float angle = random.nextFloat() * Mth.TWO_PI;
            float drift = 0.35F + 0.45F * life;
            Vec3 at = at(t, angle, drift).add(axisDown(0.12F * life));
            float size = (1.4F + 2.2F * life) * (0.7F + 0.6F * random.nextFloat()) * PX;
            float alpha = Mth.sin(life * Mth.PI) * (0.45F + 0.3F * random.nextFloat());
            darkSpot(poseStack, collector, at, size, alpha);
        }
        // Tendrils of the Void creeping out of the tool, twisting, gone again.
        for (int w = 0; w < 6; w++) {
            float period = 30 + (w * 29 % 23);
            float clock = time + w * 9.1F;
            int cycle = (int) Math.floor(clock / period);
            float life = (clock - cycle * period) / period;
            RandomSource random = RandomSource.create(cycle * 6007L + w * 7877L + 5);
            float t = 0.08F + 0.84F * random.nextFloat();
            float angle = random.nextFloat() * Mth.TWO_PI;
            float bend = (random.nextFloat() - 0.5F) * 3;
            float reach = (0.5F + 0.6F * random.nextFloat()) * Math.min(1, life * 2.2F);
            int points = 8;
            Vec3[] tendril = new Vec3[points];
            float[] glow = new float[points];
            for (int i = 0; i < points; i++) {
                float u = i / (float) (points - 1);
                float wiggle = Mth.sin(u * 7 + time * 0.3F + w) * 0.25F;
                tendril[i] = at(t + (u * 0.08F * bend), angle + u * bend + wiggle, 0.2F + u * reach);
                glow[i] = (1 - u) * Mth.sin(life * Mth.PI);
            }
            strip(poseStack, collector, tendril, glow, 0.9F * PX, ABYSS, 0.6F);
            strip(poseStack, collector, tendril, glow, 0.25F * PX, RIM, 0.55F);
        }
        // Tears into the abyss: now and then a short jagged fissure flares at a random place for a few frames.
        int frame = (int) Math.floor(time * 24 / 20F);
        for (int k = 0; k < 2; k++) {
            int span = 5;
            int cycle = (frame + k * 3) / span;
            RandomSource random = RandomSource.create(cycle * 31337L + k * 977L);
            if (random.nextFloat() < 0.6F) {
                continue;
            }
            float age = ((frame + k * 3) % span) / (float) span;
            float t = random.nextFloat();
            Vec3 a = at(t, random.nextFloat() * Mth.TWO_PI, 0.5F + 0.4F * random.nextFloat());
            Vec3 b = a.add(new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian() * 0.4).normalize()
                    .scale((2 + 2.5 * random.nextFloat()) * PX));
            Vec3[] tear = jagged(a, b, random, 3, 0.35);
            float[] light = new float[tear.length];
            Arrays.fill(light, 1 - age);
            strip(poseStack, collector, tear, light, 1.2F * PX, RIM, 0.55F);
            strip(poseStack, collector, tear, light, 0.3F * PX, PALE, 0.9F);
            darkSpot(poseStack, collector, a.lerp(b, 0.5), 2.2F * PX, 0.6F * (1 - age));
        }
        // A few faint stars in the dark, each lit for a while somewhere new.
        for (int s = 0; s < 7; s++) {
            float period = 50 + s * 11;
            float clock = time + s * 17.3F;
            int cycle = (int) Math.floor(clock / period);
            float life = (clock - cycle * period) / period;
            RandomSource random = RandomSource.create(cycle * 4211L + s * 613L);
            Vec3 at = at(random.nextFloat(), random.nextFloat() * Mth.TWO_PI, 0.4F + 0.5F * random.nextFloat());
            star(poseStack, collector, at, (0.6F + 0.7F * random.nextFloat()) * PX, STAR, 0.7F * Mth.sin(life * Mth.PI), s % 4,
                    random.nextFloat() * 6.28F);
        }
    }

    /** Down along the tool by {@code amount} of its length (things sink into the abyss towards the pommel). */
    private Vec3 axisDown(float amount) {
        return axis.scale(-amount);
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

    /** A soft additive sprite at a point, lying in the tool's face (one plane: crossed planes show as crosses). */
    private void puff(PoseStack poseStack, SubmitNodeCollector collector, Identifier texture, Vec3 at, float size, int color, float alpha,
                             float roll) {
        if (alpha <= 0.01F || texture == HexFx.HALO && IrisCompat.shadersOn()) {
            return;
        }
        collector.submitCustomGeometry(poseStack, HexGlow.textured(texture),
                (pose, consumer) -> flat(consumer, pose, at, size, color, alpha, roll, 0, 1));
    }

    /** One frame of the four-star glint sheet, lying in the tool's face. */
    private void star(PoseStack poseStack, SubmitNodeCollector collector, Vec3 at, float size, int color, float alpha, int frame,
                             float roll) {
        if (alpha <= 0.01F) {
            return;
        }
        collector.submitCustomGeometry(poseStack, HexGlow.textured(HexFx.GLINT),
                (pose, consumer) -> flat(consumer, pose, at, size, color, alpha, roll, frame / 4F, (frame + 1) / 4F));
    }

    /** A small soft dark mote in the tool's face (translucent: additive light cannot darken what lies behind it). */
    private void darkSpot(PoseStack poseStack, SubmitNodeCollector collector, Vec3 at, float size, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(HexFx.MOTE),
                (pose, consumer) -> flat(consumer, pose, at, size, SMOKE, alpha, 0, 0, 1));
    }

    /**
     * A square sprite in the plane of the tool's face (across it and along it), turned by {@code roll}, both windings: the
     * tool is seen mostly face-on, so its sprites are too.
     */
    private void flat(VertexConsumer consumer, PoseStack.Pose pose, Vec3 at, float size, int color, float alpha, float roll, float u0,
                      float u1) {
        float h = size / 2;
        Vec3 along = axis.normalize();
        Vec3 u = side.scale(Mth.cos(roll)).add(along.scale(Mth.sin(roll))).scale(h);
        Vec3 v = along.scale(Mth.cos(roll)).subtract(side.scale(Mth.sin(roll))).scale(h);
        float[][] corners = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                float[] c = corners[pass == 0 ? k : 3 - k];
                HexFx.vertex(consumer, pose, (float) (at.x + u.x * c[0] + v.x * c[1]), (float) (at.y + u.y * c[0] + v.y * c[1]),
                        (float) (at.z + u.z * c[0] + v.z * c[1]), Mth.lerp(c[2], u0, u1), c[3], color, alpha);
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
