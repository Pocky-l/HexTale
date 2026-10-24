package com.pockyl.hextale.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.screen.MultiblockPreview;
import com.pockyl.hextale.multiblock.Multiblock;
import com.pockyl.hextale.multiblock.MultiblockPattern;
import com.pockyl.hextale.multiblock.Multiblocks;
import com.pockyl.hextale.registry.ModItems;

import java.util.List;

/**
 * Multiblocks: the structure as a turning 3D model that builds itself layer by layer (arrows, or the wheel over the
 * model, pick a layer; dragging turns it), under it every block it takes with counts and the hextech dust that wakes it.
 */
public class MultiblockCategory extends AbstractRecipeCategory<Multiblock> {
    public static final IRecipeType<Multiblock> TYPE = IRecipeType.create(Hextale.MOD_ID, "multiblock", Multiblock.class);
    private static final int WIDTH = 160;
    private static final int SCENE = 124;
    private static final int PITCH = 18;
    private static final int PER_ROW = WIDTH / PITCH;
    private static final int MAX_ROWS = 2;

    public MultiblockCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.hextale.multiblock"), guiHelper.createDrawableItemLike(ModItems.HEXTECH_DUST.get()),
                WIDTH, SCENE + 4 + MAX_ROWS * PITCH + 12);
    }

    public static List<Multiblock> all() {
        return Multiblocks.all();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Multiblock multiblock, IFocusGroup focuses) {
        List<MultiblockPattern.Material> materials = multiblock.pattern().materials(-1);
        int top = SCENE + 4;
        for (int i = 0; i < materials.size() && i < PER_ROW * MAX_ROWS - 1; i++) {
            MultiblockPattern.Material material = materials.get(i);
            List<ItemStack> stacks = material.key().shown().get().stream().map(stack -> stack.copyWithCount(material.count())).toList();
            builder.addInputSlot(1 + i % PER_ROW * PITCH, top + i / PER_ROW * PITCH).setStandardSlotBackground().addItemStacks(stacks);
        }
        builder.addInputSlot(WIDTH - 17, top).setStandardSlotBackground().addItemStacks(multiblock.activators())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.multiblock.wake")));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Multiblock multiblock, IFocusGroup focuses) {
        Model model = new Model(new MultiblockPreview(multiblock));
        builder.addWidget(model);
        builder.addInputHandler(model);
    }

    @Override
    public ResourceLocation getIdentifier(Multiblock multiblock) {
        return Hextale.id("multiblock/" + multiblock.id());
    }

    /** The 3D model as a JEI widget: drawn in the recipe's top part, takes clicks, drags and the wheel. */
    private record Model(MultiblockPreview preview) implements IRecipeWidget, IJeiInputHandler {
        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public ScreenRectangle getArea() {
            return new ScreenRectangle(0, 0, WIDTH, SCENE);
        }

        @Override
        public void drawWidget(GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            preview.render(graphics, 0, 0, WIDTH, SCENE, (int) mouseX, (int) mouseY, System.currentTimeMillis() / 50F);
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
            List<Component> lines = preview.tooltip(mouseX, mouseY);
            if (lines != null) {
                tooltip.addAll(lines);
            }
        }

        @Override
        public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
            if (input.getKey().getType() != InputConstants.Type.MOUSE) {
                return false;
            }
            if (input.isSimulate()) {
                return preview.overScene(mouseX, mouseY) || mouseY >= SCENE - MultiblockPreview.BAR && mouseY < SCENE;
            }
            return preview.click(mouseX, mouseY);
        }

        @Override
        public boolean handleMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            return preview.scroll(mouseX, mouseY, scrollDeltaY);
        }

        @Override
        public boolean handleMouseDragged(double mouseX, double mouseY, InputConstants.Key mouseKey, double dragX, double dragY) {
            if (!preview.overScene(mouseX, mouseY)) {
                return false;
            }
            preview.drag(dragX);
            return true;
        }
    }
}
