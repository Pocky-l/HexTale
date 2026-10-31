package com.pockyl.hextale.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.crafting.AlloyRecipe;
import com.pockyl.hextale.crafting.AwakeningRecipe;
import com.pockyl.hextale.crafting.DataRecipes;
import com.pockyl.hextale.crafting.HexCraftingRecipe;
import com.pockyl.hextale.infusion.InfusionRecipes;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The mod's recipes live in the vanilla recipe manager (so data packs and recipe scripts can change them): every kind is
 * loaded there, a recipe written as JSON by a pack decodes through the registered serializer, a removed one is gone for the
 * machines, and shared materials are asked for by common tags.
 */
public final class RecipeGameTests {
    private RecipeGameTests() {
    }

    @HexTest
    public static void everyKindLivesInTheRecipeManager(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        for (DataRecipes<?> kind : DataRecipes.kinds()) {
            int inManager = recipes.getAllRecipesFor(kind.type()).size();
            helper.assertTrue(inManager > 0, "recipe type hextale:" + kind.name() + " has recipes");
            helper.assertTrue(kind.get(false).size() == inManager, kind.name() + " reads what the recipe manager holds");
        }
        helper.assertTrue(recipes.byKey(Hextale.id("hex_crafting/alembic")).isPresent(),
                "the alembic's workbench recipe has the id hextale:hex_crafting/alembic");
        helper.assertTrue(HexCraftingRecipe.ALL.named(Hextale.id("alembic"), false) != null, "a guide page still finds it by its old name");
        helper.succeed();
    }

    @HexTest
    public static void aPackRecipeDecodesAndARemovedOneIsGone(GameTestHelper helper) {
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        // What a data pack or a recipe script (event.custom) writes (1.21.1 ingredients are objects).
        String json = """
                {"type": "hextale:hex_crafting", "ingredients": [{"item": "minecraft:dirt"}, {"tag": "c:ingots/brass"}],
                 "result": {"id": "minecraft:diamond"}, "charge": 3}""";
        Recipe<?> decoded = Recipe.CODEC.parse(ops, JsonParser.parseString(json)).getOrThrow();
        helper.assertTrue(decoded instanceof DataRecipes.Entry<?> entry && entry.kind() == HexCraftingRecipe.ALL,
                "a hextale:hex_crafting JSON decodes into a workbench recipe");
        HexCraftingRecipe recipe = (HexCraftingRecipe) ((DataRecipes.Entry<?>) decoded).value();
        helper.assertTrue(recipe.charge() == 3 && recipe.shapeless(), "its fields are read");

        RecipeManager live = helper.getLevel().getServer().getRecipeManager();
        net.minecraft.resources.ResourceLocation alembic = Hextale.id("hex_crafting/alembic");
        List<RecipeHolder<?>> changed = new ArrayList<>();
        for (RecipeHolder<?> holder : live.getRecipes()) {
            if (!holder.id().equals(alembic)) {
                changed.add(holder);
            }
        }
        changed.add(new RecipeHolder<>(Hextale.id("test/dirt_diamond"), decoded));
        RecipeManager copy = new RecipeManager(helper.getLevel().registryAccess());
        copy.replaceRecipes(changed);
        Map<net.minecraft.resources.ResourceLocation, HexCraftingRecipe> read = HexCraftingRecipe.ALL.read(copy);
        helper.assertFalse(read.containsKey(Hextale.id("hex_crafting/alembic")), "a removed recipe is gone");
        helper.assertTrue(read.containsKey(Hextale.id("test/dirt_diamond")), "an added recipe is there");
        helper.succeed();
    }

    @HexTest
    public static void sharedMaterialsAreAskedForByTag(GameTestHelper helper) {
        AlloyRecipe brass = AlloyRecipe.ALL.named(Hextale.id("brass_ingot"), false);
        helper.assertTrue(brass != null, "the brass alloy is loaded");
        helper.assertTrue(brass.inputs().stream().anyMatch(input -> input.test(new ItemStack(ModItems.ZINC_INGOT.get()))),
                "our zinc goes into brass");
        HexCraftingRecipe separator = HexCraftingRecipe.ALL.named(Hextale.id("separator"), false);
        helper.assertTrue(separator.grid().stream().flatMap(java.util.Optional::stream)
                .anyMatch(ingredient -> ingredient.test(new ItemStack(ModItems.BRASS_INGOT.get()))), "the separator takes our brass");
        helper.assertTrue(new ItemStack(ModItems.BRASS_INGOT.get()).is(net.minecraft.tags.TagKey.create(Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/brass"))), "brass is in c:ingots/brass");
        helper.assertTrue(new ItemStack(Items.PAPER).is(ModTags.RESEARCH_PAPER), "paper is research paper");
        helper.assertTrue(new ItemStack(Items.BLAZE_POWDER).is(ModTags.SEPARATOR_FUELS), "blaze powder fuels the separator");
        helper.assertTrue(!InfusionRecipes.get(false).isEmpty(), "altar recipes are loaded");
        helper.succeed();
    }

    @HexTest
    public static void awakeningsAreData(GameTestHelper helper) {
        ItemStack dust = new ItemStack(ModItems.HEXTECH_DUST.get());
        AwakeningRecipe table = AwakeningRecipe.of(Blocks.CRAFTING_TABLE.defaultBlockState(), dust, false);
        helper.assertTrue(table != null && table.result() == ModBlocks.HEX_WORKBENCH.get(), "dust wakes a crafting table into a workbench");
        AwakeningRecipe cauldron = AwakeningRecipe.of(Blocks.WATER_CAULDRON.defaultBlockState(), dust, false);
        helper.assertTrue(cauldron != null && cauldron.result() == ModBlocks.CRUCIBLE.get(), "a water cauldron wakes into a crucible");
        helper.assertTrue(AwakeningRecipe.of(Blocks.CRAFTING_TABLE.defaultBlockState(), new ItemStack(Items.REDSTONE), false) == null,
                "redstone wakes nothing");
        helper.assertTrue(AwakeningRecipe.of(Blocks.STONE.defaultBlockState(), dust, false) == null, "stone does not wake");
        helper.succeed();
    }
}
