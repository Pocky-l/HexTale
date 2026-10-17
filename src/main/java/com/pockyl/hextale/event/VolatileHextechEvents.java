package com.pockyl.hextale.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.item.UnstableHextechItem;
import com.pockyl.hextale.registry.ModItems;

import java.util.List;

/** Raw hextech caught in a blast bursts on its own instead of just being destroyed (see {@link UnstableHextechItem}). */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class VolatileHextechEvents {
    private VolatileHextechEvents() {
    }

    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<ItemEntity> orbs = event.getAffectedEntities().stream()
                .filter(entity -> entity instanceof ItemEntity item && item.isAlive() && item.getItem().is(ModItems.UNSTABLE_HEXTECH.get()))
                .map(ItemEntity.class::cast).toList();
        if (orbs.isEmpty()) {
            return;
        }
        event.getAffectedEntities().removeIf(orbs::contains);
        for (ItemEntity orb : orbs) {
            UnstableHextechItem.burst(level, orb);
        }
    }
}
