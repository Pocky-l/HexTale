package com.pocky.hextale.common.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pocky.hextale.common.recipe.containers.HexcoreContainer;
import com.pocky.hextale.common.register.ModBlocks;
import com.pocky.hextale.common.register.ModRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HexcoreRecipe implements Recipe<HexcoreContainer> {

    protected final List<Ingredient> ingredients;
    protected final ItemStack result;
    private final RecipeType<?> type;
    private final RecipeSerializer<?> serializer;
    protected final ResourceLocation id;
    protected final String group;

    public HexcoreRecipe(ResourceLocation p_44478_, String p_44479_, List<Ingredient> p_44480_, ItemStack p_44481_) {
        this(ModRecipeTypes.HEXCORE.get(), ModRecipeSerializer.HEXCORE.get(), p_44478_, p_44479_, p_44480_, p_44481_);
    }

    public HexcoreRecipe(RecipeType<?> p_44416_, RecipeSerializer<?> p_44417_, ResourceLocation p_44418_, String p_44419_, List<Ingredient> p_44420_, ItemStack p_44421_) {
        this.type = p_44416_;
        this.serializer = p_44417_;
        this.id = p_44418_;
        this.group = p_44419_;
        this.ingredients = p_44420_;
        this.result = p_44421_;
    }

    public boolean matches(HexcoreContainer container, Level p_44484_) {
        if (ingredients.size() >= container.getContainerSize()) {
            return false; // Recipe doesn't match container size
        }

        HexcoreContainer copyContainer = new HexcoreContainer();

        container.forEach(e -> copyContainer.setItem(copyContainer.size(), e.copy()));

        AtomicInteger matchCount = new AtomicInteger();
        ingredients.forEach(e -> {
            AtomicBoolean flag = new AtomicBoolean(false);
            copyContainer.forEach(i -> {
                if (!flag.get() && e.getItems()[0].equals(i, false)) {
                    matchCount.getAndIncrement();
                    flag.set(true);
                    i.setCount(0);
                }
            });
        });
        return matchCount.get() == ingredients.size();
    }

    public ItemStack getToastSymbol() {
        return new ItemStack(ModBlocks.HEXCORE.get());
    }

    public RecipeType<?> getType() {
        return this.type;
    }

    public RecipeSerializer<?> getSerializer() {
        return this.serializer;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public String getGroup() {
        return this.group;
    }

    public ItemStack getResultItem(RegistryAccess p_266964_) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> allIngredients = NonNullList.create();
        allIngredients.add(Ingredient.EMPTY);
        allIngredients.addAll(ingredients);
        return allIngredients;
    }

    public boolean canCraftInDimensions(int p_44424_, int p_44425_) {
        return true;
    }

    public ItemStack assemble(HexcoreContainer p_44427_, RegistryAccess p_266999_) {
        return this.result.copy();
    }

    public static class Serializer<T extends HexcoreRecipe> implements RecipeSerializer<T> {
        final HexcoreRecipe.Serializer.SingleItemMaker<T> factory;

        protected Serializer(HexcoreRecipe.Serializer.SingleItemMaker<T> p_44435_) {
            this.factory = p_44435_;
        }

        public T fromJson(ResourceLocation p_44449_, JsonObject p_44450_) {
            String group = GsonHelper.getAsString(p_44450_, "group", ""); // Get optional group name
            List<Ingredient> ingredients = new ArrayList<>();
            for (JsonElement ingredientElement : GsonHelper.getAsJsonArray(p_44450_, "ingredients")) {
                ItemStack s = Ingredient.fromJson(ingredientElement.getAsJsonObject()).getItems()[0];
                int i = GsonHelper.getAsInt(ingredientElement.getAsJsonObject(), "count");
                s.setCount(i);
                ingredients.add(Ingredient.of(s));
            }
            String s1 = GsonHelper.getAsString(p_44450_, "result");
            int i = GsonHelper.getAsInt(p_44450_, "count");
            ItemStack result = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(s1)), i);
            return this.factory.create(p_44449_, group, ingredients, result);
        }

        public T fromNetwork(ResourceLocation p_44452_, FriendlyByteBuf p_44453_) {
            String s = p_44453_.readUtf();
            int ingredientCount = p_44453_.readVarInt();
            List<Ingredient> ingredients = new ArrayList<>();
            for (int i = 0; i < ingredientCount; i++) {
                ingredients.add(Ingredient.fromNetwork(p_44453_));
            }
            ItemStack itemstack = p_44453_.readItem();
            return this.factory.create(p_44452_, s, ingredients, itemstack);
        }

        public void toNetwork(FriendlyByteBuf p_44440_, T p_44441_) {
            p_44440_.writeUtf(p_44441_.group);
            p_44440_.writeVarInt(p_44441_.ingredients.size());
            for (Ingredient ingredient : p_44441_.ingredients) {
                ingredient.toNetwork(p_44440_);
            }
            p_44440_.writeItem(p_44441_.result);
        }

        interface SingleItemMaker<T extends HexcoreRecipe> {
            T create(ResourceLocation p_44455_, String p_44456_, List<Ingredient> p_44457_, ItemStack p_44458_);
        }
    }
}
