package com.pockyl.hextale.research;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import com.pockyl.hextale.rune.Essence;

import java.util.List;
import java.util.function.Supplier;

/**
 * Something of the mod a researcher must work out at the Research Table before it can be made: a device or a tool. Its
 * parchment is themed by two primal essences ({@code action} and {@code target}, as a rune's board used to be) and is
 * harder the deeper the topic lies ({@code tier} 1-3: stone walls, then dark fields with curses). One topic can open
 * several items made together (the altar opens its whole circle).
 *
 * @param id    stable id, saved in the researcher's knowledge
 * @param items what the topic lets the researcher make; the first one names and shows it
 */
public record ResearchTopic(String id, List<Supplier<? extends ItemLike>> items, Essence action, Essence target, int tier) {
    public Item item() {
        return items.getFirst().get().asItem();
    }

    public ItemStack icon() {
        return new ItemStack(item());
    }

    public String translationKey() {
        return item().getDescriptionId();
    }

    /** The topic's colour: the essence the parchment's ink starts from. */
    public int color() {
        return action.color();
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
