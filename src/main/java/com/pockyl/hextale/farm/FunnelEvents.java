package com.pockyl.hextale.farm;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

import com.pockyl.hextale.block.GreedyFunnelBlockEntity;

/**
 * Deaths reach the Greedy Funnels here. Both events come from one call at the moment of death, experience first: the
 * funnel covering the spot ({@link FunnelIndex}) takes the experience, then the drops, before anything is spawned.
 * Lowest priority, so the sword's runes and other mods have already changed the drops; players' deaths are never
 * swallowed.
 */
public final class FunnelEvents {
    private FunnelEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, FunnelEvents::onExperience);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, FunnelEvents::onDrops);
    }

    private static void onExperience(LivingExperienceDropEvent event) {
        LivingEntity dead = event.getEntity();
        if (event.isCanceled() || dead instanceof Player || !(dead.level() instanceof ServerLevel level)) {
            return;
        }
        GreedyFunnelBlockEntity funnel = FunnelIndex.covering(level, dead.position());
        if (funnel != null) {
            event.setDroppedExperience(funnel.swallowExperience(level, dead, event.getDroppedExperience()));
        }
    }

    private static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (event.isCanceled() || dead instanceof Player || !(dead.level() instanceof ServerLevel level)) {
            return;
        }
        GreedyFunnelBlockEntity funnel = FunnelIndex.covering(level, dead.position());
        if (funnel != null) {
            funnel.swallow(level, dead, event.getDrops());
        }
    }
}
