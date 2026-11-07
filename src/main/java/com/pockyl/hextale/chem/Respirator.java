package com.pockyl.hextale.chem;

import com.pockyl.hextale.registry.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The respirator built into the chem-steel helmet: while the worn tank holds fuel its wearer breathes under water, for
 * one action per {@link #SIP_TICKS} ticks there. The ticks add up over dives, so short dips pay as well. Breathing is
 * decided on the server only; the bubbles from the filters are {@code ChemGearClient}'s.
 */
public final class Respirator {
    /** Ticks under water one action of fuel pays for. */
    public static final int SIP_TICKS = 200;
    /** Ticks each wearer has breathed under water since the last action spent (server thread only). */
    private static final Map<LivingEntity, Integer> BREATHED = new WeakHashMap<>();

    private Respirator() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Respirator::onBreathe);
    }

    /** Whether the entity wears the helmet and has fuel for it. */
    public static boolean works(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CHEM_STEEL_HELMET.get()) && ChemGear.power(entity) != null;
    }

    private static void onBreathe(LivingBreatheEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.canBreathe() || !works(entity)) {
            return;
        }
        event.setCanBreathe(true);
        if (BREATHED.merge(entity, 1, Integer::sum) >= SIP_TICKS) {
            BREATHED.remove(entity);
            ChemGear.spend(entity, 1);
        }
    }
}
