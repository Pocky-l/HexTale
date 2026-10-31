package com.pockyl.hextale.runework;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.item.HextechSkypackItem;
import com.pockyl.hextale.item.SkypackFlight;
import com.pockyl.hextale.network.WeaponFxPayload;
import com.pockyl.hextale.registry.ModParticles;

import java.util.List;

/**
 * The Skypack's Flamethrower: while the jets thrust after a launch, they lick out a tongue of fire behind and below the
 * wearer. Creatures inside that cone catch fire and get a small burn; the ground never does (no block is ever set
 * alight). The flame is drawn as {@link WeaponFxPayload.Kind#SCORCH}, renewed every few ticks so it reads as one stream,
 * with a soft crackle now and then.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class SkypackFlames {
    /** Ticks between two tongues of flame drawn. */
    public static final int DRAW_TICKS = 4;
    /** Ticks between two scorches of what is in the cone. */
    public static final int SCORCH_TICKS = 10;
    /** Ticks between two crackles. */
    private static final int CRACKLE_TICKS = 20;
    /** The burn of one scorch. */
    public static final float BURN = 2;
    /** Half-width of the cone at the jets, and how much it widens per block. */
    private static final double MOUTH = 0.5;
    private static final double SPREAD = 0.45;
    private static final int FLAME = 0xFF8A3A;

    private SkypackFlames() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack pack = HextechSkypackItem.worn(player);
        int stars = pack.isEmpty() ? 0 : HextechSkypackItem.strength(pack, SkypackRune.FLAMETHROWER);
        long now = level.getGameTime();
        if (stars <= 0 || !SkypackFlight.boosting(player, now)) {
            return;
        }
        long tick = now + player.getId();
        Vec3 jets = jets(player);
        Vec3 way = wake(player);
        double reach = HolderRune.at(SkypackRune.FLAME_REACH, stars);
        if (tick % DRAW_TICKS == 0) {
            WeaponFxPayload.send(level, WeaponFxPayload.Kind.SCORCH, jets, jets.add(way.scale(reach)), FLAME, (float) (MOUTH + reach * SPREAD));
        }
        if (tick % CRACKLE_TICKS == 0) {
            level.playSound(null, jets.x, jets.y, jets.z, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.25F,
                    0.9F + level.getRandom().nextFloat() * 0.3F);
        }
        if (tick % SCORCH_TICKS == 0) {
            scorch(level, player, jets, way, reach, HolderRune.at(SkypackRune.BURN_TICKS, stars));
        }
    }

    /** Where the jets are: on the wearer's back. */
    public static Vec3 jets(Player player) {
        return player.position().add(0, player.getBbHeight() * 0.5, 0).subtract(player.getLookAngle().scale(0.5));
    }

    /** The way the flame goes: back from where the wearer flies, and a little down. */
    public static Vec3 wake(Player player) {
        return player.getLookAngle().scale(-1).add(0, -0.35, 0).normalize();
    }

    /** Sets alight and burns every creature in the cone; returns how many it touched. */
    public static int scorch(ServerLevel level, Player player, Vec3 jets, Vec3 way, double reach, int burn) {
        Vec3 end = jets.add(way.scale(reach));
        List<LivingEntity> touched = level.getEntitiesOfClass(LivingEntity.class, new AABB(jets, end).inflate(MOUTH + reach * SPREAD),
                entity -> ToolEffects.mayStrike(entity, player) && inCone(entity, jets, way, reach));
        if (touched.isEmpty()) {
            return 0;
        }
        ToolEffects.quietly(() -> {
            for (LivingEntity entity : touched) {
                entity.igniteForTicks(Math.max(entity.getRemainingFireTicks(), burn));
                entity.hurtServer(level, level.damageSources().source(DamageTypes.IN_FIRE, player), BURN);
            }
        });
        for (LivingEntity entity : touched) {
            // Flames lick up the creature itself.
            level.sendParticles(ModParticles.flame(ModParticles.FIRE), entity.getX(), entity.getY() + entity.getBbHeight() * 0.4, entity.getZ(),
                    6, entity.getBbWidth() * 0.35, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.35, 0.02);
        }
        ToolEffects.puff(level, touched.getFirst().position().add(0, touched.getFirst().getBbHeight() * 0.6, 0), SkypackRune.FLAMETHROWER.rune(), 3, 0.3);
        return touched.size();
    }

    private static boolean inCone(LivingEntity entity, Vec3 jets, Vec3 way, double reach) {
        Vec3 to = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(jets);
        double along = to.dot(way);
        if (along <= 0 || along > reach + entity.getBbWidth() * 0.5) {
            return false;
        }
        double across = to.subtract(way.scale(along)).length();
        return across <= MOUTH + along * SPREAD + entity.getBbWidth() * 0.5;
    }
}
