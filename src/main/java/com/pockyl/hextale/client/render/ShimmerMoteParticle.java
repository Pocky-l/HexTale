package com.pockyl.hextale.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.GlowParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

/**
 * A mote of shimmer: the vanilla glow sprite in violet or pink, drifting slowly where it was sent and glowing in the dark
 * caves the blooms grow in.
 */
public final class ShimmerMoteParticle {
    private static final float[][] COLORS = {{0.72F, 0.36F, 1.0F}, {0.92F, 0.42F, 0.95F}, {0.58F, 0.3F, 0.98F}};

    private ShimmerMoteParticle() {
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                                       RandomSource random) {
            GlowParticle mote = new GlowParticle(level, x, y, z, 0, 0, 0, sprites) {
                @Override
                public int getLightCoords(float partialTick) {
                    return LightCoordsUtil.FULL_BRIGHT;
                }
            };
            float[] color = COLORS[random.nextInt(COLORS.length)];
            mote.setColor(color[0], color[1], color[2]);
            mote.setParticleSpeed(dx + (random.nextDouble() - 0.5) * 0.01, dy, dz + (random.nextDouble() - 0.5) * 0.01);
            mote.setLifetime(30 + random.nextInt(40));
            return mote;
        }
    }
}
