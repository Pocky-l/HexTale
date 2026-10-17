package com.pockyl.hextale.runework;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The runes burned into a hextech pickaxe or a Hex Smelter ({@link RuneHolder}), in the order they were burned in, each
 * with its stars. Immutable.
 */
public record PickaxeRunes(List<Entry> entries) {
    /** Runes any hextech pickaxe holds. */
    public static final int SLOTS = 4;
    public static final PickaxeRunes EMPTY = new PickaxeRunes(List.of());

    public static final Codec<PickaxeRunes> CODEC = Entry.CODEC.listOf().xmap(PickaxeRunes::new, PickaxeRunes::entries);
    public static final StreamCodec<ByteBuf, PickaxeRunes> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(PickaxeRunes::new, PickaxeRunes::entries);

    public PickaxeRunes {
        entries = List.copyOf(entries);
    }

    public static PickaxeRunes of(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.PICKAXE_RUNES.get(), EMPTY);
    }

    public static void set(ItemStack stack, PickaxeRunes runes) {
        if (runes.entries.isEmpty()) {
            stack.remove(ModDataComponents.PICKAXE_RUNES.get());
        } else {
            stack.set(ModDataComponents.PICKAXE_RUNES.get(), runes);
        }
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int count() {
        return entries.size();
    }

    @Nullable
    public Entry find(PickaxeRune rune) {
        for (Entry entry : entries) {
            if (entry.rune() == rune.rune()) {
                return entry;
            }
        }
        return null;
    }

    /** Stars the rune was burned in with, 0 when the pickaxe lacks it. */
    public int stars(PickaxeRune rune) {
        Entry entry = find(rune);
        return entry == null ? 0 : entry.stars();
    }

    /**
     * The strength a rune works with: its stars, one more when the pickaxe also carries a Lens (up to four); 0 when
     * the pickaxe lacks the rune.
     */
    public int strength(PickaxeRune rune) {
        int stars = stars(rune);
        if (stars == 0) {
            return 0;
        }
        return Math.min(4, stars + (rune != PickaxeRune.LENS && stars(PickaxeRune.LENS) > 0 ? 1 : 0));
    }

    /** With this rune at these stars: added at the end, or its stars replaced where it already is. */
    public PickaxeRunes with(PickaxeRune rune, int stars) {
        return with(rune.rune(), stars);
    }

    public PickaxeRunes with(ComplexRune rune, int stars) {
        List<Entry> list = new ArrayList<>(entries);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).rune() == rune) {
                list.set(i, new Entry(rune, stars));
                return new PickaxeRunes(list);
            }
        }
        list.add(new Entry(rune, stars));
        return new PickaxeRunes(list);
    }

    /** Stars of any rune in the set, 0 when it is missing. */
    public int stars(ComplexRune rune) {
        for (Entry entry : entries) {
            if (entry.rune() == rune) {
                return entry.stars();
            }
        }
        return 0;
    }

    /** The strength any rune works with: its stars, one more with a Lens in the set (up to four); 0 when missing. */
    public int strength(ComplexRune rune) {
        int stars = stars(rune);
        if (stars == 0) {
            return 0;
        }
        return Math.min(4, stars + (rune != PickaxeRune.LENS.rune() && stars(PickaxeRune.LENS.rune()) > 0 ? 1 : 0));
    }

    /** The runes of the set, in burning order. */
    public List<ComplexRune> complexRunes() {
        return entries.stream().map(Entry::rune).toList();
    }

    /** The pickaxe runes these entries hold (skipping any that is no longer a pickaxe rune). */
    public List<PickaxeRune> runes() {
        List<PickaxeRune> out = new ArrayList<>();
        for (Entry entry : entries) {
            PickaxeRune rune = PickaxeRune.of(entry.rune());
            if (rune != null) {
                out.add(rune);
            }
        }
        return out;
    }

    /** One burned-in rune and its stars (1-3). */
    public record Entry(ComplexRune rune, int stars) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComplexRunes.CODEC.fieldOf("rune").forGetter(Entry::rune),
                Codec.intRange(1, 3).fieldOf("stars").forGetter(Entry::stars)
        ).apply(instance, Entry::new));
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ComplexRunes.STREAM_CODEC, Entry::rune,
                ByteBufCodecs.VAR_INT, Entry::stars,
                Entry::new);
    }
}
