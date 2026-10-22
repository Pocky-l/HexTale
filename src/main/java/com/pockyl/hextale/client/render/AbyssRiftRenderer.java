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
 * Draws an abyss rift as a body: every piece of it is a tube along its wandering spine, thick in the middle and drawn
 * out to points, filled by the abyss shader ({@link AbyssVoid}) with a 3D field seen along the view ray and lit along
 * its silhouette from whatever side. Splinters of torn space tumble around it. The outline is built once per rift
 * ({@link RiftMesh}); every frame only the radii move: a hairline runs out from where the crack started, the body
 * swells with an overshoot, breathes in slow waves, and on closing thins and draws into a point.
 */
public class AbyssRiftRenderer extends EntityRenderer<AbyssRift, AbyssRiftRenderer.State> {
    /** Radius (blocks) of the hairline before the rift swells. */
    private static final float CRACK_RADIUS = 0.02F;
    /** Vertices around a tube. */
    private static final int SIDES = 14;
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
        float shut = smooth(state.close);
        // At the very end the whole rift draws into its centre.
        float gather = 1 - smooth((state.close - 0.45F) / 0.55F);
        if (gather <= 0.001F) {
            return;
        }
        int seedByte = state.seed & 0x7F;
        float phase = (state.seed & 0xFFFF) / 65535F * Mth.TWO_PI;
        poseStack.pushPose();
        poseStack.rotate(state.orientation);
        collector.submitCustomGeometry(poseStack, AbyssVoid.TYPE, (pose, consumer) -> {
            for (RiftMesh.Tube tube : mesh.tubes) {
                tube(consumer, pose, tube, state, shut, gather, phase, seedByte);
            }
            shards(consumer, pose, mesh, state, gather, shut, seedByte);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    /** One tube: a ring of vertices round every spine point, the ring's radius following the rift's timeline. */
    private static void tube(VertexConsumer consumer, PoseStack.Pose pose, RiftMesh.Tube tube, State state, float shut, float gather, float phase,
                             int seedByte) {
        int n = tube.x().length;
        float[][] ring = new float[n][];
        float[] open = new float[n];
        float squash = tube.depth();
        for (int i = 0; i < n; i++) {
            float when = tube.delay()[i];
            float crack = Mth.clamp((state.age - when * AbyssRift.CRACK_TICKS * 0.8F) / 3, 0, 1);
            float part = Mth.clamp((state.age - AbyssRift.CRACK_TICKS - when * 7) / (AbyssRift.OPEN_TICKS - AbyssRift.CRACK_TICKS - 7), 0, 1);
            float full = tube.radius()[i];
            float breath = 1 + 0.06F * Mth.sin(state.time * 0.11F + tube.y()[i] * 1.3F + phase)
                    + 0.03F * Mth.sin(state.time * 0.37F + tube.x()[i] * 2.1F + tube.phase());
            float radius = Math.max(full * backOut(part) * (1 - shut), Math.min(full, CRACK_RADIUS) * crack * (1 - shut * 0.6F)) * breath;
            open[i] = part * (1 - shut);
            float cx = tube.x()[i] * gather;
            float cy = tube.y()[i] * gather;
            float tx = tube.x()[Math.min(n - 1, i + 1)] - tube.x()[Math.max(0, i - 1)];
            float ty = tube.y()[Math.min(n - 1, i + 1)] - tube.y()[Math.max(0, i - 1)];
            float tl = Mth.sqrt(tx * tx + ty * ty);
            float sx = tl < 1.0E-5F ? 1 : -ty / tl;
            float sy = tl < 1.0E-5F ? 0 : tx / tl;
            // Round in section but not perfectly: fuller or flatter across the plane, a few soft lumps that turn slowly
            // along the tube, so no two pieces are alike.
            float twist = tube.phase() + i * 0.35F;
            float[] points = new float[SIDES * 6];
            for (int k = 0; k < SIDES; k++) {
                float angle = k * Mth.TWO_PI / SIDES;
                float lump = 1 + 0.05F * Mth.sin(angle * 3 + twist);
                float c = Mth.cos(angle) * lump;
                float s = Mth.sin(angle) * lump;
                points[k * 6] = cx + sx * c * radius;
                points[k * 6 + 1] = cy + sy * c * radius;
                points[k * 6 + 2] = s * radius * squash;
                // The skin's normal of an elliptic section: the radii divided out.
                float nx = sx * Mth.cos(angle);
                float ny = sy * Mth.cos(angle);
                float nz = Mth.sin(angle) / squash;
                float nl = Mth.sqrt(nx * nx + ny * ny + nz * nz);
                points[k * 6 + 3] = nx / nl;
                points[k * 6 + 4] = ny / nl;
                points[k * 6 + 5] = nz / nl;
            }
            ring[i] = points;
        }
        for (int i = 0; i + 1 < n; i++) {
            float[] a = ring[i];
            float[] b = ring[i + 1];
            for (int k = 0; k < SIDES; k++) {
                int m = (k + 1) % SIDES;
                bodyVertex(consumer, pose, a, k, open[i], seedByte);
                bodyVertex(consumer, pose, b, k, open[i + 1], seedByte);
                bodyVertex(consumer, pose, b, m, open[i + 1], seedByte);
                bodyVertex(consumer, pose, a, m, open[i], seedByte);
            }
        }
    }

    private static void bodyVertex(VertexConsumer consumer, PoseStack.Pose pose, float[] ring, int k, float open, int seedByte) {
        int at = k * 6;
        vertex(consumer, pose, ring[at], ring[at + 1], ring[at + 2], ring[at + 3], ring[at + 4], ring[at + 5], open, false, seedByte);
    }

    private static final int[][] FACES = {{0, 1, 2}, {0, 2, 3}, {0, 3, 1}, {1, 3, 2}};

    /** The splinters: fly out of the rift as it swells, drift, tumble, and are sucked back in when it shuts. */
    private static void shards(VertexConsumer consumer, PoseStack.Pose pose, RiftMesh mesh, State state, float gather, float shut, int seedByte) {
        float appear = smooth((state.age - AbyssRift.CRACK_TICKS - 4) / 20) * (1 - shut);
        if (appear <= 0.01F) {
            return;
        }
        float orbit = state.time * 0.004F;
        Vector3f[] corner = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
        Vector3f normal = new Vector3f();
        Vector3f centre = new Vector3f();
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
            centre.set(0);
            for (int i = 0; i < 4; i++) {
                turn.transform(corner[i].set(c[i * 3], c[i * 3 + 1], c[i * 3 + 2]).mul(appear)).add(px, py, pz);
                centre.add(corner[i]);
            }
            centre.mul(0.25F);
            for (int[] face : FACES) {
                Vector3f a = corner[face[0]];
                Vector3f b = corner[face[1]];
                Vector3f d = corner[face[2]];
                new Vector3f(b).sub(a).cross(new Vector3f(d).sub(a), normal);
                // The face's normal points out of the splinter.
                if (normal.dot(new Vector3f(a).sub(centre)) < 0) {
                    normal.negate();
                }
                normal.normalize();
                for (Vector3f p : new Vector3f[]{a, b, d, d}) {
                    vertex(consumer, pose, p.x, p.y, p.z, normal.x, normal.y, normal.z, 1, true, seedByte);
                }
            }
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float nx, float ny, float nz, float open,
                               boolean shard, int seedByte) {
        // Colour: unused, openness, phase (+128 on a shard), z in the rift's frame (128 = 0, 127 = Z_RANGE). The normal
        // stays in the rift's frame (no pose): the shader works there.
        consumer.addVertex(pose, x, y, z).setUv(x, y).setColor(255, Math.round(Mth.clamp(open, 0, 1) * 255), seedByte + (shard ? 128 : 0),
                128 + Math.round(Mth.clamp(z / Z_RANGE, -1, 1) * 127)).setNormal(nx, ny, nz);
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** 0 -> 1 overshooting a little before it settles: the body springs out. */
    private static float backOut(float t) {
        if (t <= 0) {
            return 0;
        }
        float c1 = 1.4F;
        float u = t - 1;
        return 1 + (c1 + 1) * u * u * u + c1 * u * u;
    }
}
