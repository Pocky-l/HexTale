package com.pockyl.hextale.thevoid.boss;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModDamageTypes;

/**
 * Every hit of the fight. A plain hit would stop mattering at the top of the mod's ladder (Bulwark levels 3 and 4 sit at
 * the game's 80% armour cap), so the armour and Protection are worked out here on the table's damage, and
 * {@link ProsectorConfig#PIERCE} of it is added on top. The damage type then skips the armour steps that were already done.
 */
public final class ProsectorDamage {
    private ProsectorDamage() {
    }

    /** What a hit of {@code damage} really costs {@code victim}, before Resistance. */
    public static float cost(ServerLevel level, LivingEntity victim, DamageSource source, float damage) {
        float toughness = (float) victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float armoured = CombatRules.getDamageAfterAbsorb(victim, damage, source, victim.getArmorValue(), toughness);
        float enchanted = EnchantmentHelper.getDamageProtection(level, victim, source);
        return CombatRules.getDamageAfterMagicAbsorb(armoured, enchanted) + damage * ProsectorConfig.PIERCE.get().floatValue();
    }

    public static boolean strike(ServerLevel level, LivingEntity victim, Entity by, float damage) {
        DamageSource source = ModDamageTypes.dissection(level, by);
        return victim.hurtServer(level, source, cost(level, victim, source, damage));
    }

    /** Sets a creature moving, on the server and, for a player, on their own client too. */
    public static void throwAt(LivingEntity victim, Vec3 speed) {
        victim.setDeltaMovement(speed);
        victim.needsSync = true;
        victim.hurtMarked = victim instanceof Player;
    }
}
