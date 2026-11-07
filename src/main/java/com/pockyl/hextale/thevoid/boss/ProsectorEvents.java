package com.pockyl.hextale.thevoid.boss;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;


/** The game hook of the fight: the hurt gate's last word on a hit. */
public final class ProsectorEvents {
    private ProsectorEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ProsectorEvents::onIncomingDamage);
    }

    /** Lowest priority: whatever runes and other mods add to a hit must still meet the cap. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Prosector boss && !boss.level().isClientSide()) {
            event.setAmount(boss.gated(event.getSource(), event.getAmount()));
        }
    }
}
