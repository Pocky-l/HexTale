package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A tongue of flame: licks upwards from where it was lit, its sprite running from a tall flame to a small flicker as
 * it burns out, swaying a little. Glowing.
 */
public class HexFlameParticle extends HexParticle {
    private static final int FRAMES = 5;

    protected HexFlameParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, ColorParticleOption option,
                               SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites, 0.05F + level.getRandom().nextFloat() * 0.04F);
        lifetime = 9 + random.nextInt(8);
        roll = (random.nextFloat() - 0.5F) * 0.25F;
        oRoll = roll;
        tint(option, 0.07F);
        frame(0, FRAMES);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        oRoll = roll;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        xd *= 0.85;
        zd *= 0.85;
        yd = yd * 0.9 + 0.003;
        move(xd, yd, zd);
        roll = Mth.sin(age * 0.6F + phase) * 0.12F;
        frame(age * FRAMES / Math.max(1, lifetime), FRAMES);
    }

    @Override
    protected float sizeAt(float t) {
        return fadeIn(t, 0.15F) * (1 - 0.45F * t);
    }

    @Override
    protected float alphaAt(float t) {
        return fadeOut(t, 0.7F);
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption option, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            RandomSource random = level.getRandom();
            return new HexFlameParticle(level, x, y, z, xd, yd, zd, option, sprites);
        }
    }
}
