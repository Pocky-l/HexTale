package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;

/**
 * A drop of liquid: thrown with its velocity, it falls, stretching into a tear as it speeds up, and splashes where it
 * lands - a crown of spray, then a flat spread that fades. Takes the world's light.
 */
public class EssenceDropParticle extends HexParticle {
    private static final int FRAMES = 4;
    private static final int SPLASH_TICKS = 7;
    private int splashedAt = -1;

    protected EssenceDropParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, ColorParticleOption option,
                                  SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites, 0.035F + level.getRandom().nextFloat() * 0.025F);
        lifetime = 60;
        hasPhysics = true;
        gravity = 0.7F;
        friction = 0.98F;
        setSize(0.04F, 0.04F);
        tint(option, 0.06F);
        frame(0, FRAMES);
    }

    @Override
    public void tick() {
        if (splashedAt < 0) {
            super.tick();
            if (removed) {
                return;
            }
            if (onGround || (yd > -1.0E-4 && yd < 1.0E-4 && age > 2 && y == yo)) {
                splashedAt = age;
                xd = 0;
                yd = 0;
                zd = 0;
                lifetime = age + SPLASH_TICKS;
                size *= 2.2F;
                frame(2, FRAMES);
            } else {
                frame(yd < -0.12 ? 1 : 0, FRAMES);
            }
            return;
        }
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        frame(age - splashedAt < SPLASH_TICKS / 2 ? 2 : 3, FRAMES);
    }

    @Override
    protected float alphaAt(float t) {
        if (splashedAt < 0) {
            return 0.95F;
        }
        float s = (age - splashedAt) / (float) SPLASH_TICKS;
        return 0.95F * (1 - s * s);
    }

    @Override
    protected boolean glows() {
        return false;
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption option, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            RandomSource random = level.getRandom();
            return new EssenceDropParticle(level, x, y, z, xd, yd, zd, option, sprites);
        }
    }
}
