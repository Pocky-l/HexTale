package com.pockyl.hextale.client.screen.book;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.HexGrid;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The Researcher's Diary: a magic book whose pages are a dark sky. Two bookmarks turn it between the researcher's path
 * (quests from the first table to the full codex, see {@link QuestPath}) and the star map, where the runes hang as
 * stars in constellations (see {@link StarMap}). Either sky can be dragged around and zoomed with the mouse wheel.
 * Clicking a star opens a small plate with the rune; on a Research Table the plate sends the rune to the parchment.
 * Shown by {@link StarBookScreen} in hand and inside the table's screen.
 */
public final class StarBook {
    private static final Identifier COSMOS = Hextale.id("textures/gui/cosmos.png");
    private static final Identifier FRAME = Hextale.id("textures/gui/star_book_frame.png");
    private static final int COSMOS_SIZE = 256;
    private static final int FRAME_SIZE = 48;
    private static final int FRAME_CORNER = 16;
    /** The frame's leather band; the sky starts inside it. */
    private static final int BORDER = 7;
    private static final float[] ZOOMS = {0.5F, 0.75F, 1, 1.5F, 2, 3};
    private static final int NODE = 15;
    private static final int GLYPH = 11;
    private static final int PLATE_WIDTH = 118;
    static final int BUTTON_HEIGHT = 16;
    static final int GOLD = 0xE0B85A;
    private static final int DRAG_THRESHOLD = 3;
    private static final int BOOKMARK_WIDTH = 20;
    private static final int BOOKMARK_HEIGHT = 22;

    /** The book's two pages. */
    enum Page {
        PATH,
        STARS
    }

    /** Each page's sky stays where the reader left it, between openings. */
    private static final float[] SAVED_X = new float[Page.values().length];
    private static final float[] SAVED_Y = new float[Page.values().length];
    private static final int[] SAVED_ZOOM = {2, 2};
    private static float cameraX;
    private static float cameraY;
    private static int zoomIndex = 2;
    private static boolean placed;
    private static Page page = Page.PATH;

    /** Where the book is open: in hand (nothing to send runes to) or on a table. */
    public interface Host {
        /** The rune lying on the table, or null. */
        @Nullable
        ComplexRune studying();

        void study(ComplexRune rune);
    }

    @Nullable
    private final Host host;
    private final Font font;
    private int left;
    private int top;
    private int width;
    private int height;
    private final QuestPath path;
    @Nullable
    private ComplexRune selected;
    private boolean dragging;
    private boolean pressed;
    private double pressX;
    private double pressY;
    private float pressCameraX;
    private float pressCameraY;

    public StarBook(@Nullable Host host) {
        this.host = host;
        this.font = Minecraft.getInstance().font;
        this.path = new QuestPath(this);
        RuneKnowledge knowledge = ClientKnowledge.get();
        ComplexRune onTable = host == null ? null : host.studying();
        if (!placed) {
            // A new researcher starts on the path, seen whole; one who has learned the first rune starts on the stars.
            SAVED_X[Page.PATH.ordinal()] = QuestPath.CENTRE_X;
            SAVED_Y[Page.PATH.ordinal()] = QuestPath.CENTRE_Y;
            page = knowledge.questDone(Quest.FIRST_RUNE) ? Page.STARS : Page.PATH;
            cameraX = SAVED_X[page.ordinal()];
            cameraY = SAVED_Y[page.ordinal()];
            placed = true;
        }
        if (onTable != null) {
            turnTo(Page.STARS);
            selected = onTable;
            centreOn(onTable);
        }
    }

    /** Opens the other page, keeping where each page's sky was left. */
    private void turnTo(Page next) {
        if (next == page) {
            return;
        }
        SAVED_X[page.ordinal()] = cameraX;
        SAVED_Y[page.ordinal()] = cameraY;
        SAVED_ZOOM[page.ordinal()] = zoomIndex;
        page = next;
        cameraX = SAVED_X[page.ordinal()];
        cameraY = SAVED_Y[page.ordinal()];
        zoomIndex = SAVED_ZOOM[page.ordinal()];
        dragging = false;
        pressed = false;
    }

