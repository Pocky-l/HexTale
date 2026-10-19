package com.pockyl.hextale.client.screen.book;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.hextale.client.render.HexFx;
import com.pockyl.hextale.client.render.TableModels;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The ritual circle at work, as a turning 3D scene for the research book: the Hextech Altar in the middle with raw
 * hextech floating in its gimbal rings, four pedestals around it holding amethyst and brass, two rune steles and two
 * Essence Jars further out. The ritual plays in a loop - the rings wake up, the pedestals' items rise one after another
 * and fly into the altar, the jars pour their essences in as streams of motes, a flash, and the hextech gemstone turns
 * over the altar. The part a step is about (the altar, the pedestals or the steles) stands in a ring of light.
 */
public final class CircleDiorama {
    /** One loop of the ritual, in ticks: warm-up, four items, the essences, the finale, a moment to admire the result. */
    private static final float LOOP = 270;
    private static final float WARMUP = 30;
    private static final float PER_ITEM = 28;
    private static final float ESSENCES = 60;
    private static final float FINALE = 24;
    private static final float PEDESTAL_R = 2.1F;
    private static final float STELE_R = 3.1F;
    private static final float JAR_R = 3.0F;
    private static final float CENTRE_Y = 1.35F;
    private static final float TILT = 30;
    private static final List<Essence> DRAWN = List.of(Essence.CRYSTAL, Essence.ORDER, Essence.ENERGY);

    private CircleDiorama() {
    }

    public static void register(RegisterPictureInPictureRenderersEvent event) {
        event.register(State.class, Renderer::new);
    }

    /** Submits the scene into a screen box; {@code scale} = GUI pixels per block, {@code time} in ticks. */
    public static void draw(GuiGraphicsExtractor graphics, Quest.Scene focus, float time, int x0, int y0, int x1, int y1, float scale) {
        graphics.submitPictureInPictureRenderState(new State(focus, time, x0, y0, x1, y1, scale, graphics.peekScissorStack()));
    }

