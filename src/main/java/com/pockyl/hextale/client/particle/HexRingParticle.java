package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import com.pockyl.hextale.port.client.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A ring of light rushing out from a point and fading: a finished ritual, a waking block, a burst. The velocity is a
 * description, not a motion: x = the final radius in blocks (0 = 1), y &gt; 0 lays the ring flat like a shockwave on
 * the ground, otherwise it faces the camera.
 */
public class HexRingParticle extends HexParticle {
    private static final SingleQuadParticle.FacingCameraMode FLAT = (target, camera, partialTick) -> target.rotationX(-Mth.HALF_PI);
    private final boolean flat;

    protected HexRingParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, ColorParticleOption option,
                              SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0, sprites, xd > 0 ? (float) xd : 1);
        flat = yd > 0;
        lifetime = 12 + (int) (size * 4);
        roll = random.nextFloat() * Mth.TWO_PI;
        oRoll = roll;
        tint(option, 0.03F);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
        }
    }

    @Override
    public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
        return flat ? FLAT : super.getFacingCameraMode();
    }

    @Override
    protected float sizeAt(float t) {
        float u = 1 - t;
        return 0.12F + 0.88F * (1 - u * u * u);
    }

    @Override
    protected float alphaAt(float t) {
        return 1 - (float) Math.pow(t, 1.6);
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption option, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            RandomSource random = level.getRandom();
            return new HexRingParticle(level, x, y, z, xd, yd, zd, option, sprites);
        }
    }
}
