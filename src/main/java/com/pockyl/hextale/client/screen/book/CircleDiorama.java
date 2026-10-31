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

import com.pockyl.hextale.block.HextechAltarBlockEntity;
import com.pockyl.hextale.block.RuneCircleBlockEntity;
import com.pockyl.hextale.block.RuneSteleBlockEntity;
import com.pockyl.hextale.block.PedestalBlockEntity;
import com.pockyl.hextale.client.render.HexFx;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.client.render.TableModels;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechPickaxe;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The circles at work, as turning 3D scenes for the research book. The altar's: the Ritual Altar in the middle with a
 * cut crystal floating in its gimbal rings, four pedestals around it and two Essence Jars further out; the pedestals'
 * items rise one after another and fly into the altar, the jars pour their essences in as streams of motes, a flash,
 * and a pylon turns over the altar. The steles' (they serve the Rune Circle only): the carved Rune Circle with a
 * pickaxe over it, two steles just outside it with their runes, two jars; the runes run into the pickaxe as threads of
 * light while the jars pay their essence, and it comes out glowing in the runes' colours. The part a step is about (the
 * altar, the pedestals or the steles) stands in a ring of light.
 */
public final class CircleDiorama {
    /** One loop of the ritual, in ticks: warm-up, four items, the essences, the finale, a moment to admire the result. */
    private static final float LOOP = 270;
    private static final float WARMUP = 30;
    private static final float PER_ITEM = 28;
    private static final float ESSENCES = 60;
    private static final float FINALE = 24;
    private static final float PEDESTAL_R = 2.1F;
    private static final float JAR_R = 3.0F;
    private static final float CENTRE_Y = (float) HextechAltarBlockEntity.ITEM_Y + 0.05F;
    private static final float TILT = 30;
    private static final List<Essence> DRAWN = List.of(Essence.CRYSTAL, Essence.MAGIC, Essence.SPARK);
    /** The steles of the rune scene stand just outside the carved disc, which is three blocks across. */
    private static final float CIRCLE_STELE_R = 2.4F;
    private static final float RUNES = 90;
    private static final List<String> SCENE_RUNES = List.of("fire_earth", "spark_life");
    private static final List<Essence> RUNE_ESSENCES = List.of(Essence.FIRE, Essence.SPARK);

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
        private final ItemStackRenderState circle = new ItemStackRenderState();

        @Override
        public Class<State> getRenderStateClass() {
            return State.class;
        }

        @Override
        protected void renderToTexture(State state, PoseStack poseStack, SubmitNodeCollector collector) {
            if (state.focus() == Quest.Scene.STELE) {
                runeWork(state, poseStack, collector);
                return;
            }
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
            for (int j = 0; j < 2; j++) {
                ItemStack jar = new ItemStack(ModItems.ESSENCE_TANK.get());
                jar.set(ModDataComponents.ESSENCE.get(), EssenceMix.pure(DRAWN.get(j), 2400));
                resolver.updateForTopItem(jars[j], jar, ItemDisplayContext.NONE, minecraft.level, null, 3 + j);
            }
            ItemStack[] items = {new ItemStack(Items.HOPPER), new ItemStack(ModItems.BRASS_INGOT.get()),
                    new ItemStack(Items.BOWL), new ItemStack(ModItems.BRASS_INGOT.get())};
            for (int i = 0; i < 4; i++) {
                resolver.updateForTopItem(offerings[i], items[i], ItemDisplayContext.GROUND, minecraft.level, null, 10 + i);
            }
            resolver.updateForTopItem(centre, new ItemStack(made ? ModItems.GATHERING_PYLON.get() : ModItems.HEXTECH_CRYSTAL.get()),
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
            } else {
                for (int i = 0; i < 4; i++) {
                    groundRing(poseStack, collector, Mth.cos(pedestalAngle(i)) * PEDESTAL_R, Mth.sin(pedestalAngle(i)) * PEDESTAL_R, 0.7F, time, pulse);
                }
            }

            // The blocks of the circle.
            block(poseStack, collector, altar, 0, 0, light);
            for (int i = 0; i < 4; i++) {
                block(poseStack, collector, pedestal, Mth.cos(pedestalAngle(i)) * PEDESTAL_R, Mth.sin(pedestalAngle(i)) * PEDESTAL_R, light);
            }
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
                float y = Mth.lerp(ease, (float) PedestalBlockEntity.ITEM_Y + 0.03F, CENTRE_Y) + Mth.sin(k * Mth.PI) * 0.9F + Mth.sin(time * 0.1F + i) * 0.03F;
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

        /** The steles' scene: runes burned into a pickaxe in the Rune Circle, in a loop. */
        private void runeWork(State state, PoseStack poseStack, SubmitNodeCollector collector) {
            Minecraft minecraft = Minecraft.getInstance();
            ItemModelResolver resolver = minecraft.getItemModelResolver();
            float time = state.time();
            float t = time % LOOP;
            float runesEnd = WARMUP + RUNES;
            float finaleEnd = runesEnd + FINALE;
            boolean made = t >= runesEnd + FINALE / 2;

            resolver.updateForTopItem(circle, new ItemStack(ModItems.RUNE_CIRCLE.get()), ItemDisplayContext.NONE, minecraft.level, null, 0);
            resolver.updateForTopItem(stele, new ItemStack(ModItems.RUNE_STELE.get()), ItemDisplayContext.NONE, minecraft.level, null, 1);
            for (int j = 0; j < 2; j++) {
                ItemStack jar = new ItemStack(ModItems.ESSENCE_TANK.get());
                jar.set(ModDataComponents.ESSENCE.get(), EssenceMix.pure(RUNE_ESSENCES.get(j), 2400));
                resolver.updateForTopItem(jars[j], jar, ItemDisplayContext.NONE, minecraft.level, null, 3 + j);
            }
            resolver.updateForTopItem(centre, new ItemStack(ModItems.pickaxe(HextechPickaxe.HEXTECH).get()), ItemDisplayContext.GROUND,
                    minecraft.level, null, 20);
            minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);

            float yaw = time * 0.35F + 20;
            poseStack.scale(1, -1, -1);
            poseStack.rotate(Axis.XP.rotationDegrees(TILT));
            poseStack.rotate(Axis.YP.rotationDegrees(yaw));
            Quaternionf camera = new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD).rotateX(-TILT * Mth.DEG_TO_RAD);
            Vector3f toCamera = camera.transform(new Vector3f(0, 0, 1));
            poseStack.translate(0, -0.8F, 0);
            int light = LightCoordsUtil.FULL_BRIGHT;
            float pulse = 0.5F + 0.5F * Mth.sin(time * 0.12F);
            groundRing(poseStack, collector, CIRCLE_STELE_R, 0, 0.7F, time, pulse);
            groundRing(poseStack, collector, -CIRCLE_STELE_R, 0, 0.7F, time, pulse);

