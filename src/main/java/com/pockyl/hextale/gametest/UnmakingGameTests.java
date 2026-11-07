package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;

import com.pockyl.hextale.block.CrucibleBlockEntity;
import com.pockyl.hextale.crafting.CrucibleRecipe;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.research.ItemEssenceTable;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unmaking: the Crucible takes a made thing apart again (wool into string) for essence from the water, once the
 * researcher has worked the topic out at the Lab Bench. No unmaking makes essence, and no way back through crafting or
 * smelting gives more than went in.
 */
public final class UnmakingGameTests {
    private UnmakingGameTests() {
    }

    private static final BlockPos AT = new BlockPos(0, 2, 0);
    /** The least essence an unmaking takes on top of what its results carry more than the catalyst. */
    private static final int FEE = 2;
    /** How many recipes a way back may take (bricks to clay balls goes back over bricks' own ingredient, the brick). */
    private static final int WAY_BACK = 4;

    /** One recipe that makes {@code gain} of {@code to} for each single {@code from}. */
    private record Step(Item to, double gain) {
    }

    private static List<CrucibleRecipe> recipes() {
        return CrucibleRecipe.ALL.get(false).values().stream()
                .filter(recipe -> recipe.research().orElse(null) == ResearchTopics.UNMAKING).toList();
    }

    /** Every recipe of one kind of ingredient (four string make wool), from that ingredient to the result. */
    private static Map<Item, List<Step>> steps(GameTestHelper helper) {
        Map<Item, List<Step>> steps = new HashMap<>();
        for (RecipeHolder<?> holder : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe.isSpecial()) {
                continue;
            }
            ItemStack result = recipe.getResultItem(helper.getLevel().registryAccess());
            List<Ingredient> slots = recipe.getIngredients().stream().filter(slot -> !slot.isEmpty()).toList();
            if (result.isEmpty() || slots.isEmpty()) {
                continue;
            }
            Set<Item> shared = null;
            for (Ingredient slot : slots) {
                Set<Item> items = new HashSet<>(com.pockyl.hextale.port.PortIngredients.items(slot).map(Holder::value).toList());
                if (shared == null) {
                    shared = items;
                } else {
                    shared.retainAll(items);
                }
            }
            double gain = result.getCount() / (double) slots.size();
            for (Item item : shared) {
                steps.computeIfAbsent(item, key -> new ArrayList<>()).add(new Step(result.getItem(), gain));
            }
        }
        return steps;
    }

    /** The most of {@code goal} that {@code amount} of {@code from} can become within {@code left} recipes (0: none). */
    private static double wayBack(Map<Item, List<Step>> steps, Item from, double amount, Item goal, int left) {
        if (from == goal) {
            return amount;
        }
        double best = 0;
        if (left > 0) {
            for (Step step : steps.getOrDefault(from, List.of())) {
                best = Math.max(best, wayBack(steps, step.to(), amount * step.gain(), goal, left - 1));
            }
        }
        return best;
    }

    @HexTest
    public static void noUnmakingMakesEssence(GameTestHelper helper) {
        helper.assertTrue(recipes().size() >= 15, "plenty of things come apart");
        helper.assertTrue(ResearchTopics.UNMAKING.lab() && ResearchTopics.UNMAKING.items().isEmpty(), "a Lab Bench topic that locks no item");
        Map<Item, List<Step>> steps = steps(helper);
        for (CrucibleRecipe recipe : recipes()) {
            Item to = recipe.result().item().value();
            int count = recipe.result().count();
            for (Holder<Item> holder : com.pockyl.hextale.port.PortIngredients.items(recipe.catalyst()).toList()) {
                Item from = holder.value();
                String name = from + " -> " + count + " " + to;
                helper.assertTrue(ResearchTopics.of(from) == null && ResearchTopics.of(to) == null, name + ": plain items stay free elsewhere");
                // Crafting already takes a hay bale apart for free: the Crucible adds no essence to what that gives.
                if (wayBack(steps, from, 1, to, 1) >= count) {
                    continue;
                }
                ItemEssences gives = ItemEssenceTable.get(to);
                ItemEssences had = ItemEssenceTable.get(from);
                helper.assertTrue(gives != null && had != null, name + ": both items carry essences");
                EssenceMix paid = recipe.essences();
                int more = 0;
                for (var entry : gives.essences().entrySet()) {
                    int out = entry.getValue() * count;
                    int held = had.essences().getOrDefault(entry.getKey(), 0);
                    helper.assertTrue(out <= held + paid.get(entry.getKey()), name + " gives " + out + " " + entry.getKey().id()
                            + " but only " + (held + paid.get(entry.getKey())) + " went in");
                    more += Math.max(0, out - held);
                }
                helper.assertTrue(paid.total() >= more + FEE, name + " costs at least " + FEE + " essence on top");
            }
        }
        helper.succeed();
    }

    /** Taking a thing apart and making it again never leaves more of it than there was. */
    @HexTest
    public static void noUnmakingIsALoop(GameTestHelper helper) {
        Map<Item, List<Step>> steps = steps(helper);
        for (CrucibleRecipe recipe : recipes()) {
            Item to = recipe.result().item().value();
            for (Holder<Item> holder : com.pockyl.hextale.port.PortIngredients.items(recipe.catalyst()).toList()) {
                double back = wayBack(steps, to, recipe.result().count(), holder.value(), WAY_BACK);
                helper.assertTrue(back <= 1.001, holder.value() + " unmade and made again gives " + back + " for one");
            }
        }
        helper.succeed();
    }

    /** Wool with decay in the water dissolves for a researcher who has not worked it out, and comes apart once they have. */
    @HexTest(maxTicks = 700)
    public static void woolComesApartOnlyAfterTheResearch(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.<CrucibleBlockEntity>getBlockEntity(AT);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        ServerPlayer player = MockPlayers.inLevel(helper);
        boolean[] done = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (!done[0]) {
                crucible.pour(EssenceMix.pure(Essence.DECAY, 4));
                helper.assertTrue(crucible.take(new ItemStack(Items.RED_WOOL), 1, true, player) == 1, "the wool is taken");
                helper.assertTrue(crucible.essence().get(Essence.DECAY) == 4 && crucible.essence().get(Essence.BEAST) > 0,
                        "without the research the wool only dissolves");
                Research.research(player, ResearchTopics.UNMAKING);
                helper.assertTrue(crucible.take(new ItemStack(Items.RED_WOOL, 5), 5, true, player) == 2,
                        "four decay pays for two wool, the rest waits");
                helper.assertTrue(crucible.essence().get(Essence.DECAY) == 0, "the unmaking took the decay");
                done[0] = true;
            }
            int string = crucible.floating().stream().filter(stack -> stack.is(Items.STRING)).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(string == 8, "eight string float up");
        });
    }
}
