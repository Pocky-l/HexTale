package com.pockyl.hextale.research;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;

import com.pockyl.hextale.rune.Essence;

import java.util.Map;

/** Small text helpers shared by server messages and the screens. */
public final class ResearchText {
    private ResearchText() {
    }

    /** An item's plain name (no stack data), as items had it before names became components. */
    public static Component item(Item item) {
        return item.getName(item.getDefaultInstance());
    }

    /** The element's name in its colour. */
    public static MutableComponent essence(Essence element) {
        return Component.translatable(element.translationKey()).withColor(element.color());
    }

    /** "Fire 2, Air 1" with coloured names. */
    public static MutableComponent elements(ItemEssences elements) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (Map.Entry<Essence, Integer> entry : elements.essences().entrySet()) {
            if (!first) {
                out.append(", ");
            }
            out.append(essence(entry.getKey())).append(" " + entry.getValue());
            first = false;
        }
        return out;
    }
}
