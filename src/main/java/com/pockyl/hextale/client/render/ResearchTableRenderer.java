package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.block.TableAnimation;
import com.pockyl.hextale.block.TableGeometry;
import com.pockyl.hextale.item.HextechInkItem;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.RuneGlyphs;

/**
 * Shows what lies on the Research Table - the open diary on its rest, parchment, ink and brush, a finished scroll - and
 * the unstable hextech hanging from the brass arm: a faceted crystal turning inside two spinning gimbal rings, wrapped in
 * a soft glow with motes circling it. While someone draws, the rings and motes speed up and the orb brightens; a fusion
 * flares in the rune's colour and lifts the new rune's hieroglyph over the desk; a crack shakes the orb and greys it.
 * Glow sprites use real alpha (not additive blending over black), so they look right with shader packs too.
 */
public class ResearchTableRenderer implements BlockEntityRenderer<ResearchTableBlockEntity> {
    private static final ResourceLocation BOOK_TEXTURE = Hextale.id("textures/entity/researchers_diary.png");
    private static final ResourceLocation HALO_TEXTURE = Hextale.id("textures/misc/orb_halo.png");
    private static final ResourceLocation CORE_TEXTURE = Hextale.id("textures/misc/orb_core.png");
    private static final ResourceLocation MOTE_TEXTURE = Hextale.id("textures/misc/mote.png");
    private static final ResourceLocation WHITE_TEXTURE = Hextale.id("textures/misc/white.png");
    private static final int ORB_COLOR = 0x5CC8F2;
    private static final int CRACK_COLOR = 0x8A7F78;
    private static final int MOTES = 6;
    private static final float FLARE_TICKS = 40;
    private static final float GLYPH_TICKS = 70;
    private static final float CRACK_TICKS = 24;

    private final BookModel book;

