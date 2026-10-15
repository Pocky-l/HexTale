package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.pockyl.hextale.research.ResearchTool;

import java.util.function.Consumer;

/** A research tool: lies in its slot of the Research Table; its strength follows the stars of its rune. */
public class ResearchToolItem extends Item {
    public ResearchToolItem(Properties properties) {
        super(properties);
    }

    public ResearchTool tool() {
        for (ResearchTool tool : ResearchTool.values()) {
            if (tool.item() == this) {
                return tool;
            }
        }
        throw new IllegalStateException("Unregistered research tool " + this);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        ResearchTool tool = tool();
        tooltip.accept(Component.translatable("item.hextale.research_tool.slot").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.research_tool.strength",
                Component.translatable(tool.rune().translationKey()).withColor(tool.rune().color())).withStyle(ChatFormatting.GRAY));
        for (int level = 1; level <= 3; level++) {
            tooltip.accept(Component.literal("★".repeat(level) + " ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable(tool.translationKey() + ".level." + level).withStyle(ChatFormatting.DARK_GRAY)));
        }
    }
}
