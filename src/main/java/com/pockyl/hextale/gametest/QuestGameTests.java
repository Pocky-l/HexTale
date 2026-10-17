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
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

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

    /** Every step has its own spot, its parents come before it, every rune is on the guide exactly once. */
    @HexTest
    public static void guideIsLaidOut(GameTestHelper helper) {
        Set<Long> spots = new HashSet<>();
        for (Quest quest : Guide.all()) {
            helper.assertTrue(spots.add(((long) quest.x() << 32) | (quest.y() & 0xFFFFFFFFL)), quest.id() + " has its own spot");
            for (Quest parent : quest.parents()) {
                helper.assertTrue(parent.index() < quest.index(), quest.id() + " follows " + parent.id());
                helper.assertTrue(parent.kind() != Quest.Kind.WIP, quest.id() + " never waits for a WIP device");
            }
            helper.assertTrue(quest.parents().isEmpty() == (quest == Guide.BRASS), quest.id() + ": only Brass starts the path");
            helper.assertTrue(Guide.byId(quest.id()) == quest, quest.id() + " is found by id");
        }
        for (ComplexRune rune : ComplexRunes.all()) {
            helper.assertTrue(Guide.of(rune).rune() == rune, rune.key() + " has its step");
        }
        helper.succeed();
    }

    /** What else a step gives besides its own result: things crafted straight from it. */
    private static final Map<String, List<String>> ALSO = Map.of(
            "brass", List.of("hextale:brass_nugget", "hextale:brass_block"),
            "magic_stone", List.of("hextale:polished_magic_stone", "hextale:magic_bricks", "hextale:magic_brick_slab",
                    "hextale:magic_brick_stairs", "hextale:magic_brick_wall", "hextale:chiseled_magic_bricks"));

    /** Item ids a step yields: its icon, its picture's result and what is crafted straight from it. */
    private static Set<Identifier> yields(Quest quest) {
        Set<Identifier> out = new HashSet<>();
        out.add(BuiltInRegistries.ITEM.getKey(quest.icon().getItem()));
        QuestDisplay display = quest.display();
        if (display != null && display.result().contains(":") && display.kind() != QuestDisplay.Kind.INFUSION) {
            out.add(Identifier.parse(display.result()));
        }
        if (display != null && display.kind() == QuestDisplay.Kind.INFUSION) {
            InfusionRecipe recipe = InfusionRecipes.get(false).get(Identifier.parse(display.result()));
            if (recipe != null) {
                out.add(BuiltInRegistries.ITEM.getKey(recipe.result().item().value()));
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
     * the Pedestal, its runes after the Rune Stele and after the steps that teach them.
     */
    @HexTest
    public static void guideOrderIsLogical(GameTestHelper helper) {
        for (Quest quest : Guide.all()) {
            QuestDisplay display = quest.display();
            if (display == null) {
                continue;
            }
            Set<Quest> before = ancestors(quest);
            Set<Identifier> available = new HashSet<>();
            before.forEach(step -> available.addAll(yields(step)));
            List<Identifier> needed = new ArrayList<>();
            switch (display.kind()) {
                case SHAPED, SHAPELESS, SCHEME -> display.ingredients().stream().filter(id -> !id.isEmpty() && !id.startsWith("#"))
                        .map(Identifier::parse).forEach(needed::add);
                case INFUSION -> {
                    InfusionRecipe recipe = InfusionRecipes.get(false).get(Identifier.parse(display.result()));
                    helper.assertTrue(recipe != null, quest.id() + ": ritual " + display.result() + " exists");
                    helper.assertTrue(before.contains(Guide.HEXTECH_ALTAR), quest.id() + " comes after the altar");
                    helper.assertTrue(recipe.pedestals().isEmpty() || before.contains(Guide.PEDESTAL), quest.id() + " comes after the Pedestal");
                    helper.assertTrue(recipe.runes().isEmpty() || before.contains(Guide.RUNE_STELE), quest.id() + " comes after the Rune Stele");
                    for (ComplexRune rune : recipe.runes()) {
                        helper.assertTrue(before.contains(Guide.of(rune)), quest.id() + " comes after the rune " + rune.key());
                    }
                    List<Ingredient> parts = new ArrayList<>(recipe.pedestals());
                    parts.add(recipe.center());
                    for (Ingredient part : parts) {
                        part.items().findFirst().ifPresent(item -> needed.add(BuiltInRegistries.ITEM.getKey(item.value())));
                    }
                }
                default -> {
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

    /** Runes open along the guide: Smelting after the ink, the runes after it once Smelting is known; steps stay hidden. */
    @HexTest
    public static void runesOpenAlongTheGuide(GameTestHelper helper) {
        RuneKnowledge knowledge = new RuneKnowledge();
        ComplexRune smelting = ResearchBook.FIRST;
        helper.assertFalse(ResearchBook.open(knowledge, smelting), "Smelting waits for ink at the table");
        helper.assertFalse(Guide.visible(Guide.SMELTING, knowledge), "and is hidden until then");
        for (Quest quest : ancestors(Guide.SMELTING)) {
            knowledge.completeQuest(quest);
        }
        helper.assertTrue(ResearchBook.open(knowledge, smelting), "the ink opens Smelting");
        helper.assertFalse(ResearchBook.open(knowledge, Guide.TEMPERING.rune()), "Tempering waits for Smelting");
        helper.assertFalse(Guide.visible(Guide.HEX_SMELTER, knowledge), "the smelter is hidden");
        knowledge.learn(smelting, 1, 0);
        helper.assertTrue(ResearchBook.open(knowledge, Guide.TEMPERING.rune()), "Smelting opens Tempering");
        helper.assertFalse(ResearchBook.open(knowledge, Guide.FORGE.rune()), "Forge waits for Tempering");
        helper.assertFalse(Guide.visible(Guide.WIP_HEXCORE, knowledge), "the Hexcore is far away in the fog");
        helper.succeed();
    }
}
