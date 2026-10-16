package com.pockyl.hextale.essence;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Flasks used on a vessel by hand: a full flask pours in, an empty one draws out. */
public final class Flasks {
    private Flasks() {
    }

    /**
     * A flask in hand against a tank.
     *
     * @return whether anything happened
     */
    public static boolean use(Player player, InteractionHand hand, ItemStack held, EssenceTank tank) {
        if (!EssenceFlaskItem.isFlask(held)) {
            return false;
        }
        Essence inside = EssenceFlaskItem.contents(held);
        if (!inside.isEmpty()) {
            int poured = tank.fill(inside, false);
            if (poured <= 0) {
                return false;
            }
            Essence left = inside.minus(inside.part(poured));
            ItemStack after = EssenceFlaskItem.filled(left);
            replace(player, hand, held, after);
            return true;
        }
        if (tank.isEmpty()) {
            return false;
        }
        Essence drawn = tank.drain(EssenceFlaskItem.CAPACITY, false);
        replace(player, hand, held, EssenceFlaskItem.filled(drawn));
        return true;
    }

    /** Swaps one item of the held stack for {@code result}. */
    private static void replace(Player player, InteractionHand hand, ItemStack held, ItemStack result) {
        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(hand, result);
        } else if (!player.getInventory().add(result)) {
            player.drop(result, false, Prediction.SERVER_ONLY);
        }
    }

    /** "Life 8, Earth 8 (16/256)", or "empty". */
    public static Component describe(EssenceTank tank) {
        return tank.isEmpty() ? Component.translatable("block.hextale.essence.empty")
                : tank.contents().describe().append(" (" + tank.amount() + "/" + tank.capacity() + ")");
    }
}
