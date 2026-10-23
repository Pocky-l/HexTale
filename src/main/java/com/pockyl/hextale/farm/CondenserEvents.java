package com.pockyl.hextale.farm;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import com.pockyl.hextale.block.XpCondenserBlockEntity;

/**
 * Orbs near an Experience Condenser never appear: the moment one is about to join the level, the condenser covering
 * the spot ({@link XpCondenserBlockEntity#INDEX}) drinks it - all of it, or what fits, the rest staying a smaller orb.
 * Orbs read back from a saved chunk are left as they lie.
 */
public final class CondenserEvents {
    private CondenserEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, CondenserEvents::onJoin);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.isCanceled() || event.loadedFromDisk() || !(event.getEntity() instanceof ExperienceOrb orb)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        XpCondenserBlockEntity condenser = XpCondenserBlockEntity.INDEX.covering(level, orb.position());
        if (condenser == null) {
            return;
        }
        int taken = condenser.fill(level, orb.getValue(), orb.position());
        if (taken >= orb.getValue()) {
            event.setCanceled(true);
        } else if (taken > 0) {
            orb.setValue(orb.getValue() - taken);
        }
    }
}
