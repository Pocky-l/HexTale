package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.item.ResearchNotesItem;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ExperimentPlan;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;

import java.util.List;

/**
 * Lab Bench experiments: a glass bottle on the bench (and the sample, when the topic wants one) gives Research Notes on
 * the topic. Under them what the flask must hold to seal it and the reactions that lead there.
 */
public class LabExperimentCategory extends AbstractRecipeCategory<ExperimentPlan> {
    public static final RecipeType<ExperimentPlan> TYPE = RecipeType.create(Hextale.MOD_ID, "lab_experiment", ExperimentPlan.class);
    private static final int GOAL_Y = StepCategory.NOTE_Y;
    private static final int CHAIN_Y = GOAL_Y + 21;

    public LabExperimentCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.hextale.lab_experiment"), guiHelper.createDrawableItemLike(ModItems.LAB_BENCH.get()),
                StepCategory.WIDTH, CHAIN_Y + 20);
    }

    public static List<ExperimentPlan> all() {
        return ResearchTopics.lab().stream().map(topic -> ExperimentPlan.of(topic, true)).toList();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ExperimentPlan plan, IFocusGroup focuses) {
        builder.addInputSlot(1, StepCategory.ROW_Y).setStandardSlotBackground().addItemStack(new ItemStack(Items.GLASS_BOTTLE))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.lab_experiment.flask")));
        plan.sample().ifPresent(sample -> builder.addInputSlot(35, StepCategory.ROW_Y).setStandardSlotBackground().addIngredientsUnsafe(com.pockyl.hextale.port.PortJei.stacks(sample))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.lab_experiment.sample"))));
        ResearchTopic topic = plan.topic();
        builder.addOutputSlot(StepCategory.RESULT_X, StepCategory.ROW_Y).setOutputSlotBackground().addItemStack(ResearchNotesItem.of(topic))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.lab_experiment.opens",
                        Component.translatable(topic.translationKey()).withColor(topic.color()))));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ExperimentPlan plan, IFocusGroup focuses) {
        if (plan.needsSample()) {
            builder.addRecipePlusSign().setPosition(20, StepCategory.ROW_Y + 2);
        }
        builder.addRecipeArrow().setPosition(StepCategory.ARROW_X, StepCategory.ROW_Y);
        Component goal = Component.translatable("gui.hextale.lab_bench.goal", plan.amount(), name(plan.target()),
                Math.round(plan.purity() * 100));
        builder.addText(goal, StepCategory.WIDTH, 20).setPosition(0, GOAL_Y).setColor(StepCategory.NOTE_COLOR);
        MutableComponent chain = Component.empty();
        for (Essence compound : plan.chain()) {
            if (!chain.getSiblings().isEmpty()) {
                chain.append(Component.literal(" → ").withColor(0x6A6480));
            }
            chain.append(name(compound));
        }
        builder.addText(Component.translatable("jei.hextale.lab_experiment.chain", chain), StepCategory.WIDTH, 20)
                .setPosition(0, CHAIN_Y).setColor(StepCategory.NOTE_COLOR);
    }

    private static Component name(Essence essence) {
        // Darkened: the pale essences would not read on JEI's light panel.
        return Component.translatable(essence.translationKey()).withColor(PixelArt.mix(essence.color(), 0x000000, 0.35F));
    }
}
