package com.pockyl.hextale.transport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.Nullable;

/**
 * Where a pylon puts an item into a block. Furnaces (furnace, blast furnace, smoker) are sorted by the item, whatever face
 * they were linked by or stand under: what the furnace can cook goes into its input, fuel that it cannot cook into its
 * fuel slot, anything else stays out. Every other block takes items through the face of the link (a gathering pylon
 * puts them in through the top), so sided machines still choose their slots by face; a chest or a barrel simply fills.
 */
public final class PylonInsert {
    private PylonInsert() {
    }

    /** The handler the item goes in through, or null when this block does not take it. */
    @Nullable
    public static ResourceHandler<ItemResource> handler(ServerLevel level, BlockPos pos, Direction face, ItemResource resource) {
        if (level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace) {
            Direction side = furnaceSide(level, furnace, resource.toStack(1));
            return side == null ? null : level.getCapability(Capabilities.Item.BLOCK, pos, side);
        }
        return level.getCapability(Capabilities.Item.BLOCK, pos, face);
    }

    /** Puts up to {@code amount} in; returns how many went. */
    public static int insert(ServerLevel level, BlockPos pos, Direction face, ItemResource resource, int amount,
                             @Nullable TransactionContext transaction) {
        ResourceHandler<ItemResource> handler = handler(level, pos, face, resource);
        return handler == null ? 0 : ResourceHandlerUtil.insertStacking(handler, resource, amount, transaction);
    }

    /** The furnace face for this item: the top for what it cooks, a side for fuel, null for neither. */
    @Nullable
    static Direction furnaceSide(ServerLevel level, AbstractFurnaceBlockEntity furnace, ItemStack stack) {
        RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> type = furnace instanceof BlastFurnaceBlockEntity
                ? RecipeType.BLASTING : furnace instanceof SmokerBlockEntity ? RecipeType.SMOKING : RecipeType.SMELTING;
        if (level.recipeAccess().getRecipeFor(type, new SingleRecipeInput(stack), level).isPresent()) {
            return Direction.UP;
        }
        return stack.has(DataComponents.COOKING_FUEL) ? Direction.NORTH : null;
    }
}
