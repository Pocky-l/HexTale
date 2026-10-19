package com.pockyl.hextale.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.infusion.InfusionRecipe;
import com.pockyl.hextale.infusion.InfusionRecipes;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.quest.QuestDisplay;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The researcher's guide: its recipes match the real ones, its order is the order things can be made in, goals are noticed. */
public final class QuestGameTests {
    private QuestGameTests() {
    }

    /** The crafting grids drawn in the book are the recipes the game uses. */
    @HexTest
    public static void questRecipesMatchTheRecipes(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ContextMap context = ContextMap.builder().set(SlotDisplayContext.REGISTRIES, server.registryAccess())
                .buildAndValidate(SlotDisplayContext.CONTEXT);
        for (Quest quest : Guide.all()) {
            QuestDisplay display = quest.display();
            if (display == null || !display.isRecipe()) {
                continue;
            }
            Identifier id = Identifier.parse(display.result());
            RecipeHolder<?> holder = server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id)).orElse(null);
            helper.assertTrue(holder != null, quest.id() + ": recipe " + id + " exists");
            Recipe<?> recipe = holder.value();
            ItemStack result = recipe.display().getFirst().result().resolveForFirstStack(context);
            helper.assertTrue(result.getItem() == BuiltInRegistries.ITEM.getValue(id) && result.getCount() == display.count(),
                    quest.id() + ": the result is " + display.count() + " x " + id);
            List<Ingredient> real = recipe.placementInfo().ingredients().stream().filter(i -> !i.isEmpty()).toList();
            List<String> shown = display.ingredients().stream().filter(i -> !i.isEmpty()).toList();
            helper.assertTrue(real.size() == shown.size(), quest.id() + ": " + shown.size() + " ingredients shown, recipe has " + real.size());
            for (int i = 0; i < real.size(); i++) {
                helper.assertTrue(real.get(i).test(sample(shown.get(i))), quest.id() + ": ingredient " + i + " is " + shown.get(i));
            }
        }
        helper.succeed();
    }

    private static ItemStack sample(String id) {
        if (id.startsWith("#")) {
            Iterator<Holder<Item>> items = BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, Identifier.parse(id.substring(1)))).iterator();
            return items.hasNext() ? new ItemStack(items.next()) : ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
    }

    /** Every step has its own spot, its parents come before it, every research topic has a step. */
    @HexTest
    public static void guideIsLaidOut(GameTestHelper helper) {
        Set<Long> spots = new HashSet<>();
        for (Quest quest : Guide.all()) {
            helper.assertTrue(spots.add(((long) quest.x() << 32) | (quest.y() & 0xFFFFFFFFL)), quest.id() + " has its own spot");
            for (Quest parent : quest.parents()) {
                helper.assertTrue(parent.index() < quest.index(), quest.id() + " follows " + parent.id());
                helper.assertTrue(parent.kind() != Quest.Kind.WIP, quest.id() + " never waits for a WIP device");
            }
            helper.assertTrue(quest.parents().isEmpty() == (quest == Guide.UNSTABLE_HEXTECH || quest == Guide.SPHALERITE
                            || quest == Guide.RESEARCHERS_DIARY),
                    quest.id() + ": only raw hextech, sphalerite and the diary start the guide");
            for (Quest other : Guide.all()) {
                if (other.index() > quest.index()) {
                    double distance = Math.hypot(other.x() - quest.x(), other.y() - quest.y());
                    helper.assertTrue(distance >= 34, quest.id() + " and " + other.id() + " do not crowd each other");
                }
            }
            helper.assertTrue(Guide.byId(quest.id()) == quest, quest.id() + " is found by id");
        }
        for (ResearchTopic topic : ResearchTopics.all()) {
            helper.assertTrue(Guide.of(topic).topic() == topic, topic.id() + " has its step");
        }
        helper.succeed();
    }

    /**
     * The guide reads at a glance: no two links cross, and no link runs over a step it does not join (links go from a
     * step's first parent to the step, straight).
     */
    @HexTest
    public static void guideLinksNeverCross(GameTestHelper helper) {
        List<Quest> linked = Guide.all().stream().filter(quest -> quest.parent() != null).toList();
        for (Quest a : linked) {
            for (Quest b : linked) {
                if (a.index() < b.index() && a.parent() != b.parent() && a.parent() != b && b.parent() != a) {
                    helper.assertFalse(crosses(a.parent(), a, b.parent(), b), "the links to " + a.id() + " and " + b.id() + " cross");
                }
            }
            for (Quest step : Guide.all()) {
                if (step != a && step != a.parent()) {
                    helper.assertTrue(distanceToLink(step, a.parent(), a) >= 14, "the link to " + a.id() + " runs over " + step.id());
                }
            }
        }
        helper.succeed();
    }

    private static boolean crosses(Quest a0, Quest a1, Quest b0, Quest b1) {
        long d1 = side(b0, b1, a0);
        long d2 = side(b0, b1, a1);
        long d3 = side(a0, a1, b0);
        long d4 = side(a0, a1, b1);
        return (d1 > 0) != (d2 > 0) && (d3 > 0) != (d4 > 0) && d1 != 0 && d2 != 0 && d3 != 0 && d4 != 0;
    }

    private static long side(Quest from, Quest to, Quest point) {
        return Long.signum((long) (to.x() - from.x()) * (point.y() - from.y()) - (long) (to.y() - from.y()) * (point.x() - from.x()));
    }

    private static double distanceToLink(Quest point, Quest from, Quest to) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double t = Math.clamp(((point.x() - from.x()) * dx + (point.y() - from.y()) * dy) / Math.max(1, dx * dx + dy * dy), 0, 1);
        return Math.hypot(from.x() + dx * t - point.x(), from.y() + dy * t - point.y());
    }

    /** What else a step gives besides its own result: things crafted straight from it. */
    private static final Map<String, List<String>> ALSO = Map.of(
            "brass", List.of("hextale:brass_nugget", "hextale:brass_block", "hextale:brass_grate"),
            "sphalerite", List.of("hextale:raw_zinc", "hextale:raw_zinc_block", "hextale:deepslate_sphalerite_ore"),
            "zinc", List.of("hextale:zinc_nugget", "hextale:zinc_block"),
            "hexsteel", List.of("hextale:hexsteel_block"),
            "magic_stone", List.of("hextale:polished_magic_stone", "hextale:magic_bricks", "hextale:magic_brick_slab",
                    "hextale:magic_brick_stairs", "hextale:magic_brick_wall", "hextale:chiseled_magic_bricks", "hextale:magic_stone_pillar"));

    /** Item ids a step yields: its icon, its picture's result and what is crafted straight from it. */
    private static Set<Identifier> yields(Quest quest) {
        Set<Identifier> out = new HashSet<>();
        out.add(BuiltInRegistries.ITEM.getKey(quest.icon().getItem()));
        for (QuestDisplay display = quest.display(); display != null; display = display.then()) {
            Identifier id = display.result().contains(":") ? Identifier.parse(display.result()) : null;
            switch (display.kind()) {
                case INFUSION -> {
                    InfusionRecipe recipe = InfusionRecipes.get(false).get(id);
                    if (recipe != null) {
                        out.add(BuiltInRegistries.ITEM.getKey(recipe.result().item().value()));
                    }
                }
                case HEX_CRAFTING -> {
                    var recipe = com.pockyl.hextale.crafting.HexCraftingRecipe.ALL.get(false).get(id);
                    if (recipe != null) {
                        out.add(BuiltInRegistries.ITEM.getKey(recipe.result().item().value()));
                    }
                }
                case ALLOY -> {
                    var recipe = com.pockyl.hextale.crafting.AlloyRecipe.ALL.get(false).get(id);
                    if (recipe != null) {
                        out.add(BuiltInRegistries.ITEM.getKey(recipe.result().item().value()));
                    }
                }
                default -> {
                    if (id != null) {
                        out.add(id);
                    }
                }
            }
        }
        ALSO.getOrDefault(quest.id(), List.of()).forEach(id -> out.add(Identifier.parse(id)));
        return out;
    }

    private static Set<Quest> ancestors(Quest quest) {
        Set<Quest> out = new HashSet<>();
        List<Quest> todo = new ArrayList<>(quest.parents());
        while (!todo.isEmpty()) {
            Quest next = todo.removeLast();
            if (out.add(next)) {
                todo.addAll(next.parents());
            }
        }
        return out;
    }

    /**
     * The guide's order is the order things can be made in: every mod item a step's recipe (crafting grid, scheme or
     * altar ritual) needs is yielded by a step before it; an altar ritual comes after the altar, its pedestals after
     * the Pedestal, its runes after the Rune Stele and after the first found rune scroll. Research needs ink at the table:
     * every step with a research topic comes after the Ink step.
     */
    @HexTest
    public static void guideOrderIsLogical(GameTestHelper helper) {
        for (Quest quest : Guide.all()) {
            if (quest.topic() != null) {
                helper.assertTrue(ancestors(quest).contains(Guide.INK), quest.id() + " is researched, so it comes after the ink");
            }
            Set<Quest> before = ancestors(quest);
            Set<Identifier> available = new HashSet<>();
            before.forEach(step -> available.addAll(yields(step)));
            List<Identifier> needed = new ArrayList<>();
            for (QuestDisplay display = quest.display(); display != null; display = display.then()) {
            switch (display.kind()) {
                case SHAPED, SHAPELESS, SCHEME -> display.ingredients().stream().filter(id -> !id.isEmpty() && !id.startsWith("#"))
                        .map(Identifier::parse).forEach(needed::add);
                case INFUSION -> {
                    InfusionRecipe recipe = InfusionRecipes.get(false).get(Identifier.parse(display.result()));
                    helper.assertTrue(recipe != null, quest.id() + ": ritual " + display.result() + " exists");
                    helper.assertTrue(before.contains(Guide.HEXTECH_ALTAR), quest.id() + " comes after the altar");
                    helper.assertTrue(recipe.pedestals().isEmpty() || before.contains(Guide.PEDESTAL), quest.id() + " comes after the Pedestal");
                    helper.assertTrue(recipe.runes().isEmpty() || before.contains(Guide.RUNE_STELE), quest.id() + " comes after the Rune Stele");
                    helper.assertTrue(recipe.runes().isEmpty() || before.contains(Guide.RUNE_SCROLL), quest.id() + " comes after a found rune");
                    List<Ingredient> parts = new ArrayList<>(recipe.pedestals());
                    parts.add(recipe.center());
                    for (Ingredient part : parts) {
                        part.items().findFirst().ifPresent(item -> needed.add(BuiltInRegistries.ITEM.getKey(item.value())));
                    }
                }
                case HEX_CRAFTING -> {
                    var recipe = com.pockyl.hextale.crafting.HexCraftingRecipe.ALL.get(false).get(Identifier.parse(display.result()));
                    helper.assertTrue(recipe != null, quest.id() + ": workbench recipe " + display.result() + " exists");
                    helper.assertTrue(before.contains(Guide.HEX_WORKBENCH), quest.id() + " comes after the Hex Workbench");
                    for (var cell : recipe.grid()) {
                        cell.flatMap(part -> part.items().findFirst()).ifPresent(item -> needed.add(BuiltInRegistries.ITEM.getKey(item.value())));
                    }
                }
                case ALLOY -> {
                    var recipe = com.pockyl.hextale.crafting.AlloyRecipe.ALL.get(false).get(Identifier.parse(display.result()));
                    helper.assertTrue(recipe != null, quest.id() + ": alloy " + display.result() + " exists");
                    helper.assertTrue(before.contains(Guide.HEX_FURNACE), quest.id() + " comes after the Hex Furnace");
                    for (var input : recipe.inputs()) {
                        input.ingredient().items().findFirst().ifPresent(item -> needed.add(BuiltInRegistries.ITEM.getKey(item.value())));
                    }
                }
                default -> {
                }
            }
            }
            for (Identifier id : needed) {
                if (id.getNamespace().equals(Hextale.MOD_ID)) {
                    helper.assertTrue(available.contains(id), quest.id() + " needs " + id + ", which no step before it yields");
                }
            }
        }
        helper.succeed();
    }

    @HexTest
    public static void questsNoticeTheirGoals(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        RuneKnowledge knowledge = Research.of(player);
        Quests.check(player);
        helper.assertFalse(knowledge.questDone(Guide.RESEARCH_TABLE), "nothing is done with empty hands");

        player.getInventory().add(new ItemStack(ModItems.BRASS_INGOT.get()));
        Quests.check(player);
        helper.assertTrue(knowledge.questDone(Guide.BRASS), "a brass ingot completes Brass");
        helper.assertFalse(knowledge.questDone(Guide.RESEARCH_TABLE), "the table is still to make");

        knowledge.study(Identifier.withDefaultNamespace("coal"));
        Quests.check(player);
        helper.assertTrue(knowledge.questDone(Guide.ITEM_STUDY), "one studied item completes Studying Items");

        player.getInventory().clearContent();
        Quests.check(player);
        helper.assertTrue(knowledge.questDone(Guide.BRASS), "a done quest stays done");

        RuneKnowledge decoded = RuneKnowledge.CODEC.parse(NbtOps.INSTANCE,
                RuneKnowledge.CODEC.encodeStart(NbtOps.INSTANCE, knowledge).getOrThrow()).getOrThrow();
        helper.assertTrue(decoded.questDone(Guide.BRASS) && decoded.questDone(Guide.ITEM_STUDY)
                && !decoded.questDone(Guide.RESEARCH_TABLE), "done quests save and load");
        helper.assertTrue(knowledge.copy().questDone(Guide.ITEM_STUDY), "done quests are synced");
        helper.succeed();
    }

    /** No single road: the first brass already opens the workshop, the essence works and the goggles at once. */
    @HexTest
    public static void theGuideBranchesEarly(GameTestHelper helper) {
        RuneKnowledge knowledge = new RuneKnowledge();
        helper.assertTrue(Guide.visible(Guide.UNSTABLE_HEXTECH, knowledge) && Guide.visible(Guide.SPHALERITE, knowledge)
                && Guide.visible(Guide.RESEARCHERS_DIARY, knowledge), "three ways to begin");
        knowledge.completeQuest(Guide.UNSTABLE_HEXTECH);
        knowledge.completeQuest(Guide.HEXTECH_DUST);
        helper.assertTrue(Guide.open(Guide.HEX_WORKBENCH, knowledge) && Guide.open(Guide.HEX_FURNACE, knowledge),
                "dust opens the workbench and the furnace at once");
        helper.assertFalse(Guide.open(Guide.BRASS, knowledge), "brass also needs zinc");
        knowledge.completeQuest(Guide.HEX_FURNACE);
        knowledge.completeQuest(Guide.SPHALERITE);
        knowledge.completeQuest(Guide.ZINC);
        helper.assertTrue(Guide.open(Guide.BRASS, knowledge), "the furnace and zinc open brass");
        knowledge.completeQuest(Guide.BRASS);
        knowledge.completeQuest(Guide.HEX_WORKBENCH);
        for (Quest quest : List.of(Guide.RESEARCH_TABLE, Guide.ALEMBIC, Guide.RESEARCHERS_GOGGLES)) {
            helper.assertTrue(Guide.open(quest, knowledge), quest.id() + " opens with brass on the workbench");
        }
        helper.succeed();
    }

    /** Research opens along the guide: the circle once the ink is at the table, the gemstone once the pedestal is made. */
    @HexTest
    public static void researchOpensAlongTheGuide(GameTestHelper helper) {
        RuneKnowledge knowledge = new RuneKnowledge();
        helper.assertFalse(Guide.canResearch(ResearchTopics.CIRCLE, knowledge), "the circle waits for ink at the table");
        helper.assertFalse(Guide.visible(Guide.HEXTECH_ALTAR, knowledge), "and is hidden until then");
        for (Quest quest : ancestors(Guide.HEXTECH_ALTAR)) {
            knowledge.completeQuest(quest);
        }
        helper.assertTrue(Guide.canResearch(ResearchTopics.CIRCLE, knowledge), "the ink opens the circle's research");
        helper.assertFalse(Guide.canResearch(ResearchTopics.LAPIDARY, knowledge), "the lapidary waits for the pedestal");
        knowledge.completeQuest(Guide.HEXTECH_ALTAR);
        knowledge.completeQuest(Guide.PEDESTAL);
        helper.assertTrue(Guide.canResearch(ResearchTopics.LAPIDARY, knowledge), "the pedestal opens the lapidary");
        helper.assertFalse(Guide.canResearch(ResearchTopics.RADIANT_PICKAXE, knowledge), "the radiant pickaxe is far away in the fog");
        helper.succeed();
    }

    /** Runes are no steps of the guide; a found scroll alone opens the rune lore. */
    @HexTest
    public static void aFoundScrollOpensTheRuneLore(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        RuneKnowledge knowledge = Research.of(player);
        knowledge.completeQuest(Guide.BRASS);
        knowledge.completeQuest(Guide.UNSTABLE_HEXTECH);
        knowledge.completeQuest(Guide.RESEARCHERS_DIARY);
        Quests.check(player);
        helper.assertFalse(knowledge.questDone(Guide.RUNE_SCROLL), "no rune, no lore");
        knowledge.learn(ResearchBook.FIRST, 2);
        Quests.check(player);
        helper.assertTrue(knowledge.questDone(Guide.RUNE_SCROLL), "the first studied scroll completes the step");
        helper.assertTrue(Guide.open(Guide.BLANK_SCROLL, knowledge), "and opens the blank scroll");
        helper.succeed();
    }
}
