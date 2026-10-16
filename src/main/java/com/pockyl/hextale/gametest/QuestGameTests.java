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
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.quest.QuestDisplay;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchBook;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** The researcher's path: its recipes match the real ones and its goals are noticed. */
public final class QuestGameTests {
    private QuestGameTests() {
    }

    /** The crafting grids drawn in the book are the recipes the game uses. */
    @HexTest
    public static void questRecipesMatchTheRecipes(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ContextMap context = ContextMap.builder().set(SlotDisplayContext.REGISTRIES, server.registryAccess())
                .buildAndValidate(SlotDisplayContext.CONTEXT);
        for (Quest quest : Quest.values()) {
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

    /** Every quest has its own spot and its parent comes earlier on the path. */
    @HexTest
    public static void questPathIsLaidOut(GameTestHelper helper) {
        Set<Long> spots = new HashSet<>();
        for (Quest quest : Quest.values()) {
            helper.assertTrue(spots.add(((long) quest.x() << 32) | (quest.y() & 0xFFFFFFFFL)), quest.id() + " has its own spot");
            helper.assertTrue(quest.parent() == null ? quest == Quest.RESEARCH_TABLE : quest.parent().ordinal() < quest.ordinal(),
                    quest.id() + " follows an earlier quest");
            helper.assertTrue(Quest.byId(quest.id()) == quest, quest.id() + " is found by id");
        }
        helper.succeed();
    }

    @HexTest
    public static void questsNoticeTheirGoals(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        RuneKnowledge knowledge = Research.of(player);
        Quests.check(player);
        helper.assertFalse(knowledge.questDone(Quest.RESEARCH_TABLE), "nothing is done with empty hands");

        player.getInventory().add(new ItemStack(ModItems.BRASS_INGOT.get()));
        Quests.check(player);
        helper.assertTrue(knowledge.questDone(Quest.BRASS), "a brass ingot completes Brass");
        helper.assertFalse(knowledge.questDone(Quest.RESEARCH_TABLE), "the table is still to make");

        knowledge.study(Identifier.withDefaultNamespace("coal"));
        Quests.check(player);
        helper.assertTrue(knowledge.questDone(Quest.ITEM_STUDY), "one studied item completes Studying Items");
        helper.assertFalse(knowledge.questDone(Quest.ESSENCES), "Essences wants five");

        Research.learn(player, ResearchBook.FIRST, 3, 20);
        Quests.check(player);
        helper.assertTrue(knowledge.questDone(Quest.FIRST_RUNE) && knowledge.questDone(Quest.THREE_STARS),
                "a three-star Smelting completes First Rune and Three Stars");
        helper.assertFalse(knowledge.questDone(Quest.FIRST_SIGNS), "chapter II is not open yet");

        player.getInventory().clearContent();
        Quests.check(player);
        helper.assertTrue(knowledge.questDone(Quest.BRASS), "a done quest stays done");

        RuneKnowledge decoded = RuneKnowledge.CODEC.parse(NbtOps.INSTANCE,
                RuneKnowledge.CODEC.encodeStart(NbtOps.INSTANCE, knowledge).getOrThrow()).getOrThrow();
        helper.assertTrue(decoded.questDone(Quest.BRASS) && decoded.questDone(Quest.FIRST_RUNE)
                && !decoded.questDone(Quest.RESEARCH_TABLE), "done quests save and load");
        helper.assertTrue(knowledge.copy().questDone(Quest.ITEM_STUDY), "done quests are synced");
        helper.succeed();
    }
}
