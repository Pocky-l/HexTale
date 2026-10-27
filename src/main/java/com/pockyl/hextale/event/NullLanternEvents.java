package com.pockyl.hextale.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.NullLanternBlockEntity;
import com.pockyl.hextale.runework.NullRune;

/**
 * Null Lanterns keep hostile creatures from forming by themselves within their quiet (natural and chunk spawns only:
 * spawners, eggs and commands still work), and with Hush keep phantoms off players inside it. With deep Silence
 * vibrations inside them never reach sculk.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class NullLanternEvents {
    private NullLanternEvents() {
    }

    @SubscribeEvent
    public static void onSpawnCheck(MobSpawnEvent.PositionCheck event) {
        EntitySpawnReason reason = event.getSpawnType();
        if ((reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) && event.getEntity() instanceof Enemy
                && event.getLevel() instanceof ServerLevel level
                && NullLanternBlockEntity.covering(level, event.getEntity().position(), null) != null) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void onPhantoms(PlayerSpawnPhantomsEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level
                && NullLanternBlockEntity.covering(level, event.getEntity().position(), NullRune.HUSH) != null) {
            event.setResult(PlayerSpawnPhantomsEvent.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        NullLanternBlockEntity.forget(false);
    }

    /** A lantern with deep Silence keeps vibrations inside its quiet from reaching sculk. */
    @SubscribeEvent
    public static void onVibration(net.neoforged.neoforge.event.VanillaGameEvent event) {
        if (event.getLevel() instanceof ServerLevel level && NullLanternBlockEntity.covering(level, event.getEventPosition(),
                NullRune.SILENCE, NullRune.INSULATING) != null) {
            event.setCanceled(true);
        }
    }
}
