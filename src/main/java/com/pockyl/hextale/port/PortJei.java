package com.pockyl.hextale.port;

import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 31 calls the mod uses, on the JEI 19 API of Minecraft 1.21.1.
 */
public final class PortJei {
    private PortJei() {
    }

    /** JEI 31's {@code ISubtypeRegistration#registerFromDataComponentTypes}: stacks differ by that component. */
    public static void registerFromDataComponentTypes(ISubtypeRegistration registration, Item item, DataComponentType<?> type) {
        registration.registerSubtypeInterpreter(item, new ISubtypeInterpreter<>() {
            @Override
            public @Nullable Object getSubtypeData(ItemStack stack, UidContext context) {
                return stack.get(type);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                Object value = stack.get(type);
                return value == null ? "" : value.toString();
            }
        });
    }

    /**
     * What JEI 31's {@code IIngredientAcceptor#add} took (stacks, templates, ingredients, items, lists of them) as the
     * stacks for {@code addIngredientsUnsafe}.
     */
    public static List<ItemStack> stacks(Object ingredient) {
        List<ItemStack> stacks = new ArrayList<>();
        collect(ingredient, stacks);
        return stacks;
    }

    private static void collect(Object ingredient, List<ItemStack> out) {
        if (ingredient instanceof ItemStack stack) {
            out.add(stack);
        } else if (ingredient instanceof ItemStackTemplate template) {
            out.add(template.create());
        } else if (ingredient instanceof Ingredient vanilla) {
            out.addAll(List.of(vanilla.getItems()));
        } else if (ingredient instanceof ItemLike item) {
            out.add(new ItemStack(item));
        } else if (ingredient instanceof java.util.Optional<?> optional) {
            optional.ifPresent(value -> collect(value, out));
        } else if (ingredient instanceof Iterable<?> list) {
            for (Object element : list) {
                collect(element, out);
            }
        } else {
            throw new IllegalArgumentException("Not an item ingredient: " + ingredient);
        }
    }
}
