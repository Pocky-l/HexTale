package com.pockyl.hextale.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A glowing mote flying along an arc from (spawn + speed) into the spawn point: it lifts in the middle of the way,
 * speeds up as it nears the target and shrinks into it. Full bright, translucent, in the option's colour.
 */
public class FlowParticle extends SingleQuadParticle {
    private final double xEnd;
    private final double yEnd;
    private final double zEnd;
    private final double xFrom;
    private final double yFrom;
    private final double zFrom;
    private final float lift;
    private final float size;

    protected FlowParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, ColorParticleOption options,
                           SpriteSet sprites) {
        super(level, x + dx, y + dy, z + dz, sprites.get(level.getRandom()));
        xEnd = x;
        yEnd = y;
        zEnd = z;
        xFrom = x + dx;
        yFrom = y + dy;
        zFrom = z + dz;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        lift = (float) Math.min(1.2, 0.15 + distance * 0.12) * (0.7F + random.nextFloat() * 0.6F);
        lifetime = (int) Mth.clamp(10 + distance * 3.5, 12, 48) + random.nextInt(6);
        size = 0.05F + random.nextFloat() * 0.04F;
        quadSize = size;
        float shade = 0.85F + random.nextFloat() * 0.15F;
        setColor(options.getRed() * shade, options.getGreen() * shade, options.getBlue() * shade);
        hasPhysics = false;
        xd = 0;
        yd = 0;
        zd = 0;
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
        float t = age / (float) lifetime;
        float eased = t * t * (3 - 2 * t);
        x = Mth.lerp(eased, xFrom, xEnd);
        y = Mth.lerp(eased, yFrom, yEnd) + Mth.sin(t * Mth.PI) * lift;
        z = Mth.lerp(eased, zFrom, zEnd);
        quadSize = size * (t > 0.8F ? (1 - t) / 0.2F : Math.min(1, t * 6));
        setAlpha(t > 0.85F ? (1 - t) / 0.15F : 0.95F);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption options, ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                                       RandomSource random) {
            return new FlowParticle(level, x, y, z, dx, dy, dz, options, sprites);
        }
    }
}
