package com.pockyl.hextale.client.screen.diary;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.RuneDrawing;
import com.pockyl.hextale.network.NotesReadPayload;
import com.pockyl.hextale.rune.ComplexRune;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * The Researcher's Diary as a component: a two-page book with bookmarks for its chapters - getting started, the
 * elements, the research book (the runes to study, by chapter), notes and milestones. Shown by {@link DiaryScreen}
 * when the diary is read in hand, and inside the Research Table's screen, where its research pages can send a rune
 * to the table.
 */
public final class DiaryBook {
    static final ResourceLocation TEXTURE = Hextale.id("textures/gui/diary.png");
    static final int SHEET_WIDTH = 512;
    static final int SHEET_HEIGHT = 256;
    public static final int BOOK_WIDTH = 330;
    public static final int BOOK_HEIGHT = 206;
    /** Width with the bookmarks sticking out on the right. */
    public static final int FULL_WIDTH = BOOK_WIDTH + 27;
    static final int SLOT_U = 336;
    static final int SLOT_V = 48;
    private static final int TAB_U = 336;
    private static final int TAB_V = 0;
    private static final int TAB_WIDTH = 30;
    private static final int TAB_HEIGHT = 22;
    private static final int TAB_SPACING = 25;
    private static final int TAB_TOP = 14;
    /** Page numbers and arrows sit between the text and the bottom rule of the pages. */
    private static final int FOOTER_Y = 182;
    public static final int RESEARCH = 2;
    private static final int NOTES = 3;

    /** Where the book is open: in hand (nothing to send runes to) or on a table. */
    public interface Host {
        /** The rune lying on the table, or null. */
        @Nullable
        ComplexRune studying();

        void study(ComplexRune rune);
    }

    private final String owner;
    private final long seed;
    @Nullable
    private final Host host;
    private final DiarySection[] sections;
    private final int[] spreads;
    private final ResearchSection research = new ResearchSection();
    private final Font font;
    private int section;
    private int left;
    private int top;
    @Nullable
    private List<FormattedCharSequence> pendingTooltip;

    public DiaryBook(String owner, long seed, @Nullable Host host) {
        this.owner = owner;
        this.seed = seed;
        this.host = host;
        this.font = Minecraft.getInstance().font;
        this.sections = new DiarySection[]{new StartSection(), new ElementsSection(), research,
                new NotesSection(ClientKnowledge.get()), new MilestonesSection()};
        this.spreads = new int[sections.length];
        if (host != null) {
            section = RESEARCH;
        } else if (ClientKnowledge.get().unreadNotes() > 0) {
            section = NOTES;
        }
    }

    /** Places the book's top left corner; call on every screen resize. */
    public void layout(int left, int top) {
        this.left = left;
        this.top = top;
        sections[section].opened(context(null, 0, 0));
    }

    @Nullable
    Host host() {
        return host;
    }

    private DiaryContext context(@Nullable GuiGraphics graphics, int mouseX, int mouseY) {
        return new DiaryContext(this, graphics, font, left, top, ClientKnowledge.get(), owner, seed, mouseX, mouseY);
    }

    void setPendingTooltip(List<FormattedCharSequence> lines) {
        pendingTooltip = lines;
    }

