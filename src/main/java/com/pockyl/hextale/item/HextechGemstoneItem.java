package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Unstable hextech refined at the Hextech Altar: a smooth, steady sphere - the heart of every hextech device. */
public class HextechGemstoneItem extends Item {
    public HextechGemstoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hextale.hextech_gemstone.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
