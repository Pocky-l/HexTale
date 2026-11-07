package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.ProsectorConfig;
import com.pockyl.hextale.thevoid.boss.ProsectorState;
import com.pockyl.hextale.thevoid.boss.ProsectorState.Section;
import com.pockyl.hextale.thevoid.boss.StudyKind;

/**
 * Draws the Prosector: {@link ProsectorModel}, its iris and study sacs lit by a glow layer, the copy of the player's item in
 * its forceps, and the telegraphs that follow something (the live Incision line, the Probe, the hooks, the marker over the
 * target during Excision). What the sacs hold swells them and lights a point of its colour over each.
 */
public class ProsectorRenderer extends MobRenderer<Prosector, ProsectorRenderer.State, ProsectorModel> {
    private static final Identifier GLOW_TEXTURE = Hextale.id("textures/entity/prosector_glow.png");
    private static final RenderType GLOW = RenderTypes.eyes(GLOW_TEXTURE);
    private static final Identifier TEXTURE = Hextale.id("textures/entity/prosector.png");
    private static final int LINE = 0xE040C8;
    private static final int BEAM = 0xA060F0;
    private static final int BEAM_CORE = 0xE070E8;
    private static final int HOOK = 0xD0C8B8;
    private static final int SHIELD = 0x8C6CF0;

    public static class State extends LivingEntityRenderState {
        /** Eye 0 shut -> 1 wide; hood 0 folded -> 1 unfolded. */
        public float open;
        public float hood;
        public ProsectorState boss = ProsectorState.DORMANT;
        public Section section = Section.NONE;
        public float progress;
        public float bodyYaw;
        public float[] arms = new float[Prosector.Arm.values().length];
        public float[] fills = new float[StudyKind.values().length];
        public boolean shielded;
        /** Where the move in progress points, from its feet. */
        public Vec3 aim = Vec3.ZERO;
        public final ItemStackRenderState held = new ItemStackRenderState();
    }

    public ProsectorRenderer(EntityRendererProvider.Context context) {
        super(context, new ProsectorModel(context.bakeLayer(ProsectorModel.LAYER)), 2.0F);
        addLayer(new Glow(this));
        addLayer(new Held(this));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Prosector boss, State state, float partialTick) {
        super.extractRenderState(boss, state, partialTick);
        // Its long death is staged by the eye and the room: no flat fall to the side, no red through all of it.
        state.hasRedOverlay = boss.hurtTime > 0 && !boss.isDeadOrDying();
        state.open = boss.eyeOpen(partialTick);
        state.hood = boss.hoodOpen(partialTick);
        state.boss = boss.state();
        state.section = boss.section();
        state.progress = boss.stateProgress(partialTick);
        state.bodyYaw = Mth.rotLerp(partialTick, boss.yBodyRotO, boss.yBodyRot);
        for (Prosector.Arm arm : Prosector.Arm.values()) {
            state.arms[arm.ordinal()] = boss.armReach(arm, partialTick);
        }
        for (StudyKind kind : StudyKind.values()) {
            state.fills[kind.ordinal()] = boss.studyFill(kind);
        }
        state.shielded = boss.state().shielded();
        state.aim = boss.aim().subtract(boss.getPosition(partialTick));
        ItemStack held = boss.heldCopy();
        itemModelResolver.updateForLiving(state.held, held, ItemDisplayContext.FIXED, boss);
    }

    /** It fills the room and its telegraphs reach across it: never culled. */
    @Override
    protected boolean affectedByCulling(Prosector boss) {
        return false;
    }

    @Override
    protected float getFlipDegrees() {
        return 0;
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        Vector3f viewer = new Vector3f((float) (camera.pos.x - state.x), (float) (camera.pos.y - state.y), (float) (camera.pos.z - state.z));
        sacs(state, poseStack, collector, camera);
        telegraph(state, poseStack, collector, viewer);
        if (state.shielded && state.boss != ProsectorState.DORMANT && state.boss != ProsectorState.RISING) {
            poseStack.pushPose();
            poseStack.translate(0, 2.2, 0);
            poseStack.mulPose(camera.orientation);
            HexFx.hexRing(poseStack, collector, 3.2F, SHIELD, 0.5F);
            poseStack.popPose();
        }
    }

    /** A point of an instrument, in world axes, from its feet. */
    private static Vector3f turned(State state, float x, float y, float z) {
        float yaw = -state.bodyYaw * Mth.DEG_TO_RAD;
        float cos = Mth.cos(yaw);
        float sin = Mth.sin(yaw);
        return new Vector3f(x * cos + z * sin, y, -x * sin + z * cos);
    }

