package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;

import com.pockyl.hextale.block.CrucibleBlockEntity;
import com.pockyl.hextale.crafting.BrewRecipe;
import com.pockyl.hextale.crafting.CrucibleRecipe;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.quest.QuestDisplay;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.research.ItemEssenceTable;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;

import java.util.List;

/**
 * Transmutation: the Crucible turns an item into a kindred one (sand into red sand) and back, for essence from the
 * water, once the researcher has worked the topic out at the Lab Bench. Every change has its way back, takes at least a
 * small fee and never gives more essence than went in, so no loop of changes and dissolving makes essence.
 */
public final class TransmutationGameTests {
    private TransmutationGameTests() {
    }

    private static final BlockPos AT = new BlockPos(0, 2, 0);
    /** The least essence a change takes on top of what the result carries more than the catalyst. */
    private static final int FEE = 2;

    private static List<CrucibleRecipe> of(ResearchTopic topic) {
        return CrucibleRecipe.ALL.get(false).values().stream().filter(recipe -> recipe.research().orElse(null) == topic).toList();
    }

    private static Item catalyst(GameTestHelper helper, CrucibleRecipe recipe) {
        List<Holder<Item>> items = recipe.catalyst().items().toList();
        helper.assertTrue(items.size() == 1, "a transmutation starts from one item, not " + items.size());
        return items.getFirst().value();
    }

    @HexTest
    public static void everyTransmutationGoesBackAndMakesNoEssence(GameTestHelper helper) {
        helper.assertTrue(of(ResearchTopics.TRANSMUTATION).size() >= 40, "plenty of simple transmutations");
        helper.assertTrue(of(ResearchTopics.ADVANCED_TRANSMUTATION).size() >= 24, "plenty of advanced transmutations");
        for (ResearchTopic topic : List.of(ResearchTopics.TRANSMUTATION, ResearchTopics.ADVANCED_TRANSMUTATION)) {
            helper.assertTrue(topic.lab() && topic.items().isEmpty(), topic.id() + " is a Lab Bench topic that locks no item");
            List<CrucibleRecipe> recipes = of(topic);
            for (CrucibleRecipe recipe : recipes) {
                Item from = catalyst(helper, recipe);
                Item to = recipe.result().item().value();
                String name = from + " -> " + to;
                helper.assertTrue(recipe.result().count() == 1, name + " gives one item for one");
                helper.assertTrue(recipes.stream().anyMatch(back -> catalyst(helper, back) == to && back.result().item().value() == from),
                        name + " also works backwards under the same research");
                ItemStack thrown = new ItemStack(from);
                long starts = CrucibleRecipe.ALL.get(false).values().stream().filter(other -> other.catalyst().test(thrown)).count();
                helper.assertTrue(starts == 1, name + ": no other Crucible recipe starts from " + from);
                BrewRecipe brew = BrewRecipe.anyFor(thrown, false);
                if (brew != null) {
                    for (Essence essence : recipe.essences().asMap().keySet()) {
                        helper.assertTrue(brew.essences().get(essence) == 0, name + " shares no essence with the brew of the same catalyst");
                    }
                }
                helper.assertTrue(ResearchTopics.of(from) == null && ResearchTopics.of(to) == null, name + ": plain items stay free elsewhere");
                ItemEssences gives = ItemEssenceTable.get(to);
                ItemEssences had = ItemEssenceTable.get(from);
                helper.assertTrue(gives != null && had != null, name + ": both items carry essences");
                EssenceMix paid = recipe.essences();
                for (var entry : gives.essences().entrySet()) {
                    int in = had.essences().getOrDefault(entry.getKey(), 0) + paid.get(entry.getKey());
                    helper.assertTrue(entry.getValue() <= in, name + " gives " + entry.getValue() + " " + entry.getKey().id()
                            + " but only " + in + " went in");
                }
                int more = 0;
                for (var entry : gives.essences().entrySet()) {
                    more += Math.max(0, entry.getValue() - had.essences().getOrDefault(entry.getKey(), 0));
                }
                helper.assertTrue(paid.total() >= more + FEE, name + " costs at least " + FEE + " essence on top");
            }
        }
        helper.succeed();
    }

