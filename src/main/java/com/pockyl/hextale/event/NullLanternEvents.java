package com.pockyl.hextale.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
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
 * spawners, eggs and commands still work), and with Hush keep phantoms off players inside it. Silence Wards with
 * Insulator swallow vibrations here too.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class NullLanternEvents {
    private NullLanternEvents() {
    }

    @SubscribeEvent
    public static void onSpawnCheck(MobSpawnEvent.PositionCheck event) {
        MobSpawnType reason = event.getSpawnType();
        if ((reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION) && event.getEntity() instanceof Enemy
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

    /** A Silence Ward with Insulator keeps vibrations inside it from reaching sculk. */
    @SubscribeEvent
    public static void onVibration(net.neoforged.neoforge.event.VanillaGameEvent event) {
        if (event.getLevel() instanceof ServerLevel level && com.pockyl.hextale.block.SilenceWardBlock.Ward.covering(level, event.getEventPosition(),
                com.pockyl.hextale.runework.SilenceRune.INSULATOR) != null) {
            event.setCanceled(true);
        }
    }
}
