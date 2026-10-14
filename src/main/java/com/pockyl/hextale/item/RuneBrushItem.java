package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The brush for drawing runes at the Research Table; every fused rune wears it a little. */
public class RuneBrushItem extends Item {
    public static final int DURABILITY = 128;

    public RuneBrushItem(Properties properties) {
        super(properties.durability(DURABILITY));
    }

    /** Wears the brush by one; returns the stack to keep in the slot (empty once it breaks). */
    public static ItemStack wear(ItemStack stack) {
        int damage = stack.getDamageValue() + 1;
        if (damage >= stack.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        stack.setDamageValue(damage);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hextale.rune_brush.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
