package com.pockyl.hextale.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import com.pockyl.hextale.port.client.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import com.pockyl.hextale.port.client.QuadParticleRenderState;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;

/**
 * The base of the mod's particles: a sprite from a set, a size and an opacity that follow the particle's life smoothly
 * (worked out per frame from the partial tick, not stepped per tick), translucent, glowing unless told otherwise.
 * Subclasses shape the life with {@link #sizeAt} and {@link #alphaAt} (t = 0 at birth, 1 at death).
 */
public abstract class HexParticle extends SingleQuadParticle {
    protected final SpriteSet sprites;
    protected float size;
    protected float opacity = 1;
    /** Seconds of the particle's own wobble, random per particle so a cloud of them never moves in step. */
    protected final float phase;

    protected HexParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, float size) {
        super(level, x, y, z, sprites.get(level.getRandom()));
        this.sprites = sprites;
        this.size = size;
        this.quadSize = size;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.phase = random.nextFloat() * Mth.TWO_PI;
        hasPhysics = false;
        gravity = 0;
        friction = 1;
    }

    /** The option's colour, each channel shifted a little so a stream of particles is never one flat colour. */
    protected void tint(ColorParticleOption option, float jitter) {
        float shade = 1 - jitter + random.nextFloat() * jitter * 2;
        setColor(Mth.clamp(option.getRed() * shade, 0, 1), Mth.clamp(option.getGreen() * shade, 0, 1),
                Mth.clamp(option.getBlue() * shade, 0, 1));
    }

    /** Size scale at {@code t} (0-1 of the life). */
    protected float sizeAt(float t) {
        return 1;
    }

    /** Opacity at {@code t} (0-1 of the life). */
    protected float alphaAt(float t) {
        return 1;
    }

    /** Whether it gives its own light (full bright) or takes the world's. */
    protected boolean glows() {
        return true;
    }

    /** The sprite {@code frame} of {@code frames} (the set holds exactly that many). */
    protected void frame(int frame, int frames) {
        setSprite(sprites.get(Math.floorMod(frame, frames), frames - 1));
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        float t = lifetime <= 0 ? 1 : Mth.clamp((age + partialTick) / lifetime, 0, 1);
        quadSize = size * sizeAt(t);
        alpha = Mth.clamp(opacity * alphaAt(t), 0, 1);
        if (alpha <= 0.004F || quadSize <= 0.001F) {
            return;
        }
        super.extract(state, camera, partialTick);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return glows() ? LightTexture.FULL_BRIGHT : super.getLightCoords(partialTick);
    }

    /** 0 -> 1 with a soft start and end. */
    protected static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** 1 until {@code from}, then smoothly down to 0 at the end of the life. */
    protected static float fadeOut(float t, float from) {
        return t <= from ? 1 : 1 - smooth((t - from) / (1 - from));
    }

    /** 0 at birth, smoothly up to 1 at {@code until}. */
    protected static float fadeIn(float t, float until) {
        return t >= until ? 1 : smooth(t / until);
    }
}
