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

import com.pockyl.hextale.thevoid.TheVoid;
import com.pockyl.hextale.thevoid.VoidLayout;
import com.pockyl.hextale.thevoid.VoidShape;

/**
 * Draws the Void as a thin slab of square cells: their faces look into a whirling tunnel (the shader), their outline
 * burns, and a glow spreads over the free cells around it. Every frame decides which cells show - the breach glitches
 * open cell by cell, teeth and loose cells flicker, now and then a region of it flashes - and what each cell knows of
 * its neighbours (which sides are open, where seams run), packed into the vertex data.
 */
public class TheVoidRenderer extends EntityRenderer<TheVoid, TheVoidRenderer.State> {
    // Neighbour bits in the masks the shaders read: four sides, then the four corners.
    private static final int[][] NEIGHBOURS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {-1, 1}, {-1, -1}, {1, -1}};

    public TheVoidRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    public static class State extends EntityRenderState {
        VoidShape shape = VoidShape.GATE;
        float size;
        int seed;
        float age;
        float reveal;
        final Quaternionf orientation = new Quaternionf();
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
        state.age = breach.age(partialTick);
        state.reveal = breach.reveal(partialTick);
        state.orientation.set(breach.orientation());
    }

    @Override
    protected AABB getBoundingBoxForCulling(TheVoid breach, float partialTick) {
        return new AABB(breach.position(), breach.position()).inflate(breach.layout().reach() + 1);
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
                if (layout.shows(i, j, state.reveal, state.age)) {
                    shows[i + j * w] = true;
                    count++;
                }
            }
        }
        if (count == 0) {
            return;
        }
        Frame frame = new Frame(layout, shows, state);
        poseStack.pushPose();
        poseStack.rotate(state.orientation);
        collector.submitCustomGeometry(poseStack, VoidPipelines.BODY_TYPE, (pose, consumer) -> frame.body(consumer, pose));
        collector.submitCustomGeometry(poseStack, VoidPipelines.HALO_TYPE, (pose, consumer) -> frame.halo(consumer, pose));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
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
        private final int glitch;

        Frame(VoidLayout layout, boolean[] shows, State state) {
            this.layout = layout;
            this.shows = shows;
            open = Math.round(Mth.clamp(state.reveal, 0, 1) * 255);
            seed = state.seed & 0x7F;
            cellMm = Math.round(layout.cell * 1000);
            core = Math.round(state.shape.core() * 1000);
            sizeCm = Math.round(state.size * 100);
            reveal = state.reveal;
            glitch = (int) (state.age / 7);
        }

        private boolean at(int i, int j) {
            return layout.inside(i, j) && shows[i + j * layout.width];
        }

        /** 1-15 per region (0 = a dead cell), with a rare flash when the region glitches. */
        private int shade(byte region) {
            if (region == VoidLayout.DEAD) {
                return 0;
            }
            if (VoidLayout.hash(region * 31 + layout.seed, glitch) > 0.965F) {
                return 15;
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
                    // Front and back: both look into the Void.
                    face(consumer, pose, x0, y0, x1, y1, t, off, data, fresh, 1);
                    face(consumer, pose, x0, y0, x1, y1, -t, off, data, fresh, -1);
                    // The burning sides where the breach ends.
                    if ((off & 1) != 0) {
                        side(consumer, pose, x1, y0, x1, y1, t, 1, 0, fresh);
                    }
                    if ((off & 2) != 0) {
                        side(consumer, pose, x0, y1, x1, y1, t, 0, 1, fresh);
                    }
                    if ((off & 4) != 0) {
                        side(consumer, pose, x0, y0, x0, y1, t, -1, 0, fresh);
                    }
                    if ((off & 8) != 0) {
                        side(consumer, pose, x0, y0, x1, y0, t, 0, -1, fresh);
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

        /** A side wall from (ax, ay) to (bx, by) through the slab; UV = (along, z share -1..1). */
        private void side(VertexConsumer consumer, PoseStack.Pose pose, float ax, float ay, float bx, float by, float t, float nx, float ny, int fresh) {
            float a = ax + ay;
            float b = bx + by;
            int flag = seed + 128;
            vertex(consumer, pose, ax, ay, -t, a, -1, 0, flag, 0, fresh, nx, ny, 0);
            vertex(consumer, pose, bx, by, -t, b, -1, 0, flag, 0, fresh, nx, ny, 0);
            vertex(consumer, pose, bx, by, t, b, 1, 0, flag, 0, fresh, nx, ny, 0);
            vertex(consumer, pose, ax, ay, t, a, 1, 0, flag, 0, fresh, nx, ny, 0);
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
