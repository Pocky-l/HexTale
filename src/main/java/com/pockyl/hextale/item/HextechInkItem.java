package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import com.pockyl.hextale.registry.ModItems;

import java.util.List;

/** Glowing ink for rune brushes; every fused rune uses one dose, an empty bottle is left behind. */
public class HextechInkItem extends Item {
    public static final int USES = 16;

    public HextechInkItem(Properties properties) {
        super(properties.durability(USES));
    }

    public static boolean hasInk(ItemStack stack) {
        return stack.is(ModItems.HEXTECH_INK.get()) && stack.getDamageValue() < stack.getMaxDamage();
    }

    /** Uses one dose; returns the stack to keep in the slot (an empty glass bottle after the last dose). */
    public static ItemStack useDose(ItemStack stack) {
        int damage = stack.getDamageValue() + 1;
        if (damage >= stack.getMaxDamage()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        stack.setDamageValue(damage);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int left = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("item.hextale.hextech_ink.doses", left, stack.getMaxDamage()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.hextale.hextech_ink.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
