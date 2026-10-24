package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * The 26.x camera-facing sprite particle over the 1.21.1 {@link TextureSheetParticle}: {@link #extract} is called
 * from {@code render} with the particle buffer in a {@link QuadParticleRenderState}.
 *
 * <p>port-of: net.minecraft.client.particle.SingleQuadParticle
 */
public abstract class SingleQuadParticle extends TextureSheetParticle {
    protected SingleQuadParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z);
        setSprite(sprite);
    }

    protected SingleQuadParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
        super(level, x, y, z, xd, yd, zd);
        setSprite(sprite);
    }

    public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        super.render(state.buffer(), camera, partialTick);
    }

    @Override
    public final void render(VertexConsumer buffer, Camera camera, float partialTick) {
        extract(new QuadParticleRenderState(buffer), camera, partialTick);
    }

    public abstract Layer getLayer();

    @Override
    public ParticleRenderType getRenderType() {
        return getLayer() == Layer.OPAQUE ? ParticleRenderType.PARTICLE_SHEET_OPAQUE : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public int getLightCoords(float partialTick) {
        return super.getLightColor(partialTick);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return getLightCoords(partialTick);
    }

    public enum Layer {
        OPAQUE,
        TRANSLUCENT
    }
}
