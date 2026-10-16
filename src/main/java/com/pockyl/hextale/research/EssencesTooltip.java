package com.pockyl.hextale.research;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Tooltip data of a studied item: its essences, drawn as the essences' signs with their strength. */
public record EssencesTooltip(ItemEssences essences) implements TooltipComponent {
}
