package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A four-pointed glint that twinkles once: opens out of nothing, turns a little, flashes and closes again while it
 * drifts off with its velocity and floats up a touch. Glowing.
 */
public class GlintParticle extends HexParticle {
    private final float spin;

    protected GlintParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, ColorParticleOption option,
                            SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites, 0.06F + level.getRandom().nextFloat() * 0.07F);
        lifetime = 14 + random.nextInt(14);
        friction = 0.88F;
        spin = (random.nextFloat() - 0.5F) * 0.08F;
        roll = (random.nextFloat() - 0.5F) * 0.5F;
        oRoll = roll;
        tint(option, 0.06F);
    }

    @Override
    public void tick() {
        oRoll = roll;
        super.tick();
        yd += 0.0015;
        roll += spin;
    }

    @Override
    protected float sizeAt(float t) {
        return (float) Math.pow(Mth.sin(t * Mth.PI), 0.7);
    }

    @Override
    protected float alphaAt(float t) {
        return Math.min(1, 1.6F * Mth.sin(t * Mth.PI));
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption option, ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                                       RandomSource random) {
            return new GlintParticle(level, x, y, z, xd, yd, zd, option, sprites);
        }
    }
}
