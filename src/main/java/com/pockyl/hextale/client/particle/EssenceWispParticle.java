package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Living essence: a blob of glowing liquid that seeps out slowly, wobbles (its sprite is a looping animation of a
 * drop changing shape with a droplet circling it), floats upwards as if lighter than air, sways on its way and
 * evaporates - swelling a little, then shrinking and fading. Moves on its own velocity, slows down, never falls.
 */
public class EssenceWispParticle extends HexParticle {
    private static final int FRAMES = 8;
    private static final int TICKS_PER_FRAME = 4;
    private final int frameOffset;
    private final float spin;
    private final float sway;

    protected EssenceWispParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, ColorParticleOption option,
                                  SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites, 0.075F + level.getRandom().nextFloat() * 0.05F);
        lifetime = 70 + random.nextInt(50);
        frameOffset = random.nextInt(FRAMES);
        spin = (random.nextFloat() - 0.5F) * 0.035F;
        sway = 0.0007F + random.nextFloat() * 0.0006F;
        roll = random.nextFloat() * Mth.TWO_PI;
        oRoll = roll;
        opacity = 0.9F;
        tint(option, 0.08F);
        frame(frameOffset, FRAMES);
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
        // Lighter than air: it rises, gently, never faster than a slow drift; the sideways push dies away.
        yd = Math.min(yd + 0.0006, 0.022) * 0.985;
        xd = xd * 0.95 + Mth.cos(age * 0.11F + phase) * sway;
        zd = zd * 0.95 + Mth.sin(age * 0.09F + phase * 1.7F) * sway;
        move(xd, yd, zd);
        roll += spin;
        frame(age / TICKS_PER_FRAME + frameOffset, FRAMES);
    }

    @Override
    protected float sizeAt(float t) {
        // Seeps out small, fills out, then evaporates away to nothing.
        float grow = 0.35F + 0.65F * smooth(t / 0.25F);
        float shrink = t < 0.55F ? 1 : 1 - 0.75F * smooth((t - 0.55F) / 0.45F);
        return grow * shrink * (1 + 0.06F * Mth.sin(age * 0.3F + phase));
    }

    @Override
    protected float alphaAt(float t) {
        return fadeIn(t, 0.12F) * fadeOut(t, 0.6F);
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption option, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            RandomSource random = level.getRandom();
            return new EssenceWispParticle(level, x, y, z, xd, yd, zd, option, sprites);
        }
    }
}
