package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;

/**
 * Status effects that only SHOW a state the mod keeps elsewhere: they do nothing themselves and are put on and taken off
 * by their system (the shimmer veins: {@link com.pockyl.hextale.chem.ShimmerVeins}), so a milk bucket clears them only
 * until the next check.
 */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Hextale.MOD_ID);

    /** Fed shimmer veins: the level is the number of veins, the time is how long the feeding lasts. */
    public static final DeferredHolder<MobEffect, MobEffect> SHIMMER_VEINS = EFFECTS.register("shimmer_veins",
            () -> new Shown(MobEffectCategory.BENEFICIAL, 0xC24CF0));
    /** Veins whose feeding ran out: their gifts are withheld until the next vial (no harm). */
    public static final DeferredHolder<MobEffect, MobEffect> SHIMMER_CRAVING = EFFECTS.register("shimmer_craving",
            () -> new Shown(MobEffectCategory.NEUTRAL, 0x7A6A88));

    /** Marked by a Seer of the Void: the Void's creatures nearby come for the marked one. */
    public static final DeferredHolder<MobEffect, MobEffect> VOID_MARKED = EFFECTS.register("void_marked",
            () -> new com.pockyl.hextale.thevoid.realm.Seer.Marked(0xA25CE8));

    private ModEffects() {
    }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    /** An effect with no behaviour of its own. */
    private static final class Shown extends MobEffect {
        Shown(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
