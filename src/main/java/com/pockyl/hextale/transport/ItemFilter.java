package com.pockyl.hextale.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The settings of a Pylon Filter (an item; set into a pylon, they rule what it moves). Up to {@value #SLOTS} entries, each
 * an item and a number.
 * <ul>
 * <li>{@link #whitelist}: only the listed items pass; otherwise everything but them. An empty filter lets all pass.</li>
 * <li>{@link #exact}: entries match only with the same components (enchantments, contents, damage); else any item of
 * the kind.</li>
 * <li>{@link #rule}, with each entry's number N: {@link Rule#KEEP} - a sending pylon leaves N of the item in its
 * container; {@link Rule#LIMIT} - an item goes into a target only while it holds fewer than N of it.</li>
 * </ul>
 */
public record ItemFilter(List<Entry> entries, boolean whitelist, boolean exact, Rule rule) {
    public static final int SLOTS = 9;
    public static final int MAX_COUNT = 9999;
    public static final ItemFilter EMPTY = new ItemFilter(List.of(), true, false, Rule.NONE);

    public enum Rule implements StringRepresentable {
        NONE, KEEP, LIMIT;

        public static final Codec<Rule> CODEC = StringRepresentable.fromEnum(Rule::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Rule next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** One entry: the item (count ignored) in slot {@code slot}, and its number. */
    public record Entry(int slot, ItemStack item, int count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, SLOTS - 1).fieldOf("slot").forGetter(Entry::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(Entry::item),
                Codec.intRange(1, MAX_COUNT).optionalFieldOf("count", 1).forGetter(Entry::count)
        ).apply(instance, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::slot, ItemStack.STREAM_CODEC, Entry::item, ByteBufCodecs.VAR_INT, Entry::count, Entry::new);
    }

    public static final Codec<ItemFilter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(ItemFilter::entries),
            Codec.BOOL.optionalFieldOf("whitelist", true).forGetter(ItemFilter::whitelist),
            Codec.BOOL.optionalFieldOf("exact", false).forGetter(ItemFilter::exact),
            Rule.CODEC.optionalFieldOf("rule", Rule.NONE).forGetter(ItemFilter::rule)
    ).apply(instance, ItemFilter::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemFilter> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(SLOTS)), ItemFilter::entries,
            ByteBufCodecs.BOOL, ItemFilter::whitelist,
            ByteBufCodecs.BOOL, ItemFilter::exact,
            ByteBufCodecs.idMapper(i -> Rule.values()[i], Rule::ordinal), ItemFilter::rule,
            ItemFilter::new);

    public ItemFilter {
        entries = List.copyOf(entries);
    }

    public @Nullable Entry at(int slot) {
        for (Entry entry : entries) {
            if (entry.slot() == slot) {
                return entry;
            }
        }
        return null;
    }

    /** The filter with slot {@code slot} set to this item (empty = cleared), its number kept or 1. */
    public ItemFilter with(int slot, ItemStack item) {
        List<Entry> out = new ArrayList<>();
        Entry old = at(slot);
        for (Entry entry : entries) {
            if (entry.slot() != slot) {
                out.add(entry);
            }
        }
        if (!item.isEmpty()) {
            out.add(new Entry(slot, item.copyWithCount(1), old == null ? Math.max(1, item.getCount()) : old.count()));
        }
        return new ItemFilter(out, whitelist, exact, rule);
    }

    /** The filter with slot {@code slot}'s number changed by {@code delta} (kept within 1-{@value #MAX_COUNT}). */
    public ItemFilter add(int slot, int delta) {
        List<Entry> out = new ArrayList<>();
        for (Entry entry : entries) {
            out.add(entry.slot() == slot ? new Entry(slot, entry.item(), Math.clamp(entry.count() + delta, 1, MAX_COUNT)) : entry);
        }
        return new ItemFilter(out, whitelist, exact, rule);
    }

    public ItemFilter withWhitelist(boolean value) {
        return new ItemFilter(entries, value, exact, rule);
    }

    public ItemFilter withExact(boolean value) {
        return new ItemFilter(entries, whitelist, value, rule);
    }

    public ItemFilter withRule(Rule value) {
        return new ItemFilter(entries, whitelist, exact, value);
    }

    /** The entry this item matches, or null. */
    public @Nullable Entry match(ItemStack stack) {
        for (Entry entry : entries) {
            if (exact ? ItemStack.isSameItemSameComponents(entry.item(), stack) : entry.item().is(stack.getItem())) {
                return entry;
            }
        }
        return null;
    }

    public @Nullable Entry match(ItemResource resource) {
        return match(resource.toStack(1));
    }

    /** Whether the item may pass: a listed one on a whitelist, an unlisted one on a blacklist, anything when empty. */
    public boolean passes(ItemStack stack) {
        if (entries.isEmpty()) {
            return true;
        }
        return (match(stack) != null) == whitelist;
    }

    public boolean passes(ItemResource resource) {
        return passes(resource.toStack(1));
    }

    /** Whether the item is named on a whitelist (not merely let through by an empty filter or a blacklist). */
    public boolean lists(ItemResource resource) {
        return whitelist && match(resource) != null;
    }

    /**
     * How many of this item a sending pylon may take out of a container that holds {@code held} of it, after
     * {@link Rule#KEEP}: all of them otherwise.
     */
    public int sendable(ItemResource resource, int held) {
        Entry entry = rule == Rule.KEEP && whitelist ? match(resource) : null;
        return entry == null ? held : Math.max(0, held - entry.count());
    }

    /** How many more of this item a target holding {@code held} of it may take, after {@link Rule#LIMIT}. */
    public int room(ItemResource resource, int held) {
        Entry entry = rule == Rule.LIMIT && whitelist ? match(resource) : null;
        return entry == null ? Integer.MAX_VALUE : Math.max(0, entry.count() - held);
    }

    /** How many of the item (by this filter's matching) a handler holds. */
    public int count(ResourceHandler<ItemResource> handler, ItemResource resource) {
        ItemStack probe = resource.toStack(1);
        int total = 0;
        for (int i = 0; i < handler.size(); i++) {
            ItemResource there = handler.getResource(i);
            if (!there.isEmpty() && (exact ? there.matches(probe) : there.is(probe.getItem()))) {
                total += handler.getAmountAsInt(i);
            }
        }
        return total;
    }
}
