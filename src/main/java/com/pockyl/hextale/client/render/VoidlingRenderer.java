package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.pockyl.hextale.port.client.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import com.pockyl.hextale.port.client.LivingEntityRenderState;
import net.minecraft.client.renderer.RenderType;
import com.pockyl.hextale.port.client.RenderTypes;
import net.minecraft.resources.ResourceLocation;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.Voidling;

/** Draws a voidling at half the size of its model, as the original did, its eyes and veins glowing in the dark. */
public class VoidlingRenderer extends MobRenderer<Voidling, VoidlingRenderer.State, VoidlingModel> {
    public static final ResourceLocation TEXTURE = Hextale.id("textures/entity/voidling.png");
    private static final RenderType GLOW = RenderTypes.eyes(Hextale.id("textures/entity/voidling_glow.png"));

    public static class State extends LivingEntityRenderState {
        /** 0 -> 1 through a bite. */
        public float bite;
        /** 1 just out of a breach, 0 once unfolded. */
        public float emerge;
        /** Which of its idle breaths this voidling keeps. */
        public int idleVariant;
    }

    public VoidlingRenderer(EntityRendererProvider.Context context) {
        super(context, new VoidlingModel(context.bakeLayer(VoidlingModel.LAYER)), 0.5F);
        addLayer(new Glow(this));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Voidling voidling, State state, float partialTick) {
        super.extractRenderState(voidling, state, partialTick);
        state.bite = voidling.getAttackAnim(partialTick);
        state.emerge = voidling.emerging(partialTick);
        state.idleVariant = voidling.getId();
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        // Half size; the vanilla shift that follows works in these halved units, so the feet stay on the ground.
        poseStack.scale(0.5F, 0.5F, 0.5F);
        // Squeezed out of the breach: narrow and drawn out at first, then it fills out.
        float k = state.emerge * state.emerge;
        if (k > 0) {
            poseStack.scale(1 - 0.35F * k, 1 + 0.15F * k, 1 + 0.25F * k);
        }
    }

    @Override
    protected float getFlipDegrees() {
        return 180.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(State state) {
        return TEXTURE;
    }

    /** 1.21.1: a vanilla eyes layer over the renderer's model adapter. */
    private static final class Glow extends EyesLayer<Voidling, MobRenderer.Adapter<Voidling, State>> {
        Glow(RenderLayerParent<Voidling, MobRenderer.Adapter<Voidling, State>> renderer) {
            super(renderer);
        }

        @Override
        public RenderType renderType() {
            return GLOW;
        }
    }
}
