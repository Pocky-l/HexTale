package com.pockyl.hextale.compat.projecte;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.IRecipeTypeMapper;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.crafting.AlloyRecipe;
import com.pockyl.hextale.crafting.AwakeningRecipe;
import com.pockyl.hextale.crafting.CrucibleRecipe;
import com.pockyl.hextale.crafting.DataRecipes;
import com.pockyl.hextale.crafting.HexCraftingRecipe;
import com.pockyl.hextale.crafting.TemperingRecipe;
import com.pockyl.hextale.infusion.InfusionRecipe;
import com.pockyl.hextale.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Tells ProjectE what the mod's own recipes cost, so what is made at the workbench, the altar, the furnace's alloys, the
 * Crucible and the Chem Vat, or woken with dust, gets an EMC value from its items. ProjectE finds this class by its annotation, and loads it only
 * when it is installed.
 * <p>
 * Only items count. Essence is left out on purpose: it boils out of cheap items, so a price on it would let a player turn
 * cobblestone into EMC. Only the mod's own results are mapped, so a Crucible transmutation never moves a vanilla price.
 */
@RecipeTypeMapper
public class HextaleRecipeMapper implements IRecipeTypeMapper {
    /** What a recipe takes and what it makes. */
    private record Parts(List<SizedIngredient> inputs, ItemStack result) {
    }

    @Override
    public String getName() {
        return "HexTaleRecipeMapper";
    }

    @Override
    public String getTranslationKey() {
        return "hextale.projecte.recipe_mapper";
    }

    @Override
    public String getDescription() {
        return "Maps the recipes of HexTale's workbench, altar, alloys, Crucible and Chem Vat.";
    }

    @Override
    public boolean canHandle(RecipeType<?> recipeType) {
        return DataRecipes.kinds().stream().anyMatch(kind -> kind.type() == recipeType);
    }

    @Override
    public boolean handleRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper, RecipeHolder<?> holder, RegistryAccess registries,
            INSSFakeGroupManager groups) {
        if (!(holder.value() instanceof DataRecipes.Entry<?> entry)) {
            return false;
        }
        Parts parts = parts(entry.value());
        if (parts == null || !Hextale.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(parts.result().getItem()).getNamespace())) {
            return true;
        }
        Object2IntMap<NormalizedSimpleStack> cost = new Object2IntOpenHashMap<>();
        for (SizedIngredient input : parts.inputs()) {
            if (!input.ingredient().isEmpty() && !add(mapper, groups, cost, input)) {
                return true;
            }
        }
        if (!cost.isEmpty()) {
            mapper.addConversion(parts.result().getCount(), NSSItem.createItem(parts.result().getItem()), cost);
        }
        return true;
    }

    /** Adds one input to the cost: its item, or one stand-in for all the items it accepts. False when it accepts nothing. */
    private static boolean add(IMappingCollector<NormalizedSimpleStack, Long> mapper, INSSFakeGroupManager groups,
            Object2IntMap<NormalizedSimpleStack> cost, SizedIngredient input) {
        ItemStack[] accepted = input.ingredient().getItems();
        if (accepted.length == 0) {
            return false;
        }
        if (accepted.length == 1) {
            price(cost, accepted[0], input.count());
            return true;
        }
        Object2IntMap<NormalizedSimpleStack> members = new Object2IntOpenHashMap<>(accepted.length);
        for (ItemStack stack : accepted) {
            members.put(NSSItem.createItem(stack.getItem()), 1);
        }
        INSSFakeGroupManager.FakeGroupData group = groups.getOrCreateFakeGroupDirect(members, true, true);
        cost.mergeInt(group.dummy(), input.count(), Integer::sum);
        if (group.created()) {
            for (ItemStack stack : accepted) {
                Object2IntMap<NormalizedSimpleStack> single = new Object2IntOpenHashMap<>(2);
                price(single, stack, 1);
                mapper.addConversion(1, group.dummy(), single);
            }
        }
        return true;
    }

    /** An item's price, whatever brew or data it holds; what it leaves behind, like a bucket, is taken off. */
    private static void price(Object2IntMap<NormalizedSimpleStack> cost, ItemStack stack, int count) {
        cost.mergeInt(NSSItem.createItem(stack.getItem()), count, Integer::sum);
        if (stack.hasCraftingRemainingItem()) {
            cost.mergeInt(NSSItem.createItem(stack.getCraftingRemainingItem()), -count, Integer::sum);
        }
    }

    private static @Nullable Parts parts(Object recipe) {
        return switch (recipe) {
            case HexCraftingRecipe crafting -> new Parts(sized(crafting.shape().map(pattern -> pattern.ingredients(), list -> list)), crafting.result().create());
            case InfusionRecipe infusion -> {
                List<Ingredient> inputs = new ArrayList<>(infusion.pedestals());
                inputs.add(infusion.center());
                yield new Parts(sized(inputs), infusion.result().create());
            }
            case AlloyRecipe alloy -> new Parts(alloy.inputs(), alloy.result().create());
            case CrucibleRecipe crucible -> new Parts(sized(List.of(crucible.catalyst())), crucible.result().create());
            case TemperingRecipe tempering -> new Parts(tempering.inputs(), tempering.result().create());
            case AwakeningRecipe awakening -> {
                // The dust that wakes a block is spent unless the recipe names its own catalyst.
                Ingredient block = Ingredient.of(awakening.block().stream().map(holder -> holder.value().asItem())
                        .filter(item -> item != Items.AIR).map(ItemStack::new));
                yield new Parts(sized(List.of(block, awakening.catalyst().orElse(Ingredient.of(ModItems.HEXTECH_DUST.get())))),
                        new ItemStack(awakening.result()));
            }
            default -> null;
        };
    }

    private static List<SizedIngredient> sized(List<Ingredient> ingredients) {
        return ingredients.stream().map(ingredient -> new SizedIngredient(ingredient, 1)).toList();
    }
}
