package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.render.PixelArt;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * One step done in the world or in a device, drawn like the diary draws it: the inputs stand in a column, dotted arrows
 * run from each into the place where it happens (the device or block, in a brass-rimmed well), an arrow leads on to the
 * result, and a short note sits under them. Without a place the inputs flow straight into the arrow. Every simple
 * process of the mod (hextech working, the still, the tank, the drain valve, writing a rune) is shown the same way, so
 * they read alike.
 */
public class StepCategory extends AbstractRecipeCategory<StepCategory.Step> {
    public static final int WIDTH = 150;
    /** The row other categories line their slots up on. */
    static final int ROW_Y = 5;
    static final int ARROW_X = 96;
    static final int RESULT_X = WIDTH - 21;
    /** Where other categories start their notes, under one row of slots. */
    static final int NOTE_Y = ROW_Y + 26;
    static final int NOTE_COLOR = 0xFF404040;
    /** The flow area: up to two inputs in a column, the place's well in the middle of it. */
    private static final int FLOW_HEIGHT = 42;
    private static final int MID_Y = FLOW_HEIGHT / 2;
    private static final int STEP_NOTE_Y = FLOW_HEIGHT + 3;
    private static final int INPUT_PITCH = 21;
    private static final int WELL = 26;
    private static final int WELL_X = 50;
    private static final int LINE = 0x8A6420;
    private static final int BRASS = 0xB8913A;

    /** One input slot: the stacks it cycles through, and an optional line added to their tooltip. */
    public record Input(List<ItemStack> stacks, @Nullable Component tooltip) {
        public static Input of(ItemStack... stacks) {
            return new Input(List.of(stacks), null);
        }

        public static Input of(List<ItemStack> stacks, Component tooltip) {
            return new Input(stacks, tooltip);
        }
    }

    /** The inputs, the place they go into (null: none to show), the result (one stack or several it cycles) and the note. */
    public record Step(List<Input> inputs, @Nullable Input place, Input result, Component note) {
        public Step(List<Input> inputs, ItemStack result, Component note) {
            this(inputs, null, Input.of(result), note);
        }

        public Step(List<Input> inputs, Input result, Component note) {
            this(inputs, null, result, note);
        }

        public Step(List<Input> inputs, ItemStack place, ItemStack result, Component note) {
            this(inputs, Input.of(place), Input.of(result), note);
        }
    }

    private final int noteLines;

    public StepCategory(RecipeType<Step> type, Component title, IDrawable icon, int noteLines) {
        super(type, title, icon, WIDTH, STEP_NOTE_Y + noteLines * 10);
        this.noteLines = noteLines;
    }

    static RecipeType<Step> type(String path) {
        return RecipeType.create(Hextale.MOD_ID, path, Step.class);
    }

    /** The y of an input's item (its 18 px background one pixel round it). */
    private static int inputY(Step step, int i) {
        int column = step.inputs().size() * INPUT_PITCH - (INPUT_PITCH - 18);
        return MID_Y - column / 2 + 1 + i * INPUT_PITCH;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Step step, IFocusGroup focuses) {
        for (int i = 0; i < step.inputs().size(); i++) {
            slot(builder.addInputSlot(1, inputY(step, i)).setStandardSlotBackground(), step.inputs().get(i));
        }
        if (step.place() != null) {
            slot(builder.addSlot(RecipeIngredientRole.CATALYST, WELL_X + (WELL - 16) / 2, MID_Y - 8), step.place());
        }
        slot(builder.addOutputSlot(RESULT_X, MID_Y - 8).setOutputSlotBackground(), step.result());
    }

    private static void slot(IRecipeSlotBuilder slot, Input input) {
        slot.addItemStacks(input.stacks());
        if (input.tooltip() != null) {
            slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(input.tooltip()));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Step step, IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(ARROW_X - 4, MID_Y - 8);
        builder.addText(step.note(), WIDTH, noteLines * 10).setPosition(0, STEP_NOTE_Y).setColor(NOTE_COLOR);
    }

    @Override
    public void draw(Step step, IRecipeSlotsView slots, net.minecraft.client.gui.GuiGraphics portGraphics, double mouseX, double mouseY) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        boolean place = step.place() != null;
        int targetX = place ? WELL_X - 2 : ARROW_X - 6;
        int count = step.inputs().size();
        int flow = (int) (Util.getMillis() / 120 % 6);
        int color = PixelArt.argb(LINE, 1);
        for (int i = 0; i < count; i++) {
            int fromY = inputY(step, i) + 8;
            int endY = count == 1 ? MID_Y : MID_Y - WELL / 4 + (WELL / 2) * i / (count - 1);
            PixelArt.dotted(graphics, 19, fromY, targetX - 3, endY, 3, 3, 6 - flow, color);
            for (int k = 0; k < 4; k++) {
                PixelArt.fill(graphics, targetX - 3 + k, endY - 3 + k, targetX - 2 + k, endY + 4 - k, color);
            }
        }
        if (place) {
            well(graphics, WELL_X, MID_Y - WELL / 2);
        }
        PixelArt.flush(graphics);
    }

    /** The place's well: a dark recess with a brass rim and studs in the corners. */
    private static void well(GuiGraphicsExtractor graphics, int x, int y) {
        int rim = PixelArt.argb(BRASS, 1);
        PixelArt.fill(graphics, x - 1, y - 1, x + WELL + 1, y + WELL + 1, rim);
        PixelArt.fill(graphics, x, y, x + WELL, y + WELL, PixelArt.argb(0x3A3450, 1));
        PixelArt.fill(graphics, x, y, x + WELL, y + 1, PixelArt.argb(0x1A1628, 1));
        PixelArt.fill(graphics, x, y, x + 1, y + WELL, PixelArt.argb(0x1A1628, 1));
        int stud = PixelArt.argb(0xE8C36A, 1);
        for (int[] c : new int[][]{{x - 2, y - 2}, {x + WELL - 1, y - 2}, {x - 2, y + WELL - 1}, {x + WELL - 1, y + WELL - 1}}) {
            PixelArt.fill(graphics, c[0], c[1], c[0] + 3, c[1] + 3, stud);
        }
    }
}
