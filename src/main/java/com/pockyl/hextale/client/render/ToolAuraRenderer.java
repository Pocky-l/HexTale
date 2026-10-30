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
 * An aura AROUND the tools of the high levels (not tied to their model): a field of light filling an ellipsoid round the
 * tool's head, given in model pixels by the item model definition. Drawn wherever the item is (first and third person,
 * the ground, the inventory), everything from one clock:
 * <ul>
 * <li>{@code lightning} (radiant, level 3): a faint blue glow breathing in the middle of the field and now and then a
 * quiet arc; every {@link #PULSE} ticks a discharge - the glow flares and a few arcs crackle across the field and out of
 * its heart; a few sparks circle on tilted orbits. Kept restrained so the tool does not shout.</li>
 * <li>{@code void} (riftforged, level 4): a slow spiral of violet light in a tilted disc round the head, its arms winding
 * in to a small dark heart, with stars and dark motes drifting in along them to go out; every {@link #INHALE} ticks the
 * Void breathes in: the spiral winds tighter and brightens.</li>
 * </ul>
 * {@code {"type": "hextale:tool_aura", "kind": "lightning", "center": [8, 17, 8], "radius": [16, 9, 6]}} in a
 * {@code minecraft:special} model laid over the tool's model, the whole wrapped in {@code hextale:animated}.
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
    private final Vector3f center;
    private final Vector3f radius;

    private ToolAuraRenderer(Kind kind, Vector3fc center, Vector3fc radius) {
        this.kind = kind;
        this.center = new Vector3f(center).mul(PX);
        this.radius = new Vector3f(radius).mul(PX);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        Minecraft minecraft = Minecraft.getInstance();
        float time = HexFx.time(minecraft.level, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        if (kind == Kind.LIGHTNING) {
            lightning(poseStack, collector, time);
        } else {
            voidSpiral(poseStack, collector, time);
        }
    }

    /** A point of the field: (x, y, z) each in -1..1 of the radii, around the middle. */
    private Vec3 at(double x, double y, double z) {
        return new Vec3(center.x + x * radius.x, center.y + y * radius.y, center.z + z * radius.z);
    }

    /** A random point on the shell of the field (the ellipsoid scaled by {@code shell}). */
    private Vec3 onShell(RandomSource random, double shell) {
        Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        if (dir.lengthSqr() < 1.0E-6) {
            dir = new Vec3(1, 0, 0);
        }
        dir = dir.normalize().scale(shell);
        return at(dir.x, dir.y, dir.z);
    }

    private float size() {
        return Math.max(radius.x, Math.max(radius.y, radius.z));
    }

    // ---- Lightning ---------------------------------------------------------------------------------------------------

    /** Ticks between discharges. */
    public static final float PULSE = 60;
    private static final int CORE_WHITE = 0xEAFBFF;
    private static final int SHEATH = 0x4FC4FF;
    private static final int GLOW = 0x2E8CFF;

    private void lightning(PoseStack poseStack, SubmitNodeCollector collector, float time) {
        float phase = (time % PULSE) / PULSE;
        // The discharge: a sharp flare at the start of the beat, gone within a fifth of it.
        float flare = phase < 0.2F ? (float) Math.exp(-phase * 14) : 0;
        float breath = 0.5F + 0.5F * Mth.sin(time * 0.25F);
        float size = size();
        puff(poseStack, collector, HexFx.HALO, at(0, 0, 0), size * (1.1F + 0.3F * flare), GLOW, 0.08F + 0.05F * breath + 0.2F * flare, 0);
        puff(poseStack, collector, HexFx.HALO, at(0, 0, 0), size * 0.5F, SHEATH, 0.14F + 0.08F * breath + 0.25F * flare, 0.7F);
        // Arcs: one now and then, a few in the discharge; each lives a couple of frames.
        int frame = (int) Math.floor(time * 24 / 20F);
        int arcs = 1 + Math.round(4 * flare);
        for (int arc = 0; arc < arcs; arc++) {
            RandomSource random = RandomSource.create((frame / 2) * 7919L + arc * 104729L);
            if (flare < 0.05F && random.nextFloat() < 0.35F) {
                continue; // between discharges the field is often quiet
            }
            Vec3 from;
            Vec3 to;
            if (arc % 3 == 0) {
                from = at(0, 0, 0);
                to = onShell(random, 0.9 + random.nextDouble() * 0.25);
            } else {
                from = onShell(random, 0.85 + random.nextDouble() * 0.3);
                to = onShell(random, 0.85 + random.nextDouble() * 0.3);
                if (from.distanceTo(to) > size * 1.2) {
                    to = from.lerp(to, 0.5);
                }
            }
            Vec3[] bolt = jagged(from, to, random, 4, 0.3);
            float flicker = 0.65F + 0.35F * random.nextFloat();
            float[] light = new float[bolt.length];
            Arrays.fill(light, flicker);
            strip(poseStack, collector, bolt, light, (1.3F + 0.6F * flare) * PX, SHEATH, 0.45F);
            strip(poseStack, collector, bolt, light, 0.35F * PX, CORE_WHITE, 0.9F);
            if (random.nextFloat() < 0.4F) {
                Vec3 root = bolt[bolt.length / 2 + random.nextInt(bolt.length / 3)];
                Vec3 out = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize()
                        .scale(size * (0.2 + random.nextDouble() * 0.2));
                Vec3[] fork = jagged(root, root.add(out), random, 3, 0.35);
                float[] fade = new float[fork.length];
                for (int i = 0; i < fork.length; i++) {
                    fade[i] = flicker * (1 - i / (float) fork.length);
                }
                strip(poseStack, collector, fork, fade, 0.9F * PX, SHEATH, 0.4F);
                strip(poseStack, collector, fork, fade, 0.25F * PX, CORE_WHITE, 0.8F);
            }
            puff(poseStack, collector, HexFx.MOTE, to, (0.8F + 0.6F * random.nextFloat()) * PX, CORE_WHITE, 0.6F * flicker, 0);
        }
        // A few sparks on tilted orbits, with short tails.
        for (int s = 0; s < 4; s++) {
            RandomSource random = RandomSource.create(s * 977L + 3);
            Quaternionf tilt = new Quaternionf().rotateXYZ(random.nextFloat() * 6.28F, random.nextFloat() * 6.28F, random.nextFloat() * 6.28F);
            float speed = (0.12F + random.nextFloat() * 0.1F) * (random.nextBoolean() ? 1 : -1);
            float shell = 0.75F + random.nextFloat() * 0.3F;
            Vec3[] tail = new Vec3[6];
            float[] fade = new float[6];
            for (int i = 0; i < 6; i++) {
                float angle = (time - i * 0.6F) * speed + s;
                Vector3f p = tilt.transform(new Vector3f(Mth.cos(angle), Mth.sin(angle), 0));
                tail[5 - i] = at(p.x * shell, p.y * shell, p.z * shell);
                fade[5 - i] = 1 - i / 6F;
            }
            strip(poseStack, collector, tail, fade, 0.5F * PX, SHEATH, 0.55F);
            puff(poseStack, collector, HexFx.MOTE, tail[5], 0.8F * PX, CORE_WHITE, 0.7F, 0);
        }
    }

    // ---- Void ------------------------------------------------------------------------------------------------------------

    /** Ticks between the Void's breaths. */
    public static final float INHALE = 80;
    private static final int SMOKE = 0x0B0414;
    private static final int DEEP = 0x4A1A9C;
    private static final int RIM = 0x8F55FF;
    private static final int PALE = 0xD9C2FF;
    private static final int STAR = 0xE8DCFF;

    private void voidSpiral(PoseStack poseStack, SubmitNodeCollector collector, float time) {
        float breath = (time % INHALE) / INHALE;
        float inhale = (float) Math.exp(-Math.pow((breath - 0.25F) / 0.1F, 2));
        float size = size();
        // A deep violet haze round the head.
        puff(poseStack, collector, HexFx.HALO, at(0, 0, 0), size * 1.4F, DEEP, 0.16F + 0.12F * inhale, 0);
        // A slow spiral of violet light in a tilted disc: arms that wind in towards the heart, tighter on the breath.
        Quaternionf disc = new Quaternionf().rotateXYZ(0.45F, 0, 0.3F).rotateY(time * 0.02F);
        int arms = 4;
        int points = 22;
        float wind = 2.6F + 0.8F * inhale;
        for (int arm = 0; arm < arms; arm++) {
            Vec3[] curve = new Vec3[points];
            float[] glow = new float[points];
            for (int i = 0; i < points; i++) {
                float u = i / (float) (points - 1);
                float r = 1.05F - 0.85F * u;
                float angle = arm * Mth.TWO_PI / arms + u * wind;
                Vector3f p = disc.transform(new Vector3f(Mth.cos(angle) * r, 0.06F * Mth.sin(u * 9 + time * 0.1F + arm), Mth.sin(angle) * r));
                curve[i] = at(p.x, p.y, p.z);
                glow[i] = Mth.sin(u * Mth.PI) * (0.55F + 0.45F * Mth.sin(time * 0.08F + arm * 1.7F + u * 4));
            }
            strip(poseStack, collector, curve, glow, (2.2F + 0.8F * inhale) * PX, RIM, 0.5F);
            strip(poseStack, collector, curve, glow, 0.5F * PX, PALE, 0.55F);
        }
        // The heart: a small dark knot with a violet edge.
        dark(poseStack, collector, HexFx.MOTE, at(0, 0, 0), size * 0.35F, 0.65F, 0);
        puff(poseStack, collector, HexFx.HALO, at(0, 0, 0), size * 0.45F, RIM, 0.25F + 0.25F * inhale, 0);
        // Stars spiralling in along the disc, going out at the heart; dark motes drifting in among them.
        for (int s = 0; s < 18; s++) {
            RandomSource random = RandomSource.create(s * 977L + 41);
            float period = 60 + random.nextFloat() * 50;
            float life = ((time + random.nextFloat() * period) % period) / period;
            float angle = random.nextFloat() * 6.28F + life * 3.5F;
            float r = 1.2F - life;
            Vector3f p = disc.transform(new Vector3f(Mth.cos(angle) * r, (random.nextFloat() - 0.5F) * 0.3F, Mth.sin(angle) * r));
            Vec3 at = at(p.x, p.y, p.z);
            float alpha = Mth.sin(life * Mth.PI);
            if (s % 3 == 0) {
                dark(poseStack, collector, HexFx.MOTE, at, (0.6F + random.nextFloat() * 0.6F) * PX, 0.7F * alpha, 0);
            } else {
                star(poseStack, collector, at, (0.9F + random.nextFloat() * 0.9F) * PX, STAR, alpha * 0.9F, s % 4, angle);
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
        Vec3 dir = points[points.length - 1].subtract(points[0]);
        Vector3f along = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z);
        Vector3f a = new Vector3f(along).cross(0, 1, 0);
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

    /** A soft dark spot (translucent: additive light cannot darken what lies behind it). */
    private static void dark(PoseStack poseStack, SubmitNodeCollector collector, Identifier texture, Vec3 at, float size, float alpha,
                             float roll) {
        if (alpha <= 0.01F) {
            return;
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture),
                (pose, consumer) -> crossed(consumer, pose, at, size, SMOKE, alpha, roll, 0, 1));
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
        output.accept(new Vector3f(center).sub(new Vector3f(radius).mul(1.6F)));
        output.accept(new Vector3f(center).add(new Vector3f(radius).mul(1.6F)));
    }

    /** The JSON side. */
    public record Unbaked(Kind kind, Vector3fc center, Vector3fc radius) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Kind.CODEC.fieldOf("kind").forGetter(Unbaked::kind),
                ExtraCodecs.VECTOR3F.fieldOf("center").forGetter(Unbaked::center),
                ExtraCodecs.VECTOR3F.fieldOf("radius").forGetter(Unbaked::radius)
        ).apply(instance, Unbaked::new));

        @Override
        public ToolAuraRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new ToolAuraRenderer(kind, center, radius);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