    /** The whole book including its frame. */
    public void layout(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    private void centreOn(ComplexRune rune) {
        int[] p = StarMap.position(rune);
        cameraX = p[0];
        cameraY = p[1];
    }

    float zoom() {
        return ZOOMS[zoomIndex];
    }

    int skyLeft() {
        return left + BORDER;
    }

    int skyTop() {
        return top + BORDER;
    }

    int skyWidth() {
        return width - BORDER * 2;
    }

    int skyHeight() {
        return height - BORDER * 2;
    }

    private float centreX() {
        return skyLeft() + skyWidth() / 2.0F;
    }

    private float centreY() {
        return skyTop() + skyHeight() / 2.0F;
    }

    /** Map coordinates under a screen point. */
    float mapX(double screenX) {
        return (float) ((screenX - centreX()) / zoom() + cameraX);
    }

    float mapY(double screenY) {
        return (float) ((screenY - centreY()) / zoom() + cameraY);
    }

    private boolean overSky(double x, double y) {
        return x >= skyLeft() && x < skyLeft() + skyWidth() && y >= skyTop() && y < skyTop() + skyHeight();
    }

    // ---- Rendering ------------------------------------------------------------------------------------------------

    /** Draws the book; returns the tooltip to show on top of everything, or null. */
    @Nullable
    public List<FormattedCharSequence> render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        long now = Util.getMillis();
        RuneKnowledge knowledge = ClientKnowledge.get();
        graphics.enableScissor(skyLeft(), skyTop(), skyLeft() + skyWidth(), skyTop() + skyHeight());
        renderSpace(graphics, now);

        graphics.pose().pushMatrix();
        graphics.pose().translate(centreX(), centreY());
        graphics.pose().scale(zoom(), zoom());
        graphics.pose().translate(-cameraX, -cameraY);
        boolean free = !dragging && overSky(mouseX, mouseY) && bookmarkAt(mouseX, mouseY) == null
                && (page == Page.STARS ? !overPlate(mouseX, mouseY) : !path.overPlate(mouseX, mouseY, knowledge));
        ComplexRune hovered = null;
        Quest hoveredQuest = null;
        if (page == Page.STARS) {
            renderSigns(graphics, knowledge);
            renderLinks(graphics, knowledge);
            hovered = free ? starAt(mouseX, mouseY) : null;
            for (Map.Entry<ComplexRune, int[]> entry : StarMap.positions().entrySet()) {
                renderStar(graphics, entry.getKey(), entry.getValue()[0], entry.getValue()[1], knowledge, now, entry.getKey() == hovered);
            }
        } else {
            hoveredQuest = free ? path.questAt(mouseX, mouseY) : null;
            path.renderMap(graphics, knowledge, now, hoveredQuest);
        }
        PixelArt.flush(graphics);
        graphics.pose().popMatrix();

        List<FormattedCharSequence> tooltip = null;
        if (page == Page.STARS && selected != null) {
            renderPlate(graphics, selected, knowledge, mouseX, mouseY, now);
        } else if (page == Page.PATH) {
            tooltip = path.renderPlate(graphics, knowledge, mouseX, mouseY, now);
        }
        Page bookmark = bookmarkAt(mouseX, mouseY);
        renderBookmarks(graphics, bookmark);
        // The pixel art is batched: draw it while the scissor still clips it to the sky.
        PixelArt.flush(graphics);
        graphics.disableScissor();
        renderFrame(graphics);
        PixelArt.flush(graphics);

        if (bookmark != null) {
            return List.of(Component.translatable("book.hextale.page." + bookmark.name().toLowerCase(Locale.ROOT))
                    .withColor(GOLD).getVisualOrderText());
        }
        if (hoveredQuest != null && tooltip == null) {
            tooltip = path.tooltip(hoveredQuest, knowledge);
        }
        if (hovered != null && tooltip == null) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            if (ResearchBook.open(knowledge, hovered)) {
                lines.add(Component.translatable(hovered.translationKey()).withColor(hovered.color()).getVisualOrderText());
                lines.add(stars(knowledge.stars(hovered)).getVisualOrderText());
            } else {
                lines.add(Component.translatable("book.hextale.unknown_star").withColor(0x9A90B8).getVisualOrderText());
            }
            tooltip = lines;
        }
        return tooltip;
    }

    /** The nebula drifts slower than the stars of the map; three layers of small stars drift at their own speeds. */
    private void renderSpace(GuiGraphicsExtractor graphics, long now) {
        int x0 = skyLeft();
        int y0 = skyTop();
        int w = skyWidth();
        int h = skyHeight();
        PixelArt.fill(graphics, x0, y0, x0 + w, y0 + h, 0xFF06050D);
        int offsetX = Math.floorMod((int) (cameraX * 0.25F), COSMOS_SIZE);
        int offsetY = Math.floorMod((int) (cameraY * 0.25F), COSMOS_SIZE);
        for (int ty = -offsetY; ty < h; ty += COSMOS_SIZE) {
            for (int tx = -offsetX; tx < w; tx += COSMOS_SIZE) {
                PixelArt.flush(graphics);
                graphics.blit(RenderPipelines.GUI_TEXTURED, COSMOS, x0 + tx, y0 + ty, 0, 0, COSMOS_SIZE, COSMOS_SIZE, COSMOS_SIZE, COSMOS_SIZE);
            }
        }
        float[] speeds = {0.12F, 0.3F, 0.55F};
        int[] counts = {90, 60, 34};
        for (int layer = 0; layer < speeds.length; layer++) {
            float shiftX = cameraX * speeds[layer] * zoom();
            float shiftY = cameraY * speeds[layer] * zoom();
            int cell = 512;
            int count = counts[layer] * Math.max(1, w * h / (300 * 180));
            for (int i = 0; i < count; i++) {
                long seed = i * 2654435761L + layer * 40503L;
                int sx = (int) Math.floorMod((long) (hash(seed) % cell) - (long) shiftX, cell);
                int sy = (int) Math.floorMod((long) (hash(seed + 7) % cell) - (long) shiftY, cell);
                if (sx >= w || sy >= h) {
                    continue;
                }
                float twinkle = 0.55F + 0.45F * Mth.sin(now / (700.0F + hash(seed + 3) % 900) + i);
                int tint = layer == 2 ? 0xFFF6E0 : hash(seed + 5) % 3 == 0 ? 0xB8C8FF : 0xE0D8F0;
                int argb = PixelArt.argb(tint, (0.25F + 0.25F * layer) * twinkle);
                PixelArt.fill(graphics, x0 + sx, y0 + sy, x0 + sx + 1, y0 + sy + 1, argb);
                if (layer == 2 && hash(seed + 11) % 4 == 0) {
                    int dim = PixelArt.argb(tint, 0.25F * twinkle);
                    PixelArt.fill(graphics, x0 + sx - 1, y0 + sy, x0 + sx, y0 + sy + 1, dim);
                    PixelArt.fill(graphics, x0 + sx + 1, y0 + sy, x0 + sx + 2, y0 + sy + 1, dim);
                    PixelArt.fill(graphics, x0 + sx, y0 + sy - 1, x0 + sx + 1, y0 + sy, dim);
                    PixelArt.fill(graphics, x0 + sx, y0 + sy + 1, x0 + sx + 1, y0 + sy + 2, dim);
                }
            }
        }
    }

    private static long hash(long value) {
        long x = value * 0x9E3779B97F4A7C15L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 27;
        return Math.abs(x);
    }

    /** The nine elements' signs far out on their rays, like the names of constellations. */
    private void renderSigns(GuiGraphicsExtractor graphics, RuneKnowledge knowledge) {
        for (Essence element : HexGrid.RING) {
            double[] u = HexGrid.unit(element);
            int x = (int) Math.round(u[0] * StarMap.SIGN_RADIUS);
            int y = (int) Math.round(u[1] * StarMap.SIGN_RADIUS);
            PixelArt.essence(graphics, element, x - 13, y - 13, 27, PixelArt.argb(element.color(), 0.45F));
            ComplexRune head = StarMap.rayHeads().get(element);
            if (head != null) {
                int[] p = StarMap.position(head);
                double[] from = {u[0] * (StarMap.TOOL_RING + 16), u[1] * (StarMap.TOOL_RING + 16)};
                PixelArt.dotted(graphics, (int) Math.round(from[0]), (int) Math.round(from[1]), p[0], p[1], 1, 3, 0,
                        PixelArt.argb(element.color(), 0.18F));
            }
        }
    }

    private void renderLinks(GuiGraphicsExtractor graphics, RuneKnowledge knowledge) {
        for (StarMap.Link link : StarMap.links()) {
            int[] a = StarMap.position(link.from());
            int[] b = StarMap.position(link.to());
            boolean lit = knowledge.knows(link.from()) && knowledge.knows(link.to());
            boolean open = ResearchBook.open(knowledge, link.to());
            int color = lit ? PixelArt.argb(GOLD, 0.7F) : open ? PixelArt.argb(0x8C9AD8, 0.35F) : PixelArt.argb(0x5A5080, 0.18F);
            if (link.chain() && !lit) {
                PixelArt.dotted(graphics, a[0], a[1], b[0], b[1], 2, 2, 0, color);
            } else {
                PixelArt.line(graphics, a[0], a[1], b[0], b[1], color);
            }
        }
    }

    private void renderStar(GuiGraphicsExtractor graphics, ComplexRune rune, int x, int y, RuneKnowledge knowledge, long now, boolean hovered) {
        boolean open = ResearchBook.open(knowledge, rune);
        int stars = knowledge.stars(rune);
        float pulse = 0.5F + 0.5F * Mth.sin(now / 420.0F + rune.index());
        if (!open) {
            // An unknown star: a faint point of light, no shape yet.
            int argb = PixelArt.argb(0x9A90B8, 0.35F + 0.2F * pulse);
            PixelArt.fill(graphics, x, y - 1, x + 1, y + 2, argb);
            PixelArt.fill(graphics, x - 1, y, x + 2, y + 1, argb);
            return;
        }
        boolean onTable = host != null && host.studying() == rune;
        int color = rune.color();
        if (stars > 0) {
            // A learned rune glows in its category's colour.
            for (int ring = 0; ring < 3; ring++) {
                PixelArt.hex(graphics, x, y, NODE + 4 + ring * 4, PixelArt.argb(color, (0.12F - ring * 0.035F) * (0.7F + 0.3F * pulse)), 0);
            }
        }
        int rim = stars == 3 ? GOLD : stars > 0 ? PixelArt.mix(color, 0xFFFFFF, 0.2F) : 0x6C78B8;
        if (onTable) {
            rim = PixelArt.mix(0x5CC8F2, 0xFFFFFF, 0.4F * pulse);
        }
        if (rune == selected || hovered) {
            rim = PixelArt.mix(GOLD, 0xFFF6D6, pulse);
        }
        PixelArt.hex(graphics, x, y, NODE, PixelArt.argb(stars > 0 ? PixelArt.mix(color, 0x06050D, 0.78F) : 0x0E0C1E, 1),
                PixelArt.argb(rim, 1));
        int glyph = stars > 0 ? PixelArt.mix(color, 0xFFFFFF, 0.15F) : 0x8A86B0;
        PixelArt.glyph(graphics, rune, x - GLYPH / 2, y - GLYPH / 2, GLYPH, PixelArt.argb(glyph, 1));
        for (int s = 0; s < 3; s++) {
            int dot = s < stars ? PixelArt.argb(GOLD, 1) : PixelArt.argb(0x3A3460, 1);
            PixelArt.fill(graphics, x - 4 + s * 3, y + 10, x - 2 + s * 3, y + 12, dot);
        }
        if (rune == selected) {
            PixelArt.hexRing(graphics, x, y, NODE + 4, PixelArt.argb(GOLD, 0.5F + 0.5F * pulse));
        }
    }

    @Nullable
    private ComplexRune starAt(double mouseX, double mouseY) {
        float mx = mapX(mouseX);
        float my = mapY(mouseY);
        float reach = Math.max(8, 8 / zoom());
        ComplexRune best = null;
        float bestDistance = reach * reach;
        for (Map.Entry<ComplexRune, int[]> entry : StarMap.positions().entrySet()) {
            float dx = entry.getValue()[0] + 0.5F - mx;
            float dy = entry.getValue()[1] + 0.5F - my;
            float d = dx * dx + dy * dy;
            if (d < bestDistance) {
                bestDistance = d;
                best = entry.getKey();
            }
        }
        return best;
    }

    // ---- The plate of the selected rune ---------------------------------------------------------------------------

    private int plateLeft() {
        return skyLeft() + skyWidth() - PLATE_WIDTH - 6;
    }

    private int plateTop() {
        return skyTop() + 6;
    }

    private Component plateName(ComplexRune rune) {
        return ResearchBook.open(ClientKnowledge.get(), rune) ? Component.translatable(rune.translationKey())
                : Component.translatable("book.hextale.unknown_star");
    }

    /** Glyph, name lines and the stars come first; the button sits under them. */
    private int buttonTop(ComplexRune rune) {
        return plateTop() + 54 + font.split(plateName(rune), PLATE_WIDTH - 12).size() * 10;
    }

    private int plateHeight(ComplexRune rune) {
        return buttonTop(rune) - plateTop() + (host != null ? BUTTON_HEIGHT + 6 : 0);
    }

    private boolean overPlate(double x, double y) {
        return selected != null && x >= plateLeft() && x < plateLeft() + PLATE_WIDTH && y >= plateTop()
                && y < plateTop() + plateHeight(selected);
    }

    private void renderPlate(GuiGraphicsExtractor graphics, ComplexRune rune, RuneKnowledge knowledge, int mouseX, int mouseY,
                                                    long now) {
        int x = plateLeft();
        int y = plateTop();
        int h = plateHeight(rune);
        int w = PLATE_WIDTH;
        boolean open = ResearchBook.open(knowledge, rune);
        plateBackground(graphics, x, y, w, h);
        int stars = knowledge.stars(rune);
        int cx = x + w / 2;
        int color = rune.color();
        float pulse = 0.5F + 0.5F * Mth.sin(now / 380.0F);
        if (open) {
            for (int ring = 0; ring < 3; ring++) {
                PixelArt.hex(graphics, cx, y + 21, 25 + ring * 6, PixelArt.argb(color, (0.14F - ring * 0.04F) * (0.6F + 0.4F * pulse)), 0);
            }
            PixelArt.glyph(graphics, rune, cx - 13, y + 8, 27, PixelArt.argb(PixelArt.mix(color, 0xFFFFFF, 0.15F), 1), 0xFF000000);
        } else {
            PixelArt.flush(graphics);
            graphics.centeredText(font, "?", cx, y + 17, 0xFF9A90B8);
        }
        int ty = y + 40;
        Component name = plateName(rune);
        for (FormattedCharSequence line : font.split(name, w - 12)) {
            PixelArt.flush(graphics);
            graphics.centeredText(font, line, cx, ty, 0xFF000000 | (open ? PixelArt.mix(color, 0xFFFFFF, 0.25F) : 0x9A90B8));
            ty += 10;
        }
        PixelArt.flush(graphics);
        graphics.centeredText(font, stars(stars), cx, ty + 1, 0xFFFFFFFF);
        ty += 13;
        if (host != null) {
            boolean onTable = host.studying() == rune;
            boolean active = open && !onTable;
            Component label = Component.translatable(!open ? "book.hextale.closed" : onTable ? "book.hextale.on_table"
                    : stars > 0 ? "book.hextale.restudy" : "book.hextale.study");
            int bx = x + 8;
            int by = buttonTop(rune);
            int bw = w - 16;
            boolean hovered = active && mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + BUTTON_HEIGHT;
            magicButton(graphics, bx, by, bw, label, active, hovered, now);
        }
    }

    /** Dark glass in a gold rim with cyan corner studs: the plates of both pages. */
    static void plateBackground(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        PixelArt.fill(graphics, x, y, x + w, y + h, 0xE0080614);
        int rim = PixelArt.argb(GOLD, 0.8F);
        PixelArt.fill(graphics, x, y, x + w, y + 1, rim);
        PixelArt.fill(graphics, x, y + h - 1, x + w, y + h, rim);
        PixelArt.fill(graphics, x, y, x + 1, y + h, rim);
        PixelArt.fill(graphics, x + w - 1, y, x + w, y + h, rim);
        for (int[] corner : new int[][]{{x, y}, {x + w - 1, y}, {x, y + h - 1}, {x + w - 1, y + h - 1}}) {
            PixelArt.fill(graphics, corner[0] - 1, corner[1] - 1, corner[0] + 2, corner[1] + 2, PixelArt.argb(0x5CC8F2, 1));
        }
    }

    // ---- Bookmarks ------------------------------------------------------------------------------------------------

    private int bookmarkX(Page bookmark) {
        return skyLeft() + 8 + bookmark.ordinal() * (BOOKMARK_WIDTH + 4);
    }

    @Nullable
    private Page bookmarkAt(double x, double y) {
        for (Page bookmark : Page.values()) {
            int bx = bookmarkX(bookmark);
            int h = bookmark == page ? BOOKMARK_HEIGHT + 4 : BOOKMARK_HEIGHT;
            if (x >= bx && x < bx + BOOKMARK_WIDTH && y >= skyTop() && y < skyTop() + h) {
                return bookmark;
            }
        }
        return null;
    }

    /** Leather ribbons hanging from the top of the book; the open page's is longer and edged in gold. */
    private void renderBookmarks(GuiGraphicsExtractor graphics, @Nullable Page hovered) {
        for (Page bookmark : Page.values()) {
            int x = bookmarkX(bookmark);
            int y = skyTop();
            boolean open = bookmark == page;
            int h = open ? BOOKMARK_HEIGHT + 4 : BOOKMARK_HEIGHT;
            int leather = PixelArt.argb(open ? 0x5A2E1E : bookmark == hovered ? 0x4A2618 : 0x351A10, 1);
            PixelArt.fill(graphics, x, y, x + BOOKMARK_WIDTH, y + h - 3, leather);
            // A swallowtail end.
            for (int i = 0; i < 3; i++) {
                PixelArt.fill(graphics, x, y + h - 3 + i, x + BOOKMARK_WIDTH / 2 - 1 - i * 2, y + h - 2 + i, leather);
                PixelArt.fill(graphics, x + BOOKMARK_WIDTH / 2 + 1 + i * 2, y + h - 3 + i, x + BOOKMARK_WIDTH, y + h - 2 + i, leather);
            }
            int edge = PixelArt.argb(open ? GOLD : 0x1E0E08, 1);
            PixelArt.fill(graphics, x, y, x + 1, y + h - 3, edge);
            PixelArt.fill(graphics, x + BOOKMARK_WIDTH - 1, y, x + BOOKMARK_WIDTH, y + h - 3, edge);
            PixelArt.flush(graphics);
            graphics.item(bookmark == Page.PATH ? new ItemStack(Items.MAP) : new ItemStack(ModItems.RUNE_SCROLL.get()), x + 2, y + h - 21);
        }
    }

    /** A rune-plate button: dark glass in a glowing rim, no vanilla stone. */
    void magicButton(GuiGraphicsExtractor graphics, int x, int y, int w, Component label, boolean active, boolean hovered, long now) {
        float pulse = 0.5F + 0.5F * Mth.sin(now / 300.0F);
        int rim = !active ? 0x4A4468 : hovered ? PixelArt.mix(0x7EF0FF, 0xFFFFFF, 0.3F * pulse) : PixelArt.mix(0x2B8FB8, 0x5CC8F2, pulse);
        PixelArt.fill(graphics, x, y, x + w, y + BUTTON_HEIGHT, PixelArt.argb(hovered ? 0x123048 : 0x0C1426, 0.95F));
        PixelArt.fill(graphics, x + 1, y, x + w - 1, y + 1, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x + 1, y + BUTTON_HEIGHT - 1, x + w - 1, y + BUTTON_HEIGHT, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x, y + 1, x + 1, y + BUTTON_HEIGHT - 1, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x + w - 1, y + 1, x + w, y + BUTTON_HEIGHT - 1, PixelArt.argb(rim, 1));
        if (active) {
            PixelArt.fill(graphics, x + 2, y + 2, x + w - 2, y + 3, PixelArt.argb(rim, 0.25F));
            PixelArt.hex(graphics, x + 7, y + BUTTON_HEIGHT / 2, 5, PixelArt.argb(rim, 1), 0);
            PixelArt.hex(graphics, x + w - 8, y + BUTTON_HEIGHT / 2, 5, PixelArt.argb(rim, 1), 0);
        }
        PixelArt.flush(graphics);
        graphics.centeredText(font, label, x + w / 2, y + 4, active ? 0xFFE8FAFF : 0xFF7A7498);
    }

    private void renderFrame(GuiGraphicsExtractor graphics) {
        int c = FRAME_CORNER;
        int mid = FRAME_SIZE - 2 * c;
        PixelArt.flush(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left, top, 0, 0, c, c, FRAME_SIZE, FRAME_SIZE);
        PixelArt.flush(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left + width - c, top, FRAME_SIZE - c, 0, c, c, FRAME_SIZE, FRAME_SIZE);
        PixelArt.flush(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left, top + height - c, 0, FRAME_SIZE - c, c, c, FRAME_SIZE, FRAME_SIZE);
        PixelArt.flush(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left + width - c, top + height - c, FRAME_SIZE - c, FRAME_SIZE - c, c, c, FRAME_SIZE, FRAME_SIZE);
        for (int x = left + c; x < left + width - c; x += mid) {
            int piece = Math.min(mid, left + width - c - x);
            PixelArt.flush(graphics);
            graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, x, top, c, 0, piece, c, FRAME_SIZE, FRAME_SIZE);
            PixelArt.flush(graphics);
            graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, x, top + height - c, c, FRAME_SIZE - c, piece, c, FRAME_SIZE, FRAME_SIZE);
        }
        for (int y = top + c; y < top + height - c; y += mid) {
            int piece = Math.min(mid, top + height - c - y);
            PixelArt.flush(graphics);
            graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left, y, 0, c, c, piece, FRAME_SIZE, FRAME_SIZE);
            PixelArt.flush(graphics);
            graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left + width - c, y, FRAME_SIZE - c, c, c, piece, FRAME_SIZE, FRAME_SIZE);
        }
    }

    private static Component stars(int count) {
        return Component.literal("★".repeat(count)).withColor(GOLD).append(Component.literal("★".repeat(3 - count)).withColor(0x3A3460));
    }

    // ---- Input ----------------------------------------------------------------------------------------------------

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT || !overSky(mouseX, mouseY)) {
            return false;
        }
        Page bookmark = bookmarkAt(mouseX, mouseY);
        if (bookmark != null) {
            if (bookmark != page) {
                turnTo(bookmark);
                sound(bookmark == Page.STARS ? 1.2F : 1.0F);
            }
            return true;
        }
        if (page == Page.PATH && path.overPlate(mouseX, mouseY, ClientKnowledge.get())) {
            if (path.plateClicked(mouseX, mouseY, ClientKnowledge.get())) {
                turnTo(Page.STARS);
                selected = ResearchBook.FIRST;
                centreOn(ResearchBook.FIRST);
                sound(1.2F);
            }
            return true;
        }
        if (page == Page.STARS && selected != null && host != null && overPlate(mouseX, mouseY)) {
            int bx = plateLeft() + 8;
            int by = buttonTop(selected);
            boolean open = ResearchBook.open(ClientKnowledge.get(), selected);
            if (open && host.studying() != selected && mouseX >= bx && mouseX < bx + PLATE_WIDTH - 16 && mouseY >= by
                    && mouseY < by + BUTTON_HEIGHT) {
                host.study(selected);
            }
            return true;
        }
        if (page == Page.STARS && overPlate(mouseX, mouseY)) {
            return true;
        }
        pressed = true;
        dragging = false;
        pressX = mouseX;
        pressY = mouseY;
        pressCameraX = cameraX;
        pressCameraY = cameraY;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!pressed || button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (!dragging && Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) >= DRAG_THRESHOLD) {
            dragging = true;
        }
        if (dragging) {
            cameraX = pressCameraX - (float) ((mouseX - pressX) / zoom());
            cameraY = pressCameraY - (float) ((mouseY - pressY) / zoom());
            clampCamera();
        }
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!pressed || button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        pressed = false;
        if (!dragging && page == Page.STARS) {
            ComplexRune star = starAt(mouseX, mouseY);
            if (star != selected) {
                selected = star;
                if (star != null) {
                    sound(ResearchBook.open(ClientKnowledge.get(), star) ? 1.4F : 0.8F);
                }
            }
        } else if (!dragging) {
            Quest quest = path.questAt(mouseX, mouseY);
            if (path.select(quest) && quest != null) {
                sound(QuestPath.state(quest, ClientKnowledge.get()) == QuestPath.State.LOCKED ? 0.8F : 1.4F);
            }
        }
        dragging = false;
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!overSky(mouseX, mouseY) || scrollY == 0) {
            return false;
        }
        if (page == Page.PATH && path.overPlate(mouseX, mouseY, ClientKnowledge.get())) {
            path.scrollPlate(scrollY);
            return true;
        }
        int next = Mth.clamp(zoomIndex + (scrollY > 0 ? 1 : -1), 0, ZOOMS.length - 1);
        if (next != zoomIndex) {
            // Zoom around the cursor: the point under it stays put.
            float beforeX = mapX(mouseX);
            float beforeY = mapY(mouseY);
            zoomIndex = next;
            cameraX += beforeX - mapX(mouseX);
            cameraY += beforeY - mapY(mouseY);
            clampCamera();
        }
        return true;
    }

    private void clampCamera() {
        if (page == Page.PATH) {
            cameraX = Mth.clamp(cameraX, QuestPath.MIN_X, QuestPath.MAX_X);
            cameraY = Mth.clamp(cameraY, QuestPath.MIN_Y, QuestPath.MAX_Y);
        } else {
            cameraX = Mth.clamp(cameraX, -StarMap.EXTENT, StarMap.EXTENT);
            cameraY = Mth.clamp(cameraY, -StarMap.EXTENT, StarMap.EXTENT);
        }
    }

    private static void sound(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, pitch, 0.6F));
    }
}
