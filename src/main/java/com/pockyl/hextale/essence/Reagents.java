package com.pockyl.hextale.essence;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.rune.SimpleRune;

import org.jspecify.annotations.Nullable;

/**
 * Separation reagents: each binds one element and pulls it out of a mixture in the Separator. They are item tags,
 * {@code hextale:reagents/<element>}, so data packs can change them.
 */
public final class Reagents {
    /** Drops of its element one reagent item binds. */
    public static final int BINDS = 8;
    private static final TagKey<Item>[] TAGS = tags();

    private Reagents() {
    }

    @SuppressWarnings("unchecked")
    private static TagKey<Item>[] tags() {
        TagKey<Item>[] out = new TagKey[SimpleRune.values().length];
        for (SimpleRune element : SimpleRune.values()) {
            out[element.ordinal()] = TagKey.create(Registries.ITEM, Hextale.id("reagents/" + element.id()));
        }
        return out;
    }

    public static TagKey<Item> tag(SimpleRune element) {
        return TAGS[element.ordinal()];
    }

    /** The element this item binds, or null when it is no reagent. */
    @Nullable
    public static SimpleRune element(ItemStack stack) {
        for (SimpleRune element : SimpleRune.values()) {
            if (stack.is(TAGS[element.ordinal()])) {
                return element;
            }
        }
        return null;
    }
}
