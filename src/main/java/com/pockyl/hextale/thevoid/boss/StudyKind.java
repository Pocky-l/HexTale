package com.pockyl.hextale.thevoid.boss;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.entity.BiterGrenade;
import com.pockyl.hextale.entity.ThrownChemVial;

/** The four ways to hurt it, read from the damage source; each is studied apart and answered with its own mimic attack. */
public enum StudyKind {
    /** Direct melee: answered by Riposte. */
    BLADE(ProsectorState.RIPOSTE, 0xE8E0D0),
    /** Anything shot: answered by Return Volley. */
    SHOT(ProsectorState.VOLLEY, 0x8CE0C0),
    /** Explosions, thrown vials and grenades: answered by Returned Charge. */
    BLAST(ProsectorState.CHARGE, 0xFFB050),
    /** Everything else, magic, fire, lightning, chem: answered by Inverted Circle. */
    RUNE(ProsectorState.CIRCLE, 0xB070F0);

    private static final StudyKind[] ALL = values();
    private final ProsectorState mimic;
    private final int color;

    StudyKind(ProsectorState mimic, int color) {
        this.mimic = mimic;
        this.color = color;
    }

    public ProsectorState mimic() {
        return mimic;
    }

    public int color() {
        return color;
    }

    public static StudyKind byId(int id) {
        return ALL[Math.floorMod(id, ALL.length)];
    }

    public static StudyKind of(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (source.is(DamageTypeTags.IS_EXPLOSION) || direct instanceof BiterGrenade || direct instanceof ThrownChemVial) {
            return BLAST;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return SHOT;
        }
        boolean blow = source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
        return blow && source.isDirect() ? BLADE : RUNE;
    }

    /** The thing that did it, to be held up in the forceps: what hit, what flew, what was thrown. */
    public ItemStack copyFrom(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (this != BLADE && direct instanceof ItemSupplier thrown) {
            return thrown.getItem().copyWithCount(1);
        }
        ItemStack weapon = source.getWeaponItem();
        if (weapon != null && !weapon.isEmpty()) {
            return weapon.copyWithCount(1);
        }
        return source.getEntity() instanceof LivingEntity attacker ? attacker.getMainHandItem().copyWithCount(1) : ItemStack.EMPTY;
    }
}
