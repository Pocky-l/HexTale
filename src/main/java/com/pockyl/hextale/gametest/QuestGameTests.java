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
import java.util.HashMap;
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
        for (Quest quest : Guide.server().all()) {
            for (QuestDisplay display : quest.displays()) {
            if (!display.isRecipe()) {
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

    /** The guide comes from its data files, and every step in them made it in (no missing parents, no circles). */
    @HexTest
    public static void guideLoadsFromData(GameTestHelper helper) {
        Guide guide = Guide.server();
        helper.assertTrue(guide.all().size() > 100, "the guide's data files were read");
        helper.assertTrue(guide.all().size() == guide.definitions().size(), "every step in the data is in the guide");
        helper.succeed();
    }

    /** Every step has its own spot, its parents come before it, every research topic has a step. */
    @HexTest
    public static void guideIsLaidOut(GameTestHelper helper) {
        Set<Long> spots = new HashSet<>();
        for (Quest quest : Guide.server().all()) {
            helper.assertTrue(spots.add(((long) quest.x() << 32) | (quest.y() & 0xFFFFFFFFL)), quest.id() + " has its own spot");
            for (Quest parent : quest.parents()) {
                helper.assertTrue(parent.index() < quest.index(), quest.id() + " follows " + parent.id());
                // A step may stand behind a device still being worked out (the plan between two benches); it only has to
                // be seen, so it never blocks the way. The stages only go forward.
                helper.assertTrue(parent.stage().ordinal() <= quest.stage().ordinal(),
                        quest.id() + " (" + quest.stage() + ") never needs a later stage's " + parent.id());
            }
            helper.assertTrue(quest.parents().isEmpty() == (quest == step("sphalerite") || quest == step("researchers_diary")
                            || quest == step("the_thinning")),
                    quest.id() + ": only sphalerite, the diary and the Thinning (the Void's own branch) start the guide");
            for (Quest other : Guide.server().all()) {
                if (other.index() > quest.index()) {
                    double distance = Math.hypot(other.x() - quest.x(), other.y() - quest.y());
                    helper.assertTrue(distance >= 34, quest.id() + " and " + other.id() + " do not crowd each other");
                }
            }
            helper.assertTrue(Guide.server().byId(quest.id()) == quest, quest.id() + " is found by id");
        }
        for (ResearchTopic topic : ResearchTopics.all()) {
            helper.assertTrue(Guide.server().forTopic(topic) != null, topic.id() + " has its step");
        }
        helper.succeed();
    }

    /**
     * The guide reads at a glance: no two links cross, and no link runs over a step it does not join (links go from a
     * step's first parent to the step, bowed as the layout chose).
     */
    @HexTest
    public static void guideLinksNeverCross(GameTestHelper helper) {
        List<Quest> linked = Guide.server().all().stream().filter(quest -> quest.parent() != null).toList();
        Map<Quest, double[][]> curves = new HashMap<>();
        for (Quest quest : linked) {
            double[][] curve = new double[Guide.LINK_SAMPLES + 1][];
            for (int i = 0; i <= Guide.LINK_SAMPLES; i++) {
                curve[i] = Guide.linkPoint(quest.parent(), quest, quest.bow(), i / (double) Guide.LINK_SAMPLES);
            }
            curves.put(quest, curve);
        }
        for (Quest a : linked) {
            for (Quest b : linked) {
                if (a.index() < b.index() && a.parent() != b.parent() && a.parent() != b && b.parent() != a) {
                    helper.assertFalse(cross(curves.get(a), curves.get(b)), "the links to " + a.id() + " and " + b.id() + " cross");
                }
            }
            for (Quest step : Guide.server().all()) {
                if (step != a && step != a.parent()) {
                    helper.assertTrue(Guide.distanceToCurve(step.x(), step.y(), curves.get(a)) >= 14,
                            "the link to " + a.id() + " runs over " + step.id());
                }
            }
        }
        helper.succeed();
    }

    private static boolean cross(double[][] a, double[][] b) {
        for (int i = 0; i + 1 < a.length; i++) {
            for (int j = 0; j + 1 < b.length; j++) {
                if (Guide.segmentsCross(a[i], a[i + 1], b[j], b[j + 1])) {
                    return true;
                }
            }
        }
        return false;
    }

    /** What else a step gives besides its own result: things crafted straight from it. */
    private static final Map<String, List<String>> ALSO = Map.of(
            "brass", List.of("hextale:brass_nugget", "hextale:brass_block", "hextale:brass_grate"),
            "sphalerite", List.of("hextale:raw_zinc", "hextale:raw_zinc_block", "hextale:deepslate_sphalerite_ore"),
            "zinc", List.of("hextale:zinc_nugget", "hextale:zinc_block"),
            "hexsteel", List.of("hextale:hexsteel_block"),
            "fire_bricks", List.of("hextale:fire_brick", "hextale:fire_bricks", "hextale:fire_brick_stairs", "hextale:fire_brick_slab",
                    "hextale:fire_brick_wall"),
            "magic_stone", List.of("hextale:polished_magic_stone", "hextale:magic_bricks", "hextale:magic_brick_slab",
                    "hextale:magic_brick_stairs", "hextale:magic_brick_wall", "hextale:chiseled_magic_bricks", "hextale:magic_stone_pillar"));

    /** Every picture of a step, the ones under the others included. */
    private static List<QuestDisplay> pictures(Quest quest) {
        List<QuestDisplay> out = new ArrayList<>();
        for (QuestDisplay first : quest.displays()) {
            for (QuestDisplay display = first; display != null; display = display.then()) {
                out.add(display);
            }
        }
        return out;
    }

    /** Item ids a step yields: its icons, its pictures' results and what is crafted straight from it. */
    private static Set<Identifier> yields(Quest quest) {
        Set<Identifier> out = new HashSet<>();
        quest.icons().forEach(icon -> out.add(BuiltInRegistries.ITEM.getKey(icon.getItem())));
        for (QuestDisplay display : pictures(quest)) {
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
     * every step with a research topic comes after the Ink step; a chemtech topic hangs straight off the Lab Bench.
     */
    @HexTest
    public static void guideOrderIsLogical(GameTestHelper helper) {
        for (Quest quest : Guide.server().all()) {
            if (quest.topic() != null && quest.topic().lab()) {
                helper.assertTrue(quest.parents().contains(step("lab_bench")), quest.id() + " is worked out at the Lab Bench, so it hangs off it");
            } else if (quest.topic() != null) {
                helper.assertTrue(ancestors(quest).contains(step("ink")), quest.id() + " is researched, so it comes after the ink");
            }
            Set<Quest> before = ancestors(quest);
            Set<Identifier> available = new HashSet<>();
            before.forEach(step -> available.addAll(yields(step)));
            // A step about finding things (sick land: its soil and rock) may show what they are made into.
            quest.icons().forEach(icon -> available.add(BuiltInRegistries.ITEM.getKey(icon.getItem())));
            List<Identifier> needed = new ArrayList<>();
            for (QuestDisplay display : pictures(quest)) {
            switch (display.kind()) {
                case SHAPED, SHAPELESS, SCHEME -> display.ingredients().stream().filter(id -> !id.isEmpty() && !id.startsWith("#"))
                        .map(Identifier::parse).forEach(needed::add);
                case INFUSION -> {
                    InfusionRecipe recipe = InfusionRecipes.get(false).get(Identifier.parse(display.result()));
                    helper.assertTrue(recipe != null, quest.id() + ": ritual " + display.result() + " exists");
                    helper.assertTrue(before.contains(step("hextech_altar")), quest.id() + " comes after the altar");
                    helper.assertTrue(recipe.pedestals().isEmpty() || before.contains(step("pedestal")), quest.id() + " comes after the Pedestal");
                    helper.assertTrue(recipe.runes().isEmpty() || before.contains(step("rune_stele")), quest.id() + " comes after the Rune Stele");
                    helper.assertTrue(recipe.runes().isEmpty() || before.contains(step("rune_scroll")), quest.id() + " comes after a found rune");
                    List<Ingredient> parts = new ArrayList<>(recipe.pedestals());
                    parts.add(recipe.center());
                    for (Ingredient part : parts) {
                        part.items().findFirst().ifPresent(item -> needed.add(BuiltInRegistries.ITEM.getKey(item.value())));
                    }
                }
                case HEX_CRAFTING -> {
                    var recipe = com.pockyl.hextale.crafting.HexCraftingRecipe.ALL.get(false).get(Identifier.parse(display.result()));
                    helper.assertTrue(recipe != null, quest.id() + ": workbench recipe " + display.result() + " exists");
                    helper.assertTrue(before.contains(step("hex_workbench")), quest.id() + " comes after the Hex Workbench");
                    for (var cell : recipe.grid()) {
                        cell.flatMap(part -> part.items().findFirst()).ifPresent(item -> needed.add(BuiltInRegistries.ITEM.getKey(item.value())));
                    }
                }
                case ALLOY -> {
                    var recipe = com.pockyl.hextale.crafting.AlloyRecipe.ALL.get(false).get(Identifier.parse(display.result()));
                    helper.assertTrue(recipe != null, quest.id() + ": alloy " + display.result() + " exists");
                    helper.assertTrue(before.contains(step("hex_furnace")), quest.id() + " comes after the Hex Furnace");
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
        helper.assertFalse(knowledge.questDone(step("research_table")), "nothing is done with empty hands");

        player.getInventory().add(new ItemStack(ModItems.BRASS_INGOT.get()));
        Quests.check(player);
        helper.assertFalse(knowledge.questDone(step("brass")), "the path is walked in order: brass waits for the furnace and zinc");
        for (Quest quest : ancestors(step("brass"))) {
            knowledge.completeQuest(quest);
        }
        Quests.check(player);
        helper.assertTrue(knowledge.questDone(step("brass")), "a brass ingot completes Brass");
        helper.assertFalse(knowledge.questDone(step("research_table")), "the table is still to make");

        knowledge.study(Identifier.withDefaultNamespace("coal"));
        Quests.check(player);
        helper.assertFalse(knowledge.questDone(step("item_study")), "a studied item waits for the table and the diary");
        for (Quest quest : ancestors(step("item_study"))) {
            knowledge.completeQuest(quest);
        }
        Quests.check(player);
        helper.assertTrue(knowledge.questDone(step("item_study")), "one studied item completes Studying Items");

        player.getInventory().clearContent();
        Quests.check(player);
        helper.assertTrue(knowledge.questDone(step("brass")), "a done quest stays done");

        RuneKnowledge decoded = RuneKnowledge.CODEC.parse(NbtOps.INSTANCE,
                RuneKnowledge.CODEC.encodeStart(NbtOps.INSTANCE, knowledge).getOrThrow()).getOrThrow();
        helper.assertTrue(decoded.questDone(step("brass")) && decoded.questDone(step("item_study"))
                && !decoded.questDone(step("alembic")), "done quests save and load");
        helper.assertTrue(knowledge.copy().questDone(step("item_study")), "done quests are synced");
        helper.succeed();
    }

    /** A step that asks for research is done by researching it: holding the item (found, given) teaches nothing. */
    @HexTest
    public static void researchStepsWantTheResearch(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        RuneKnowledge knowledge = Research.of(player);
        for (Quest quest : ancestors(step("separator"))) {
            knowledge.completeQuest(quest);
        }
        for (Quest quest : ancestors(step("drain_valve"))) {
            knowledge.completeQuest(quest);
        }
        player.getInventory().add(new ItemStack(ModItems.DRAIN_VALVE.get()));
        Quests.check(player);
        helper.assertFalse(knowledge.questDone(step("drain_valve")), "a drain valve in the pocket is not the research");
        Research.research(player, ResearchTopics.DRAIN_VALVE);
        Quests.check(player);
        helper.assertTrue(knowledge.questDone(step("drain_valve")), "researching it completes the step");
        helper.succeed();
    }

    /**
     * The diary points straight at unstable hextech; no single road after it: the first brass already opens the workshop,
     * the essence works and the goggles at once.
     */
    @HexTest
    public static void theGuideBranchesEarly(GameTestHelper helper) {
        RuneKnowledge knowledge = new RuneKnowledge();
        helper.assertTrue(Guide.visible(step("sphalerite"), knowledge) && Guide.visible(step("researchers_diary"), knowledge),
                "two ways to begin");
        helper.assertFalse(Guide.open(step("unstable_hextech"), knowledge), "unstable hextech waits for the diary");
        knowledge.completeQuest(step("researchers_diary"));
        helper.assertTrue(Guide.open(step("unstable_hextech"), knowledge), "the diary leads to unstable hextech");
        helper.assertFalse(Guide.visible(step("rune_scroll"), knowledge), "runes are no first step");
        knowledge.completeQuest(step("unstable_hextech"));
        knowledge.completeQuest(step("hextech_dust"));
        helper.assertTrue(Guide.open(step("hex_workbench"), knowledge) && Guide.open(step("hex_furnace"), knowledge),
                "dust opens the workbench and the furnace at once");
        helper.assertFalse(Guide.open(step("brass"), knowledge), "brass also needs zinc");
        knowledge.completeQuest(step("hex_furnace"));
        knowledge.completeQuest(step("sphalerite"));
        knowledge.completeQuest(step("zinc"));
        helper.assertTrue(Guide.open(step("brass"), knowledge), "the furnace and zinc open brass");
        knowledge.completeQuest(step("brass"));
        knowledge.completeQuest(step("hex_workbench"));
        for (Quest quest : List.of(step("research_table"), step("alembic"))) {
            helper.assertTrue(Guide.open(quest, knowledge), quest.id() + " opens with brass on the workbench");
        }
        helper.assertTrue(step("hextech_altar").stage() == Quest.Stage.MID && step("hexcore").stage() == Quest.Stage.LATE,
                "the altar opens the middle of the path, the Hexcore its end");
        helper.succeed();
    }

    /** Research opens along the guide: the circle once the ink is at the table, the gemstone once the pedestal is made. */
    @HexTest
    public static void researchOpensAlongTheGuide(GameTestHelper helper) {
        RuneKnowledge knowledge = new RuneKnowledge();
        helper.assertFalse(Guide.server().canResearch(ResearchTopics.CIRCLE, knowledge), "the circle waits for ink at the table");
        helper.assertFalse(Guide.visible(step("hextech_altar"), knowledge), "and is hidden until then");
        for (Quest quest : ancestors(step("hextech_altar"))) {
            knowledge.completeQuest(quest);
        }
        helper.assertTrue(Guide.server().canResearch(ResearchTopics.CIRCLE, knowledge), "the ink opens the circle's research");
        helper.assertFalse(Guide.server().canResearch(ResearchTopics.LAPIDARY, knowledge), "the lapidary waits for the pedestal");
        knowledge.completeQuest(step("hextech_altar"));
        knowledge.completeQuest(step("pedestal"));
        helper.assertTrue(Guide.server().canResearch(ResearchTopics.LAPIDARY, knowledge), "the pedestal opens the lapidary");
        helper.assertFalse(Guide.server().canResearch(ResearchTopics.RADIANT_PICKAXE, knowledge), "the radiant pickaxe is far away in the fog");
        helper.succeed();
    }

    /** Runes come after the altar: once it stands, a found scroll opens the rune lore, and the blank scroll with it. */
    @HexTest
    public static void aFoundScrollOpensTheRuneLore(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        RuneKnowledge knowledge = Research.of(player);
        for (Quest quest : ancestors(step("hextech_altar"))) {
            knowledge.completeQuest(quest);
        }
        knowledge.learn(ResearchBook.FIRST, 2);
        Quests.check(player);
        helper.assertFalse(knowledge.questDone(step("rune_scroll")), "the rune lore waits for the altar");
        knowledge.completeQuest(step("hextech_altar"));
        Quests.check(player);
        helper.assertTrue(knowledge.questDone(step("rune_scroll")), "with the altar standing, the studied scroll completes the step");
        helper.assertTrue(Guide.open(step("blank_scroll"), knowledge), "the altar and the lore open the blank scroll");
        helper.succeed();
    }

    /**
     * The Void's branch stands on its own: it appears only once the land shows the haze, its happenings (a mite, a breach)
     * stay in the fog until they happen and are written down when the game reports them.
     */
    @HexTest
    public static void theVoidBranchOpensWithTheHaze(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        RuneKnowledge knowledge = Research.of(player);
        var level = helper.getLevel();
        var chunk = level.getChunkAt(player.blockPosition());
        com.pockyl.hextale.thevoid.Thinning.set(level, chunk, 0);
        helper.assertFalse(Guide.visible(step("the_thinning"), knowledge) || Guide.ahead(step("the_thinning"), knowledge),
                "nothing tells of the Void before the land shows it");
        Quests.check(player);
        helper.assertFalse(knowledge.questDone(step("the_thinning")), "clean land writes nothing");
        com.pockyl.hextale.thevoid.Thinning.set(level, chunk, com.pockyl.hextale.thevoid.Thinning.Stage.HAZE.from);
        Quests.check(player);
        com.pockyl.hextale.thevoid.Thinning.set(level, chunk, 0);
        helper.assertTrue(knowledge.questDone(step("the_thinning")), "the haze opens the Void's branch");
        helper.assertTrue(Guide.visible(step("let_it_spread"), knowledge), "and with it the way to let it be");
        helper.assertFalse(Guide.visible(step("hollowbloom"), knowledge), "the bush to watch it by still waits for the dust");
        for (Quest quest : ancestors(step("hollowbloom"))) {
            knowledge.completeQuest(quest);
        }
        helper.assertTrue(Guide.visible(step("hollowbloom"), knowledge), "with the dust, the bush to watch it by");

        Quests.fire(player, Quests.BREACH_OPENED);
        helper.assertFalse(knowledge.questDone(step("void_breach")), "a breach is written down only after the sick land");
        for (Quest quest : ancestors(step("void_breach"))) {
            knowledge.completeQuest(quest);
        }
        helper.assertFalse(Guide.visible(step("void_breach"), knowledge) || Guide.ahead(step("void_breach"), knowledge),
                "the breach stays in the fog until it happens");
        Quests.fire(player, Quests.BREACH_OPENED);
        helper.assertTrue(knowledge.questDone(step("void_breach")) && Guide.visible(step("void_chitin"), knowledge),
                "a breach seen tearing open is written down and opens what came out of it");
        Quests.fire(player, Quests.VOID_MITE_LATCHED);
        helper.assertTrue(knowledge.questDone(step("void_mites")), "a mite that latched on is written down");

        Quest clothes = step("researcher_robe");
        helper.assertTrue(clothes.icons().size() == 4 && clothes.displays().size() == 4, "the researcher's clothes are one step for four pieces");
        helper.succeed();
    }

    /** A step of the guide the server loaded, by id. */
    private static Quest step(String id) {
        Quest quest = Guide.server().byId(id);
        if (quest == null) {
            throw new IllegalStateException("No guide step " + id);
        }
        return quest;
    }
}
