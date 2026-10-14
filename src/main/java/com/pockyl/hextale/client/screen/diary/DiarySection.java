package com.pockyl.hextale.client.screen.diary;

import net.minecraft.network.chat.Component;

import com.pockyl.hextale.client.render.InkRenderer;

/** One bookmark of the diary: a run of two-page spreads. */
interface DiarySection {
    Component title();

    /** Draws the bookmark's little icon, centred on {@code x, y}. */
    void icon(InkRenderer ink, float x, float y, int color);

    int spreads(DiaryContext context);

    void render(DiaryContext context, int spread, int mouseX, int mouseY);

    default boolean mouseClicked(DiaryContext context, int spread, double mouseX, double mouseY) {
        return false;
    }

    /** Whether the footer shows page numbers. */
    default boolean numberedPages() {
        return true;
    }

    /** Called when the player turns to this section. */
    default void opened(DiaryContext context) {
    }
}