    /** The four sacs on its back: they fill with light as it learns. */
    private static void sacs(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (StudyKind kind : StudyKind.values()) {
            float fill = state.fills[kind.ordinal()];
            Vector3f at = turned(state, kind.ordinal() % 2 == 0 ? 0.62F : -0.62F, kind.ordinal() < 2 ? 3.95F : 2.95F, -1.75F);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.MOTE, 0.3F + 0.5F * fill, kind.color(), 0.15F + 0.85F * fill);
            poseStack.popPose();
        }
    }

    private static void telegraph(State state, PoseStack poseStack, SubmitNodeCollector collector, Vector3f viewer) {
        boolean startup = state.section == Section.STARTUP;
        boolean active = state.section == Section.ACTIVE;
        switch (state.boss) {
            case INCISION -> {
                if (startup) {
                    groundLine(state, poseStack, collector, 0.5F + 0.5F * state.progress);
                }
            }
            case EXCISION -> {
                if (startup) {
                    groundHex(state, poseStack, collector, ProsectorConfig.EXCISION.size().get().floatValue(), 0.4F + 0.6F * state.progress);
                }
            }
            case PROBE -> {
                if (startup) {
                    beam(state, poseStack, collector, viewer, 0.03F, BEAM, 0.25F + 0.5F * state.progress);
                } else if (active) {
                    float flicker = 0.75F + 0.25F * Mth.sin(state.ageInTicks * 2.9F);
                    beam(state, poseStack, collector, viewer, 0.28F, BEAM, 0.55F * flicker);
                    beam(state, poseStack, collector, viewer, 0.1F, BEAM_CORE, 0.95F * flicker);
                }
            }
            case RETRACTOR -> {
                if (startup) {
                    beam(state, poseStack, collector, viewer, 0.03F, HOOK, 0.2F + 0.5F * state.progress);
                } else if (active) {
                    beam(state, poseStack, collector, viewer, 0.12F, HOOK, 1);
                    hook(state, poseStack, collector, viewer);
                }
            }
            default -> {
            }
        }
    }

    private static Vector3f origin(State state) {
        return turned(state, 0, 3.2F, 1.0F);
    }

    private static void beam(State state, PoseStack poseStack, SubmitNodeCollector collector, Vector3f viewer, float width, int color, float alpha) {
        Vector3f from = origin(state);
        poseStack.pushPose();
        poseStack.translate(from.x, from.y, from.z);
        HexFx.beam(poseStack, collector, (float) state.aim.x - from.x, (float) state.aim.y - from.y, (float) state.aim.z - from.z, width, color, alpha, viewer);
        poseStack.popPose();
    }

    /** The second hook runs beside the first. */
    private static void hook(State state, PoseStack poseStack, SubmitNodeCollector collector, Vector3f viewer) {
        Vector3f from = turned(state, 2.2F, 1.3F, 1.4F);
        poseStack.pushPose();
        poseStack.translate(from.x, from.y, from.z);
        HexFx.beam(poseStack, collector, (float) state.aim.x - from.x, (float) state.aim.y - from.y, (float) state.aim.z - from.z, 0.12F, HOOK, 1, viewer);
        poseStack.popPose();
    }

    /** The live Incision line: from under it along the floor to the synced end. */
    private static void groundLine(State state, PoseStack poseStack, SubmitNodeCollector collector, float alpha) {
        float dx = (float) state.aim.x;
        float dz = (float) state.aim.z;
        poseStack.pushPose();
        poseStack.translate(0, state.aim.y + 0.07, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            HexFx.glowSegment(consumer, pose, 0, 0, dx, dz, 1.2F, LINE, alpha * 0.45F);
            HexFx.glowSegment(consumer, pose, 0, 0, dx, dz, 0.2F, 0xFFD0F4, alpha);
        });
        poseStack.popPose();
    }

    private static void groundHex(State state, PoseStack poseStack, SubmitNodeCollector collector, float radius, float alpha) {
        poseStack.pushPose();
        poseStack.translate(state.aim.x, state.aim.y + 0.07, state.aim.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        HexFx.hexRing(poseStack, collector, radius, LINE, alpha);
        poseStack.popPose();
    }

    private static final class Glow extends RenderLayer<State, ProsectorModel> {
        Glow(RenderLayerParent<State, ProsectorModel> renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, State state, float yRot, float xRot) {
            collector.order(1).submitModel(getParentModel(), state, poseStack, GLOW, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        }
    }

    /** The copy in its forceps, large; in a Riposte larger still: the arm swings it. */
    private static final class Held extends RenderLayer<State, ProsectorModel> {
        Held(RenderLayerParent<State, ProsectorModel> renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, State state, float yRot, float xRot) {
            if (state.held.isEmpty()) {
                return;
            }
            float size = state.boss == ProsectorState.RIPOSTE ? 5.5F : 2.5F;
            poseStack.pushPose();
            getParentModel().translateToGrip(poseStack);
            // Its top along the jaws, the diagonal of a sprite straightened, the grip between them.
            poseStack.mulPose(Axis.ZP.rotationDegrees(135));
            poseStack.scale(size, size, size);
            poseStack.translate(0, 0.3, 0);
            state.held.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
