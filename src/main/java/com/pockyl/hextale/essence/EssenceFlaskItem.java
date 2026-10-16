package com.pockyl.hextale.essence;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.function.Consumer;

/**
 * A glass flask for essence. Empty flasks stack; a filled one holds up to {@value #CAPACITY} drops of one liquid and
 * stacks no more. A flask of a single element is a concentrate - the only ink the Research Table takes.
 */
public class EssenceFlaskItem extends Item {
    public static final int CAPACITY = 16;

    public EssenceFlaskItem(Properties properties) {
        super(properties);
    }

    public static Essence contents(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ESSENCE.get(), Essence.EMPTY);
    }

    public static boolean isFlask(ItemStack stack) {
        return stack.is(ModItems.ESSENCE_FLASK.get());
    }

    public static boolean isEmptyFlask(ItemStack stack) {
        return isFlask(stack) && contents(stack).isEmpty();
    }

    /** A new flask holding this essence (an empty one for no essence). */
    public static ItemStack filled(Essence essence) {
        ItemStack stack = new ItemStack(ModItems.ESSENCE_FLASK.get());
        if (!essence.isEmpty()) {
            stack.set(ModDataComponents.ESSENCE.get(), essence);
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        }
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        Essence essence = contents(stack);
        SimpleRune pure = essence.pureElement();
        if (pure != null) {
            return Component.translatable("item.hextale.essence_flask.concentrate", ResearchText.element(pure));
        }
        return essence.isEmpty() ? super.getName(stack) : Component.translatable("item.hextale.essence_flask.mixture");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        Essence essence = contents(stack);
        if (essence.isEmpty()) {
            return;
        }
        tooltip.accept(essence.describe());
        tooltip.accept(Component.translatable("item.hextale.essence_flask.drops", essence.total(), CAPACITY).withStyle(ChatFormatting.GRAY));
        if (essence.pureElement() == null) {
            tooltip.accept(Component.translatable("item.hextale.essence_flask.mixture.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
