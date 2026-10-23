package com.pockyl.hextale.farm;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import com.pockyl.hextale.block.FieldWardBlockEntity;

/**
 * What a Field Ward forbids. Natural monster spawns within its watch fail at the placement check - before the creature
 * is even made, so a warded base costs the spawner nothing and the mob cap is left to the farms. Spawners, structures
 * and summons are not natural and stay as they are. Nothing tramples farmland it watches.
 */
public final class WardEvents {
    private WardEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(WardEvents::onPlacement);
        NeoForge.EVENT_BUS.addListener(WardEvents::onTrample);
    }

    private static void onPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getSpawnType() != EntitySpawnReason.NATURAL || event.getEntityType().getCategory() != MobCategory.MONSTER) {
            return;
        }
        ServerLevel level = event.getLevel().getLevel();
        if (FieldWardBlockEntity.INDEX.covering(level, Vec3.atBottomCenterOf(event.getPos())) != null) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    private static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && FieldWardBlockEntity.INDEX.covering(level, Vec3.atCenterOf(event.getPos())) != null) {
            event.setCanceled(true);
        }
    }
}
