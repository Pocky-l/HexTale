package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.crafting.CrucibleRecipe;
import com.pockyl.hextale.item.EssenceItem;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Crucible transmutations: the essences the water must hold (as essence items, with their mB under them), the
 * catalyst thrown in, and what rises out.
 */
public class CrucibleCategory extends AbstractRecipeCategory<CrucibleCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "crucible", Recipe.class);
    private static final int WIDTH = 150;
    private static final int ESSENCES_X = 1;
    private static final int CATALYST_X = 80;
    private static final int RESULT_X = 128;
    private static final int ROW_Y = 6;

    public record Recipe(Identifier id, CrucibleRecipe recipe) {
        List<Map.Entry<Essence, Integer>> essences() {
            return new ArrayList<>(recipe.essences().asMap().entrySet());
        }
    }

    public CrucibleCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("block.hextale.crucible"), guiHelper.createDrawableItemLike(ModItems.CRUCIBLE.get()), WIDTH, 40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        List<Map.Entry<Essence, Integer>> essences = recipe.essences();
        for (int i = 0; i < essences.size() && i < 4; i++) {
            builder.addInputSlot(ESSENCES_X + i * 18, ROW_Y).setStandardSlotBackground().add(EssenceItem.of(essences.get(i).getKey()));
        }
        builder.addInputSlot(CATALYST_X, ROW_Y).setStandardSlotBackground().add(recipe.recipe().catalyst());
        builder.addOutputSlot(RESULT_X, ROW_Y).setOutputSlotBackground().add(recipe.recipe().result());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        List<Map.Entry<Essence, Integer>> essences = recipe.essences();
        for (int i = 0; i < essences.size() && i < 4; i++) {
            String amount = essences.get(i).getValue() + " mB";
            int x = ESSENCES_X + i * 18 + 9 - font.width(amount) / 2;
            graphics.text(font, amount, x, ROW_Y + 20, 0xFF000000 | essences.get(i).getKey().color(), true);
        }
        // "+" between the water's essences and the catalyst, an arrow to what rises out.
        int plus = CATALYST_X - 7;
        PixelArt.fill(graphics, plus - 3, ROW_Y + 8, plus + 4, ROW_Y + 9, PixelArt.argb(0x8B8B8B, 1));
        PixelArt.fill(graphics, plus, ROW_Y + 5, plus + 1, ROW_Y + 12, PixelArt.argb(0x8B8B8B, 1));
        int ax = CATALYST_X + 22;
        PixelArt.fill(graphics, ax, ROW_Y + 8, ax + 16, ROW_Y + 9, PixelArt.argb(0x8B8B8B, 1));
        for (int i = 0; i < 4; i++) {
            PixelArt.fill(graphics, ax + 16 + i, ROW_Y + 5 + i, ax + 17 + i, ROW_Y + 12 - i, PixelArt.argb(0x8B8B8B, 1));
        }
        PixelArt.flush(graphics);
    }

    @Override
    public Identifier getIdentifier(Recipe recipe) {
        return recipe.id();
    }
}
