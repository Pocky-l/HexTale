package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;

/**
 * The Void's moving air, two looks on one sprite set of four thinning frames: a streak of a current (the Undertow),
 * spawned with its speed along the current, and the mark a Seer leaves (spawned with a slow rise round the marked one).
 * Both fade in, run through their frames as they thin, and fade out.
 */
public class VoidStreakParticle extends HexParticle {
    protected VoidStreakParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, ColorParticleOption option,
                                 SpriteSet sprites, float size, int life) {
        super(level, x, y, z, xd, yd, zd, sprites, size);
        tint(option, 0.12F);
        opacity = option.getAlpha();
        lifetime = life + random.nextInt(life / 2 + 1);
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
        x += xd;
        y += yd;
        z += zd;
        frame(age * 4 / Math.max(1, lifetime), 4);
    }

    @Override
    protected float alphaAt(float t) {
        return fadeIn(t, 0.2F) * fadeOut(t, 0.6F);
    }

    @Override
    protected float sizeAt(float t) {
        return 0.8F + 0.2F * (1 - t);
    }

    public static class Current implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Current(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption option, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            RandomSource random = level.getRandom();
            return new VoidStreakParticle(level, x, y, z, xd, yd, zd, option, sprites, 0.22F + random.nextFloat() * 0.12F, 22);
        }
    }

    public static class Mark implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Mark(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption option, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            RandomSource random = level.getRandom();
            return new VoidStreakParticle(level, x, y, z, xd, yd, zd, option, sprites, 0.18F + random.nextFloat() * 0.1F, 26);
        }
    }
}
