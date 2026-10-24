package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A bubble: rises with its velocity, wobbling from side to side and breathing between round and squashed, and bursts
 * at the end of its short life into a ring of spray. Spawned with no upward speed it sits on the surface and just
 * bursts. Takes the world's light.
 */
public class EssenceBubbleParticle extends HexParticle {
    private static final int FRAMES = 5;
    private static final int POP_TICKS = 4;

    protected EssenceBubbleParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, ColorParticleOption option,
                                    SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites, 0.03F + level.getRandom().nextFloat() * 0.035F);
        lifetime = (yd > 0.005 ? 14 : 5) + random.nextInt(12) + POP_TICKS;
        tint(option, 0.05F);
        frame(0, FRAMES);
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
        int popping = age - (lifetime - POP_TICKS);
        if (popping >= 0) {
            frame(2 + Math.min(2, popping * 3 / POP_TICKS), FRAMES);
            return;
        }
        yd = yd * 0.9 + (yd > 0.002 ? 0.002 : 0);
        xd = xd * 0.8 + Mth.cos(age * 0.7F + phase) * 0.003;
        zd = zd * 0.8 + Mth.sin(age * 0.6F + phase) * 0.003;
        move(xd, yd, zd);
        frame((age / 3) % 2, FRAMES);
    }

    @Override
    protected float sizeAt(float t) {
        float popStart = 1 - POP_TICKS / (float) lifetime;
        return t < popStart ? fadeIn(t, 0.15F) * 0.4F + 0.6F : 1 + (t - popStart) / (1 - popStart) * 0.5F;
    }

    @Override
    protected float alphaAt(float t) {
        return 0.9F * fadeOut(t, 0.85F);
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
            return new EssenceBubbleParticle(level, x, y, z, xd, yd, zd, option, sprites);
        }
    }
}
