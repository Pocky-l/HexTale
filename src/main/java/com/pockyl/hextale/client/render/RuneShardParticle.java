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
import net.minecraft.world.phys.Vec3;

/**
 * A shard of a rune's hieroglyph peeled off a stele: it rises out of the glyph, winds towards the spawn point along a
 * narrowing corkscrew around the straight way, turning slowly and flickering, and flares as it sinks into the target.
 * Spawn it AT the destination with the offset to its start as speed. Starts white-hot, cools into the rune's colour.
 */
public class RuneShardParticle extends SingleQuadParticle {
    private final Vec3 from;
    private final Vec3 to;
    /** Two unit vectors across the way, for the corkscrew. */
    private final Vec3 side;
    private final Vec3 up;
    private final float radius;
    private final float phase;
    private final float turns;
    private final float spin;
    private final float size;
    private final float red;
    private final float green;
    private final float blue;

    protected RuneShardParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, ColorParticleOption options,
                                SpriteSet sprites) {
        super(level, x + dx, y + dy, z + dz, sprites.get(level.getRandom()));
        to = new Vec3(x, y, z);
        from = new Vec3(x + dx, y + dy, z + dz);
        Vec3 way = to.subtract(from);
        double distance = way.length();
        Vec3 axis = distance < 1.0E-4 ? new Vec3(0, 1, 0) : way.scale(1 / distance);
        Vec3 across = Math.abs(axis.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        side = axis.cross(across).normalize();
        up = side.cross(axis).normalize();
        radius = 0.18F + random.nextFloat() * 0.14F;
        phase = random.nextFloat() * Mth.TWO_PI;
        turns = (1.2F + random.nextFloat() * 0.8F) * (random.nextBoolean() ? 1 : -1);
        spin = (random.nextFloat() - 0.5F) * 0.25F;
        lifetime = (int) Mth.clamp(18 + distance * 4.5, 22, 60) + random.nextInt(8);
        size = 0.07F + random.nextFloat() * 0.035F;
        quadSize = 0;
        red = options.getRed();
        green = options.getGreen();
        blue = options.getBlue();
        roll = random.nextFloat() * Mth.TWO_PI;
        oRoll = roll;
        hasPhysics = false;
        xd = 0;
        yd = 0;
        zd = 0;
        tint(0);
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
        float t = age / (float) lifetime;
        // Slow peel-off, then a pull that quickens towards the target.
        float eased = t * t * (3 - 2 * t);
        float wind = phase + turns * Mth.TWO_PI * eased;
        // The corkscrew opens out of the glyph and closes into the target.
        float r = radius * Mth.sin(Math.min(1, eased * 1.15F) * Mth.PI);
        // A small rise as the shard leaves its stele.
        float lift = 0.35F * Mth.sin(Math.min(1, t * 2.2F) * Mth.PI) * (1 - eased);
        Vec3 along = from.lerp(to, eased);
        double cos = Mth.cos(wind) * r;
        double sin = Mth.sin(wind) * r;
        x = along.x + side.x * cos + up.x * sin;
        y = along.y + side.y * cos + up.y * sin + lift;
        z = along.z + side.z * cos + up.z * sin;
        roll += spin * (1 + 2 * eased);
        // Grows in, holds, flares as it lands and goes out.
        float grow = Math.min(1, t * 5);
        float flare = t > 0.86F ? 1 + Mth.sin((t - 0.86F) / 0.14F * Mth.PI) * 0.8F : 1;
        quadSize = size * grow * flare * (t > 0.94F ? (1 - t) / 0.06F : 1);
        float flicker = 0.78F + 0.22F * Mth.sin(age * 1.9F + phase * 3);
        setAlpha(Math.min(1, (t > 0.9F ? (1 - t) / 0.1F : 1) * flicker));
        tint(t);
    }

    /** White-hot at the stele, the rune's colour on the way, bright again at the flare. */
    private void tint(float t) {
        float white = t < 0.25F ? 1 - t / 0.25F : t > 0.86F ? (t - 0.86F) / 0.14F * 0.6F : 0;
        setColor(Mth.lerp(white, red, 1), Mth.lerp(white, green, 1), Mth.lerp(white, blue, 1));
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
            return new RuneShardParticle(level, x, y, z, dx, dy, dz, options, sprites);
        }
    }
}
