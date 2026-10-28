package com.pockyl.hextale.client.screen.book;

import net.minecraft.client.Minecraft;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.network.QuestNoticedPayload;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.quest.Region;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Arrows that point the reader at what is new in the guide: a step that just opened, or one written down by itself
 * because something happened (that one wears a gold ring). Each arrow is drawn in the look of its step's branch
 * ({@link MarkArrows}). A new step out of view gets an arrow on the edge of the sky pointing at it; one in view, an arrow
 * over it pointing down. Arrows of one branch and kind that would sit on top of each other on the edge become one with
 * a count. A click on an arrow flies the sky to its step; the mouse
 * going over the step itself takes it in, and its arrow is gone (on the server too).
 */
final class FreshMarks {
    /** How far in from the sky's rim the edge arrows sit. */
    private static final int EDGE = 13;
    /** Edge arrows closer than this along the rim become one. */
    private static final int MERGE = 22;
    /** How far from its centre an arrow takes a click or the mouse. */
    private static final int REACH = 10;
    /** How far above a step's medallion its arrow hangs. */
    private static final int OVER = 13;
    private static final int OPENED = 0x5CC8F2;
    private static final int HAPPENED = StarBook.GOLD;

    /** One arrow: where it sits (screen), where it points, and the new steps it stands for (the nearest first). */
    record Mark(int x, int y, float angle, boolean over, Guide.Fresh kind, List<Quest> steps) {
        Quest target() {
            return steps.getFirst();
        }
    }

    private final StarBook book;
    private List<Mark> marks = List.of();

    FreshMarks(StarBook book) {
        this.book = book;
    }

    /** Works out this frame's arrows: called before the map is drawn, so the map knows which points they cover. */
    void layout(RuneKnowledge knowledge) {
        int left = book.skyLeft();
        int top = book.skyTop();
        int right = left + book.skyWidth();
        int bottom = top + book.skyHeight();
        float cx = (left + right) / 2.0F;
        float cy = (top + bottom) / 2.0F;
        List<Mark> over = new ArrayList<>();
        List<Mark> edge = new ArrayList<>();
        for (Quest quest : Guide.client().all()) {
            Guide.Fresh kind = Guide.fresh(quest, knowledge);
            if (kind == Guide.Fresh.NONE) {
                continue;
            }
            float sx = book.screenX(quest.x());
            float sy = book.screenY(quest.y());
            float radius = 12 * book.zoom();
            if (sx >= left + EDGE && sx <= right - EDGE && sy - radius - OVER >= top + EDGE && sy <= bottom - EDGE
                    && !book.overTabBar(sx, sy - radius - OVER)) {
                over.add(new Mark(Math.round(sx), Math.round(sy - radius - OVER), Mth.HALF_PI, true, kind, List.of(quest)));
                continue;
            }
            // Out of view: where the line from the middle of the sky towards the step leaves the inset rim.
            float dx = sx - cx;
            float dy = sy - cy;
            float tx = dx == 0 ? Float.MAX_VALUE : ((dx > 0 ? right - EDGE : left + EDGE) - cx) / dx;
            float ty = dy == 0 ? Float.MAX_VALUE : ((dy > 0 ? bottom - EDGE : top + EDGE) - cy) / dy;
            float t = Math.min(tx, ty);
            int ex = Math.round(cx + dx * t);
            int ey = Math.round(cy + dy * t);
            if (book.overTabBar(ex, ey)) {
                // The tab bar holds the top left corner: slide along the rim past it.
                if (ex <= left + EDGE) {
                    ey = book.tabBarBottom() + EDGE;
                } else {
                    ex = book.tabBarRight() + EDGE;
                }
            }
            edge.add(new Mark(ex, ey, (float) Math.atan2(dy, dx), false, kind, List.of(quest)));
        }
        List<Mark> out = new ArrayList<>(over);
        out.addAll(merge(edge, left, top, right, bottom));
        marks = out;
    }

