package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;

/** The research a result waits for, as a line under the result slot's tooltip (no text in the recipe layout). */
final class ResearchNote {
    private ResearchNote() {
    }

    static IRecipeSlotBuilder add(IRecipeSlotBuilder slot, Item result) {
        ResearchTopic topic = ResearchTopics.of(result);
        if (topic != null) {
            slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.needs_research",
                    Component.translatable(topic.translationKey())).withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
        return slot;
    }
}
