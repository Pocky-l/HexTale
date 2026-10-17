package com.pockyl.hextale.runework;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Things that take runes like enchantments at the Hextech Altar: what each one takes, how many, which runes exclude each
 * other and what burning one in costs. The runes live on the item as a {@link PickaxeRunes} component.
 */
public enum RuneHolder {
    PICKAXE,
    SMELTER,
    /** The hex pylons share one set of runes. */
    PYLON;

    @Nullable
    public static RuneHolder of(ItemStack stack) {
        if (HextechPickaxeItem.is(stack)) {
            return PICKAXE;
        }
        if (stack.is(ModItems.HEX_SMELTER.get())) {
            return SMELTER;
        }
        return ModItems.isPylon(stack) ? PYLON : null;
    }

    public DataComponentType<PickaxeRunes> component() {
        return switch (this) {
            case PICKAXE -> ModDataComponents.PICKAXE_RUNES.get();
            case SMELTER -> ModDataComponents.SMELTER_RUNES.get();
            case PYLON -> ModDataComponents.PYLON_RUNES.get();
        };
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
        return switch (this) {
            case PICKAXE -> PickaxeRunes.SLOTS;
            case SMELTER -> SmelterRune.SLOTS;
            case PYLON -> PylonRune.SLOTS;
        };
    }

    /** Every rune it takes, in the order the book and JEI show them. */
    public List<ComplexRune> takes() {
        return switch (this) {
            case PICKAXE -> Arrays.stream(PickaxeRune.values()).map(PickaxeRune::rune).toList();
            case SMELTER -> Arrays.stream(SmelterRune.values()).map(SmelterRune::rune).toList();
            case PYLON -> Arrays.stream(PylonRune.values()).map(PylonRune::rune).toList();
        };
    }

    public boolean takes(ComplexRune rune) {
        return switch (this) {
            case PICKAXE -> PickaxeRune.of(rune) != null;
            case SMELTER -> SmelterRune.of(rune) != null;
            case PYLON -> PylonRune.of(rune) != null;
        };
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
        return switch (this) {
            case PICKAXE -> Objects.requireNonNull(PickaxeRune.of(rune)).cost(stars);
            case SMELTER -> Objects.requireNonNull(SmelterRune.of(rune)).cost(stars);
            case PYLON -> Objects.requireNonNull(PylonRune.of(rune)).cost(stars);
        };
    }

    /** What the rune does here, one short line. */
    public String descriptionKey(ComplexRune rune) {
        return switch (this) {
            case PICKAXE -> Objects.requireNonNull(PickaxeRune.of(rune)).descriptionKey();
            case SMELTER -> Objects.requireNonNull(SmelterRune.of(rune)).descriptionKey();
            case PYLON -> Objects.requireNonNull(PylonRune.of(rune)).descriptionKey();
        };
    }

    /** The line with the rune's number at a strength ({@code %s} = {@link #amount}). */
    public String valueKey(ComplexRune rune) {
        return switch (this) {
            case PICKAXE -> Objects.requireNonNull(PickaxeRune.of(rune)).valueKey();
            case SMELTER -> Objects.requireNonNull(SmelterRune.of(rune)).valueKey();
            case PYLON -> Objects.requireNonNull(PylonRune.of(rune)).valueKey();
        };
    }

    public String amount(ComplexRune rune, int strength) {
        return switch (this) {
            case PICKAXE -> PickaxeEffects.amount(Objects.requireNonNull(PickaxeRune.of(rune)), strength);
            case SMELTER -> Objects.requireNonNull(SmelterRune.of(rune)).amount(strength);
            case PYLON -> Objects.requireNonNull(PylonRune.of(rune)).amountText(strength);
        };
    }

    /** What it looks like on the altar and in JEI: every item of the kind. */
    public List<ItemStack> items() {
        return switch (this) {
            case PICKAXE -> ModItems.PICKAXES.values().stream().map(item -> new ItemStack(item.get())).toList();
            case SMELTER -> List.of(new ItemStack(ModItems.HEX_SMELTER.get()));
            case PYLON -> List.of(new ItemStack(ModItems.GATHERING_PYLON.get()), new ItemStack(ModItems.SENDING_PYLON.get()),
                    new ItemStack(ModItems.RELAY_PYLON.get()), new ItemStack(ModItems.SIPHON_PYLON.get()));
        };
    }

    /** Lower-case name used in translation keys. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The note under the runes in the book: "up to %s runes ...". */
    public String bookNoteKey() {
        return "book.hextale.quest." + id() + "_rune.note";
    }

    /** The note under a rune in JEI; the pickaxe keeps the old key. */
    public String jeiNoteKey() {
        return this == PICKAXE ? "jei.hextale.rune_burning.note" : "jei.hextale.rune_burning.note_" + id();
    }

    /** The altar's refusal when a stele's rune does not go on this holder. */
    public String notTakenRefusal() {
        return "not_" + id();
    }

    /** Suffix of the altar's refusal keys: the pickaxe keeps the old keys. */
    public String refusalSuffix() {
        return this == PICKAXE ? "" : "_" + id();
    }
}
