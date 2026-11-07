package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.CameraRenderState;
import com.pockyl.hextale.port.client.LevelRenderState;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.pockyl.hextale.port.client.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionfc;

import java.util.ArrayList;
import java.util.List;

import com.pockyl.hextale.entity.BiterGrenade;
import com.pockyl.hextale.network.BiterBlastPayload;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;

/**
 * A Biter Grenade's burst, all from one word of the server, staged here tick by tick: a bright core flash, a billowing
 * green-yellow fireball, pieces of the jaw flying out and falling, acid-green smoke rising and thinning, and a soft shake
 * of the view that fades with distance and follows the game's screen effects setting.
 */
public final class BiterBlastFx {
    private static final int LASTS = 38;
    private static final int FLASH_TICKS = 5;
    private static final int FIREBALL_TICKS = 14;
    private static final int FIRE_PUFFS = 18;
    private static final float HEART_TICKS = 9;
    private static final float FIRE_REACH = 2.6F;
    private static final int SMOKE_PUFFS = 16;
    private static final int SMOKE_FROM = 3;
    /** Ticks between one puff of smoke and the next, so the column goes on rising after the fire. */
    private static final float SMOKE_EVERY = 1.5F;
    private static final int SHARDS = 18;
    private static final int SHAKE_TICKS = 14;
    private static final float SHAKE_RANGE = 28;
    private static final float SHAKE_DEGREES = 2.2F;
    private static final int WHITE = 0xFFFFE8;
    private static final int YELLOW = 0xF0E870;
    private static final int GREEN = 0x8CE050;
    private static final int ACID = 0x6E9A38;
    private static final List<Blast> BLASTS = new ArrayList<>();

    private BiterBlastFx() {
    }

    /** One big sprite of the fireball or the smoke: which way it goes, how far, how big, and how it is turned. */
    private record Puff(Vec3 way, float reach, float size, float roll) {
        static Puff[] many(int count, RandomSource random) {
            Puff[] puffs = new Puff[count];
            for (int i = 0; i < count; i++) {
                puffs[i] = new Puff(away(random), 0.5F + 0.5F * random.nextFloat(), 0.7F + 0.6F * random.nextFloat(), random.nextFloat() * Mth.TWO_PI);
            }
            return puffs;
        }
    }

