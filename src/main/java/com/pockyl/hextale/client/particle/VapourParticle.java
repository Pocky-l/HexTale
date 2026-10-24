package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A puff of steam or smoke: leaves with its velocity, slows down, rises on its warmth, swells and turns slowly as it
 * thins out to nothing. Dark colours (soot) rise more slowly and linger. Takes the world's light.
 */
public class VapourParticle extends HexParticle {
    private final float spin;
    private final float swell;

    protected VapourParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, ColorParticleOption option,
                             SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites, 0.13F + level.getRandom().nextFloat() * 0.1F);
        float brightness = (option.getRed() + option.getGreen() + option.getBlue()) / 3;
        lifetime = (int) ((40 + random.nextInt(30)) * (1.6F - brightness * 0.6F));
        spin = (random.nextFloat() - 0.5F) * 0.04F;
        swell = 1.6F + random.nextFloat() * 0.9F;
        roll = random.nextFloat() * Mth.TWO_PI;
        oRoll = roll;
        opacity = 0.42F + brightness * 0.22F;
        tint(option, 0.05F);
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
        xd = xd * 0.9 + Mth.cos(age * 0.05F + phase) * 0.0006;
        zd = zd * 0.9 + Mth.sin(age * 0.05F + phase) * 0.0006;
        yd = yd * 0.92 + 0.0016;
        move(xd, yd, zd);
        roll += spin;
    }

    @Override
    protected float sizeAt(float t) {
        return 0.5F + (swell - 0.5F) * (1 - (1 - t) * (1 - t));
    }

    @Override
    protected float alphaAt(float t) {
        return fadeIn(t, 0.1F) * fadeOut(t, 0.3F);
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
            return new VapourParticle(level, x, y, z, xd, yd, zd, option, sprites);
        }
    }
}
