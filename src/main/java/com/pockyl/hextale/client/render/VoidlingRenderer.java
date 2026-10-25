package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.Voidling;

/** Draws a voidling at half the size of its model, as the original did, its eyes and veins glowing in the dark. */
public class VoidlingRenderer extends MobRenderer<Voidling, VoidlingRenderer.State, VoidlingModel> {
    public static final Identifier TEXTURE = Hextale.id("textures/entity/voidling.png");
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
        state.bite = voidling.getSwingAnimation(partialTick);
        state.emerge = voidling.emerging(partialTick);
        state.idleVariant = voidling.getId();
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        // Half size, feet kept on the ground: the vanilla shift that follows is in these halved units.
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
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
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }

    private static final class Glow extends EyesLayer<State, VoidlingModel> {
        Glow(RenderLayerParent<State, VoidlingModel> renderer) {
            super(renderer);
        }

        @Override
        public RenderType renderType() {
            return GLOW;
        }
    }
}
