package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.block.TableAnimation;
import com.pockyl.hextale.block.TableGeometry;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.RuneGlyphs;

/**
 * Shows what lies on the Research Table - the open diary on its rest, parchment, ink and brush, a finished scroll - and
 * the unstable hextech hanging from the brass arm: a faceted crystal turning inside two spinning gimbal rings, wrapped in
 * a soft glow with motes circling it. While someone draws, the rings and motes speed up and the orb brightens; a fusion
 * flares in the rune's colour and lifts the new rune's hieroglyph over the desk; a lost sheet shakes the orb and greys it for a moment.
 * Glow sprites use real alpha (not additive blending over black), so they look right with shader packs too.
 *
 * <p>The table is read into a {@link State} on extraction; {@link #submit} only turns that state into geometry.
 */
public class ResearchTableRenderer implements BlockEntityRenderer<ResearchTableBlockEntity, ResearchTableRenderer.State> {
    private static final Identifier BOOK_TEXTURE = Hextale.id("textures/entity/researchers_diary.png");
    private static final Identifier HALO_TEXTURE = Hextale.id("textures/misc/orb_halo.png");
    private static final Identifier CORE_TEXTURE = Hextale.id("textures/misc/orb_core.png");
    private static final Identifier MOTE_TEXTURE = Hextale.id("textures/misc/mote.png");
    private static final Identifier WHITE_TEXTURE = Hextale.id("textures/misc/white.png");
    private static final int ORB_COLOR = 0x5CC8F2;
    private static final int CRACK_COLOR = 0x8A7F78;
    private static final int MOTES = 6;
    private static final float FLARE_TICKS = 40;
    private static final float GLYPH_TICKS = 70;
    private static final float CRACK_TICKS = 24;
    private static final float[][] CUBE_FACES = {
            {-1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1, 0, 0, 1},
            {1, -1, -1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 0, 0, -1},
            {1, -1, 1, 1, -1, -1, 1, 1, -1, 1, 1, 1, 1, 0, 0},
            {-1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1, -1, 0, 0},
            {-1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, -1, 0, 1, 0},
            {-1, -1, -1, 1, -1, -1, 1, -1, 1, -1, -1, 1, 0, -1, 0},
    };
    private static final float[][] CUBE_UVS = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};

    private final BookModel book;

    /** What the renderer needs of one table in one frame. */
    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float time;
        float flare;
        float sinceFlare;
        float sinceFizzle;
        int flareColor;
        int flareRune = -1;
        boolean diary;
        boolean scroll;
        boolean orb;
        boolean drawing;
        float outerPhase;
        float innerPhase;
        float glow;
    }

    public ResearchTableRenderer(BlockEntityRendererProvider.Context context) {
        this.book = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ResearchTableBlockEntity table, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(table, state, partialTick, cameraPosition, breakProgress);
        Level level = table.getLevel();
        float time = level == null ? 0 : level.getGameTime() + partialTick;
        state.facing = table.facing();
        state.time = time;
        state.sinceFlare = time - table.flareTime;
        state.sinceFizzle = time - table.fizzleTime;
        state.flare = fade(state.sinceFlare, FLARE_TICKS);
        state.flareColor = table.flareColor;
        state.flareRune = table.flareRune;
        state.diary = !table.getItem(ResearchTableBlockEntity.DIARY).isEmpty();
        state.scroll = table.getItem(ResearchTableBlockEntity.OUTPUT).is(ModItems.RUNE_SCROLL.get());
        state.orb = !table.getItem(ResearchTableBlockEntity.HEXTECH).isEmpty();
        state.drawing = table.isDrawing();
        TableAnimation animation = table.animation;
        animation.update(time, state.drawing, state.flare);
        state.outerPhase = animation.outerPhase;
        state.innerPhase = animation.innerPhase;
        state.glow = animation.glow;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        TableModels.rotateToFacing(poseStack, state.facing);
        submitProps(state, poseStack, collector, light);
        if (state.diary) {
            submitDiary(state, poseStack, collector, light);
        }
        submitRings(state, poseStack, collector, light);
        poseStack.popPose();

        Vec3 orb = TableGeometry.toWorld(BlockPos.ZERO, state.facing, TableGeometry.ORB);
        poseStack.pushPose();
        poseStack.translate(orb.x, orb.y, orb.z);
        submitOrb(state, poseStack, collector, camera.orientation);
        submitGlyph(state, poseStack, collector, camera.orientation);
        poseStack.popPose();
    }

    /** The parchment, the inkwell and the brush always lie on the desk; a finished scroll waits beside them. */
    private static void submitProps(State state, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        TableModels.submit(poseStack, collector, TableModels.PARCHMENT, light);
        TableModels.submit(poseStack, collector, TableModels.PARCHMENT_STACK, light);
        TableModels.submit(poseStack, collector, TableModels.INK_BOTTLE, light);
        TableModels.submit(poseStack, collector, TableModels.BRUSH_IN_BOTTLE, light);
        if (state.scroll) {
            TableModels.submit(poseStack, collector, TableModels.SCROLL, light);
        }
    }

    /** The diary lies open on the slanted rest; its pages riffle when a new rune is written into it. */
    private void submitDiary(State state, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        Vec3 at = TableGeometry.DIARY;
        poseStack.pushPose();
        poseStack.translate(at.x / 16, at.y / 16, at.z / 16);
        poseStack.rotate(Axis.YP.rotationDegrees(90));
        poseStack.rotate(Axis.ZP.rotationDegrees(67.5F));
        poseStack.translate(0, -0.125, 0);
        poseStack.scale(0.8F, 0.8F, 0.8F);
        float flip = state.sinceFlare >= 0 && state.sinceFlare < 24 ? Mth.clamp(state.sinceFlare / 16.0F, 0, 1) : 0;
        BookModel.State pages = BookModel.State.forAnimation(state.time, 0.1F + flip * 0.8F, 0.9F, 1.2F);
        collector.submitModel(book, pages, poseStack, BOOK_TEXTURE, light, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /** Outer ring turns around the vertical axis under the claw; the inner ring tumbles on the outer ring's pivots. */
    private static void submitRings(State state, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        Vec3 orb = TableGeometry.ORB;
        poseStack.pushPose();
        poseStack.translate(orb.x / 16, orb.y / 16, orb.z / 16);
        poseStack.rotate(Axis.YP.rotationDegrees(state.outerPhase));
        poseStack.pushPose();
        poseStack.translate(-0.5, -0.5, -0.5);
        TableModels.submit(poseStack, collector, TableModels.RING_OUTER, light);
        poseStack.popPose();
        poseStack.rotate(Axis.XP.rotationDegrees(state.innerPhase));
        poseStack.translate(-0.5, -0.5, -0.5);
        TableModels.submit(poseStack, collector, TableModels.RING_INNER, light);
        poseStack.popPose();
    }

    private static void submitOrb(State state, PoseStack poseStack, SubmitNodeCollector collector, Quaternionfc camera) {
        float crack = 0.5F * fade(state.sinceFizzle, CRACK_TICKS);
        float fizzle = state.sinceFizzle >= 0 && state.sinceFizzle < 12 ? (state.sinceFizzle % 4 < 2 ? 0.3F : 1) : 1;
        if (!state.orb && state.flare <= 0 && crack <= 0) {
            return;
        }
        float time = state.time;
        boolean drawing = state.drawing;
        float pulse = 0.85F + 0.15F * Mth.sin(time * (drawing ? 0.55F : 0.12F));
        float intensity = (state.glow * pulse + state.flare * 1.3F) * fizzle;
        int color = crack > 0 ? RuneDrawing.mix(ORB_COLOR, CRACK_COLOR, crack) : RuneDrawing.mix(ORB_COLOR, state.flareColor, state.flare);
        float bob = Mth.sin(time * 0.07F) * 0.012F;
        float shake = crack * 0.025F * Mth.sin(time * 3.1F);

        poseStack.pushPose();
        poseStack.translate(shake, bob, 0);
        sprite(poseStack, collector, camera, HALO_TEXTURE, 0.42F + 0.16F * intensity, color, Math.min(0.9F, 0.25F + 0.4F * intensity));
        if (state.orb && crack < 0.6F) {
            poseStack.pushPose();
            poseStack.rotate(Axis.YP.rotationDegrees(time * (drawing ? 4.5F : 1.4F)));
            poseStack.rotate(Axis.XP.rotationDegrees(35 + Mth.sin(time * 0.05F) * 12));
            poseStack.rotate(Axis.ZP.rotationDegrees(45));
            cube(poseStack, collector, (2.6F + 0.3F * state.flare) / 16, RuneDrawing.lighten(color, 0.25F * intensity), 1);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.rotate(Axis.YP.rotationDegrees(-time * (drawing ? 3 : 0.9F)));
            poseStack.rotate(Axis.ZP.rotationDegrees(30));
            cube(poseStack, collector, (3.7F + 0.4F * intensity) / 16, color, 0.28F + 0.12F * intensity);
            poseStack.popPose();
        }
        submitMotes(poseStack, collector, camera, time, drawing, color, intensity);
        poseStack.popPose();
    }

    /** Sparkles on tilted orbits around the orb; more of them, faster and wider while someone draws. */
    private static void submitMotes(PoseStack poseStack, SubmitNodeCollector collector, Quaternionfc camera, float time, boolean drawing,
                                    int color, float intensity) {
        int count = drawing ? MOTES : MOTES / 2;
        float speed = drawing ? 0.16F : 0.05F;
        for (int i = 0; i < count; i++) {
            float angle = time * speed * (1 + i * 0.13F) + i * Mth.TWO_PI / count;
            float tilt = i * 1.1F;
            float radius = (drawing ? 4.6F : 3.6F) / 16 + Mth.sin(time * 0.1F + i) * 0.01F;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(angle) * radius, Mth.sin(angle) * radius * Mth.sin(tilt), Mth.sin(angle) * radius * Mth.cos(tilt));
            float twinkle = 0.6F + 0.4F * Mth.sin(time * 0.6F + i * 2.3F);
            sprite(poseStack, collector, camera, MOTE_TEXTURE, 0.07F, RuneDrawing.lighten(color, 0.4F),
                    Math.min(1, 0.55F * twinkle * (0.6F + intensity)));
            poseStack.popPose();
        }
    }

    /** The hieroglyph of a freshly fused rune rises over the orb in the rune's colour and fades. */
    private static void submitGlyph(State state, PoseStack poseStack, SubmitNodeCollector collector, Quaternionfc camera) {
        float since = state.sinceFlare;
        if (state.flareRune < 0 || since < 0 || since > GLYPH_TICKS) {
            return;
        }
        ComplexRune rune = ComplexRunes.all().get(state.flareRune);
        float t = since / GLYPH_TICKS;
        float alpha = Math.min(1, since / 8) * (t > 0.6F ? 1 - (t - 0.6F) / 0.4F : 1);
        float size = 0.42F + 0.08F * Math.min(1, since / 10);
        int color = rune.color();
        poseStack.pushPose();
        poseStack.translate(0, 0.32F + t * 0.18F, 0);
        poseStack.rotate(camera);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(WHITE_TEXTURE), (pose, consumer) -> {
            for (float[] stroke : RuneGlyphs.glyph(rune)) {
                for (int i = 0; i + 3 < stroke.length; i += 2) {
                    float x0 = (stroke[i] - 0.5F) * size;
                    float y0 = (0.5F - stroke[i + 1]) * size;
                    float x1 = (stroke[i + 2] - 0.5F) * size;
                    float y1 = (0.5F - stroke[i + 3]) * size;
                    segment(consumer, pose, x0, y0, x1, y1, size * 0.11F, color, alpha * 0.25F);
                    segment(consumer, pose, x0, y0, x1, y1, size * 0.045F, RuneDrawing.lighten(color, 0.5F), alpha);
                }
            }
        });
        poseStack.popPose();
    }

    /** A line segment as a quad of the given width, with square caps that overlap the next segment; both windings. */
    private static void segment(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float width, int color,
                                float alpha) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4F) {
            return;
        }
        float h = width / 2;
        float nx = -dy / length * h;
        float ny = dx / length * h;
        float ex = dx / length * h;
        float ey = dy / length * h;
        x0 -= ex;
        y0 -= ey;
        x1 += ex;
        y1 += ey;
        vertex(consumer, pose, x0 + nx, y0 + ny, 0, 0, color, alpha);
        vertex(consumer, pose, x0 - nx, y0 - ny, 0, 1, color, alpha);
        vertex(consumer, pose, x1 - nx, y1 - ny, 1, 1, color, alpha);
        vertex(consumer, pose, x1 + nx, y1 + ny, 1, 0, color, alpha);
        vertex(consumer, pose, x1 + nx, y1 + ny, 1, 0, color, alpha);
        vertex(consumer, pose, x1 - nx, y1 - ny, 1, 1, color, alpha);
        vertex(consumer, pose, x0 - nx, y0 - ny, 0, 1, color, alpha);
        vertex(consumer, pose, x0 + nx, y0 + ny, 0, 0, color, alpha);
    }

    /** A cube of edge {@code size} (blocks) centred on the pose origin, each face showing the whole core texture. */
    private static void cube(PoseStack poseStack, SubmitNodeCollector collector, float size, int color, float alpha) {
        float h = size / 2;
        int argb = RuneDrawing.argb(color, alpha);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(CORE_TEXTURE), (pose, consumer) -> {
            for (float[] f : CUBE_FACES) {
                for (int v = 0; v < 4; v++) {
                    consumer.addVertex(pose, f[v * 3] * h, f[v * 3 + 1] * h, f[v * 3 + 2] * h).setColor(argb)
                            .setUv(CUBE_UVS[v][0], CUBE_UVS[v][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
                            .setNormal(pose, f[12], f[13], f[14]);
                }
            }
        });
    }

    /** A camera-facing sprite at the current pose origin, drawn with both windings. */
    private static void sprite(PoseStack poseStack, SubmitNodeCollector collector, Quaternionfc camera, Identifier texture, float size, int color,
                               float alpha) {
        poseStack.pushPose();
        poseStack.rotate(camera);
        float h = size / 2;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(texture), (pose, consumer) -> {
            vertex(consumer, pose, -h, -h, 0, 1, color, alpha);
            vertex(consumer, pose, h, -h, 1, 1, color, alpha);
            vertex(consumer, pose, h, h, 1, 0, color, alpha);
            vertex(consumer, pose, -h, h, 0, 0, color, alpha);
            vertex(consumer, pose, -h, h, 0, 0, color, alpha);
            vertex(consumer, pose, h, h, 1, 0, color, alpha);
            vertex(consumer, pose, h, -h, 1, 1, color, alpha);
            vertex(consumer, pose, -h, -h, 0, 1, color, alpha);
        });
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v, int color, float alpha) {
        consumer.addVertex(pose, x, y, 0).setColor(RuneDrawing.argb(color, alpha)).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
    }

    /** 0 when {@code since} is outside [0, duration), else falling linearly from 1 to 0. */
    private static float fade(float since, float duration) {
        return since >= 0 && since < duration ? 1 - since / duration : 0;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ResearchTableBlockEntity table) {
        return TableGeometry.renderBounds(table.getBlockPos(), table.facing());
    }
}
