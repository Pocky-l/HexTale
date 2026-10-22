package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.hextale.abyss.AbyssRift;
import com.pockyl.hextale.abyss.RiftShape;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Draws an abyss rift: the opening through the abyss shader ({@link AbyssVoid}), the splinters of torn space around
 * it with the same shader, and a soft additive glow hugging the torn edge ({@link HexGlow}). The mesh is built once per
 * rift ({@link RiftMesh}); every frame only moves its vertices: the crack runs out from where it started, the tear
 * parts with an overshoot, breathes in slow waves, and on closing presses shut and draws into a point.
 */
public class AbyssRiftRenderer extends EntityRenderer<AbyssRift, AbyssRiftRenderer.State> {
    /** Width (blocks) of the hairline crack before the rift parts. */
    private static final float CRACK_WIDTH = 0.035F;
    private static final int MAX_MESHES = 48;
    /** The z the vertex colour can carry to the shader (must match abyss_rift.vsh). */
    private static final float Z_RANGE = 2.0F;
    private static final Map<Long, RiftMesh> MESHES = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, RiftMesh> eldest) {
            return size() > MAX_MESHES;
        }
    };

    public AbyssRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    public static class State extends EntityRenderState {
        RiftShape shape = RiftShape.TEAR;
        float size;
        int seed;
        float age;
        float close;
        float time;
        final Quaternionf orientation = new Quaternionf();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AbyssRift rift, State state, float partialTick) {
        super.extractRenderState(rift, state, partialTick);
        state.shape = rift.shape();
        state.size = rift.size();
        state.seed = rift.seed();
        state.age = rift.age(partialTick);
        state.close = rift.closeProgress(partialTick);
        state.time = HexFx.time(rift.level(), partialTick);
        state.orientation.set(rift.orientation());
    }

    @Override
    protected AABB getBoundingBoxForCulling(AbyssRift rift, float partialTick) {
        double reach = rift.size() * 0.75 + 1;
        return new AABB(rift.position(), rift.position()).inflate(reach);
    }

    private static RiftMesh mesh(State state) {
        long key = (long) state.seed << 32 ^ (long) state.shape.ordinal() << 24 ^ Float.floatToIntBits(state.size);
        return MESHES.computeIfAbsent(key, k -> RiftMesh.build(state.shape, state.size, state.seed));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        RiftMesh mesh = mesh(state);
        float age = state.age;
        float shut = smooth(state.close);
        // At the very end the whole tear draws into its centre.
        float gather = 1 - smooth((state.close - 0.45F) / 0.55F);
        if (gather <= 0.001F) {
            return;
        }
        int n = mesh.count;
        float[] x = new float[n];
        float[] y = new float[n];
        float[] open = new float[n];
        float[] crackGlow = new float[n];
        float[] z = new float[n];
        // The rift is a body of abyss, not a sheet: every piece swells into a closed, faceted lens - front and back skins
        // sewn together along the torn edge. The shader fills it with a 3D abyss, right from every side. A fissure is
        // flatter (its lower skin sinks into the ground).
        float depth = state.shape.halfWidth(state.size) * (state.shape.lying() ? 0.35F : 0.8F);
        float phase = (state.seed & 0xFFFF) / 65535F * Mth.TWO_PI;
        for (int v = 0; v < n; v++) {
            float when = mesh.delay[v];
            float crack = Mth.clamp((age - when * AbyssRift.CRACK_TICKS * 0.8F) / 3, 0, 1);
            float part = Mth.clamp((age - AbyssRift.CRACK_TICKS - when * 7) / (AbyssRift.OPEN_TICKS - AbyssRift.CRACK_TICKS - 7), 0, 1);
            float widen = backOut(part) * (1 - shut);
            float offset = Mth.sqrt(mesh.ox[v] * mesh.ox[v] + mesh.oy[v] * mesh.oy[v]);
            float thin = offset < 1.0E-4F ? 0 : Math.min(1, CRACK_WIDTH / offset) * crack * (1 - shut * 0.6F);
            float breath = 1 + 0.05F * Mth.sin(state.time * 0.11F + mesh.cy[v] * 1.3F + phase)
                    + 0.03F * Mth.sin(state.time * 0.37F + mesh.cx[v] * 2.1F);
            float f = Math.max(widen, thin) * breath;
            x[v] = mesh.cx[v] * gather + mesh.ox[v] * f;
            y[v] = mesh.cy[v] * gather + mesh.oy[v] * f;
            open[v] = part * (1 - shut);
            crackGlow[v] = crack * (1 - part);
            z[v] = depth * mesh.swell[v] * Mth.sin(mesh.edge[v] * Mth.HALF_PI) * Math.min(1, f) * gather;
        }

        poseStack.pushPose();
        poseStack.rotate(state.orientation);
        int seedByte = state.seed & 0x7F;
        collector.submitCustomGeometry(poseStack, AbyssVoid.TYPE, (pose, consumer) -> {
            for (int q = 0; q < mesh.quadCount; q++) {
                for (int side = -1; side <= 1; side += 2) {
                    for (int k = 0; k < 4; k++) {
                        int v = mesh.quads[q * 4 + k];
                        voidVertex(consumer, pose, x[v], y[v], z[v] * side, mesh.edge[v], open[v], false, seedByte);
                    }
                }
            }
            shards(consumer, pose, mesh, state, gather, shut, seedByte);
        });

        float glowWidth = 0.08F + state.size * 0.012F;
        float pulse = 0.85F + 0.15F * Mth.sin(state.time * 0.23F + phase);
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            for (int[] rim : mesh.rims) {
                for (int i = 0; i + 1 < rim.length; i++) {
                    int a = rim[i];
                    int b = rim[i + 1];
                    float strengthA = Math.max(open[a] * 0.32F, crackGlow[a] * 0.8F) * pulse;
                    float strengthB = Math.max(open[b] * 0.32F, crackGlow[b] * 0.8F) * pulse;
                    if (strengthA <= 0.01F && strengthB <= 0.01F) {
                        continue;
                    }
                    // From a little inside the cut (the shader eats into the edge) out into the air.
                    float ax = Mth.lerp(0.78F, mesh.cx[a] * gather, x[a]);
                    float ay = Mth.lerp(0.78F, mesh.cy[a] * gather, y[a]);
                    float bx = Mth.lerp(0.78F, mesh.cx[b] * gather, x[b]);
                    float by = Mth.lerp(0.78F, mesh.cy[b] * gather, y[b]);
                    band(consumer, pose, ax, ay, bx, by, mesh, a, b, glowWidth, AbyssRift.MAGENTA, strengthA, strengthB);
                    band(consumer, pose, ax, ay, bx, by, mesh, a, b, glowWidth * 3, AbyssRift.VIOLET, strengthA * 0.18F,
                            strengthB * 0.18F);
                }
            }
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    /** One quad of glow from the edge segment a-b outwards, fading to nothing. */
    private static void band(VertexConsumer consumer, PoseStack.Pose pose, float ax, float ay, float bx, float by, RiftMesh mesh, int a, int b,
                             float width, int colour, float alphaA, float alphaB) {
        consumer.addVertex(pose, ax, ay, 0).setColor(RuneDrawing.argb(colour, alphaA));
        consumer.addVertex(pose, bx, by, 0).setColor(RuneDrawing.argb(colour, alphaB));
        consumer.addVertex(pose, bx + mesh.nx[b] * width, by + mesh.ny[b] * width, 0).setColor(RuneDrawing.argb(colour, 0));
        consumer.addVertex(pose, ax + mesh.nx[a] * width, ay + mesh.ny[a] * width, 0).setColor(RuneDrawing.argb(colour, 0));
    }

    private static final int[][] FACES = {{0, 1, 2}, {0, 2, 3}, {0, 3, 1}, {1, 3, 2}};

    /** The splinters: fly out of the tear as it parts, drift, tumble, and are sucked back in when it shuts. */
    private static void shards(VertexConsumer consumer, PoseStack.Pose pose, RiftMesh mesh, State state, float gather, float shut, int seedByte) {
        float appear = smooth((state.age - AbyssRift.CRACK_TICKS - 4) / 20) * (1 - shut);
        if (appear <= 0.01F) {
            return;
        }
        float orbit = state.time * 0.004F;
        Vector3f[] corner = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
        Quaternionf turn = new Quaternionf();
        for (RiftMesh.Shard shard : mesh.shards) {
            float out = (0.3F + 0.7F * appear) * gather;
            float drift = Mth.sin(state.time * 0.05F + shard.phase());
            float cos = Mth.cos(orbit + shard.phase() * 0.1F);
            float sin = Mth.sin(orbit + shard.phase() * 0.1F);
            float px = (shard.x() * cos - shard.y() * sin) * out;
            float py = (shard.x() * sin + shard.y() * cos) * out + drift * 0.05F;
            float pz = shard.z() * out + Mth.cos(state.time * 0.04F + shard.phase()) * 0.06F;
            turn.rotationAxis(state.time * shard.spin() + shard.phase(), shard.axisX(), shard.axisY(), shard.axisZ());
            float[] c = shard.corners();
            for (int i = 0; i < 4; i++) {
                turn.transform(corner[i].set(c[i * 3], c[i * 3 + 1], c[i * 3 + 2]).mul(appear)).add(px, py, pz);
            }
            for (int[] face : FACES) {
                Vector3f a = corner[face[0]];
                Vector3f b = corner[face[1]];
                Vector3f d = corner[face[2]];
                float mx = (a.x + b.x + d.x) / 3;
                float my = (a.y + b.y + d.y) / 3;
                float mz = (a.z + b.z + d.z) / 3;
                // Each face is a fan from its middle, so its edges are lit and its middle shows the abyss.
                Vector3f[] ring = {a, b, d};
                for (int i = 0; i < 3; i++) {
                    Vector3f p = ring[i];
                    Vector3f q = ring[(i + 1) % 3];
                    voidVertex(consumer, pose, mx, my, mz, 1, 1, true, seedByte);
                    voidVertex(consumer, pose, p.x, p.y, p.z, 0, 1, true, seedByte);
                    voidVertex(consumer, pose, q.x, q.y, q.z, 0, 1, true, seedByte);
                    voidVertex(consumer, pose, q.x, q.y, q.z, 0, 1, true, seedByte);
                }
            }
        }
    }

    private static void voidVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float edge, float open, boolean shard,
                                   int seedByte) {
        // Colour: edge depth, openness, phase (+128 on a shard), and z in the rift's frame (128 = 0, 127 = Z_RANGE).
        consumer.addVertex(pose, x, y, z).setUv(x, y).setColor(Math.round(Mth.clamp(edge, 0, 1) * 255), Math.round(Mth.clamp(open, 0, 1) * 255),
                seedByte + (shard ? 128 : 0), 128 + Math.round(Mth.clamp(z / Z_RANGE, -1, 1) * 127));
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** 0 -> 1 overshooting a little before it settles: the tear springs open. */
    private static float backOut(float t) {
        if (t <= 0) {
            return 0;
        }
        float c1 = 1.4F;
        float u = t - 1;
        return 1 + (c1 + 1) * u * u * u + c1 * u * u;
    }
}
