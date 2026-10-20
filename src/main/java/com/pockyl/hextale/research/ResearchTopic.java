package com.pockyl.hextale.research;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * Something of the mod a researcher must work out before it can be made: a device or a tool, or a chemtech brew. Hextech
 * is worked out at the Research Table ({@link Bench#TABLE}) on a parchment themed by two primal essences ({@code action}
 * and {@code target}, as a rune's board used to be), harder the deeper the topic lies ({@code tier} 1-3: stone walls,
 * then dark fields with curses). Chemtech - its devices and every brew - is worked out at the Lab Bench
 * ({@link Bench#LAB}) by making the topic's compound in a flask ({@link ExperimentPlan}). One topic can open several items
 * made together (the altar opens its whole circle); a brew topic opens brewing that brew in the Crucible instead.
 *
 * @param id    stable id, saved in the researcher's knowledge
 * @param items what the topic lets the researcher make; the first one names and shows it (none for a brew)
 * @param brew  the brew a chemtech reaction topic opens, or null
 */
public record ResearchTopic(String id, List<Supplier<? extends ItemLike>> items, Essence action, Essence target, int tier, Bench bench,
                            @Nullable Brew brew) {
    /** Where a topic is worked out. */
    public enum Bench {
        /** The Research Table: a hexagon parchment and an ink thread. */
        TABLE,
        /** The Lab Bench: a flask, a flame and drops of essence. */
        LAB
    }

    public Item item() {
        return brew != null ? ModItems.CHEM_VIAL.get() : items.getFirst().get().asItem();
    }

    public ItemStack icon() {
        return brew != null ? ChemVialItem.of(brew) : new ItemStack(item());
    }

    public String translationKey() {
        return brew != null ? brew.translationKey() : item().getDescriptionId();
    }

    /** The topic's colour: the brew's, or the essence the parchment's ink starts from. */
    public int color() {
        return brew != null ? brew.color() : action.color();
    }

    public boolean lab() {
        return bench == Bench.LAB;
    }

    public boolean opens(Item item) {
        for (Supplier<? extends ItemLike> supplier : items) {
            if (supplier.get().asItem() == item) {
                return true;
            }
        }
        return false;
    }

    /** Position in {@link ResearchTopics#all()}. */
    public int index() {
        return ResearchTopics.all().indexOf(this);
    }

    @Override
    public String toString() {
        return id;
    }
}
