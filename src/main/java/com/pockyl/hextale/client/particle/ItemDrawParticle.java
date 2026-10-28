package com.pockyl.hextale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModParticles;

/**
 * A fragment of a pedestal item drawn into the altar: a piece of the item's own texture (like the crumbs of a broken
 * item) that peels off and rises out of the item, swings over in its own high arc - the fragments of one item fan out,
 * never one straight line - and is sucked in faster and faster, winding round the altar's item in a closing whirl
 * before it sinks in. On the way it takes on the hextech light and now and then sheds a glint. Type
 * {@code hextale:item_draw} ({@code ModParticles.itemDraw}): spawned AT the altar's item with the offset to its start as
 * velocity, like the flow motes.
 */
public class ItemDrawParticle extends BreakingItemParticle {
    /** Share of the life spent peeling off and rising out of the item. */
    private static final float PEEL = 0.2F;
    private static final int[] TRAIL = {0xF0D27A, 0x9FE6FF};

    private final Vec3 from;
    private final Vec3 lifted;
    private final Vec3 control1;
    private final Vec3 control2;
    private final Vec3 to;
    private final float whirlStart;
    private final float whirlTurns;
    private final float whirlRadius;
    private final float spin;
    private final float size;

    protected ItemDrawParticle(ClientLevel level, Vec3 from, Vec3 to, TextureAtlasSprite sprite) {
        super(level, from.x, from.y, from.z, sprite);
        this.from = from;
        this.to = to;
        Vec3 flat = new Vec3(to.x - from.x, 0, to.z - from.z);
        double distance = flat.length();
        Vec3 toward = distance < 1.0E-4 ? new Vec3(1, 0, 0) : flat.scale(1 / distance);
        Vec3 side = new Vec3(-toward.z, 0, toward.x);
        // Peels off upwards and a little back and aside, as if torn out of the item.
        float scatter = (random.nextFloat() - 0.5F) * 0.5F;
        lifted = from.add(0, 0.3 + random.nextFloat() * 0.25, 0).add(toward.scale(-0.12)).add(side.scale(scatter * 0.6));
        // Its own arc: high over the circle, bowing to one side; the second control falls on the altar from above.
        double rise = 0.7 + distance * 0.18 + random.nextFloat() * 0.6;
        double bow = (random.nextFloat() - 0.5F) * (0.6 + distance * 0.35);
        control1 = lifted.add(toward.scale(distance * 0.3)).add(side.scale(bow)).add(0, rise, 0);
        control2 = to.add(toward.scale(-distance * 0.15)).add(side.scale(-bow * 0.5)).add(0, rise * 0.55, 0);
        whirlStart = random.nextFloat() * Mth.TWO_PI;
        whirlTurns = (1.0F + random.nextFloat() * 0.7F) * (random.nextBoolean() ? 1 : -1);
        whirlRadius = 0.3F + random.nextFloat() * 0.2F;
        spin = (random.nextFloat() - 0.5F) * 0.25F;
        size = quadSize * (0.6F + random.nextFloat() * 0.3F);
        lifetime = (int) Mth.clamp(22 + distance * 3.2, 24, 48) + random.nextInt(6);
        gravity = 0;
        hasPhysics = false;
        roll = random.nextFloat() * Mth.TWO_PI;
        oRoll = roll;
        xd = yd = zd = 0;
    }


    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        oRoll = roll;
        if (age++ >= lifetime) {
            if (random.nextInt(3) == 0) {
                level.addParticle(ModParticles.glint(ModParticles.HEX_LIGHT), to.x, to.y, to.z, 0, 0.01, 0);
            }
            remove();
            return;
        }
        float t = age / (float) lifetime;
        Vec3 at;
        float charge;
        if (t < PEEL) {
            float k = t / PEEL;
            k = k * k * (3 - 2 * k);
            at = from.lerp(lifted, k).add(0, Mth.sin(age * 0.9F) * 0.01, 0);
            charge = 0;
        } else {
            float u = (t - PEEL) / (1 - PEEL);
            // Slow over the top, sucked in faster and faster at the end.
            float e = u * u * (2.2F - 1.2F * u);
            at = bezier(e);
            // The closing whirl round the altar's item: opens as it arrives, shrinks to nothing as it sinks in.
            float w = Mth.clamp((e - 0.55F) / 0.45F, 0, 1);
            float angle = whirlStart + whirlTurns * Mth.TWO_PI * w * w;
            float r = whirlRadius * Mth.sin(w * Mth.PI) * (1 - 0.3F * w);
            at = at.add(Mth.cos(angle) * r, Mth.sin(angle * 2) * r * 0.25F, Mth.sin(angle) * r);
            charge = w;
            if (u < 0.9F && age % 3 == 0 && random.nextInt(2) == 0) {
                level.addParticle(ModParticles.glint(TRAIL[random.nextInt(TRAIL.length)]), at.x, at.y, at.z, 0, 0.004, 0);
            }
        }
        x = at.x;
        y = at.y;
        z = at.z;
        roll += spin * (1 + 3 * t);
        // Takes on the hextech light as it is drawn in.
        setColor(Mth.lerp(charge, 1, 0.72F), Mth.lerp(charge, 1, 0.95F), 1);
    }

    private Vec3 bezier(float e) {
        float a = 1 - e;
        return lifted.scale(a * a * a).add(control1.scale(3 * a * a * e)).add(control2.scale(3 * a * e * e)).add(to.scale(e * e * e));
    }

    @Override
    public float getQuadSize(float partialTick) {
        float t = Mth.clamp((age + partialTick) / lifetime, 0, 1);
        float grow = t < PEEL ? 0.6F + 0.4F * t / PEEL : 1;
        float sink = t > 0.85F ? 1 - 0.8F * (t - 0.85F) / 0.15F : 1;
        return size * grow * sink;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    public static class Provider extends BreakingItemParticle.ItemParticleProvider<ItemParticleOption> {
        @Override
        public Particle createParticle(ItemParticleOption option, ClientLevel level, double x, double y, double z, double dx, double dy,
                                       double dz, RandomSource random) {
            Vec3 to = new Vec3(x, y, z);
            return new ItemDrawParticle(level, to.add(dx, dy, dz), to, getSprite(option.getItem(), level, random));
        }
    }
}
