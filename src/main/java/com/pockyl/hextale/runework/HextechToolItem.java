package com.pockyl.hextale.runework;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.pockyl.hextale.rune.ComplexRune;

import java.util.function.Consumer;

/**
 * A hextech tool (pickaxe, shovel...): a plain tool of its kind plus rune slots and an essence cell. The essence cell
 * is drawn in the tooltip by the client; the runes are listed here.
 */
public class HextechToolItem extends Item {
    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V"};

    public HextechToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        RuneSlots runes = HextechTools.runes(stack);
        if (runes.isEmpty()) {
            tooltip.accept(Component.translatable("item.hextale.hextech_tool.no_runes").withStyle(ChatFormatting.DARK_GRAY));
        }
        for (RuneSlots.Engraving engraving : runes.engravings()) {
            ComplexRune rune = engraving.rune();
            int stars = engraving.stars();
            tooltip.accept(Component.literal(NUMERALS[engraving.slot()] + " ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.translatable(rune.translationKey()).withColor(rune.color()))
                    .append(Component.literal(" " + "★".repeat(stars)).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("★".repeat(3 - stars)).withStyle(ChatFormatting.DARK_GRAY)));
        }
    }
}
