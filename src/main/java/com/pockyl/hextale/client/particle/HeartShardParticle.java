package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * A shard of a cracked Bulwark heart: thrown out with the given velocity, it tumbles end over end through its four
 * frames, falls, bounces once off the ground and fades. Lit from inside.
 */
public class HeartShardParticle extends HexParticle {
    private static final int FRAMES = 4;

    private final float spin;

    protected HeartShardParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites, 0.06F + level.getRandom().nextFloat() * 0.03F);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        lifetime = 26 + random.nextInt(14);
        hasPhysics = true;
        gravity = 0.9F;
        friction = 0.94F;
        spin = 0.35F + random.nextFloat() * 0.3F;
        frame(random.nextInt(FRAMES), FRAMES);
    }

    @Override
    public void tick() {
        boolean wasOnGround = onGround;
        super.tick();
        if (onGround && !wasOnGround) {
            yd = 0.12;
            xd *= 0.5;
            zd *= 0.5;
        }
        frame((int) (age * spin), FRAMES);
    }

    @Override
    protected float alphaAt(float t) {
        return fadeOut(t, 0.6F);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                                       RandomSource random) {
            return new HeartShardParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
