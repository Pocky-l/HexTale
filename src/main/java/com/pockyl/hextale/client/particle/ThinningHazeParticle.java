package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The haze of a thinning land: a wide, faint veil hugging the ground. It drifts sideways with its velocity, never rises,
 * swells a little and lingers for seconds - many of them together read as a low mist. Takes the world's light.
 */
public class ThinningHazeParticle extends HexParticle {
    private final float spin;

    protected ThinningHazeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                                   ColorParticleOption option, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites, 0.9F + level.getRandom().nextFloat() * 0.9F);
        lifetime = 120 + random.nextInt(100);
        spin = (random.nextFloat() - 0.5F) * 0.006F;
        roll = random.nextFloat() * Mth.TWO_PI;
        oRoll = roll;
        opacity = 0.1F + random.nextFloat() * 0.08F;
        tint(option, 0.04F);
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
        x += xd + Mth.cos(age * 0.02F + phase) * 0.002;
        y += yd;
        z += zd + Mth.sin(age * 0.02F + phase) * 0.002;
        roll += spin;
    }

    @Override
    protected float sizeAt(float t) {
        return 0.8F + 0.4F * t;
    }

    @Override
    protected float alphaAt(float t) {
        return fadeIn(t, 0.25F) * fadeOut(t, 0.4F);
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
        public Particle createParticle(ColorParticleOption option, ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                                       RandomSource random) {
            return new ThinningHazeParticle(level, x, y, z, xd, yd, zd, option, sprites);
        }
    }
}
