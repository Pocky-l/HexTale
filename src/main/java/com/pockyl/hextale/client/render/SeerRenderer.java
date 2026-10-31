package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.realm.Seer;

/**
 * Draws a Seer, its iris and tendril tips glowing, and while it marks someone the beam: one thin violet line from the
 * pupil to the eyes of the marked, flickering, swelling and fading with the mark ({@link Seer#marking}).
 */
public class SeerRenderer extends MobRenderer<Seer, SeerRenderer.State, SeerModel> {
    public static final Identifier TEXTURE = Hextale.id("textures/entity/seer.png");
    private static final RenderType GLOW = RenderTypes.eyes(Hextale.id("textures/entity/seer_glow.png"));
    /** Height of the eye's centre over the Seer's feet, and how far the pupil sits in front of it (blocks). */
    private static final float EYE_Y = 7 / 16.0F;
    private static final float PUPIL = 4.6F / 16.0F;
    private static final int BEAM_COLOR = 0xA060F0;
    private static final int BEAM_CORE = 0xE070E8;

    public static class State extends LivingEntityRenderState {
        /** Petals 0 shut -> 1 open. */
        public float open;
        /** 0..1 while the beam touches someone. */
        public float marking;
        /** Which of its idles this Seer keeps, and its phase. */
        public int idleVariant;
        /** The beam's start (the pupil) relative to the Seer, and its end relative to that start; null without a beam. */
        public Vec3 beamFrom;
        public Vec3 beam;
    }

    public SeerRenderer(EntityRendererProvider.Context context) {
        super(context, new SeerModel(context.bakeLayer(SeerModel.LAYER)), 0.3F);
        addLayer(new Glow(this));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Seer seer, State state, float partialTick) {
        super.extractRenderState(seer, state, partialTick);
        state.open = seer.open(partialTick);
        state.marking = seer.marking(partialTick);
        state.idleVariant = seer.getId();
        state.beamFrom = null;
        state.beam = null;
        Entity target = state.marking > 0 ? seer.level().getEntity(seer.markTargetId()) : null;
        if (target != null) {
            Vec3 look = Vec3.directionFromRotation(seer.getXRot(partialTick), Mth.rotLerp(partialTick, seer.yHeadRotO, seer.yHeadRot));
            Vec3 from = new Vec3(0, EYE_Y, 0).add(look.scale(PUPIL));
            Vec3 seerPos = seer.getPosition(partialTick);
            state.beamFrom = from;
            // Not quite into the eyes: a beam aimed straight at the camera would be seen end on by the one it marks.
            Vec3 touch = target.getEyePosition(partialTick).subtract(0, Math.min(0.3, target.getEyeHeight() * 0.2), 0);
            state.beam = touch.subtract(seerPos).subtract(from);
        }
    }

    /** The beam reaches past the Seer's own box: keep drawing while either end is in view. */
    @Override
    protected AABB getBoundingBoxForCulling(Seer seer) {
        AABB box = super.getBoundingBoxForCulling(seer);
        Entity target = seer.marking(1) > 0 ? seer.level().getEntity(seer.markTargetId()) : null;
        return target == null ? box : box.minmax(target.getBoundingBox());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        if (state.beam == null) {
            return;
        }
        // A thin line that flickers like a nerve firing, swelling in and out with the mark.
        float flicker = 0.65F + 0.35F * Mth.sin(state.ageInTicks * 2.9F) * Mth.sin(state.ageInTicks * 1.3F + 1);
        float alpha = state.marking * flicker;
        Vec3 toCamera = camera.pos.subtract(state.x + state.beamFrom.x, state.y + state.beamFrom.y, state.z + state.beamFrom.z);
        Vector3f viewer = new Vector3f((float) toCamera.x, (float) toCamera.y, (float) toCamera.z);
        poseStack.pushPose();
        poseStack.translate(state.beamFrom.x, state.beamFrom.y, state.beamFrom.z);
        float dx = (float) state.beam.x;
        float dy = (float) state.beam.y;
        float dz = (float) state.beam.z;
        HexFx.beam(poseStack, collector, dx, dy, dz, 0.07F, BEAM_COLOR, 0.55F * alpha, viewer);
        HexFx.beam(poseStack, collector, dx, dy, dz, 0.025F, BEAM_CORE, 0.9F * alpha, viewer);
        poseStack.popPose();
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }

    private static final class Glow extends EyesLayer<State, SeerModel> {
        Glow(RenderLayerParent<State, SeerModel> renderer) {
            super(renderer);
        }

        @Override
        public RenderType renderType() {
            return GLOW;
        }
    }
}
