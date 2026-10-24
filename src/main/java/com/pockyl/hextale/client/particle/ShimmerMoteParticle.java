package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A mote of shimmer: a soft violet or pink light with a twinkling sparkle (a looping animation), wandering slowly in
 * the direction it was sent, its glow breathing. Lights up the dark caves the blooms grow in.
 */
public class ShimmerMoteParticle extends HexParticle {
    private static final float[][] COLORS = {{0.74F, 0.38F, 1.0F}, {0.95F, 0.45F, 0.92F}, {0.6F, 0.34F, 1.0F}, {1.0F, 0.6F, 0.95F}};
    private static final int FRAMES = 6;
    private final int frameOffset;

    protected ShimmerMoteParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd + (level.getRandom().nextDouble() - 0.5) * 0.01, yd, zd + (level.getRandom().nextDouble() - 0.5) * 0.01,
                sprites, 0.045F + level.getRandom().nextFloat() * 0.04F);
        lifetime = 40 + random.nextInt(50);
        frameOffset = random.nextInt(FRAMES);
        float[] color = COLORS[random.nextInt(COLORS.length)];
        setColor(color[0], color[1], color[2]);
        frame(frameOffset, FRAMES);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        xd = xd * 0.97 + Mth.cos(age * 0.07F + phase) * 0.0006;
        yd = yd * 0.97 + Mth.sin(age * 0.05F + phase) * 0.0004;
        zd = zd * 0.97 + Mth.sin(age * 0.06F + phase * 2) * 0.0006;
        move(xd, yd, zd);
        frame(age / 3 + frameOffset, FRAMES);
    }

    @Override
    protected float sizeAt(float t) {
        return fadeIn(t, 0.2F) * (0.85F + 0.15F * Mth.sin(age * 0.25F + phase));
    }

    @Override
    protected float alphaAt(float t) {
        return fadeOut(t, 0.6F) * (0.75F + 0.25F * Mth.sin(age * 0.18F + phase * 2));
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            RandomSource random = level.getRandom();
            return new ShimmerMoteParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
