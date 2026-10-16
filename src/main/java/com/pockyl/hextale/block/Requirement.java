package com.pockyl.hextale.block;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.item.ResearchersDiaryItem;
import com.pockyl.hextale.registry.ModItems;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/**
 * What the Research Table needs to learn a rune (the diary, the orb and paper), in the order the screen asks for it.
 * Checked by the screen (to light up the slot that needs something) and again by the server.
 */
public enum Requirement {
    DIARY,
    OWN_DIARY,
    HEXTECH,
    PAPER;

    /** The player's own (or an unsigned) diary must lie on the table for any research. */
    @Nullable
    public static Requirement diary(Container table, Player player) {
        ItemStack diary = table.getItem(ResearchTableBlockEntity.DIARY);
        if (!diary.is(ModItems.RESEARCHERS_DIARY.get())) {
            return DIARY;
        }
        return ResearchersDiaryItem.canResearchWith(diary, player) ? null : OWN_DIARY;
    }

    public String translationKey() {
        return "gui.hextale.research_table.need." + name().toLowerCase(Locale.ROOT);
    }

    /** The first unmet requirement for this player, or null when everything is in place. */
    @Nullable
    public static Requirement firstMissing(Container table, Player player) {
        Requirement diary = diary(table, player);
        if (diary != null) {
            return diary;
        }
        if (table.getItem(ResearchTableBlockEntity.HEXTECH).isEmpty()) {
            return HEXTECH;
        }
        return paper(table, player);
    }

    /** Every finished research - a rune or an item study - is written on a paper from the table's paper slot. */
    @Nullable
    public static Requirement paper(Container table, Player player) {
        return player.getAbilities().instabuild || table.getItem(ResearchTableBlockEntity.PAPER).is(Items.PAPER) ? null : PAPER;
    }
}
