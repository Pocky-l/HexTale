package com.pockyl.hextale.port;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModAttachments;

/**
 * 26.x has a saddle slot on every mob ({@code EquipmentSlot.SADDLE}, guaranteed drop, shears take it off). 1.21.1 has
 * none: a cat's saddle is the attachment {@code panther_saddle}, synced for its render layer, and dropped here.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class PortSaddle {
    private PortSaddle() {
    }

    public static boolean has(Mob animal) {
        return animal.getData(ModAttachments.PANTHER_SADDLE);
    }

    public static void set(Mob animal, boolean saddled) {
        animal.setData(ModAttachments.PANTHER_SADDLE, saddled);
        PortSync.syncData(animal, ModAttachments.PANTHER_SADDLE);
    }

    /** Takes the saddle off and drops it. */
    public static void drop(Mob animal) {
        set(animal, false);
        animal.spawnAtLocation(Items.SADDLE);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob animal && !animal.level().isClientSide() && has(animal)) {
            drop(animal);
        }
    }
}
