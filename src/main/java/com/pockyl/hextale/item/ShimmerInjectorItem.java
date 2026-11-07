package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.chem.ShimmerMutations;
import com.pockyl.hextale.registry.ModDataComponents;

import java.util.function.Consumer;

/**
 * Holds up to {@value #MAX_DOSES} doses of refined shimmer, one vial each, and gives one per shot to a companion
 * ({@link ShimmerMutations}). A vial is carried onto it in the inventory, or it is used with a vial in the other hand. A
 * sneaking click on a mutated animal only reads how long the mutation has left.
 */
public class ShimmerInjectorItem extends Item {
    public static final int MAX_DOSES = 4;

    public ShimmerInjectorItem(Properties properties) {
        super(properties);
    }

    public static int doses(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SHIMMER_DOSES.get(), 0);
    }

    private static void setDoses(ItemStack stack, int doses) {
        if (doses <= 0) {
            stack.remove(ModDataComponents.SHIMMER_DOSES.get());
        } else {
            stack.set(ModDataComponents.SHIMMER_DOSES.get(), doses);
        }
    }

    /** Takes as many vials of refined shimmer from {@code vials} as fit. Returns whether any went in. */
    public static boolean fill(ItemStack injector, ItemStack vials, Player player) {
        int count = Math.min(vials.getCount(), MAX_DOSES - doses(injector));
        if (ChemVialItem.brew(vials) != Brew.REFINED_SHIMMER || count <= 0) {
            return false;
        }
        setDoses(injector, doses(injector) + count);
        vials.shrink(count);
        player.playSound(net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY, 0.6F, 1.2F);
        return true;
    }

    /** A click with the injector on an animal; PASS leaves the click to the animal. */
    public static InteractionResult shoot(ItemStack stack, Player player, Mob animal) {
        ShimmerMutations.Kind kind = ShimmerMutations.kindFor(animal.getType());
        if (kind == null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
            shootOnServer(stack, server, animal, kind);
        }
        return InteractionResult.SUCCESS;
    }

    private static void shootOnServer(ItemStack stack, ServerPlayer player, Mob animal, ShimmerMutations.Kind kind) {
        ShimmerMutations.Mutation mutation = ShimmerMutations.of(animal);
        if (player.isSecondaryUseActive() && mutation != null) {
            long left = mutation.until() - animal.level().getGameTime();
            player.displayClientMessage(Component.translatable("item.hextale.shimmer_injector.left",
                    String.format(java.util.Locale.ROOT, "%.1f", left / (double) ShimmerMutations.DAY)), true);
        } else if (animal.isBaby()) {
            player.displayClientMessage(Component.translatable("item.hextale.shimmer_injector.too_young"), true);
        } else if (!ShimmerMutations.owns(player, animal)) {
            player.displayClientMessage(Component.translatable("item.hextale.shimmer_injector.not_yours"), true);
        } else if (doses(stack) <= 0 && !player.hasInfiniteMaterials()) {
            player.displayClientMessage(Component.translatable("item.hextale.shimmer_injector.empty"), true);
        } else {
            if (!player.hasInfiniteMaterials()) {
                setDoses(stack, doses(stack) - 1);
            }
            ShimmerMutations.dose(player, animal, kind);
        }
    }

    /** Used in the air with a vial in the other hand, it draws the vial up. */
    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return fill(player.getItemInHand(hand), other, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.Interactions.holder(usePort(level, player, hand), player.getItemInHand(hand));
    }

    /** A vial carried onto the injector goes in, as many as fit. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedItem) {
        return clickAction == ClickAction.PRIMARY && fill(self, other, player);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return doses(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * doses(stack) / MAX_DOSES);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Brew.REFINED_SHIMMER.color();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        tooltip.accept(Component.translatable("item.hextale.shimmer_injector.doses", doses(stack), MAX_DOSES).withStyle(ChatFormatting.GRAY));
    }
}
