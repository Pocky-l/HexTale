package com.pockyl.hextale.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.realm.PitMaw;

/**
 * Draws a Pit Maw: shut, a bulge of Void soil in the ground; open, a funnel of petals whose throat and veins glow in the
 * dark. It casts no shadow (it is part of the ground) and does not keel over when it dies.
 */
public class PitMawRenderer extends MobRenderer<PitMaw, PitMawRenderer.State, PitMawModel> {
    public static final Identifier TEXTURE = Hextale.id("textures/entity/pit_maw.png");
    private static final RenderType GLOW = RenderTypes.eyes(Hextale.id("textures/entity/pit_maw_glow.png"));

    public static class State extends LivingEntityRenderState {
        /** 0 shut -> 1 torn fully open. */
        public float open;
        /** Folding shut (the open value runs back down). */
        public boolean closing;
        /** Open and drawing someone in. */
        public boolean pulling;
        /** 0 -> 1 through a bite. */
        public float bite;
        /** Phase of its breathing, so neighbouring maws never breathe in step. */
        public int idVariant;
    }

    public PitMawRenderer(EntityRendererProvider.Context context) {
        super(context, new PitMawModel(context.bakeLayer(PitMawModel.LAYER)), 0.0F);
        addLayer(new Glow(this));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PitMaw maw, State state, float partialTick) {
        super.extractRenderState(maw, state, partialTick);
        state.open = maw.open(partialTick);
        state.closing = maw.state() == PitMaw.CLOSING;
        state.pulling = maw.isPulling();
        state.bite = maw.getAttackAnim(partialTick);
        state.idVariant = maw.getId();
    }

    @Override
    protected float getFlipDegrees() {
        return 0.0F;
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }

    private static final class Glow extends EyesLayer<State, PitMawModel> {
        Glow(RenderLayerParent<State, PitMawModel> renderer) {
            super(renderer);
        }

        @Override
        public RenderType renderType() {
            return GLOW;
        }
    }
}
