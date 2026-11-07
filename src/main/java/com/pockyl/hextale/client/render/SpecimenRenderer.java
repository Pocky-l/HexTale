package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.Specimen;

/**
 * STAND-IN look of a Specimen: a ring of light round the copy it holds, both turning slowly. The sinew to the Prosector's body
 * is the part to keep: that line tells a player which jar answers for what, and it must always be drawn.
 */
public class SpecimenRenderer extends EntityRenderer<Specimen, SpecimenRenderer.State> {
    private static final int SINEW = 0xE040C8;
    private final ItemModelResolver items;

    public static class State extends EntityRenderState {
        public int color;
        public float spin;
        public final ItemStackRenderState copy = new ItemStackRenderState();
        /** From the jar to the Prosector's back; null while it is out of reach of the client. */
        public @Nullable Vec3 sinew;
    }

    public SpecimenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Specimen specimen, State state, float partialTick) {
        super.extractRenderState(specimen, state, partialTick);
        state.color = specimen.kind().color();
        state.spin = (specimen.tickCount + partialTick) * 2;
        items.updateForNonLiving(state.copy, specimen.copy(), ItemDisplayContext.FIXED, specimen);
        Entity owner = specimen.level().getEntity(specimen.ownerId());
        state.sinew = owner instanceof Prosector boss
                ? boss.getPosition(partialTick).add(0, boss.getBbHeight() * 0.7, 0).subtract(specimen.getPosition(partialTick).add(0, 0.6, 0)) : null;
    }

    /** The sinew crosses the room: the jar must draw whenever it is near enough to matter. */
    @Override
    protected boolean affectedByCulling(Specimen specimen) {
        return false;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        poseStack.pushPose();
        poseStack.translate(0, 0.6, 0);
        poseStack.pushPose();
        poseStack.rotate(camera.orientation);
        poseStack.rotate(Axis.ZP.rotationDegrees(state.spin * 0.25F));
        HexFx.hexRing(poseStack, collector, 1.1F, state.color, 0.9F);
        poseStack.popPose();
        if (state.sinew != null) {
            Vector3f viewer = new Vector3f((float) (camera.pos.x - state.x), (float) (camera.pos.y - state.y - 0.6), (float) (camera.pos.z - state.z));
            float pulse = 0.6F + 0.3F * Mth.sin(state.ageInTicks * 0.2F);
            HexFx.beam(poseStack, collector, (float) state.sinew.x, (float) state.sinew.y, (float) state.sinew.z, 0.12F, SINEW, pulse, viewer);
        }
        poseStack.rotate(Axis.YP.rotationDegrees(state.spin));
        poseStack.scale(0.8F, 0.8F, 0.8F);
        state.copy.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
