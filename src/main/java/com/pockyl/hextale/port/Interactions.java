package com.pockyl.hextale.port;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * Converts between the Minecraft 26.x interaction results the mod's code returns and the 1.21.1 result types. 26.x's
 * {@code TRY_WITH_EMPTY_HAND} is written as {@code PASS} on 1.21.1; a PASS that reaches {@link #itemOn} therefore lets the
 * block's empty-hand use try next.
 */
public final class Interactions {
    private Interactions() {
    }

    /** An item's use result as the 1.21.1 holder (the held stack stays in the hand). */
    public static InteractionResultHolder<ItemStack> holder(InteractionResult result, ItemStack held) {
        return new InteractionResultHolder<>(result, held);
    }

    /** A plain result returned from a block's use with an item. */
    public static ItemInteractionResult itemOn(InteractionResult result) {
        return switch (result) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> ItemInteractionResult.FAIL;
            case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }

    public static ItemInteractionResult itemOn(ItemInteractionResult result) {
        return result;
    }
}
