package com.pockyl.hextale.event;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModItems;

/** The Researcher's Robe: arcane cloth and a high collar keep sparks, flames and blasts off its wearer. */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class ResearcherRobeEvents {
    /** Share of fire, lightning and blast damage that still gets through the robe. */
    public static final float SPARKS_THROUGH = 0.75F;

    private ResearcherRobeEvents() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (wearsRobe(event.getEntity()) && isSparks(event.getSource())) {
            event.setAmount(event.getAmount() * SPARKS_THROUGH);
        }
    }

    public static boolean wearsRobe(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.RESEARCHER_ROBE.get());
    }

    public static boolean isSparks(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_LIGHTNING);
    }
}
