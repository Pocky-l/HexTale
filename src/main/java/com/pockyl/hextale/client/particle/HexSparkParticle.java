package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A crackle of hextech lightning: flies off with its velocity, brakes hard, and flickers between forks of light (a
 * new sprite and turn every tick) - white-hot at first, its colour after. Short-lived, glowing.
 */
public class HexSparkParticle extends HexParticle {
    private final float red;
    private final float green;
    private final float blue;

    protected HexSparkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, ColorParticleOption option,
                               SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites, 0.06F + level.getRandom().nextFloat() * 0.07F);
        lifetime = 5 + random.nextInt(7);
        hasPhysics = true;
        gravity = 0.15F;
        friction = 0.8F;
        red = option.getRed();
        green = option.getGreen();
        blue = option.getBlue();
        roll = random.nextFloat() * Mth.TWO_PI;
        oRoll = roll;
        setColor(1, 1, 1);
    }

    @Override
    public void tick() {
        super.tick();
        oRoll = roll;
        roll = random.nextFloat() * Mth.TWO_PI;
        setSprite(sprites.get(random));
        float t = Math.min(1, age / (float) lifetime * 2.5F);
        setColor(Mth.lerp(t, 1, red), Mth.lerp(t, 1, green), Mth.lerp(t, 1, blue));
        opacity = 0.65F + random.nextFloat() * 0.35F;
    }

    @Override
    protected float sizeAt(float t) {
        return 1 - 0.5F * t;
    }

    @Override
    protected float alphaAt(float t) {
        return fadeOut(t, 0.5F);
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption option, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            RandomSource random = level.getRandom();
            return new HexSparkParticle(level, x, y, z, xd, yd, zd, option, sprites);
        }
    }
}
