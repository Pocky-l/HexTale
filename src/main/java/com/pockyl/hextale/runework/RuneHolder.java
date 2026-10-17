package com.pockyl.hextale.runework;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Things that take runes like enchantments at the Hextech Altar: what each one takes, how many, which runes exclude each
 * other and what burning one in costs. The runes live on the item as a {@link PickaxeRunes} component.
 */
public enum RuneHolder {
    PICKAXE,
    SMELTER;

    @Nullable
    public static RuneHolder of(ItemStack stack) {
        if (HextechPickaxeItem.is(stack)) {
            return PICKAXE;
        }
        return stack.is(ModItems.HEX_SMELTER.get()) ? SMELTER : null;
    }

    public DataComponentType<PickaxeRunes> component() {
        return this == PICKAXE ? ModDataComponents.PICKAXE_RUNES.get() : ModDataComponents.SMELTER_RUNES.get();
    }

    public PickaxeRunes runes(ItemStack stack) {
        return stack.getOrDefault(component(), PickaxeRunes.EMPTY);
    }

    public void set(ItemStack stack, PickaxeRunes runes) {
        if (runes.isEmpty()) {
            stack.remove(component());
        } else {
            stack.set(component(), runes);
        }
    }

    public int slots() {
        return this == PICKAXE ? PickaxeRunes.SLOTS : SmelterRune.SLOTS;
    }

    public boolean takes(ComplexRune rune) {
        return this == PICKAXE ? PickaxeRune.of(rune) != null : SmelterRune.of(rune) != null;
    }

    /** Whether the two runes cannot share one holder (only pickaxe runes clash). */
    public boolean clashes(ComplexRune a, ComplexRune b) {
        if (this != PICKAXE) {
            return false;
        }
        PickaxeRune x = PickaxeRune.of(a);
        PickaxeRune y = PickaxeRune.of(b);
        return x != null && y != null && x.clashes(y);
    }

    /** Essence for burning the rune in with these stars. */
    public EssenceMix cost(ComplexRune rune, int stars) {
        return this == PICKAXE ? Objects.requireNonNull(PickaxeRune.of(rune)).cost(stars) : Objects.requireNonNull(SmelterRune.of(rune)).cost(stars);
    }

    /** Suffix of the altar's refusal keys: the pickaxe keeps the old keys. */
    public String refusalSuffix() {
        return this == PICKAXE ? "" : "_smelter";
    }
}
