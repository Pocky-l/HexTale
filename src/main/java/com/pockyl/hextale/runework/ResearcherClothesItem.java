package com.pockyl.hextale.runework;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/** A piece of the researcher's clothes: armour of arcane cloth that takes one rune at the Hextech Altar. */
public class ResearcherClothesItem extends ArmorItem {
    public ResearcherClothesItem(Holder<ArmorMaterial> material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
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
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", runes.count(), holder.slots())
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
