package com.pockyl.hextale.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ImpulsePlateBlock;

/** Soft landings after an Impulse Plate's throw with Breath. */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class GadgetEvents {
    private GadgetEvents() {
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        long now = event.getEntity().level().getGameTime();
        ImpulsePlateBlock.SOFT.values().removeIf(until -> until < now);
        if (ImpulsePlateBlock.SOFT.remove(event.getEntity().getUUID()) != null) {
            event.setCanceled(true);
        }
    }
}
