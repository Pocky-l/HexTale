package com.pockyl.hextale.runework;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** A piece of the researcher's clothes: armour of arcane cloth that takes one rune at the Hextech Altar. */
public class ResearcherClothesItem extends Item {
    public ResearcherClothesItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        RuneHolder holder = RuneHolder.of(stack);
        if (holder == null) {
            return;
        }
        PickaxeRunes runes = holder.runes(stack);
        for (PickaxeRunes.Entry entry : runes.entries()) {
            tooltip.accept(Component.translatable(entry.rune().translationKey()).withColor(entry.rune().color())
                    .append(Component.literal(" " + "★".repeat(entry.stars())).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("★".repeat(3 - entry.stars())).withStyle(ChatFormatting.DARK_GRAY)));
        }
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", runes.count(), holder.slots(stack))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