    /** The diary step of each topic shows every one of its transmutations: a picture per pair, there and back. */
    @HexTest
    public static void theDiaryShowsEveryTransmutation(GameTestHelper helper) {
        for (ResearchTopic topic : List.of(ResearchTopics.TRANSMUTATION, ResearchTopics.ADVANCED_TRANSMUTATION)) {
            Quest step = Guide.server().forTopic(topic);
            helper.assertTrue(step != null, topic.id() + " has a diary step");
            helper.assertTrue(step.icon().is(ModItems.GUIDE_MARK.get()), topic.id() + " keeps its own sign on the map");
            helper.assertTrue(step.icons().size() == step.displays().size(), topic.id() + ": one thing in the row per picture");
            java.util.Set<CrucibleRecipe> shown = new java.util.HashSet<>();
            for (QuestDisplay display : step.displays()) {
                for (QuestDisplay part = display; part != null; part = part.then()) {
                    CrucibleRecipe recipe = CrucibleRecipe.ALL.named(Identifier.tryParse(part.result()), false);
                    helper.assertTrue(recipe != null && recipe.research().orElse(null) == topic,
                            topic.id() + " shows only its own transmutations, not " + part.result());
                    shown.add(recipe);
                }
            }
            for (CrucibleRecipe recipe : of(topic)) {
                helper.assertTrue(shown.contains(recipe), topic.id() + " shows " + recipe.result().item().value() + " in the diary");
            }
        }
        helper.succeed();
    }

    /**
     * No two Crucible recipes or brews that take the same item ask for the same essence: what the water holds decides
     * clearly what the item becomes (calcite: magic for magic stone, ash and shadow for nullstone).
     */
    @HexTest
    public static void noCatalystHasTwoRecipesWithTheSameEssence(GameTestHelper helper) {
        record Use(String name, Ingredient catalyst, java.util.Set<Essence> essences) {
        }
        List<Use> uses = new java.util.ArrayList<>();
        CrucibleRecipe.ALL.get(false).forEach((id, recipe) -> uses.add(new Use(id.toString(), recipe.catalyst(), recipe.essences().asMap().keySet())));
        BrewRecipe.ALL.get(false).forEach((id, recipe) -> uses.add(new Use(id.toString(), recipe.catalyst(), recipe.essences().asMap().keySet())));
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            List<Use> takes = uses.stream().filter(use -> use.catalyst().test(stack)).toList();
            for (int i = 0; i < takes.size(); i++) {
                for (int j = i + 1; j < takes.size(); j++) {
                    java.util.Set<Essence> shared = new java.util.HashSet<>(takes.get(i).essences());
                    shared.retainAll(takes.get(j).essences());
                    helper.assertTrue(shared.isEmpty(), item + " starts " + takes.get(i).name() + " and " + takes.get(j).name()
                            + " with the same essence " + shared);
                }
            }
        }
        helper.succeed();
    }

    /** Sand with magma in the water dissolves for a researcher who has not worked it out, and turns red once they have. */
    @HexTest(maxTicks = 700)
    public static void sandTurnsRedOnlyAfterTheResearch(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        boolean[] done = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (!done[0]) {
                crucible.pour(EssenceMix.pure(Essence.MAGMA, 6));
                helper.assertTrue(crucible.take(new ItemStack(Items.SAND), 1, true, player) == 1, "the sand is taken");
                helper.assertTrue(crucible.essence().get(Essence.MAGMA) == 6 && crucible.essence().get(Essence.EARTH) > 0,
                        "without the research the sand only dissolves");
                Research.research(player, ResearchTopics.TRANSMUTATION);
                helper.assertTrue(crucible.take(new ItemStack(Items.SAND, 5), 5, true, player) == 3,
                        "six magma pays for three sand, the rest waits");
                helper.assertTrue(crucible.essence().get(Essence.MAGMA) == 0, "the change took the magma");
                helper.assertTrue(crucible.take(new ItemStack(Items.GLOWSTONE_DUST), 1, true, player) == 1
                        && crucible.essence().get(Essence.SPARK) == 0, "advanced transmutation still waits for its own research");
                done[0] = true;
            }
            int red = crucible.floating().stream().filter(stack -> stack.is(Items.RED_SAND)).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(red == 3, "three red sand float up");
        });
    }
}
