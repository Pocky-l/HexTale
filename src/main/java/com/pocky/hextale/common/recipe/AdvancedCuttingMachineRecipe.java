package com.pocky.hextale.common.recipe;

import com.google.gson.JsonObject;
import com.pocky.hextale.common.register.ModBlocks;
import com.pocky.hextale.common.register.ModRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class AdvancedCuttingMachineRecipe implements Recipe<Container> {

    protected final Ingredient ingredient;
    protected final ItemStack result;
    private final RecipeType<?> type;
    private final RecipeSerializer<?> serializer;
    protected final ResourceLocation id;
    protected final String group;

    public AdvancedCuttingMachineRecipe(ResourceLocation p_44478_, String p_44479_, Ingredient p_44480_, ItemStack p_44481_) {
        this(ModRecipeTypes.ADVANCED_CUTTING_MACHINE.get(), ModRecipeSerializer.ADVANCED_CUTTING_MACHINE.get(), p_44478_, p_44479_, p_44480_, p_44481_);
    }

    public AdvancedCuttingMachineRecipe(RecipeType<?> p_44416_, RecipeSerializer<?> p_44417_, ResourceLocation p_44418_, String p_44419_, Ingredient p_44420_, ItemStack p_44421_) {
        this.type = p_44416_;
        this.serializer = p_44417_;
        this.id = p_44418_;
        this.group = p_44419_;
        this.ingredient = p_44420_;
        this.result = p_44421_;
    }

    public boolean matches(Container p_44483_, Level p_44484_) {
        return this.ingredient.test(p_44483_.getItem(0));
    }

    public ItemStack getToastSymbol() {
        return new ItemStack(ModBlocks.ADVANCED_CUTTING_MACHINE.get());
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

    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> nonnulllist = NonNullList.create();
        nonnulllist.add(this.ingredient);
        return nonnulllist;
    }

    public boolean canCraftInDimensions(int p_44424_, int p_44425_) {
        return true;
    }

    public ItemStack assemble(Container p_44427_, RegistryAccess p_266999_) {
        return this.result.copy();
    }

    public static class Serializer<T extends AdvancedCuttingMachineRecipe> implements RecipeSerializer<T> {
        final AdvancedCuttingMachineRecipe.Serializer.SingleItemMaker<T> factory;

        protected Serializer(AdvancedCuttingMachineRecipe.Serializer.SingleItemMaker<T> p_44435_) {
            this.factory = p_44435_;
        }

        public T fromJson(ResourceLocation p_44449_, JsonObject p_44450_) {
            String s = GsonHelper.getAsString(p_44450_, "group", "");
            Ingredient ingredient;
            if (GsonHelper.isArrayNode(p_44450_, "ingredient")) {
                ingredient = Ingredient.fromJson(GsonHelper.getAsJsonArray(p_44450_, "ingredient"), false);
            } else {
                ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(p_44450_, "ingredient"), false);
            }

            String s1 = GsonHelper.getAsString(p_44450_, "result");
            int i = GsonHelper.getAsInt(p_44450_, "count");
            ItemStack itemstack = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(s1)), i);
            return this.factory.create(p_44449_, s, ingredient, itemstack);
        }

        public T fromNetwork(ResourceLocation p_44452_, FriendlyByteBuf p_44453_) {
            String s = p_44453_.readUtf();
            Ingredient ingredient = Ingredient.fromNetwork(p_44453_);
            ItemStack itemstack = p_44453_.readItem();
            return this.factory.create(p_44452_, s, ingredient, itemstack);
        }

        public void toNetwork(FriendlyByteBuf p_44440_, T p_44441_) {
            p_44440_.writeUtf(p_44441_.group);
            p_44441_.ingredient.toNetwork(p_44440_);
            p_44440_.writeItem(p_44441_.result);
        }

        interface SingleItemMaker<T extends AdvancedCuttingMachineRecipe> {
            T create(ResourceLocation p_44455_, String p_44456_, Ingredient p_44457_, ItemStack p_44458_);
        }
    }
}
