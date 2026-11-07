package com.pockyl.hextale.item;

import com.pockyl.hextale.block.ChemCarboyBlock;
import com.pockyl.hextale.chem.CarboyContents;
import com.pockyl.hextale.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** A Chem Carboy in hand: the brew it was broken with shows in the tooltip and comes back when it is set down. */
public class ChemCarboyItem extends BlockItem {
    public ChemCarboyItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        CarboyContents contents = stack.getOrDefault(ModDataComponents.CHEM_CARBOY.get(), CarboyContents.EMPTY);
        if (!contents.isEmpty()) {
            tooltip.accept(ChemCarboyBlock.status(contents).copy().withStyle(ChatFormatting.GRAY));
        }
    }
}