    record State(Quest.Scene focus, float time, int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea,
                 @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
        State(Quest.Scene focus, float time, int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea) {
            this(focus, time, x0, y0, x1, y1, scale, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
        }
    }

    /** Angle of pedestal {@code i} around the altar (between the steles and the jars). */
    private static float pedestalAngle(int i) {
        return Mth.PI / 4 + i * Mth.HALF_PI;
    }

    static final class Renderer extends PictureInPictureRenderer<State> {
        private final ItemStackRenderState altar = new ItemStackRenderState();
        private final ItemStackRenderState pedestal = new ItemStackRenderState();
        private final ItemStackRenderState stele = new ItemStackRenderState();
        private final ItemStackRenderState[] jars = {new ItemStackRenderState(), new ItemStackRenderState()};
        private final ItemStackRenderState[] offerings = {new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState(),
                new ItemStackRenderState()};
        private final ItemStackRenderState centre = new ItemStackRenderState();

        @Override
        public Class<State> getRenderStateClass() {
            return State.class;
        }

        @Override
        protected void renderToTexture(State state, PoseStack poseStack, SubmitNodeCollector collector) {
            Minecraft minecraft = Minecraft.getInstance();
            ItemModelResolver resolver = minecraft.getItemModelResolver();
            float time = state.time();
            float t = time % LOOP;
            float itemsEnd = WARMUP + 4 * PER_ITEM;
            float essencesEnd = itemsEnd + ESSENCES;
            float finaleEnd = essencesEnd + FINALE;
            boolean made = t >= essencesEnd + FINALE / 2;

            resolver.updateForTopItem(altar, new ItemStack(ModItems.HEXTECH_ALTAR.get()), ItemDisplayContext.NONE, minecraft.level, null, 0);
            resolver.updateForTopItem(pedestal, new ItemStack(ModItems.PEDESTAL.get()), ItemDisplayContext.NONE, minecraft.level, null, 1);
            resolver.updateForTopItem(stele, new ItemStack(ModItems.RUNE_STELE.get()), ItemDisplayContext.NONE, minecraft.level, null, 2);
            for (int j = 0; j < 2; j++) {
                ItemStack jar = new ItemStack(ModItems.ESSENCE_TANK.get());
                jar.set(ModDataComponents.ESSENCE.get(), EssenceMix.pure(DRAWN.get(j), 2400));
                resolver.updateForTopItem(jars[j], jar, ItemDisplayContext.NONE, minecraft.level, null, 3 + j);
            }
            ItemStack[] items = {new ItemStack(Items.AMETHYST_SHARD), new ItemStack(ModItems.BRASS_NUGGET.get()),
                    new ItemStack(Items.AMETHYST_SHARD), new ItemStack(ModItems.BRASS_NUGGET.get())};
            for (int i = 0; i < 4; i++) {
                resolver.updateForTopItem(offerings[i], items[i], ItemDisplayContext.GROUND, minecraft.level, null, 10 + i);
            }
            resolver.updateForTopItem(centre, new ItemStack(made ? ModItems.HEXTECH_GEMSTONE.get() : ModItems.UNSTABLE_HEXTECH.get()),
                    ItemDisplayContext.GROUND, minecraft.level, null, 20);
            minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);

            float yaw = time * 0.35F + 20;
            poseStack.scale(1, -1, -1);
            poseStack.rotate(Axis.XP.rotationDegrees(TILT));
            poseStack.rotate(Axis.YP.rotationDegrees(yaw));
            Quaternionf camera = new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD).rotateX(-TILT * Mth.DEG_TO_RAD);
            Vector3f toCamera = camera.transform(new Vector3f(0, 0, 1));
            poseStack.translate(0, -0.8F, 0);
            int light = LightCoordsUtil.FULL_BRIGHT;
            Quest.Scene focus = state.focus();

            // The focus: a ring of light on the ground under what the step is about.
            float pulse = 0.5F + 0.5F * Mth.sin(time * 0.12F);
            if (focus == Quest.Scene.ALTAR) {
                groundRing(poseStack, collector, 0, 0, 1.1F, time, pulse);
            } else if (focus == Quest.Scene.PEDESTAL) {
                for (int i = 0; i < 4; i++) {
                    groundRing(poseStack, collector, Mth.cos(pedestalAngle(i)) * PEDESTAL_R, Mth.sin(pedestalAngle(i)) * PEDESTAL_R, 0.7F, time, pulse);
                }
            } else {
                groundRing(poseStack, collector, STELE_R, 0, 0.7F, time, pulse);
                groundRing(poseStack, collector, -STELE_R, 0, 0.7F, time, pulse);
            }

            // The blocks of the circle.
            block(poseStack, collector, altar, 0, 0, light);
            for (int i = 0; i < 4; i++) {
                block(poseStack, collector, pedestal, Mth.cos(pedestalAngle(i)) * PEDESTAL_R, Mth.sin(pedestalAngle(i)) * PEDESTAL_R, light);
            }
            block(poseStack, collector, stele, STELE_R, 0, light);
            block(poseStack, collector, stele, -STELE_R, 0, light);
            block(poseStack, collector, jars[0], 0, JAR_R, light);
            block(poseStack, collector, jars[1], 0, -JAR_R, light);

            // The pedestals' items: waiting over their pedestals, then one after another rising and flying into the altar.
            for (int i = 0; i < 4; i++) {
                float start = WARMUP + i * PER_ITEM;
                float k = Mth.clamp((t - start) / PER_ITEM, 0, 1);
                if (t >= start + PER_ITEM) {
                    continue;
                }
                float px = Mth.cos(pedestalAngle(i)) * PEDESTAL_R;
                float pz = Mth.sin(pedestalAngle(i)) * PEDESTAL_R;
                float ease = k * k * (3 - 2 * k);
                float x = Mth.lerp(ease, px, 0);
                float z = Mth.lerp(ease, pz, 0);
                float y = Mth.lerp(ease, 1.15F, CENTRE_Y) + Mth.sin(k * Mth.PI) * 0.9F + Mth.sin(time * 0.1F + i) * 0.03F;
                float size = 0.55F * (1 - 0.7F * ease * ease);
                poseStack.pushPose();
                poseStack.translate(x, y, z);
                if (k > 0 && k < 1) {
                    HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.5F, HexFx.HEXTECH, 0.5F * Mth.sin(k * Mth.PI));
                }
                poseStack.rotate(Axis.YP.rotationDegrees(time * 3 + i * 90));
                poseStack.scale(size, size, size);
                poseStack.translate(0, -0.15F, 0);
                offerings[i].submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }

