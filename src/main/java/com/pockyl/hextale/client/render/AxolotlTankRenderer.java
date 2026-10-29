package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.AxolotlModel;
import net.minecraft.client.model.geom.ModelLayers;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import net.minecraft.client.renderer.RenderType;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.AxolotlTankBlockEntity;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Draws the water of an Axolotl Tank and the axolotl living in it: the vanilla model in its own colours, hovering and
 * now and then paddling slowly round the tank, gills waving. The water is always there; the axolotl only while the tank
 * has one.
 *
 * <p>1.21.1: the vanilla model animates from an entity, so a stand-in axolotl that never joins the level carries its
 * animation state (26.x fills an AxolotlRenderState).
 */
public class AxolotlTankRenderer implements BlockEntityRenderer<AxolotlTankBlockEntity, AxolotlTankRenderer.State> {
    private static final float MIN = 2.02F / 16;
    private static final float MAX = 13.98F / 16;
    private static final float BOTTOM = 3.02F / 16;
    private static final float TOP = 12.5F / 16;
    private static final int WATER = 0x8C357F78;
    private static final float SCALE = 0.42F;
    /** Ticks of one lap round the tank. */
    private static final float LAP = 360;
    private static final Map<Axolotl.Variant, ResourceLocation> TEXTURES = new EnumMap<>(Axolotl.Variant.class);

    static {
        for (Axolotl.Variant variant : Axolotl.Variant.values()) {
            TEXTURES.put(variant, ResourceLocation.withDefaultNamespace(
                    String.format(Locale.ROOT, "textures/entity/axolotl/axolotl_%s.png", variant.getName())));
        }
    }

    private final AxolotlModel<Axolotl> model;
    private @Nullable Axolotl axolotl;

    public static final class State extends BlockEntityRenderState {
        boolean hasAxolotl;
        Axolotl.Variant variant = Axolotl.Variant.LUCY;
        float time;
        long seed;
    }

    public AxolotlTankRenderer(BlockEntityRendererProvider.Context context) {
        model = new AxolotlModel<>(context.bakeLayer(ModelLayers.AXOLOTL));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AxolotlTankBlockEntity tank, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tank, state, partialTick, cameraPosition, breakProgress);
        Level level = tank.getLevel();
        state.time = level == null ? 0 : level.getGameTime() + partialTick;
        state.seed = tank.getBlockPos().asLong();
        state.hasAxolotl = tank.hasAxolotl();
        state.variant = tank.variant();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.hasAxolotl) {
            submitAxolotl(state, poseStack, collector);
        }
        LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, BOTTOM, TOP, WATER, state.time, state.seed, 3);
    }

    /**
     * The axolotl hovers for a while, then paddles a slow lap: its path is a small ellipse in the middle of the tank, its
     * nose follows the path, it bobs up and down. Each tank keeps its own rhythm.
     */
    private void submitAxolotl(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        float time = state.time + (state.seed & 0xFFF);
        // A lap of paddling, then a pause of hovering: the angle eases in and out of each lap.
        float cycle = time % (LAP * 2);
        float swim = cycle < LAP ? Mth.sin(cycle / LAP * Mth.PI) : 0;
        float lap = (float) Math.floor(time / (LAP * 2));
        float eased = cycle < LAP ? (1 - Mth.cos(cycle / LAP * Mth.PI)) * 0.5F : 1;
        float angle = (lap + eased) * Mth.TWO_PI;
        float x = 0.5F + Mth.cos(angle) * 0.13F;
        float z = 0.5F + Mth.sin(angle) * 0.1F;
        float y = 0.3F + Mth.sin(time * 0.05F) * 0.03F;
        // Facing along the path (the model looks along -z at yaw 0, so it turns with the tangent).
        float yaw = (float) Math.toDegrees(Math.atan2(-Mth.sin(angle) * 0.13F, Mth.cos(angle) * 0.1F));

        Axolotl axolotl = standIn();
        if (axolotl == null) {
            return;
        }
        model.setupAnim(axolotl, 0, swim > 0.02F ? swim : 0, time, 0, 0);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        poseStack.scale(-SCALE, -SCALE, SCALE);
        poseStack.translate(0, -1.501F, 0);
        model.renderToBuffer(poseStack, collector.buffer(RenderType.entityCutoutNoCull(TEXTURES.get(state.variant))), state.lightCoords,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /** The stand-in the model reads: in water, never moving by itself. */
    private @Nullable Axolotl standIn() {
        Level level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        if (axolotl == null || axolotl.level() != level) {
            axolotl = new Axolotl(EntityType.AXOLOTL, level) {
                {
                    wasTouchingWater = true;
                }
            };
        }
        return axolotl;
    }
}
