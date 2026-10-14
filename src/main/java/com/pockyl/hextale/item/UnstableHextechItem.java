package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A lumpy orb of lightning-awakened amethyst; it listens to runes drawn next to it. */
public class UnstableHextechItem extends Item {
    public UnstableHextechItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hextale.unstable_hextech.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