            block(poseStack, collector, circle, 0, 0, light);
            block(poseStack, collector, stele, CIRCLE_STELE_R, 0, light);
            block(poseStack, collector, stele, -CIRCLE_STELE_R, 0, light);
            block(poseStack, collector, jars[0], 0, JAR_R, light);
            block(poseStack, collector, jars[1], 0, -JAR_R, light);

            boolean running = t >= WARMUP && t < runesEnd;
            float flash = t >= runesEnd && t < finaleEnd ? Mth.sin((t - runesEnd) / FINALE * Mth.PI) : 0;
            float itemY = (float) RuneCircleBlockEntity.ITEM_Y + Mth.sin(time * 0.08F) * 0.04F;
            int blend = PixelArt.mix(ComplexRunes.byKey(SCENE_RUNES.get(0)).color(), ComplexRunes.byKey(SCENE_RUNES.get(1)).color(), 0.5F);

            // The light in the circle's grooves, brighter while the runes run in.
            poseStack.pushPose();
            poseStack.translate(0, 0.09F, 0);
            poseStack.rotate(Axis.XP.rotationDegrees(90));
            poseStack.rotate(Axis.ZP.rotationDegrees(time * (running ? 1.6F : 0.5F)));
            HexFx.sigilRing(poseStack, collector, 1.05F, 12, blend, (running ? 0.5F : 0.25F) + 0.25F * pulse);
            poseStack.popPose();

            // The runes over the steles, always facing the reader, and their threads into the pickaxe while they burn in.
            for (int side = 0; side < 2; side++) {
                ComplexRune rune = ComplexRunes.byKey(SCENE_RUNES.get(side));
                float sx = side == 0 ? CIRCLE_STELE_R : -CIRCLE_STELE_R;
                float glyphY = RuneSteleBlockEntity.GLYPH_Y + Mth.sin(time * 0.06F + side) * 0.02F;
                poseStack.pushPose();
                poseStack.translate(sx, glyphY, 0);
                HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.6F, rune.color(), 0.3F + 0.25F * pulse);
                poseStack.rotate(camera);
                poseStack.translate(0, 0, 0.03F);
                HexFx.glyph(poseStack, collector, rune, 0.34F, rune.color(), 0.9F);
                poseStack.popPose();
                if (running) {
                    poseStack.pushPose();
                    poseStack.translate(sx, glyphY, 0);
                    HexFx.beam(poseStack, collector, -sx, itemY - glyphY, 0, 0.06F, rune.color(), 0.25F + 0.3F * pulse, toCamera);
                    float run = ((t - WARMUP) * 0.03F + side * 0.5F) % 1;
                    poseStack.translate(-sx * run, (itemY - glyphY) * run, 0);
                    HexFx.sprite(poseStack, collector, camera, HexFx.MOTE, 0.15F, rune.color(), 0.9F);
                    poseStack.popPose();
                }
            }

            // The jars pay the runes' essence: streams of motes from their lids into the pickaxe.
            if (running) {
                float k = (t - WARMUP) / RUNES;
                for (int j = 0; j < 2; j++) {
                    int color = RUNE_ESSENCES.get(j).color();
                    float jz = (j == 0 ? 1 : -1) * JAR_R;
                    for (int m = 0; m < 4; m++) {
                        float run = (time * 0.04F + m * 0.25F + j * 0.1F) % 1;
                        poseStack.pushPose();
                        poseStack.translate(0, Mth.lerp(run, 1.05F, itemY) + Mth.sin(run * Mth.PI) * 0.5F, Mth.lerp(run, jz, 0));
                        HexFx.sprite(poseStack, collector, camera, HexFx.MOTE, 0.13F, color, 0.9F * Mth.sin(k * Mth.PI));
                        poseStack.popPose();
                    }
                }
            }

            // The pickaxe, glowing in the runes' colours once they are in.
            poseStack.pushPose();
            poseStack.translate(0, itemY, 0);
            HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.8F + 1.4F * flash, made ? blend : HexFx.HEXTECH,
                    (made ? 0.45F : 0.25F) + 0.5F * flash);
            poseStack.rotate(Axis.YP.rotationDegrees(-time * 2.4F));
            poseStack.scale(0.9F, 0.9F, 0.9F);
            poseStack.translate(0, -0.15F, 0);
            centre.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
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
