package com.pockyl.hextale.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.item.HextechSkypackItem;
import com.pockyl.hextale.item.SkypackFlight;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.SkypackRune;
import com.pockyl.hextale.runework.ToolEffects;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The server side of the Hextech Skypack: it lends its wearer the gliding attribute only while its tanks hold air, a
 * take-off from the ground launches (air spent, the jets catch with a sound), sneaking on the ground near jars refills
 * it, and a Breath rune softens walls and landings for a moment after a flight.
 */
public final class SkypackEvents {
    /** Ticks after a flight a landing still counts as part of it (Breath). */
    private static final int LANDING_TICKS = 40;
    /** The game tick each player was last seen gliding with a pack. */
    private static final Map<Player, Long> LAST_FLIGHT = new WeakHashMap<>();

    private SkypackEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkypackEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SkypackEvents::onIncomingDamage);
    }

    /** The crystal stays awake (and the pack glides) only while there is air in the tanks. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack pack = HextechSkypackItem.worn(player);
        if (pack.isEmpty()) {
            LAST_FLIGHT.remove(player);
            SkypackFlight.forget(player);
            return;
        }
        long now = level.getGameTime();
        if (SkypackFlight.tick(player, pack, now)) {
            // A take-off: the jets catch with a thump and a rush of air (their light is drawn on the wearer).
            HextechSkypackItem.launch(level, player, pack);
            Vec3 back = player.position().add(0, 0.4, 0).subtract(player.getLookAngle().scale(0.6));
            level.playSound(null, back.x, back.y, back.z, ModSounds.SKYPACK_IGNITE.get(), SoundSource.PLAYERS, 0.9F,
                    0.9F + level.getRandom().nextFloat() * 0.2F);
        }
        if (player.isFallFlying()) {
            LAST_FLIGHT.put(player, now);
        } else if (player.onGround() && player.isShiftKeyDown() && now % HextechSkypackItem.SIP_TICKS == 0) {
            HextechSkypackItem.sip(level, player, pack);
        }
    }

    /** Breath: a wall flown into or a landing right after a flight hurts less, or not at all. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        boolean wall = event.getSource().is(DamageTypes.FLY_INTO_WALL);
        if (!wall && !event.getSource().is(DamageTypeTags.IS_FALL)) {
            return;
        }
        ItemStack pack = HextechSkypackItem.worn(player);
        int breath = pack.isEmpty() ? 0 : HextechSkypackItem.strength(pack, SkypackRune.BREATH);
        Long last = LAST_FLIGHT.get(player);
        if (breath == 0 || (!player.isFallFlying() && (last == null || level.getGameTime() - last > LANDING_TICKS))) {
            return;
        }
        float cushion = HolderRune.at(SkypackRune.CUSHION, breath);
        ToolEffects.puff(level, player.position().add(0, 0.5, 0), SkypackRune.BREATH.rune(), 10, 0.5);
        if (cushion >= 1) {
            event.setCanceled(true);
        } else {
            event.setAmount(event.getAmount() * (1 - cushion));
        }
    }
}