    public ResearchTableRenderer(BlockEntityRendererProvider.Context context) {
        this.book = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(ResearchTableBlockEntity table, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
                       int overlay) {
        Level level = table.getLevel();
        if (level == null) {
            return;
        }
        float time = level.getGameTime() + partialTick;
        Direction facing = table.facing();
        float flare = fade(time - table.flareTime, FLARE_TICKS);
        TableAnimation animation = table.animation;
        animation.update(time, table.isDrawing(), flare);

        poseStack.pushPose();
        TableModels.rotateToFacing(poseStack, facing);
        renderProps(table, poseStack, buffers, light, overlay);
        if (!table.getItem(ResearchTableBlockEntity.DIARY).isEmpty()) {
            renderDiary(table, poseStack, buffers, light, overlay, time);
        }
        renderRings(poseStack, buffers, animation, light, overlay);
        poseStack.popPose();

        Vec3 orb = TableGeometry.toWorld(BlockPos.ZERO, facing, TableGeometry.ORB);
        poseStack.pushPose();
        poseStack.translate(orb.x, orb.y, orb.z);
        renderOrb(table, poseStack, buffers, animation, time, flare);
        renderGlyph(table, poseStack, buffers, time);
        poseStack.popPose();
    }

    private static void renderProps(ResearchTableBlockEntity table, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        ItemStack paper = table.getItem(ResearchTableBlockEntity.PAPER);
        if (!paper.isEmpty()) {
            TableModels.render(poseStack, buffers, TableModels.PARCHMENT, light, overlay);
            if (paper.getCount() > 1) {
                TableModels.render(poseStack, buffers, TableModels.PARCHMENT_STACK, light, overlay);
            }
        }
        ItemStack ink = table.getItem(ResearchTableBlockEntity.INK);
        if (!ink.isEmpty()) {
            TableModels.render(poseStack, buffers, HextechInkItem.hasInk(ink) ? TableModels.INK_BOTTLE : TableModels.EMPTY_BOTTLE, light,
                    overlay);
        }
        if (!table.getItem(ResearchTableBlockEntity.BRUSH).isEmpty()) {
            TableModels.render(poseStack, buffers, ink.isEmpty() ? TableModels.BRUSH_LYING : TableModels.BRUSH_IN_BOTTLE, light, overlay);
        }
        if (table.getItem(ResearchTableBlockEntity.OUTPUT).is(ModItems.RUNE_SCROLL.get())) {
            TableModels.render(poseStack, buffers, TableModels.SCROLL, light, overlay);
        }
    }

    /** The diary lies open on the slanted rest; its pages riffle when a new rune is written into it. */
    private void renderDiary(ResearchTableBlockEntity table, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
                             float time) {
        Vec3 at = TableGeometry.DIARY;
        poseStack.pushPose();
        poseStack.translate(at.x / 16, at.y / 16, at.z / 16);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(67.5F));
        poseStack.translate(0, -0.125, 0);
        poseStack.scale(0.8F, 0.8F, 0.8F);
        float sinceFlare = time - table.flareTime;
        float flip = sinceFlare >= 0 && sinceFlare < 24 ? Mth.clamp(sinceFlare / 16.0F, 0, 1) : 0;
        book.setupAnim(time, 0.1F + flip * 0.8F, 0.9F, 1.2F);
        book.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entitySolid(BOOK_TEXTURE)), light, overlay, -1);
        poseStack.popPose();
    }

    /** Outer ring turns around the vertical axis under the claw; the inner ring tumbles on the outer ring's pivots. */
    private static void renderRings(PoseStack poseStack, MultiBufferSource buffers, TableAnimation animation, int light, int overlay) {
        Vec3 orb = TableGeometry.ORB;
        poseStack.pushPose();
        poseStack.translate(orb.x / 16, orb.y / 16, orb.z / 16);
        poseStack.mulPose(Axis.YP.rotationDegrees(animation.outerPhase));
        poseStack.pushPose();
        poseStack.translate(-0.5, -0.5, -0.5);
        TableModels.render(poseStack, buffers, TableModels.RING_OUTER, light, overlay);
        poseStack.popPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(animation.innerPhase));
        poseStack.translate(-0.5, -0.5, -0.5);
        TableModels.render(poseStack, buffers, TableModels.RING_INNER, light, overlay);
        poseStack.popPose();
    }

    private static void renderOrb(ResearchTableBlockEntity table, PoseStack poseStack, MultiBufferSource buffers, TableAnimation animation,
                                  float time, float flare) {
        boolean present = !table.getItem(ResearchTableBlockEntity.HEXTECH).isEmpty();
        float crack = fade(time - table.crackTime, CRACK_TICKS);
        float sinceFizzle = time - table.fizzleTime;
        float fizzle = sinceFizzle >= 0 && sinceFizzle < 12 ? (sinceFizzle % 4 < 2 ? 0.3F : 1) : 1;
        if (!present && flare <= 0 && crack <= 0) {
            return;
        }
        boolean drawing = table.isDrawing();
        float pulse = 0.85F + 0.15F * Mth.sin(time * (drawing ? 0.55F : 0.12F));
        float intensity = (animation.glow * pulse + flare * 1.3F) * fizzle;
        int color = crack > 0 ? RuneDrawing.mix(ORB_COLOR, CRACK_COLOR, crack) : RuneDrawing.mix(ORB_COLOR, table.flareColor, flare);
        float bob = Mth.sin(time * 0.07F) * 0.012F;
        float shake = crack * 0.025F * Mth.sin(time * 3.1F);

        poseStack.pushPose();
        poseStack.translate(shake, bob, 0);
        Quad camera = new Quad(poseStack, Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        camera.sprite(buffers, HALO_TEXTURE, 0.42F + 0.16F * intensity, color, Math.min(0.9F, 0.25F + 0.4F * intensity));
        if (present && crack < 0.6F) {
            VertexConsumer core = buffers.getBuffer(RenderType.entityTranslucentEmissive(CORE_TEXTURE));
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(time * (drawing ? 4.5F : 1.4F)));
            poseStack.mulPose(Axis.XP.rotationDegrees(35 + Mth.sin(time * 0.05F) * 12));
            poseStack.mulPose(Axis.ZP.rotationDegrees(45));
            float size = (2.6F + 0.3F * flare) / 16;
            cube(core, poseStack, size, RuneDrawing.lighten(color, 0.25F * intensity), 1);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-time * (drawing ? 3 : 0.9F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(30));
            cube(core, poseStack, (3.7F + 0.4F * intensity) / 16, color, 0.28F + 0.12F * intensity);
            poseStack.popPose();
        }
        renderMotes(poseStack, buffers, time, drawing, color, intensity);
        poseStack.popPose();
    }

    /** Sparkles on tilted orbits around the orb; more of them, faster and wider while someone draws. */
    private static void renderMotes(PoseStack poseStack, MultiBufferSource buffers, float time, boolean drawing, int color, float intensity) {
        int count = drawing ? MOTES : MOTES / 2;
        float speed = drawing ? 0.16F : 0.05F;
        for (int i = 0; i < count; i++) {
            float angle = time * speed * (1 + i * 0.13F) + i * Mth.TWO_PI / count;
            float tilt = i * 1.1F;
            float radius = (drawing ? 4.6F : 3.6F) / 16 + Mth.sin(time * 0.1F + i) * 0.01F;
            float x = Mth.cos(angle) * radius;
            float y = Mth.sin(angle) * radius * Mth.sin(tilt);
            float z = Mth.sin(angle) * radius * Mth.cos(tilt);
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            float twinkle = 0.6F + 0.4F * Mth.sin(time * 0.6F + i * 2.3F);
            new Quad(poseStack, Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation())
                    .sprite(buffers, MOTE_TEXTURE, 0.07F, RuneDrawing.lighten(color, 0.4F), Math.min(1, 0.55F * twinkle * (0.6F + intensity)));
            poseStack.popPose();
        }
    }

    /** The hieroglyph of a freshly fused rune rises over the orb in the rune's colour and fades. */
    private static void renderGlyph(ResearchTableBlockEntity table, PoseStack poseStack, MultiBufferSource buffers, float time) {
        float since = time - table.flareTime;
        if (table.flareRune < 0 || since < 0 || since > GLYPH_TICKS) {
            return;
        }
        ComplexRune rune = ComplexRunes.all().get(table.flareRune);
        float t = since / GLYPH_TICKS;
        float alpha = Math.min(1, since / 8) * (t > 0.6F ? 1 - (t - 0.6F) / 0.4F : 1);
        float size = 0.42F + 0.08F * Math.min(1, since / 10);
        poseStack.pushPose();
        poseStack.translate(0, 0.32F + t * 0.18F, 0);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
        Matrix4f pose = poseStack.last().pose();
        int color = rune.color();
        for (float[] stroke : RuneGlyphs.glyph(rune)) {
            for (int i = 0; i + 3 < stroke.length; i += 2) {
                float x0 = (stroke[i] - 0.5F) * size;
                float y0 = (0.5F - stroke[i + 1]) * size;
                float x1 = (stroke[i + 2] - 0.5F) * size;
                float y1 = (0.5F - stroke[i + 3]) * size;
                segment(consumer, pose, poseStack, x0, y0, x1, y1, size * 0.11F, color, alpha * 0.25F);
                segment(consumer, pose, poseStack, x0, y0, x1, y1, size * 0.045F, RuneDrawing.lighten(color, 0.5F), alpha);
            }
        }
        poseStack.popPose();
    }

    /** A line segment as a quad of the given width, with square caps that overlap the next segment. */
    private static void segment(VertexConsumer consumer, Matrix4f pose, PoseStack poseStack, float x0, float y0, float x1, float y1,
                                float width, int color, float alpha) {
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
        Quad.vertex(consumer, pose, poseStack, x0 + nx, y0 + ny, 0, 0, color, alpha);
        Quad.vertex(consumer, pose, poseStack, x0 - nx, y0 - ny, 0, 1, color, alpha);
        Quad.vertex(consumer, pose, poseStack, x1 - nx, y1 - ny, 1, 1, color, alpha);
        Quad.vertex(consumer, pose, poseStack, x1 + nx, y1 + ny, 1, 0, color, alpha);
        Quad.vertex(consumer, pose, poseStack, x1 + nx, y1 + ny, 1, 0, color, alpha);
        Quad.vertex(consumer, pose, poseStack, x1 - nx, y1 - ny, 1, 1, color, alpha);
        Quad.vertex(consumer, pose, poseStack, x0 - nx, y0 - ny, 0, 1, color, alpha);
        Quad.vertex(consumer, pose, poseStack, x0 + nx, y0 + ny, 0, 0, color, alpha);
    }

    /** A cube of edge {@code size} (blocks) centred on the pose origin, each face showing the whole texture. */
    private static void cube(VertexConsumer consumer, PoseStack poseStack, float size, int color, float alpha) {
        Matrix4f pose = poseStack.last().pose();
        float h = size / 2;
        float[][] faces = {
                {-h, -h, h, h, -h, h, h, h, h, -h, h, h, 0, 0, 1},
                {h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, 0, 0, -1},
                {h, -h, h, h, -h, -h, h, h, -h, h, h, h, 1, 0, 0},
                {-h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -1, 0, 0},
                {-h, h, h, h, h, h, h, h, -h, -h, h, -h, 0, 1, 0},
                {-h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, 0, -1, 0},
        };
        float[][] uvs = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        int a = (int) (Mth.clamp(alpha, 0, 1) * 255);
        for (float[] f : faces) {
            for (int v = 0; v < 4; v++) {
                consumer.addVertex(pose, f[v * 3], f[v * 3 + 1], f[v * 3 + 2]).setColor(r, g, b, a).setUv(uvs[v][0], uvs[v][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), f[12], f[13], f[14]);
            }
        }
    }

    /** 0 when {@code since} is outside [0, duration), else falling linearly from 1 to 0. */
    private static float fade(float since, float duration) {
        return since >= 0 && since < duration ? 1 - since / duration : 0;
    }

    @Override
    public boolean shouldRenderOffScreen(ResearchTableBlockEntity table) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ResearchTableBlockEntity table) {
        return TableGeometry.renderBounds(table.getBlockPos(), table.facing());
    }

    /** Camera-facing sprites at the current pose origin. */
    private record Quad(PoseStack poseStack, org.joml.Quaternionf camera) {
        void sprite(MultiBufferSource buffers, ResourceLocation texture, float size, int color, float alpha) {
            poseStack.pushPose();
            poseStack.mulPose(camera);
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(texture));
            Matrix4f pose = poseStack.last().pose();
            float h = size / 2;
            // Both windings, so the sprite shows whichever way the camera's frame turns it.
            vertex(consumer, pose, poseStack, -h, -h, 0, 1, color, alpha);
            vertex(consumer, pose, poseStack, h, -h, 1, 1, color, alpha);
            vertex(consumer, pose, poseStack, h, h, 1, 0, color, alpha);
            vertex(consumer, pose, poseStack, -h, h, 0, 0, color, alpha);
            vertex(consumer, pose, poseStack, -h, h, 0, 0, color, alpha);
            vertex(consumer, pose, poseStack, h, h, 1, 0, color, alpha);
            vertex(consumer, pose, poseStack, h, -h, 1, 1, color, alpha);
            vertex(consumer, pose, poseStack, -h, -h, 0, 1, color, alpha);
            poseStack.popPose();
        }

        static void vertex(VertexConsumer consumer, Matrix4f pose, PoseStack poseStack, float x, float y, float u, float v, int color,
                           float alpha) {
            consumer.addVertex(pose, x, y, 0).setColor(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, (int) (Mth.clamp(alpha, 0, 1) * 255))
                    .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0, 0, 1);
        }
    }
}