            // The runes on the steles, always facing the reader, and their threads into the altar while the ritual runs.
            ComplexRune rune = ComplexRunes.byKey("fire_earth");
            boolean running = t >= WARMUP && t < essencesEnd;
            for (int side = -1; side <= 1; side += 2) {
                float sx = STELE_R * side;
                poseStack.pushPose();
                poseStack.translate(sx, 1.45F + Mth.sin(time * 0.06F + side) * 0.04F, 0);
                HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.6F, rune.color(), 0.3F + (focus == Quest.Scene.STELE ? 0.25F * pulse : 0));
                poseStack.rotate(camera);
                poseStack.translate(0, 0, 0.03F);
                HexFx.glyph(poseStack, collector, rune, 0.4F, rune.color(), 0.9F);
                poseStack.popPose();
                if (running) {
                    poseStack.pushPose();
                    poseStack.translate(sx, 1.45F, 0);
                    HexFx.beam(poseStack, collector, -sx, CENTRE_Y - 1.45F, 0, 0.05F, rune.color(), 0.25F + 0.2F * pulse, toCamera);
                    poseStack.popPose();
                }
            }

            // The essences: streams of motes from the jars' lids into the altar.
            if (t >= itemsEnd && t < essencesEnd) {
                float k = (t - itemsEnd) / ESSENCES;
                for (int j = 0; j < 2; j++) {
                    int color = DRAWN.get(j).color();
                    float jz = (j == 0 ? 1 : -1) * JAR_R;
                    poseStack.pushPose();
                    poseStack.translate(0, 1.05F, jz);
                    HexFx.beam(poseStack, collector, 0, CENTRE_Y - 1.05F, -jz, 0.08F, color, 0.35F * Mth.sin(k * Mth.PI), toCamera);
                    poseStack.popPose();
                    for (int m = 0; m < 5; m++) {
                        float run = (time * 0.04F + m * 0.2F + j * 0.1F) % 1;
                        poseStack.pushPose();
                        poseStack.translate(0, Mth.lerp(run, 1.05F, CENTRE_Y) + Mth.sin(run * Mth.PI) * 0.6F, Mth.lerp(run, jz, 0));
                        HexFx.sprite(poseStack, collector, camera, HexFx.MOTE, 0.14F, color, 0.9F);
                        poseStack.popPose();
                    }
                }
            }

            // The altar's gimbal rings - faster while the ritual runs - and what floats inside them.
            float spin = t < WARMUP ? t / WARMUP : t < essencesEnd ? 1 : Math.max(0.2F, 1 - (t - essencesEnd) / FINALE);
            float flash = t >= essencesEnd && t < finaleEnd ? Mth.sin((t - essencesEnd) / FINALE * Mth.PI) : 0;
            poseStack.pushPose();
            poseStack.translate(0, CENTRE_Y + Mth.sin(time * 0.08F) * 0.04F, 0);
            HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.9F + 1.6F * flash, made ? 0x9FE6FF : HexFx.HEXTECH, 0.3F + 0.2F * spin + 0.5F * flash);
            poseStack.pushPose();
            poseStack.rotate(Axis.YP.rotationDegrees(time * (1.5F + 5 * spin)));
            poseStack.pushPose();
            poseStack.translate(-0.5, -0.5, -0.5);
            TableModels.submit(poseStack, collector, TableModels.ALTAR_RING_OUTER, light);
            poseStack.popPose();
            poseStack.rotate(Axis.XP.rotationDegrees(time * (2 + 6 * spin)));
            poseStack.translate(-0.5, -0.5, -0.5);
            TableModels.submit(poseStack, collector, TableModels.ALTAR_RING_INNER, light);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.rotate(Axis.YP.rotationDegrees(-time * 2.4F));
            poseStack.scale(0.8F, 0.8F, 0.8F);
            poseStack.translate(0, -0.15F, 0);
            centre.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
            poseStack.popPose();
        }

        private static void block(PoseStack poseStack, SubmitNodeCollector collector, ItemStackRenderState block, float x, float z, int light) {
            poseStack.pushPose();
            poseStack.translate(x, 0.5F, z);
            block.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        /** A turning hex sigil laid flat on the ground around a block, breathing with {@code pulse}. */
        private static void groundRing(PoseStack poseStack, SubmitNodeCollector collector, float x, float z, float radius, float time, float pulse) {
            poseStack.pushPose();
            poseStack.translate(x, 0.02F, z);
            poseStack.rotate(Axis.XP.rotationDegrees(90));
            poseStack.rotate(Axis.ZP.rotationDegrees(time * 0.8F));
            HexFx.sigilRing(poseStack, collector, radius, 12, 0x9FE6FF, 0.35F + 0.35F * pulse);
            poseStack.popPose();
        }

        @Override
        protected float getTranslateY(int height, int guiScale) {
            return height / 2.0F;
        }

        @Override
        protected String getTextureLabel() {
            return "hextale circle diorama";
        }
    }
}