    private record Blast(Vec3 at, long start, RandomSource random, Puff[] fire, Puff[] smoke) {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BiterBlastFx::tick);
        NeoForge.EVENT_BUS.addListener(BiterBlastFx::submit);
        NeoForge.EVENT_BUS.addListener(BiterBlastFx::shake);
    }

    public static void accept(BiterBlastPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            RandomSource random = RandomSource.create();
            BLASTS.add(new Blast(payload.at(), level.getGameTime(), random, Puff.many(FIRE_PUFFS, random), Puff.many(SMOKE_PUFFS, random)));
        }
    }

    private static void tick(ClientTickEvent.Post event) {
        if (BLASTS.isEmpty()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            BLASTS.clear();
            return;
        }
        BLASTS.removeIf(blast -> {
            int age = (int) (level.getGameTime() - blast.start());
            if (age < 0 || age > LASTS) {
                return true;
            }
            stage(level, blast, age);
            return false;
        });
    }

    private static void stage(ClientLevel level, Blast blast, int age) {
        Vec3 at = blast.at();
        RandomSource random = blast.random();
        if (age == 0) {
            level.addParticle(ModParticles.ring(GREEN), at.x, at.y, at.z, BiterGrenade.RADIUS + 1, 0, 0);
            ItemParticleOption shard = new ItemParticleOption(ParticleTypes.ITEM, new net.minecraft.world.item.ItemStack(ModItems.BITER_GRENADE.get()));
            for (int i = 0; i < SHARDS; i++) {
                Vec3 out = away(random);
                level.addParticle(shard, at.x, at.y, at.z, out.x * 0.42, 0.18 + Math.abs(out.y) * 0.4, out.z * 0.42);
            }
        }
        if (age < FIREBALL_TICKS / 2) {
            // Embers thrown out of the fireball; its body is drawn in submit.
            for (int i = 0; i < 5; i++) {
                Vec3 out = away(random);
                level.addParticle(ModParticles.flame(YELLOW), at.x + out.x, at.y + out.y, at.z + out.z, out.x * 0.14, out.y * 0.14 + 0.03, out.z * 0.14);
            }
        }
    }

    /** The fireball: big puffs that billow out and cool from yellow to green. */
    private static void fire(List<float[]> quads, Blast blast, float age, Vec3 at) {
        float left = 1 - age / FIREBALL_TICKS;
        if (left <= 0) {
            return;
        }
        float reach = FIRE_REACH * (1 - (float) Math.exp(-age / 3.5F));
        int frame = Mth.clamp((int) (age / FIREBALL_TICKS * HexFx.VAPOUR_FRAMES), 0, HexFx.VAPOUR_FRAMES - 1);
        float alpha = (float) Math.pow(left, 1.3);
        int cool = mix(YELLOW, GREEN, Mth.clamp(age / 12, 0, 1));
        for (Puff puff : blast.fire()) {
            Vec3 p = at.add(puff.way().scale(reach * puff.reach()));
            quads.add(HexFx.quad(p.x, p.y, p.z, puff.size() * (0.7F + age * 0.11F), frame, puff.roll() + age * 0.05F, alpha, cool));
        }
        // Its hot heart, drawn over the rest, stays white and yellow while the outside cools.
        int hot = mix(WHITE, YELLOW, Mth.clamp(age / 6, 0, 1));
        float heart = Math.max(0, 1 - age / HEART_TICKS);
        for (int i = 0; i < blast.fire().length; i += 2) {
            Puff puff = blast.fire()[i];
            Vec3 p = at.add(puff.way().scale(reach * puff.reach() * 0.45));
            quads.add(HexFx.quad(p.x, p.y, p.z, puff.size() * (0.6F + age * 0.08F), frame, puff.roll(), heart, hot));
        }
    }

    /** The smoke: puffs let go one after another, rising, spreading and thinning until nothing is left. */
    private static void smoke(List<float[]> quads, Blast blast, float age, Vec3 at) {
        Puff[] puffs = blast.smoke();
        for (int i = 0; i < puffs.length; i++) {
            float born = SMOKE_FROM + i * SMOKE_EVERY;
            float life = (age - born) / (LASTS - born);
            if (life < 0 || life >= 1) {
                continue;
            }
            Puff puff = puffs[i];
            Vec3 p = at.add(puff.way().x * (0.6 + 1.5 * life), 0.3 + 3 * life, puff.way().z * (0.6 + 1.5 * life));
            int frame = Mth.clamp((int) (life * HexFx.VAPOUR_FRAMES), 0, HexFx.VAPOUR_FRAMES - 1);
            float alpha = 0.6F * (1 - life) * (1 - life) * Math.min(1, (age - born) / 3);
            quads.add(HexFx.quad(p.x, p.y, p.z, puff.size() * (0.8F + 1.5F * life), frame, puff.roll(), alpha, ACID));
        }
    }

    private static int mix(int from, int to, float share) {
        int r = (int) Mth.lerp(share, from >> 16 & 255, to >> 16 & 255);
        int g = (int) Mth.lerp(share, from >> 8 & 255, to >> 8 & 255);
        int b = (int) Mth.lerp(share, from & 255, to & 255);
        return r << 16 | g << 8 | b;
    }

    private static Vec3 away(RandomSource random) {
        return new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize();
    }

    private static void submit(SubmitCustomGeometryEvent event) {
        if (BLASTS.isEmpty()) {
            return;
        }
        LevelRenderState state = event.getLevelRenderState();
        CameraRenderState camera = state.cameraRenderState;
        float now = state.gameTime + state.worldPartialTicks;
        Quaternionfc facing = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        PoseStack pose = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        List<float[]> quads = new ArrayList<>();
        for (Blast blast : BLASTS) {
            float age = now - blast.start();
            if (age < 0) {
                continue;
            }
            Vec3 at = blast.at().subtract(camera.pos);
            smoke(quads, blast, age, at);
            fire(quads, blast, age, at);
            if (age < FLASH_TICKS) {
                float left = 1 - age / FLASH_TICKS;
                pose.pushPose();
                pose.translate(at.x, at.y, at.z);
                HexFx.sprite(pose, collector, facing, HexFx.HALO, 7 + age * 2, WHITE, left * left);
                HexFx.sprite(pose, collector, facing, HexFx.HALO, 3.5F + age * 1.2F, WHITE, left);
                pose.popPose();
            }
        }
        HexFx.sheet(pose, collector, facing, HexFx.VAPOUR, HexFx.VAPOUR_FRAMES, quads);
    }

    private static void shake(ViewportEvent.ComputeCameraAngles event) {
        if (BLASTS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float now = minecraft.level.getGameTime() + (float) event.getPartialTick();
        Vec3 view = event.getCamera().getPosition();
        float strength = 0;
        for (Blast blast : BLASTS) {
            float left = 1 - (now - blast.start()) / SHAKE_TICKS;
            float near = 1 - Mth.clamp((float) blast.at().distanceTo(view) / SHAKE_RANGE, 0, 1);
            if (left > 0) {
                strength = Math.max(strength, near * near * left * left);
            }
        }
        if (strength <= 0.001F) {
            return;
        }
        float degrees = strength * SHAKE_DEGREES * minecraft.options.screenEffectScale().get().floatValue();
        event.setPitch(event.getPitch() + Mth.sin(now * 2.7F) * degrees);
        event.setYaw(event.getYaw() + Mth.sin(now * 3.3F + 1) * degrees * 0.7F);
        event.setRoll(event.getRoll() + Mth.sin(now * 2.1F + 2) * degrees * 0.6F);
    }
}
