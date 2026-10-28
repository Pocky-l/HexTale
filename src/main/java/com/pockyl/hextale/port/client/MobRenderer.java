package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * A 26.x mob renderer (extract a render state, pose the model from it) on the 1.21.1 one: the vanilla renderer draws
 * an {@link Adapter} that poses the 26.x model from the state extracted for this frame.
 *
 * <p>port-of: net.minecraft.client.renderer.entity.MobRenderer
 */
public abstract class MobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<S>>
        extends net.minecraft.client.renderer.entity.MobRenderer<T, MobRenderer.Adapter<T, S>> {
    protected final M portModel;
    private S current;

    protected MobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
        super(context, new Adapter<>(model), shadowRadius);
        this.portModel = model;
        getModel().renderer = this;
    }

    public abstract S createRenderState();

    public abstract ResourceLocation getTextureLocation(S state);

    public void extractRenderState(T entity, S state, float partialTick) {
        state.x = entity.getX(partialTick);
        state.y = entity.getY(partialTick);
        state.z = entity.getZ(partialTick);
        state.ageInTicks = entity.tickCount + partialTick;
        state.partialTick = partialTick;
        state.bodyRot = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        state.yRot = Mth.wrapDegrees(Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot) - state.bodyRot);
        state.xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        state.walkAnimationPos = entity.walkAnimation.position(partialTick);
        state.walkAnimationSpeed = entity.walkAnimation.speed(partialTick);
        state.scale = entity.getScale();
        state.isInvisible = entity.isInvisible();
    }

    protected void scale(S state, PoseStack poseStack) {
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        scale(state(entity, partialTick), poseStack);
    }

    protected float getFlipDegrees() {
        return 90.0F;
    }

    @Override
    protected float getFlipDegrees(T entity) {
        return getFlipDegrees();
    }

    /** The state of the frame being drawn (extracted on first use in it). */
    public S state(T entity, float partialTick) {
        if (current == null) {
            current = createRenderState();
            extractRenderState(entity, current, partialTick);
        }
        return current;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        current = null;
        S state = state(entity, partialTick);
        state.lightCoords = light;
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
        current = null;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return getTextureLocation(state(entity, 0));
    }

    /** The vanilla model the vanilla renderer draws: poses and draws the 26.x model. */
    public static final class Adapter<T extends Mob, S extends LivingEntityRenderState> extends net.minecraft.client.model.EntityModel<T> {
        private final EntityModel<S> model;
        MobRenderer<T, S, ?> renderer;

        Adapter(EntityModel<S> model) {
            this.model = model;
        }

        public EntityModel<S> model() {
            return model;
        }

        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            model.setupAnim(renderer.state(entity, ageInTicks - entity.tickCount));
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
            model.renderToBuffer(poseStack, buffer, light, overlay, color);
        }
    }
}
