package com.pockyl.hextale.research;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import com.pockyl.hextale.rune.SimpleRune;

import java.util.Map;

/** Small text helpers shared by server messages and the screens. */
public final class ResearchText {
    private ResearchText() {
    }

    /** The element's name in its colour. */
    public static MutableComponent element(SimpleRune element) {
        return Component.translatable(element.translationKey()).withColor(element.color());
    }

    /** "Fire 2, Air 1" with coloured names. */
    public static MutableComponent elements(ItemElements elements) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (Map.Entry<SimpleRune, Integer> entry : elements.elements().entrySet()) {
            if (!first) {
                out.append(", ");
            }
            out.append(element(entry.getKey())).append(" " + entry.getValue());
            first = false;
        }
        return out;
    }
}
