package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A mote of the Void's own air: it rises from the abyss very slowly, swaying, swelling and dimming as if it breathed,
 * and fades out long before it reaches anything. Spawned by the Void's biomes as ambient particles.
 */
public class VoidDriftParticle extends HexParticle {
    private final float sway;
    private final float rise;

    protected VoidDriftParticle(ClientLevel level, double x, double y, double z, ColorParticleOption option, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0, sprites, 0.035F + level.getRandom().nextFloat() * 0.05F);
        tint(option, 0.18F);
        lifetime = 140 + random.nextInt(120);
        sway = 0.004F + random.nextFloat() * 0.006F;
        rise = 0.004F + random.nextFloat() * 0.005F;
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
        float s = age * 0.045F + phase;
        x += Mth.sin(s) * sway;
        z += Mth.cos(s * 0.8F) * sway;
        y += rise;
        frame(age / 6 + (int) (phase * 4), 4);
    }

    @Override
    protected float sizeAt(float t) {
        return 0.75F + 0.25F * Mth.sin(age * 0.11F + phase);
    }

    @Override
    protected float alphaAt(float t) {
        return fadeIn(t, 0.2F) * fadeOut(t, 0.65F) * (0.55F + 0.3F * Mth.sin(age * 0.07F + phase * 2));
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption option, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            RandomSource random = level.getRandom();
            return new VoidDriftParticle(level, x, y, z, option, sprites);
        }
    }
}
