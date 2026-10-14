package com.pockyl.hextale.research;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Tooltip data of a studied item: its elements, drawn as the elements' signs with their strength. */
public record ElementsTooltip(ItemElements elements) implements TooltipComponent {
}
