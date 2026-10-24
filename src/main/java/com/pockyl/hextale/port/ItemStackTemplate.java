package com.pockyl.hextale.port;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jspecify.annotations.Nullable;

/**
 * An immutable item stack description (item, count, component changes), as in Minecraft 26.x: recipes and guide data
 * keep their results in it and make a fresh stack with {@link #create()}.
 *
 * <p>port-of: net.minecraft.world.item.ItemStackTemplate
 */
public record ItemStackTemplate(Holder<Item> item, int count, DataComponentPatch components) {
    private static final Codec<Holder<Item>> ITEM_CODEC = BuiltInRegistries.ITEM.holderByNameCodec();
    public static final MapCodec<ItemStackTemplate> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                            ITEM_CODEC.fieldOf("id").forGetter(ItemStackTemplate::item),
                            ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(ItemStackTemplate::count),
                            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ItemStackTemplate::components))
                    .apply(i, ItemStackTemplate::new));
    public static final Codec<ItemStackTemplate> CODEC = Codec.withAlternative(MAP_CODEC.codec(), ITEM_CODEC, ItemStackTemplate::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackTemplate> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(Registries.ITEM), ItemStackTemplate::item,
            ByteBufCodecs.VAR_INT, ItemStackTemplate::count,
            DataComponentPatch.STREAM_CODEC, ItemStackTemplate::components,
            ItemStackTemplate::new);

    public ItemStackTemplate {
        if (count == 0 || item.is(Items.AIR.builtInRegistryHolder())) {
            throw new IllegalStateException("Item must be non-empty");
        }
    }

    public ItemStackTemplate(Item item) {
        this(item.builtInRegistryHolder(), 1, DataComponentPatch.EMPTY);
    }

    public ItemStackTemplate(Item item, int count) {
        this(item.builtInRegistryHolder(), count, DataComponentPatch.EMPTY);
    }

    public ItemStackTemplate(Item item, DataComponentPatch patch) {
        this(item.builtInRegistryHolder(), 1, patch);
    }

    public ItemStackTemplate(Item item, int count, DataComponentPatch patch) {
        this(item.builtInRegistryHolder(), count, patch);
    }

    public ItemStackTemplate(Holder<Item> item) {
        this(item, 1, DataComponentPatch.EMPTY);
    }

    public ItemStackTemplate(Holder<Item> item, int count) {
        this(item, count, DataComponentPatch.EMPTY);
    }

    public ItemStackTemplate(Holder<Item> item, DataComponentPatch patch) {
        this(item, 1, patch);
    }

    public static ItemStackTemplate fromNonEmptyStack(ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Stack must be non-empty");
        }
        return fromStack(stack);
    }

    public static ItemStackTemplate fromStack(ItemStack stack) {
        return new ItemStackTemplate(stack.getItemHolder(), stack.getCount(), stack.getComponentsPatch());
    }

    public static ItemStackTemplate fromNonEmptyStack(ItemStack stack, int newCount) {
        if (stack.isEmpty() || newCount <= 0) {
            throw new IllegalStateException("Stack must be non-empty");
        }
        return new ItemStackTemplate(stack.getItemHolder(), newCount, stack.getComponentsPatch());
    }

    /** {@code stack.getCraftingRemainder()} of 26.x: what is left in the slot after the stack is used up, or null. */
    public static @Nullable ItemStackTemplate remainderOf(ItemStack stack) {
        ItemStack remainder = stack.getCraftingRemainingItem();
        return remainder.isEmpty() ? null : fromStack(remainder);
    }

    public ItemStackTemplate withCount(int count) {
        return this.count == count ? this : new ItemStackTemplate(item, count, components);
    }

    public ItemStack create() {
        return validate(new ItemStack(item, count, components));
    }

    public ItemStack apply(DataComponentPatch additionalPatch) {
        return apply(count, additionalPatch);
    }

    public ItemStack apply(int count, DataComponentPatch additionalPatch) {
        ItemStack result = new ItemStack(item, count, additionalPatch);
        result.applyComponents(components);
        return validate(result);
    }

    public Holder<Item> typeHolder() {
        return item;
    }

    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        return create().get(type);
    }

    /** 26.x drops stacks that fail the strict check; 1.21.1 keeps that check private, so the stack is used as is. */
    private ItemStack validate(ItemStack result) {
        return result;
    }
}
