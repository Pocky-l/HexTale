package com.pockyl.hextale.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FallingLeavesParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

/** A leaf of the magic tree: drifts down like a cherry petal, swaying, and glows in the dark. */
public class MagicLeafParticle extends FallingLeavesParticle {
    public MagicLeafParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite, 0.12F, 4.0F, true, true, 1.4F, 0.01F);
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                                       RandomSource random) {
            return new MagicLeafParticle(level, x, y, z, sprites.get(random));
        }
    }
}
