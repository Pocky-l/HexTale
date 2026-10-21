package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * A mote of streaming essence. Spawned AT its destination with the offset to its start as velocity, it lifts off the
 * start, flies an arc that bows up and a little sideways (each mote its own, so a stream braids), speeds into the
 * target and shrinks into it. Its light breathes on the way. Glowing.
 */
public class FlowParticle extends HexParticle {
    private final Vec3 from;
    private final Vec3 to;
    private final Vec3 bow;

    protected FlowParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, ColorParticleOption option,
                           SpriteSet sprites) {
        super(level, x + dx, y + dy, z + dz, 0, 0, 0, sprites, 0.045F + level.getRandom().nextFloat() * 0.035F);
        to = new Vec3(x, y, z);
        from = new Vec3(x + dx, y + dy, z + dz);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Vec3 way = distance < 1.0E-4 ? new Vec3(0, 1, 0) : to.subtract(from).scale(1 / distance);
        Vec3 side = Math.abs(way.y) > 0.95 ? new Vec3(1, 0, 0) : way.cross(new Vec3(0, 1, 0)).normalize();
        float lift = (float) Math.min(1.2, 0.15 + distance * 0.12) * (0.7F + random.nextFloat() * 0.6F);
        float swing = (random.nextFloat() - 0.5F) * (float) Math.min(0.6, distance * 0.12);
        bow = new Vec3(0, lift, 0).add(side.scale(swing));
        lifetime = (int) Mth.clamp(10 + distance * 3.5, 12, 48) + random.nextInt(6);
        tint(option, 0.1F);
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
        float eased = smooth(t);
        float arc = Mth.sin(t * Mth.PI);
        x = Mth.lerp(eased, from.x, to.x) + bow.x * arc;
        y = Mth.lerp(eased, from.y, to.y) + bow.y * arc;
        z = Mth.lerp(eased, from.z, to.z) + bow.z * arc;
    }

    @Override
    protected float sizeAt(float t) {
        float breath = 1 + 0.18F * Mth.sin(age * 0.8F + phase);
        return breath * (t > 0.8F ? (1 - t) / 0.2F : fadeIn(t, 0.15F));
    }

    @Override
    protected float alphaAt(float t) {
        return 0.95F * fadeOut(t, 0.85F);
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption option, ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                                       RandomSource random) {
            return new FlowParticle(level, x, y, z, dx, dy, dz, option, sprites);
        }
    }
}
