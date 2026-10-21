package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A leaf of the magic tree: drifts down like a petal, swinging from side to side along its own line and fluttering
 * (rocking as it swings), settles on the ground and fades there. Takes the world's light.
 */
public class MagicLeafParticle extends HexParticle {
    private final float swingX;
    private final float swingZ;
    private final float baseRoll;
    private int landedAt = -1;

    protected MagicLeafParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0, -0.01, 0, sprites, 0.05F + level.getRandom().nextFloat() * 0.035F);
        lifetime = 160 + random.nextInt(80);
        hasPhysics = true;
        float heading = random.nextFloat() * Mth.TWO_PI;
        swingX = Mth.cos(heading);
        swingZ = Mth.sin(heading);
        baseRoll = random.nextFloat() * Mth.TWO_PI;
        roll = baseRoll;
        oRoll = roll;
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
        if (landedAt >= 0) {
            if (age - landedAt > 30) {
                remove();
            }
            return;
        }
        float swing = Mth.sin(age * 0.09F + phase);
        double side = swing * 0.025;
        yd = Math.max(yd - 0.002, -0.035 - Math.abs(swing) * 0.01);
        move(swingX * side, yd, swingZ * side);
        roll = baseRoll + Mth.cos(age * 0.09F + phase) * 0.9F;
        if (onGround) {
            landedAt = age;
        }
    }

    @Override
    protected float alphaAt(float t) {
        float settled = landedAt < 0 ? 1 : 1 - Math.min(1, (age - landedAt) / 30F);
        return fadeIn(t, 0.05F) * settled * fadeOut(t, 0.9F);
    }

    @Override
    protected boolean glows() {
        return false;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                                       RandomSource random) {
            return new MagicLeafParticle(level, x, y, z, sprites);
        }
    }
}
