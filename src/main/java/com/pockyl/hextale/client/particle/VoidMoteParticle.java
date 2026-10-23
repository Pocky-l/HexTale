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
 * A mote of the world falling into the Void. Spawned AT the point it falls into with the offset to its start as
 * velocity: it hangs and drifts a moment as if unsure, swirls round the way in, then is dragged in faster and faster,
 * flaring as it goes, and is gone at the opening.
 */
public class VoidMoteParticle extends HexParticle {
    private final Vec3 from;
    private final Vec3 to;
    private final Vec3 side;
    private final Vec3 lift;
    private final float swirl;

    protected VoidMoteParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, ColorParticleOption option,
                                SpriteSet sprites) {
        super(level, x + dx, y + dy, z + dz, 0, 0, 0, sprites, 0.03F + level.getRandom().nextFloat() * 0.045F);
        to = new Vec3(x, y, z);
        from = new Vec3(x + dx, y + dy, z + dz);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Vec3 way = distance < 1.0E-4 ? new Vec3(0, -1, 0) : to.subtract(from).scale(1 / distance);
        Vec3 across = Math.abs(way.y) > 0.95 ? new Vec3(1, 0, 0) : way.cross(new Vec3(0, 1, 0)).normalize();
        side = across;
        lift = across.cross(way).normalize();
        swirl = (float) Math.min(0.8, 0.2 + distance * 0.18) * (random.nextBoolean() ? 1 : -1);
        lifetime = (int) Mth.clamp(22 + distance * 9, 26, 70) + random.nextInt(10);
        tint(option, 0.12F);
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
        // Slow at first, then pulled ever faster.
        float pull = t * t * t;
        float turn = t * Mth.PI * 2.2F + phase;
        float reach = swirl * Mth.sin(t * Mth.PI) * (1 - pull);
        x = Mth.lerp(pull, from.x, to.x) + (side.x * Mth.cos(turn) + lift.x * Mth.sin(turn)) * reach;
        y = Mth.lerp(pull, from.y, to.y) + (side.y * Mth.cos(turn) + lift.y * Mth.sin(turn)) * reach;
        z = Mth.lerp(pull, from.z, to.z) + (side.z * Mth.cos(turn) + lift.z * Mth.sin(turn)) * reach;
    }

    @Override
    protected float sizeAt(float t) {
        // Swells a little as it is dragged, then is swallowed.
        return fadeIn(t, 0.2F) * (1 + 0.6F * t * t) * (t > 0.9F ? (1 - t) / 0.1F : 1);
    }

    @Override
    protected float alphaAt(float t) {
        return fadeIn(t, 0.25F) * (0.55F + 0.45F * t);
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption option, ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                                       RandomSource random) {
            return new VoidMoteParticle(level, x, y, z, xd, yd, zd, option, sprites);
        }
    }
}