    /** Joins edge arrows of the same branch and kind that would overlap, then moves apart any that still do. */
    private static List<Mark> merge(List<Mark> edge, int left, int top, int right, int bottom) {
        Comparator<Mark> alongRim = Comparator.comparingDouble(mark -> rim(mark, left, top, right, bottom));
        List<Mark> out = new ArrayList<>();
        for (Guide.Fresh kind : List.of(Guide.Fresh.HAPPENED, Guide.Fresh.OPENED)) {
            for (Region region : Region.values()) {
                List<Mark> ofKind = edge.stream().filter(mark -> mark.kind() == kind && mark.target().region() == region)
                        .sorted(alongRim).toList();
                Mark open = null;
                for (Mark mark : ofKind) {
                    if (open != null && Math.hypot(mark.x() - open.x(), mark.y() - open.y()) < MERGE) {
                        List<Quest> steps = new ArrayList<>(open.steps());
                        steps.addAll(mark.steps());
                        open = new Mark(open.x(), open.y(), open.angle(), false, kind, steps);
                    } else {
                        if (open != null) {
                            out.add(open);
                        }
                        open = mark;
                    }
                }
                if (open != null) {
                    out.add(open);
                }
            }
        }
        // Arrows of other branches or kinds may still meet: the later one steps aside along the rim.
        List<Mark> placed = new ArrayList<>();
        for (Mark mark : out) {
            Mark moved = mark;
            for (int tries = 0; tries < 6 && overlaps(moved, placed); tries++) {
                boolean upright = moved.x() <= left + EDGE || moved.x() >= right - EDGE;
                moved = new Mark(upright ? moved.x() : moved.x() + MERGE, upright ? moved.y() + MERGE : moved.y(), moved.angle(), false,
                        moved.kind(), moved.steps());
            }
            placed.add(moved);
        }
        return placed;
    }

    private static boolean overlaps(Mark mark, List<Mark> placed) {
        for (Mark other : placed) {
            if (Math.hypot(mark.x() - other.x(), mark.y() - other.y()) < MERGE) {
                return true;
            }
        }
        return false;
    }

    /** A point's place along the rim, clockwise from the top left corner, so neighbours on the rim sort together. */
    private static double rim(Mark mark, int left, int top, int right, int bottom) {
        int w = right - left;
        int h = bottom - top;
        if (mark.y() <= top + EDGE) {
            return mark.x() - left;
        }
        if (mark.x() >= right - EDGE) {
            return w + mark.y() - top;
        }
        if (mark.y() >= bottom - EDGE) {
            return w + h + right - mark.x();
        }
        return 2 * w + h + bottom - mark.y();
    }

    /** The arrow under a screen point, or null. */
    @Nullable
    Mark at(double x, double y) {
        for (Mark mark : marks) {
            if (Math.hypot(x - mark.x(), y - mark.y()) <= REACH) {
                return mark;
            }
        }
        return null;
    }

    /** The reader's mouse is over a step: if it was new, it is taken in here and on the server. */
    static void notice(Quest quest, RuneKnowledge knowledge) {
        if (Guide.fresh(quest, knowledge) != Guide.Fresh.NONE && knowledge.notice(quest.id())) {
            PacketDistributor.sendToServer(new QuestNoticedPayload(quest.id()));
        }
    }

    /** Draws the arrows (screen coordinates); returns the tooltip of the one under the mouse, or null. */
    @Nullable
    List<FormattedCharSequence> render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, long now) {
        Mark hovered = at(mouseX, mouseY);
        for (Mark mark : marks) {
            MarkArrows.draw(graphics, mark.target().region(), mark.kind() == Guide.Fresh.HAPPENED, mark.x(), mark.y(), mark.angle(),
                    mark == hovered, now, mark.target().index());
        }
        PixelArt.flush(graphics);
        var font = Minecraft.getInstance().font;
        for (Mark mark : marks) {
            if (mark.steps().size() > 1) {
                // How many new steps lie that way: a small number behind the arrow, on the side towards the sky's middle.
                String count = String.valueOf(mark.steps().size());
                int nx = Math.round(mark.x() - Mth.cos(mark.angle()) * 17) - font.width(count) / 2;
                int ny = Math.round(mark.y() - Mth.sin(mark.angle()) * 17) - 4;
                graphics.text(font, count, nx, ny, 0xFF000000 | PixelArt.mix(MarkArrows.glow(mark.target().region()), 0xFFFFFF, 0.3F), true);
            }
        }
        return hovered == null ? null : tooltip(hovered);
    }

    private static List<FormattedCharSequence> tooltip(Mark mark) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        String key = mark.kind() == Guide.Fresh.HAPPENED ? "book.hextale.guide.fresh.happened" : "book.hextale.guide.fresh.opened";
        int color = mark.kind() == Guide.Fresh.HAPPENED ? HAPPENED : OPENED;
        lines.add(Component.translatable(key, Component.translatable(mark.target().translationKey()).withColor(0xFFFFFF))
                .withColor(color).getVisualOrderText());
        if (mark.steps().size() > 1) {
            lines.add(Component.translatable("book.hextale.guide.fresh.more", mark.steps().size() - 1).withColor(0xB8B0D0).getVisualOrderText());
        }
        if (!mark.over()) {
            lines.add(Component.translatable("book.hextale.guide.fresh.look").withColor(0x8A8898).getVisualOrderText());
        }
        return lines;
    }
}
