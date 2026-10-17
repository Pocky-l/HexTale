package com.pockyl.hextale.essence;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Separation reagents: an item binds the essence it carries most and pulls that essence out of a mixture in the
 * Separator - a magma block draws magma, a book knowledge. With eighty essences this needs no list: every item with
 * essences is a reagent. Item tags {@code hextale:reagents/<essence>} override it for data packs.
 */
public final class Reagents {
    /** Millibuckets of its essence one reagent item binds. */
    public static final int BINDS = 8;
    private static final TagKey<Item>[] TAGS = tags();

    private Reagents() {
    }

    @SuppressWarnings("unchecked")
    private static TagKey<Item>[] tags() {
        TagKey<Item>[] out = new TagKey[Essence.COUNT];
        for (Essence essence : Essence.values()) {
            out[essence.ordinal()] = TagKey.create(Registries.ITEM, Hextale.id("reagents/" + essence.id()));
        }
        return out;
    }

    public static TagKey<Item> tag(Essence essence) {
        return TAGS[essence.ordinal()];
    }

    /** The essence this item binds, or null when it is no reagent (no essences, or a vessel of essence). */
    @Nullable
    public static Essence essence(ItemStack stack) {
        if (stack.isEmpty() || EssenceFlaskItem.isFlask(stack) || stack.is(ModItems.ESSENCE_TANK.get())
                || stack.is(ModItems.CREATIVE_ESSENCE_TANK.get())) {
            return null;
        }
        for (Essence essence : Essence.values()) {
            if (stack.is(TAGS[essence.ordinal()])) {
                return essence;
            }
        }
        ItemEssences essences = ResearchItems.of(stack);
        return essences == null || essences.essences().isEmpty() ? null : essences.main();
    }

    /** Some items that bind this essence (tagged ones first), for books and recipe viewers. */
    public static List<ItemStack> examples(Essence essence, int max) {
        List<ItemStack> out = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(tag(essence)).forEach(holder -> {
            if (out.size() < max) {
                out.add(new ItemStack(holder));
            }
        });
        for (Item item : BuiltInRegistries.ITEM) {
            if (out.size() >= max) {
                break;
            }
            ItemStack stack = new ItemStack(item);
            if (essence(stack) == essence && out.stream().noneMatch(o -> o.is(item))) {
                out.add(stack);
            }
        }
        return out;
    }
}
