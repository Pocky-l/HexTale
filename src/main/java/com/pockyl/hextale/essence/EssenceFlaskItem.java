package com.pockyl.hextale.essence;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.rune.Essence;

import java.util.function.Consumer;

/**
 * A bottle of essence: a vanilla glass bottle filled with up to {@value #CAPACITY} mB of one liquid (it does not
 * stack, like a potion); emptied, it turns back into a glass bottle. A bottle of a single element is a concentrate -
 * the only ink the Research Table takes. "Flask" in the code means either: a glass bottle or a bottle of essence.
 */
public class EssenceFlaskItem extends Item {
    /** Millibuckets, like a vanilla bottle. */
    public static final int CAPACITY = 16;

    public EssenceFlaskItem(Properties properties) {
        super(properties);
    }

    public static EssenceMix contents(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ESSENCE.get(), EssenceMix.EMPTY);
    }

    /** A glass bottle or a bottle of essence. */
    public static boolean isFlask(ItemStack stack) {
        return stack.is(ModItems.ESSENCE_FLASK.get()) || isEmptyFlask(stack);
    }

    /** An empty vanilla glass bottle. */
    public static boolean isEmptyFlask(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE);
    }

    /** A bottle holding this essence (a glass bottle for no essence). */
    public static ItemStack filled(EssenceMix essence) {
        if (essence.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        ItemStack stack = new ItemStack(ModItems.ESSENCE_FLASK.get());
        stack.set(ModDataComponents.ESSENCE.get(), essence);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        EssenceMix essence = contents(stack);
        Essence pure = essence.pureEssence();
        if (pure != null) {
            return Component.translatable("item.hextale.essence_flask.concentrate", ResearchText.essence(pure));
        }
        if (essence.isUnknown() && essence.unknown() == essence.total()) {
            return Component.translatable("item.hextale.essence_flask.unknown");
        }
        return essence.isEmpty() ? super.getName(stack) : Component.translatable("item.hextale.essence_flask.mixture");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        EssenceMix essence = contents(stack);
        if (essence.isEmpty()) {
            return;
        }
        tooltip.accept(essence.describe());
        tooltip.accept(Component.translatable("item.hextale.essence_flask.drops", essence.total(), CAPACITY).withStyle(ChatFormatting.GRAY));
        if (essence.isUnknown()) {
            tooltip.accept(Component.translatable("item.hextale.essence_flask.unknown.hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (essence.pureEssence() == null) {
            tooltip.accept(Component.translatable("item.hextale.essence_flask.mixture.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
