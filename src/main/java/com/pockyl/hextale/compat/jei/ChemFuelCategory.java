package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ChemBurnerBlockEntity;
import com.pockyl.hextale.block.PinkBrazierBlockEntity;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;

/**
 * What the Chem Burner and the Pink Brazier burn and for how long: vials of combustible mix and shimmer, and essence piped
 * or poured into the burner's tank (per millibucket; unknown essence burns twice as long).
 */
public class ChemFuelCategory extends AbstractRecipeCategory<ChemFuelCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "chem_fuel", Recipe.class);
    private static final int TEXT_X = 22;

    /** The fuel and its lines: which device burns it and for how long. */
    public record Recipe(List<ItemStack> fuel, List<Component> lines) {
    }

    public ChemFuelCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.hextale.chem_fuel"), guiHelper.createDrawableItemLike(ModItems.CHEM_BURNER.get()), StepCategory.WIDTH, 34);
    }

    public static List<Recipe> all() {
        List<Recipe> out = new ArrayList<>();
        for (Brew brew : Brew.values()) {
            int burner = brew.burnTicks();
            int brazier = PinkBrazierBlockEntity.burnTicks(ChemVialItem.of(brew));
            if (burner <= 0 && brazier <= 0) {
                continue;
            }
            List<Component> lines = new ArrayList<>();
            if (burner > 0) {
                lines.add(Component.translatable("jei.hextale.chem_fuel.burner", DeviceSteps.seconds(burner)));
            }
            if (brazier > 0) {
                lines.add(Component.translatable("jei.hextale.chem_fuel.brazier", DeviceSteps.seconds(brazier)));
            }
            out.add(new Recipe(List.of(ChemVialItem.of(brew)), lines));
        }
        List<ItemStack> flasks = new ArrayList<>();
        for (Essence essence : Essence.values()) {
            flasks.add(HextaleJeiPlugin.concentrate(essence));
        }
        out.add(new Recipe(flasks, List.of(Component.translatable("jei.hextale.chem_fuel.essence", DeviceSteps.seconds(ChemBurnerBlockEntity.ESSENCE_TICKS)))));
        out.add(new Recipe(List.of(EssenceFlaskItem.filled(EssenceMix.unknown(EssenceFlaskItem.CAPACITY))),
                List.of(Component.translatable("jei.hextale.chem_fuel.essence", DeviceSteps.seconds(ChemBurnerBlockEntity.UNKNOWN_TICKS)))));
        return out;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStacks(recipe.fuel());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeFlame(300).setPosition(3, 20);
        for (int i = 0; i < recipe.lines().size(); i++) {
            builder.addText(recipe.lines().get(i), StepCategory.WIDTH - TEXT_X, 10).setPosition(TEXT_X, 2 + i * 12).setColor(StepCategory.NOTE_COLOR);
        }
    }
}
