package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.pockyl.hextale.port.client.EntityRenderState;
import com.pockyl.hextale.port.client.RenderTypes;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

import com.pockyl.hextale.thevoid.TheVoid;
import com.pockyl.hextale.thevoid.VoidLayout;
import com.pockyl.hextale.thevoid.VoidShape;

/**
 * Draws the Void as a thin slab of square cells: their faces look into a whirling tunnel (the shader), their outline
 * burns, and a glow spreads over the free cells around it. Every frame decides which cells show - the breach spreads
 * open from its heart and shrinks back into it - and what each cell knows of its neighbours (which sides are open,
 * where seams run), packed into the vertex data.
 */
public class TheVoidRenderer extends EntityRenderer<TheVoid, TheVoidRenderer.State> {
    // Neighbour bits in the masks the shaders read: four sides, then the four corners.
    /** The game's starfield, the dark of the plain look drawn while a shader pack is on. */
    private static final net.minecraft.resources.ResourceLocation STARS = net.minecraft.resources.ResourceLocation.withDefaultNamespace(
            "textures/entity/end_portal.png");
    /** The plain look's tint of the stars and colour of the rim. */
    private static final int PLAIN_TINT = 0xFFC9A6FF;
    private static final int RIM = 0xB98FE6;
    /** A gate's beacon: a column of warm light over it, seen from far off - the way home. */
    private static final int BEACON = 0xF2B266;
    private static final float BEACON_HEIGHT = 150;
    private static final int[][] NEIGHBOURS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {-1, 1}, {-1, -1}, {1, -1}};

    public TheVoidRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    public static class State extends EntityRenderState {
        VoidShape shape = VoidShape.GATE;
        float size;
        int seed;
        float reveal;
        final Quaternionf orientation = new Quaternionf();
        /** Where a voidling pushes out (local), and how hard (0 = nothing pushes). */
        float pushX;
        float pushY;
        float push;
        /** 0 wild .. 1 cooled by its anchors. */
        float calm;
        /** 0 .. 1 while it folds into its heart. */
        float fold;
        /** A gate inside the Void: our world shows through it. */
        boolean gate;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TheVoid breach, State state, float partialTick) {
        super.extractRenderState(breach, state, partialTick);
        state.shape = breach.shape();
        state.size = breach.size();
        state.seed = breach.seed();
        state.reveal = breach.reveal(partialTick);
        float starving = breach.starving();
        if (starving > 0) {
            // A starving breach shrinks towards its heart and its edge gutters like a flame without air.
            float gutter = Mth.sin(state.ageInTicks * 0.9F) * Mth.sin(state.ageInTicks * 0.37F + 1.3F);
            state.reveal *= 1 - starving * (0.3F + 0.18F * Math.max(0, gutter));
        }
        state.orientation.set(breach.orientation());
        state.calm = breach.calm();
        state.fold = breach.fold(partialTick);
        state.gate = breach.gate();
        org.joml.Vector3fc push = breach.pushPoint();
        state.push = push == null ? 0 : breach.push(partialTick);
        if (push != null) {
            state.pushX = push.x();
            state.pushY = push.y();
        }
    }

    @Override
    protected AABB getBoundingBoxForCulling(TheVoid breach, float partialTick) {
        AABB box = new AABB(breach.position(), breach.position()).inflate(breach.layout().reach() + 1);
        return breach.gate() ? box.expandTowards(0, BEACON_HEIGHT, 0) : box;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.reveal <= 0.001F) {
            return;
        }
        VoidLayout layout = VoidLayout.of(state.shape, state.size, state.seed);
        int w = layout.width;
        int h = layout.height;
        boolean[] shows = new boolean[w * h];
        int count = 0;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                if (layout.shows(i, j, state.reveal)) {
                    shows[i + j * w] = true;
                    count++;
                }
            }
        }
        if (count == 0) {
            return;
        }
        Frame frame = new Frame(layout, shows, state);
        if (state.gate && !com.pockyl.hextale.client.IrisCompat.shadowPass()) {
            beacon(state, layout, poseStack, collector, camera);
        }
        poseStack.pushPose();
        poseStack.mulPose(state.orientation);
        if (state.fold > 0 && !fold(state, poseStack)) {
            poseStack.popPose();
            return;
        }
        if (com.pockyl.hextale.client.IrisCompat.shadersOn()) {
            if (com.pockyl.hextale.client.IrisCompat.shadowPass()) {
                // The Void casts no shadow.
            } else if (VoidOverlay.works()) {
                // A shader pack cannot run our shaders: they are drawn once the pack has finished the picture.
                VoidOverlay.queue(poseStack.last(), frame::body, frame::halo);
            } else {
                // The plain look, a starry dark with a glowing rim, in the pack's own programs.
                collector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(STARS), (pose, consumer) -> frame.plainBody(consumer, pose));
                collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> frame.plainHalo(consumer, pose));
            }
        } else {
            collector.submitCustomGeometry(poseStack, VoidPipelines.BODY_TYPE, (pose, consumer) -> frame.body(consumer, pose));
            collector.submitCustomGeometry(poseStack, VoidPipelines.HALO_TYPE, (pose, consumer) -> frame.halo(consumer, pose));
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    /** The column of warm light standing over a gate, breathing slowly, fading as it rises. */
    private static void beacon(State state, VoidLayout layout, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float bottom = layout.reach() * state.reveal;
        int segments = 15;
        net.minecraft.world.phys.Vec3[] points = new net.minecraft.world.phys.Vec3[segments + 1];
        float[] light = new float[segments + 1];
        for (int k = 0; k <= segments; k++) {
            float t = k / (float) segments;
            points[k] = new net.minecraft.world.phys.Vec3(0, bottom + (BEACON_HEIGHT - bottom) * t, 0);
            light[k] = (1 - t) * Math.min(1, k * 0.6F);
        }
        org.joml.Vector3f eye = new org.joml.Vector3f((float) (camera.pos.x - state.x), (float) (camera.pos.y - state.y),
                (float) (camera.pos.z - state.z));
        float breath = (0.8F + 0.2F * Mth.sin(state.ageInTicks * 0.05F)) * state.reveal;
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            JarStreamFx.strip(consumer, pose, points, light, eye, 4.0F, BEACON, 0.22F * breath);
            JarStreamFx.strip(consumer, pose, points, light, eye, 1.4F, BEACON, 0.4F * breath);
            JarStreamFx.strip(consumer, pose, points, light, eye, 0.4F, 0xFFE8C0, 0.6F * breath);
        });
    }

    /**
     * Folding into its heart: it trembles, winds round its own middle faster and faster, is squeezed into a narrow seam
     * and at the end falls into a point. Returns false once nothing of it is left to draw.
     */
    private static boolean fold(State state, PoseStack poseStack) {
        float f = state.fold;
        float collapse = f < 0.85F ? 1 : 1 - (f - 0.85F) / 0.15F;
        if (collapse <= 0.01F) {
            return false;
        }
        float tremble = Mth.sin(state.ageInTicks * 3.1F) * 0.05F * Math.min(1, f * 4) * collapse;
        float squeeze = smooth(f / 0.85F);
        float shorten = smooth((f - 0.3F) / 0.55F);
        poseStack.mulPose(new org.joml.Quaternionf().rotationZ(f * f * Mth.TWO_PI * 1.25F));
        poseStack.scale((1 - 0.82F * squeeze) * collapse + tremble, (1 - 0.45F * shorten) * collapse - tremble, collapse);
        return true;
    }

    private static float smooth(float x) {
        float k = Mth.clamp(x, 0, 1);
        return k * k * (3 - 2 * k);
    }

    /** One frame of one breach: which cells show and the numbers every vertex carries. */
    private static final class Frame {
        private final VoidLayout layout;
        private final boolean[] shows;
        private final int open;
        private final int seed;
        private final int cellMm;
        private final int core;
        private final int sizeCm;
        private final float reveal;
        /** How far each cell bulges out of the front (blocks), null when nothing pushes. */
        private final float @org.jspecify.annotations.Nullable [] bulge;

        Frame(VoidLayout layout, boolean[] shows, State state) {
            this.layout = layout;
            this.shows = shows;
            open = Math.round(Mth.clamp(state.reveal, 0, 1) * 255);
            seed = state.seed & 0x7F;
            cellMm = Math.round(layout.cell * 1000);
            // A gate carries its core as a negative number: the shader shows our world through it.
            core = state.gate ? -Math.round(state.shape.core() * 1000) - 1 : Math.round(state.shape.core() * 1000);
            // The shaders read the size from the low 11 bits and how far it has cooled from the bits above.
            sizeCm = Math.min(2047, Math.round(state.size * 100)) | Math.round(Mth.clamp(state.calm, 0, 1) * 15) << 11;
            reveal = state.reveal;
            bulge = state.push > 0.001F ? bulge(layout, state) : null;
        }

        /** The cells round a voidling pushing out stretch forward, most at the point, like a skin about to give. */
        private static float[] bulge(VoidLayout layout, State state) {
            float[] out = new float[layout.width * layout.height];
            float radius = 0.95F * (0.6F + 0.4F * state.push);
            float amount = 0.55F * state.push;
            for (int j = 0; j < layout.height; j++) {
                float cy = (layout.y(j) + layout.y(j + 1)) * 0.5F - state.pushY;
                for (int i = 0; i < layout.width; i++) {
                    float cx = (layout.x(i) + layout.x(i + 1)) * 0.5F - state.pushX;
                    float d = (cx * cx + cy * cy) / (radius * radius);
                    if (d < 1) {
                        out[i + j * layout.width] = amount * (1 - d) * (1 - d);
                    }
                }
            }
            return out;
        }

        private float lift(int i, int j) {
            return bulge == null || !layout.inside(i, j) ? 0 : bulge[i + j * layout.width];
        }

        private boolean at(int i, int j) {
            return layout.inside(i, j) && shows[i + j * layout.width];
        }

        /** 1-12 per region (0 = a dead cell). */
        private int shade(byte region) {
            if (region == VoidLayout.DEAD) {
                return 0;
            }
            return 2 + (int) (VoidLayout.hash(region, layout.seed) * 11);
        }

        void body(VertexConsumer consumer, PoseStack.Pose pose) {
            float t = layout.halfThickness();
            for (int j = 0; j < layout.height; j++) {
                for (int i = 0; i < layout.width; i++) {
                    if (!at(i, j)) {
                        continue;
                    }
                    int off = 0;
                    for (int n = 0; n < 8; n++) {
                        if (!at(i + NEIGHBOURS[n][0], j + NEIGHBOURS[n][1])) {
                            off |= 1 << n;
                        }
                    }
                    byte region = layout.region(i, j);
                    int seams = 0;
                    for (int n = 0; n < 4; n++) {
                        int x = i + NEIGHBOURS[n][0];
                        int y = j + NEIGHBOURS[n][1];
                        if (at(x, y) && layout.region(x, y) != region) {
                            seams |= 1 << n;
                        }
                    }
                    int data = seams | shade(region) << 4;
                    int fresh = Math.round(layout.fresh(i, j, reveal) * 1000);
                    float x0 = layout.x(i);
                    float y0 = layout.y(j);
                    // The neighbour's own numbers, not x0 + c: rounding apart would open hairline cracks.
                    float x1 = layout.x(i + 1);
                    float y1 = layout.y(j + 1);
                    float dz = lift(i, j);
                    // Front and back: both look into the Void.
                    face(consumer, pose, x0, y0, x1, y1, t + dz, off, data, fresh, 1);
                    face(consumer, pose, x0, y0, x1, y1, -t + dz, off, data, fresh, -1);
                    // The burning sides where the breach ends, and where a bulging cell stands out of its neighbour.
                    if ((off & 1) != 0 || lift(i + 1, j) < dz - 0.002F) {
                        side(consumer, pose, x1, y0, x1, y1, -t + ((off & 1) != 0 ? dz : lift(i + 1, j)), t + dz, 1, 0, fresh);
                    }
                    if ((off & 2) != 0 || lift(i, j + 1) < dz - 0.002F) {
                        side(consumer, pose, x0, y1, x1, y1, -t + ((off & 2) != 0 ? dz : lift(i, j + 1)), t + dz, 0, 1, fresh);
                    }
                    if ((off & 4) != 0 || lift(i - 1, j) < dz - 0.002F) {
                        side(consumer, pose, x0, y0, x0, y1, -t + ((off & 4) != 0 ? dz : lift(i - 1, j)), t + dz, -1, 0, fresh);
                    }
                    if ((off & 8) != 0 || lift(i, j - 1) < dz - 0.002F) {
                        side(consumer, pose, x0, y0, x1, y0, -t + ((off & 8) != 0 ? dz : lift(i, j - 1)), t + dz, 0, -1, fresh);
                    }
                }
            }
        }

        private void face(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float z, int off, int data, int fresh,
                          float nz) {
            vertex(consumer, pose, x0, y0, z, x0, y0, off, seed, data, fresh, 0, 0, nz);
            vertex(consumer, pose, x1, y0, z, x1, y0, off, seed, data, fresh, 0, 0, nz);
            vertex(consumer, pose, x1, y1, z, x1, y1, off, seed, data, fresh, 0, 0, nz);
            vertex(consumer, pose, x0, y1, z, x0, y1, off, seed, data, fresh, 0, 0, nz);
        }

        /** A side wall from (ax, ay) to (bx, by), from z0 at the back to z1 at the front; UV = (along, z share -1..1). */
        private void side(VertexConsumer consumer, PoseStack.Pose pose, float ax, float ay, float bx, float by, float z0, float z1, float nx, float ny,
                          int fresh) {
            float a = ax + ay;
            float b = bx + by;
            int flag = seed + 128;
            vertex(consumer, pose, ax, ay, z0, a, -1, 0, flag, 0, fresh, nx, ny, 0);
            vertex(consumer, pose, bx, by, z0, b, -1, 0, flag, 0, fresh, nx, ny, 0);
            vertex(consumer, pose, bx, by, z1, b, 1, 0, flag, 0, fresh, nx, ny, 0);
            vertex(consumer, pose, ax, ay, z1, a, 1, 0, flag, 0, fresh, nx, ny, 0);
        }

        /** The glow: every free cell beside the breach, told which of its neighbours burn. */
        void halo(VertexConsumer consumer, PoseStack.Pose pose) {
            for (int j = 0; j < layout.height; j++) {
                for (int i = 0; i < layout.width; i++) {
                    if (at(i, j)) {
                        continue;
                    }
                    int on = 0;
                    for (int n = 0; n < 8; n++) {
                        if (at(i + NEIGHBOURS[n][0], j + NEIGHBOURS[n][1])) {
                            on |= 1 << n;
                        }
                    }
                    if (on == 0) {
                        continue;
                    }
                    float x0 = layout.x(i);
                    float y0 = layout.y(j);
                    float x1 = layout.x(i + 1);
                    float y1 = layout.y(j + 1);
                    vertex(consumer, pose, x0, y0, 0, x0, y0, on, seed, 0, 0, 0, 0, 1);
                    vertex(consumer, pose, x1, y0, 0, x1, y0, on, seed, 0, 0, 0, 0, 1);
                    vertex(consumer, pose, x1, y1, 0, x1, y1, on, seed, 0, 0, 0, 0, 1);
                    vertex(consumer, pose, x0, y1, 0, x0, y1, on, seed, 0, 0, 0, 0, 1);
                }
            }
        }

        /** The plain look: every shown cell a slab of stars (both faces and its open sides), each cell its own patch of sky. */
        void plainBody(VertexConsumer consumer, PoseStack.Pose pose) {
            float t = layout.halfThickness();
            for (int j = 0; j < layout.height; j++) {
                for (int i = 0; i < layout.width; i++) {
                    if (!at(i, j)) {
                        continue;
                    }
                    float x0 = layout.x(i);
                    float y0 = layout.y(j);
                    float x1 = layout.x(i + 1);
                    float y1 = layout.y(j + 1);
                    float dz = lift(i, j);
                    float u = patch(i * 31 + j * 17);
                    float v = patch(i * 13 + j * 29 + 7);
                    float s = 0.25F;
                    plain(consumer, pose, x0, y0, t + dz, u, v, 0, 0, 1);
                    plain(consumer, pose, x1, y0, t + dz, u + s, v, 0, 0, 1);
                    plain(consumer, pose, x1, y1, t + dz, u + s, v + s, 0, 0, 1);
                    plain(consumer, pose, x0, y1, t + dz, u, v + s, 0, 0, 1);
                    plain(consumer, pose, x0, y1, -t + dz, u, v + s, 0, 0, -1);
                    plain(consumer, pose, x1, y1, -t + dz, u + s, v + s, 0, 0, -1);
                    plain(consumer, pose, x1, y0, -t + dz, u + s, v, 0, 0, -1);
                    plain(consumer, pose, x0, y0, -t + dz, u, v, 0, 0, -1);
                    if (!at(i + 1, j)) {
                        plainSide(consumer, pose, x1, y0, x1, y1, -t + dz, t + dz, u, v, 1, 0);
                    }
                    if (!at(i, j + 1)) {
                        plainSide(consumer, pose, x0, y1, x1, y1, -t + dz, t + dz, u, v, 0, 1);
                    }
                    if (!at(i - 1, j)) {
                        plainSide(consumer, pose, x0, y0, x0, y1, -t + dz, t + dz, u, v, -1, 0);
                    }
                    if (!at(i, j - 1)) {
                        plainSide(consumer, pose, x0, y0, x1, y0, -t + dz, t + dz, u, v, 0, -1);
                    }
                }
            }
        }

        /** Where in the starfield a cell's patch starts (0..0.75), fixed per breach and cell. */
        private float patch(int salt) {
            int h = (salt + seed * 101) * 0x9E3779B1;
            return ((h >>> 8) & 0xFFFF) / 65536F * 0.75F;
        }

        /** A side wall of the plain look; both windings, so it shows from either side. */
        private void plainSide(VertexConsumer consumer, PoseStack.Pose pose, float ax, float ay, float bx, float by, float z0, float z1, float u,
                               float v, float nx, float ny) {
            float s = 0.25F;
            plain(consumer, pose, ax, ay, z0, u, v, nx, ny, 0);
            plain(consumer, pose, bx, by, z0, u + s, v, nx, ny, 0);
            plain(consumer, pose, bx, by, z1, u + s, v + 0.05F, nx, ny, 0);
            plain(consumer, pose, ax, ay, z1, u, v + 0.05F, nx, ny, 0);
            plain(consumer, pose, ax, ay, z1, u, v + 0.05F, nx, ny, 0);
            plain(consumer, pose, bx, by, z1, u + s, v + 0.05F, nx, ny, 0);
            plain(consumer, pose, bx, by, z0, u + s, v, nx, ny, 0);
            plain(consumer, pose, ax, ay, z0, u, v, nx, ny, 0);
        }

        private void plain(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
            consumer.addVertex(pose, x, y, z).setColor(PLAIN_TINT).setUv(u, v).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                    .setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT).setNormal(pose, nx, ny, nz);
        }

        /** The plain rim: the free cells beside the breach glow, brightest at the corners that touch it, fading outwards. */
        void plainHalo(VertexConsumer consumer, PoseStack.Pose pose) {
            int alpha = Math.round(160 * Mth.clamp(reveal, 0, 1));
            for (int j = 0; j < layout.height; j++) {
                for (int i = 0; i < layout.width; i++) {
                    if (at(i, j)) {
                        continue;
                    }
                    int c00 = touches(i, j) ? alpha : 0;
                    int c10 = touches(i + 1, j) ? alpha : 0;
                    int c11 = touches(i + 1, j + 1) ? alpha : 0;
                    int c01 = touches(i, j + 1) ? alpha : 0;
                    if ((c00 | c10 | c11 | c01) == 0) {
                        continue;
                    }
                    float x0 = layout.x(i);
                    float y0 = layout.y(j);
                    float x1 = layout.x(i + 1);
                    float y1 = layout.y(j + 1);
                    consumer.addVertex(pose, x0, y0, 0).setColor(c00 << 24 | RIM);
                    consumer.addVertex(pose, x1, y0, 0).setColor(c10 << 24 | RIM);
                    consumer.addVertex(pose, x1, y1, 0).setColor(c11 << 24 | RIM);
                    consumer.addVertex(pose, x0, y1, 0).setColor(c01 << 24 | RIM);
                }
            }
        }

        /** Whether the cell corner at (i, j) touches a shown cell. */
        private boolean touches(int i, int j) {
            return at(i, j) || at(i - 1, j) || at(i, j - 1) || at(i - 1, j - 1);
        }

        private void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int mask, int flags, int data,
                            int fresh, float nx, float ny, float nz) {
            // Colour: neighbour mask, openness, seed (+128 on a side wall), seams | shade << 4. UV1: cell size in
            // thousandths, core brightness in thousandths. UV2: fresh flash in thousandths, size in hundredths. The
            // normal stays in the breach's frame (no pose).
            consumer.addVertex(pose, x, y, z).setColor(mask, open, flags, data).setUv(u, v).setUv1(cellMm, core).setUv2(fresh, sizeCm)
                    .setNormal(nx, ny, nz);
        }
    }
}
