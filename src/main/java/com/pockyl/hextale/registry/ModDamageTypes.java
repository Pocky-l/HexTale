package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.Hextale;

/** The mod's damage types; each is a data file in {@code data/hextale/damage_type}. */
public final class ModDamageTypes {
    /** Chemtech fumes: go through armour, push nobody and eat the undead too, which poison cannot. */
    public static final ResourceKey<DamageType> MIASMA = ResourceKey.create(Registries.DAMAGE_TYPE, Hextale.id("miasma"));

    private ModDamageTypes() {
    }

    public static DamageSource miasma(Level level) {
        return level.damageSources().source(MIASMA);
    }
}
