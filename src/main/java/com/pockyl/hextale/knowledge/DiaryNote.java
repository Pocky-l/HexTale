package com.pockyl.hextale.knowledge;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/**
 * Entries the diary writes by itself when something worth noting happens. Texts: {@code diary.hextale.note.<id>}
 * (title) and {@code diary.hextale.note.<id>.text}.
 */
public enum DiaryNote {
    DIARY_BOUND,
    FIRST_HEXTECH,
    LIGHTNING_NEAR_AMETHYST,
    ROD_WITHOUT_AMETHYST,
    FIRST_TABLE,
    FIRST_SHEET,
    SHEET_FAILED,
    WALLS_DISSOLVED,
    FIRST_RUNE,
    THREE_STARS,
    MILESTONE_3,
    MILESTONE_12,
    MILESTONE_24,
    MILESTONE_36,
    MILESTONE_54,
    MILESTONE_72;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "diary.hextale.note." + id();
    }

    @Nullable
    public static DiaryNote byId(String id) {
        for (DiaryNote note : values()) {
            if (note.id().equals(id)) {
                return note;
            }
        }
        return null;
    }
}