    /** Draws the book; returns the tooltip to show on top of everything, or null. */
    @Nullable
    public List<FormattedCharSequence> render(GuiGraphics graphics, int mouseX, int mouseY) {
        pendingTooltip = null;
        renderTabs(graphics, mouseX, mouseY);
        graphics.blit(TEXTURE, left, top, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
        DiaryContext context = context(graphics, mouseX, mouseY);
        DiarySection current = sections[section];
        int count = Math.max(1, current.spreads(context));
        spreads[section] = Mth.clamp(spreads[section], 0, count - 1);
        current.render(context, spreads[section], mouseX, mouseY);
        renderFooter(graphics, count, mouseX, mouseY);
        return pendingTooltip;
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < sections.length; i++) {
            boolean active = i == section;
            int x = tabX(i);
            int y = tabY(i);
            graphics.blit(TEXTURE, x, y, TAB_U, TAB_V + (active ? TAB_HEIGHT : 0), TAB_WIDTH, TAB_HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
            InkRenderer ink = InkRenderer.begin(graphics);
            sections[i].icon(ink, x + TAB_WIDTH - 12, y + TAB_HEIGHT / 2.0F, active ? 0xF3E3B8 : 0xC8B48C);
            ink.end();
            if (isOver(mouseX, mouseY, x, y, TAB_WIDTH, TAB_HEIGHT)) {
                pendingTooltip = font.split(sections[i].title(), 200);
            }
        }
    }

    private int tabX(int i) {
        return left + BOOK_WIDTH - 6 + (i == section ? 3 : 0);
    }

    private int tabY(int i) {
        return top + TAB_TOP + i * TAB_SPACING;
    }

    private void renderFooter(GuiGraphics graphics, int count, int mouseX, int mouseY) {
        int spread = spreads[section];
        int y = top + FOOTER_Y;
        if (count > 1 && sections[section].numberedPages()) {
            Component pages = Component.literal((spread * 2 + 1) + "");
            graphics.drawString(font, pages, left + 86 - font.width(pages) / 2, y, 0xFF000000 | DiaryContext.MUTED, false);
            Component pages2 = Component.literal((spread * 2 + 2) + "");
            graphics.drawString(font, pages2, left + 244 - font.width(pages2) / 2, y, 0xFF000000 | DiaryContext.MUTED, false);
        }
        InkRenderer ink = InkRenderer.begin(graphics);
        if (spread > 0) {
            arrow(ink, left + 26, y + 4, -1, isOver(mouseX, mouseY, left + 18, y - 2, 18, 12));
        }
        if (spread < count - 1) {
            arrow(ink, left + BOOK_WIDTH - 26, y + 4, 1, isOver(mouseX, mouseY, left + BOOK_WIDTH - 36, y - 2, 18, 12));
        }
        ink.end();
    }

    private static void arrow(InkRenderer ink, float x, float y, int direction, boolean hovered) {
        int color = RuneDrawing.argb(hovered ? DiaryContext.GOLD : DiaryContext.HEADING, hovered ? 1 : 0.8F);
        float d = direction * 4;
        ink.stroke(new float[]{x - d * 1.6F, y, x + d, y}, 1.4F, color);
        ink.stroke(new float[]{x + d * 0.1F, y - 3.5F, x + d, y, x + d * 0.1F, y + 3.5F}, 1.4F, color);
    }

    private static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        for (int i = 0; i < sections.length; i++) {
            if (isOver(mouseX, mouseY, tabX(i), tabY(i), TAB_WIDTH, TAB_HEIGHT)) {
                openSection(i);
                return true;
            }
        }
        int y = top + FOOTER_Y;
        DiaryContext context = context(null, (int) mouseX, (int) mouseY);
        int count = Math.max(1, sections[section].spreads(context));
        if (spreads[section] > 0 && isOver(mouseX, mouseY, left + 18, y - 2, 18, 12)) {
            turn(-1, count);
            return true;
        }
        if (spreads[section] < count - 1 && isOver(mouseX, mouseY, left + BOOK_WIDTH - 36, y - 2, 18, 12)) {
            turn(1, count);
            return true;
        }
        return sections[section].mouseClicked(context, spreads[section], mouseX, mouseY);
    }

    public boolean keyPressed(int keyCode) {
        int count = Math.max(1, sections[section].spreads(context(null, 0, 0)));
        if (keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_PAGEUP) {
            turn(-1, count);
            return true;
        }
        if (keyCode == InputConstants.KEY_RIGHT || keyCode == InputConstants.KEY_PAGEDOWN) {
            turn(1, count);
            return true;
        }
        return false;
    }

    private void turn(int direction, int count) {
        int next = Mth.clamp(spreads[section] + direction, 0, count - 1);
        if (next != spreads[section]) {
            spreads[section] = next;
            pageSound();
        }
    }

    private void openSection(int index) {
        if (index != section) {
            section = index;
            pageSound();
            sections[section].opened(context(null, 0, 0));
        }
    }

    /** Opens the research pages on this rune's chapter with the rune selected. */
    public void showRune(ComplexRune rune) {
        spreads[RESEARCH] = research.select(rune);
        section = RESEARCH;
    }

    /** The reader has seen the notes; the "new" markers stay for this visit and are gone next time. */
    void notesSeen() {
        if (ClientKnowledge.get().unreadNotes() > 0) {
            PacketDistributor.sendToServer(NotesReadPayload.INSTANCE);
        }
    }

    void turnTo(int spread) {
        if (spread != spreads[section]) {
            spreads[section] = spread;
            pageSound();
        }
    }

    static void pageSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }
}
