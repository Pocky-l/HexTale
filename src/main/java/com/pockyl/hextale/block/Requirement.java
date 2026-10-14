package com.pockyl.hextale.block;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.item.HextechInkItem;
import com.pockyl.hextale.item.ResearchersDiaryItem;
import com.pockyl.hextale.registry.ModItems;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/**
 * What the Research Table needs before a new sheet, in the order the screen asks for it. Checked by the screen (to
 * say exactly what to put where) and again by the server.
 */
public enum Requirement {
    DIARY,
    OWN_DIARY,
    BRUSH,
    INK,
    PAPER,
    HEXTECH;

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
        if (!table.getItem(ResearchTableBlockEntity.BRUSH).is(ModItems.RUNE_BRUSH.get())) {
            return BRUSH;
        }
        if (!HextechInkItem.hasInk(table.getItem(ResearchTableBlockEntity.INK))) {
            return INK;
        }
        if (table.getItem(ResearchTableBlockEntity.PAPER).isEmpty()) {
            return PAPER;
        }
        if (table.getItem(ResearchTableBlockEntity.HEXTECH).isEmpty()) {
            return HEXTECH;
        }
        return null;
    }
}
