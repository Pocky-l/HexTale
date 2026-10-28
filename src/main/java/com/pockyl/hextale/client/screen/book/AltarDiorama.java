package com.pockyl.hextale.client.screen.book;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.hextale.block.HextechAltarBlockEntity;
import com.pockyl.hextale.block.RuneSteleBlockEntity;
import com.pockyl.hextale.client.render.HexFx;
import com.pockyl.hextale.client.render.TableModels;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;

import org.jspecify.annotations.Nullable;

/**
 * A small turning 3D scene for the research book: the Hextech Altar with its gimbal rings and a pickaxe floating over
 * it, a rune stele beside it with the chosen rune's glyph, and a thread of light carrying the rune into the pickaxe -
 * how a rune is burned in, at a glance.
 */
public final class AltarDiorama {
    /** Blocks between the altar's and the stele's centres. */
    private static final float STELE_X = 1.7F;
    private static final float TILT = 22;

    private AltarDiorama() {
    }

    public static void register(RegisterPictureInPictureRenderersEvent event) {
        event.register(State.class, Renderer::new);
    }

    /** Submits the scene into a screen box; {@code scale} = GUI pixels per block. */
    public static void draw(GuiGraphicsExtractor graphics, ItemStack pickaxe, @Nullable ComplexRune rune, float time, int x0, int y0, int x1,
                            int y1, float scale) {
        graphics.submitPictureInPictureRenderState(new State(pickaxe, rune, time, x0, y0, x1, y1, scale, graphics.peekScissorStack()));
    }

    record State(ItemStack pickaxe, @Nullable ComplexRune rune, float time, int x0, int y0, int x1, int y1, float scale,
                 @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
        State(ItemStack pickaxe, @Nullable ComplexRune rune, float time, int x0, int y0, int x1, int y1, float scale,
              @Nullable ScreenRectangle scissorArea) {
            this(pickaxe, rune, time, x0, y0, x1, y1, scale, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
        }
    }

    static final class Renderer extends PictureInPictureRenderer<State> {
        Renderer(net.minecraft.client.renderer.MultiBufferSource.BufferSource portBuffers) {
            super(portBuffers);
        }

        private final ItemStackRenderState altar = new ItemStackRenderState();
        private final ItemStackRenderState stele = new ItemStackRenderState();
        private final ItemStackRenderState pickaxe = new ItemStackRenderState();

        @Override
        public Class<State> getRenderStateClass() {
            return State.class;
        }

        @Override
        protected void renderToTexture(State state, PoseStack poseStack) {
            net.minecraft.client.renderer.feature.FeatureRenderDispatcher portFeatures =
                    net.minecraft.client.Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
            renderToTexture(state, poseStack, portFeatures.getSubmitNodeStorage());
            portFeatures.renderAllFeatures();
        }

        private void renderToTexture(State state, PoseStack poseStack, SubmitNodeCollector collector) {
            Minecraft minecraft = Minecraft.getInstance();
            var resolver = minecraft.getItemModelResolver();
            resolver.updateForTopItem(altar, new ItemStack(ModItems.HEXTECH_ALTAR.get()), ItemDisplayContext.NONE, minecraft.level, null, 0);
            resolver.updateForTopItem(stele, new ItemStack(ModItems.RUNE_STELE.get()), ItemDisplayContext.NONE, minecraft.level, null, 1);
            resolver.updateForTopItem(pickaxe, state.pickaxe(), ItemDisplayContext.GROUND, minecraft.level, null, 2);
            minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

            float time = state.time();
            float yaw = time * 0.9F;
            // GUI y grows downwards: flip it so the scene stands upright, then look at it from a little above.
            poseStack.scale(1, -1, -1);
            poseStack.mulPose(Axis.XP.rotationDegrees(TILT));
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            Quaternionf camera = new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD).rotateX(-TILT * Mth.DEG_TO_RAD);
            Vector3f toCamera = camera.transform(new Vector3f(0, 0, 1));
            // The pair turns around its middle.
            poseStack.translate(-STELE_X / 2, -0.9F, 0);

            int light = LightCoordsUtil.FULL_BRIGHT;
            poseStack.pushPose();
            poseStack.translate(0, 0.5F, 0);
            altar.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(STELE_X, 0.5F, 0);
            stele.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();

            ComplexRune rune = state.rune();
            int color = rune == null ? HexFx.HEXTECH : rune.color();
            float itemY = (float) HextechAltarBlockEntity.ITEM_Y + Mth.sin(time * 0.08F) * 0.04F;
            float glyphY = RuneSteleBlockEntity.GLYPH_Y + Mth.sin(time * 0.06F) * 0.02F;

            // The altar's gimbal and the pickaxe inside it.
            poseStack.pushPose();
            poseStack.translate(0, itemY, 0);
            HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.8F, color, 0.3F);
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.2F));
            poseStack.pushPose();
            poseStack.translate(-0.5, -0.5, -0.5);
            TableModels.submit(poseStack, collector, TableModels.ALTAR_RING_OUTER, light);
            poseStack.popPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(time * 3.1F));
            poseStack.translate(-0.5, -0.5, -0.5);
            TableModels.submit(poseStack, collector, TableModels.ALTAR_RING_INNER, light);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-time * 2.6F));
            poseStack.scale(1.1F, 1.1F, 1.1F);
            poseStack.translate(0, -0.15, 0);
            pickaxe.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
            poseStack.popPose();

            if (rune != null) {
                // The rune over its stele, always facing the reader.
                poseStack.pushPose();
                poseStack.translate(STELE_X, glyphY, 0);
                HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.7F, color, 0.35F);
                poseStack.mulPose(camera);
                poseStack.translate(0, 0, 0.03F);
                poseStack.pushPose();
                poseStack.mulPose(Axis.ZP.rotationDegrees(time * 0.6F));
                HexFx.sigilRing(poseStack, collector, 0.27F, 9, color, 0.35F);
                poseStack.popPose();
                HexFx.glyph(poseStack, collector, rune, 0.36F, color, 0.9F);
                poseStack.popPose();

                // A thread of light from the rune to the pickaxe, with a spark running along it.
                poseStack.pushPose();
                poseStack.translate(STELE_X, glyphY, 0);
                float pulse = 0.5F + 0.5F * Mth.sin(time * 0.15F);
                HexFx.beam(poseStack, collector, -STELE_X, itemY - glyphY, 0, 0.07F, color, 0.25F + 0.3F * pulse, toCamera);
                float run = (time * 0.025F) % 1;
                poseStack.translate(-STELE_X * run, (itemY - glyphY) * run, 0);
                HexFx.sprite(poseStack, collector, camera, HexFx.MOTE, 0.16F, color, 0.9F);
                poseStack.popPose();
            }
        }

        @Override
        protected float getTranslateY(int height, int guiScale) {
            return height / 2.0F;
        }

        @Override
        protected String getTextureLabel() {
            return "hextale altar diorama";
        }
    }
}
