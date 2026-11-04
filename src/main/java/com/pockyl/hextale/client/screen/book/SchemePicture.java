package com.pockyl.hextale.client.screen.book;

import net.minecraft.client.gui.Font;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.quest.QuestDisplay;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Something that happens in the world, on a diary plate, drawn as where it happens instead of "this plus that": the
 * last ingredient is the place or tool (a cauldron woken by dust, the lapidary, the still), drawn large in a brass-rimmed
 * well; the other ingredients stand left of it, the same item once with its count, and arrows run from each into it;
 * an arrow leads on to the result. A scheme of one ingredient is that item, the arrow and the result. The step's
 * caption (how it is done, in a few words) stays under the picture.
 */
final class SchemePicture {
    private static final int SLOT = 18;
    /** The place's well: the item drawn twice its size inside. */
    private static final int PLACE = 36;
    private static final int INPUT_STEP = 21;
    private static final int GAP = 28;

    private SchemePicture() {
    }

    private record Input(QuestDisplay.Part part, int count) {
    }

    private static List<Input> inputs(QuestDisplay display) {
        List<QuestDisplay.Part> ingredients = display.ingredients();
        int last = ingredients.size() > 1 ? ingredients.size() - 1 : ingredients.size();
        Map<QuestDisplay.Part, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < last; i++) {
            counts.merge(ingredients.get(i), 1, Integer::sum);
        }
        List<Input> out = new ArrayList<>();
        counts.forEach((part, count) -> out.add(new Input(part, count)));
        return out;
    }

    private static boolean hasPlace(QuestDisplay display) {
        return display.ingredients().size() > 1;
    }

    /** The picture's height without the caption under it. */
    static int height(QuestDisplay display) {
        return height(inputs(display).size(), hasPlace(display));
    }

    static int height(int inputs, boolean place) {
        int column = inputs * INPUT_STEP - (INPUT_STEP - SLOT);
        return Math.max(place ? PLACE : SLOT, column);
    }

    @Nullable
    static List<FormattedCharSequence> render(GuidePage page, GuiGraphicsExtractor graphics, QuestDisplay display, ItemStack result, int x, int y,
                                              int width, int mouseX, int mouseY, long now) {
        List<ItemStack> stacks = new ArrayList<>();
        List<Input> inputs = inputs(display);
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack stack = GuidePage.stack(inputs.get(i).part(), now, i).copy();
            if (!stack.isEmpty()) {
                stack.setCount(inputs.get(i).count());
            }
            stacks.add(stack);
        }
        ItemStack place = hasPlace(display) ? GuidePage.stack(display.ingredients().getLast(), now, 7) : null;
        return render(page, graphics, stacks, place, result, x, y, width, mouseX, mouseY, now);
    }

    /** Inputs flowing into the place (none: straight on to the result), then the result. */
    @Nullable
    static List<FormattedCharSequence> render(GuidePage page, GuiGraphicsExtractor graphics, List<ItemStack> inputs, @Nullable ItemStack where,
                                              ItemStack result, int x, int y, int width, int mouseX, int mouseY, long now) {
        List<FormattedCharSequence> tooltip = null;
        boolean place = where != null;
        int height = height(inputs.size(), place);
        int midY = y + height / 2;
        int total = SLOT + GAP + (place ? PLACE + GAP - 4 : 0) + SLOT;
        int inputX = x + (width - total) / 2;
        int placeX = inputX + SLOT + GAP;
        int targetX = place ? placeX : placeX - 4;
        int column = inputs.size() * INPUT_STEP - (INPUT_STEP - SLOT);
        int inputY = midY - column / 2;
        int flow = (int) (now / 120 % 6);
        for (int i = 0; i < inputs.size(); i++) {
            int iy = inputY + i * INPUT_STEP;
            ItemStack stack = inputs.get(i);
            // Arrows converge on the place's left side, spread over its middle half.
            int endY = inputs.size() == 1 ? midY : midY - PLACE / 4 + (PLACE / 2) * i / (inputs.size() - 1);
            flowLine(graphics, inputX + SLOT + 2, iy + SLOT / 2, targetX - 4, endY, flow);
            head(graphics, targetX - 4, endY);
            tooltip = orTooltip(tooltip, page.slot(graphics, stack, inputX, iy, mouseX, mouseY, null));
        }
        int resultX;
        if (place) {
            tooltip = orTooltip(tooltip, placeWell(page, graphics, where, placeX, midY - PLACE / 2, mouseX, mouseY, now));
            GuidePage.arrow(graphics, placeX + PLACE + 4, midY);
            resultX = placeX + PLACE + GAP - 4;
        } else {
            resultX = placeX;
        }
        tooltip = orTooltip(tooltip, page.slot(graphics, result, resultX, midY - SLOT / 2, mouseX, mouseY, null));
        return tooltip;
    }

    /** The place or tool, large: a recessed well with a brass rim that brightens and dims slowly, the item at 2x. */
    @Nullable
    private static List<FormattedCharSequence> placeWell(GuidePage page, GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX,
                                                         int mouseY, long now) {
        boolean hovered = GuidePage.over(mouseX, mouseY, x, y, PLACE, PLACE);
        float pulse = 0.55F + 0.25F * (float) Math.sin(now / 500.0);
        int rim = PixelArt.argb(StarBook.GOLD, hovered ? 0.95F : pulse);
        PixelArt.fill(graphics, x - 1, y - 1, x + PLACE + 1, y, rim);
        PixelArt.fill(graphics, x - 1, y + PLACE, x + PLACE + 1, y + PLACE + 1, rim);
        PixelArt.fill(graphics, x - 1, y, x, y + PLACE, rim);
        PixelArt.fill(graphics, x + PLACE, y, x + PLACE + 1, y + PLACE, rim);
        PixelArt.fill(graphics, x, y, x + PLACE, y + PLACE, PixelArt.argb(hovered ? 0x2E2850 : 0x15122A, 1));
        PixelArt.fill(graphics, x, y, x + PLACE, y + 1, PixelArt.argb(0x05040A, 1));
        PixelArt.fill(graphics, x, y, x + 1, y + PLACE, PixelArt.argb(0x05040A, 1));
        // Brass studs in the corners.
        int stud = PixelArt.argb(StarBook.GOLD, 1);
        for (int[] c : new int[][]{{x - 2, y - 2}, {x + PLACE - 1, y - 2}, {x - 2, y + PLACE - 1}, {x + PLACE - 1, y + PLACE - 1}}) {
            PixelArt.fill(graphics, c[0], c[1], c[0] + 3, c[1] + 3, stud);
        }
        if (stack.isEmpty()) {
            return null;
        }
        PixelArt.flush(graphics);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 2, y + 2);
        graphics.pose().scale(2, 2);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
        return hovered ? page.pointAt(stack, null) : null;
    }

    /** A dotted gold line whose dots run toward the place. */
    private static void flowLine(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int flow) {
        PixelArt.dotted(graphics, x0, y0, x1, y1, 3, 3, 6 - flow, PixelArt.argb(StarBook.GOLD, 0.85F));
    }

    /** A small arrow head pointing right, its tip at x, y. */
    private static void head(GuiGraphicsExtractor graphics, int x, int y) {
        int color = PixelArt.argb(StarBook.GOLD, 0.9F);
        for (int i = 0; i < 4; i++) {
            PixelArt.fill(graphics, x - 3 + i, y - 3 + i, x - 2 + i, y + 4 - i, color);
        }
    }

    @Nullable
    private static List<FormattedCharSequence> orTooltip(@Nullable List<FormattedCharSequence> current, @Nullable List<FormattedCharSequence> next) {
        return next != null ? next : current;
    }
}
