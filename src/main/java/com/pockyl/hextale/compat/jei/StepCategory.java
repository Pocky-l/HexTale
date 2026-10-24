package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * One step done in the world or in a device: up to three inputs joined by plus signs, an arrow, the result, and a short
 * note under them. Every simple process of the mod (hextech working, the still, the tank, the drain valve, writing a rune)
 * is shown the same way, so they read alike.
 */
public class StepCategory extends AbstractRecipeCategory<StepCategory.Step> {
    public static final int WIDTH = 150;
    static final int ROW_Y = 5;
    /** Inputs stand this far apart, a plus sign between each two. */
    private static final int INPUT_PITCH = 34;
    static final int ARROW_X = 96;
    static final int RESULT_X = WIDTH - 21;
    static final int NOTE_Y = ROW_Y + 26;
    static final int NOTE_COLOR = 0xFF404040;

    /** One input slot: the stacks it cycles through, and an optional line added to their tooltip. */
    public record Input(List<ItemStack> stacks, @Nullable Component tooltip) {
        public static Input of(ItemStack... stacks) {
            return new Input(List.of(stacks), null);
        }

        public static Input of(List<ItemStack> stacks, Component tooltip) {
            return new Input(stacks, tooltip);
        }
    }

    /** The inputs, the result (one stack or several it cycles through) and the note. */
    public record Step(List<Input> inputs, Input result, Component note) {
        public Step(List<Input> inputs, ItemStack result, Component note) {
            this(inputs, Input.of(result), note);
        }
    }

    private final int noteLines;

    public StepCategory(RecipeType<Step> type, Component title, IDrawable icon, int noteLines) {
        super(type, title, icon, WIDTH, NOTE_Y + noteLines * 10);
        this.noteLines = noteLines;
    }

    static RecipeType<Step> type(String path) {
        return RecipeType.create(Hextale.MOD_ID, path, Step.class);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Step step, IFocusGroup focuses) {
        for (int i = 0; i < step.inputs().size(); i++) {
            slot(builder.addInputSlot(1 + i * INPUT_PITCH, ROW_Y).setStandardSlotBackground(), step.inputs().get(i));
        }
        slot(builder.addOutputSlot(RESULT_X, ROW_Y).setOutputSlotBackground(), step.result());
    }

    private static void slot(IRecipeSlotBuilder slot, Input input) {
        slot.addItemStacks(input.stacks());
        if (input.tooltip() != null) {
            slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(input.tooltip()));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Step step, IFocusGroup focuses) {
        for (int i = 1; i < step.inputs().size(); i++) {
            builder.addRecipePlusSign().setPosition(1 + i * INPUT_PITCH - 15, ROW_Y + 2);
        }
        builder.addRecipeArrow().setPosition(ARROW_X, ROW_Y);
        builder.addText(step.note(), WIDTH, noteLines * 10).setPosition(0, NOTE_Y).setColor(NOTE_COLOR);
    }
}
