package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.rune.ComplexRune;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * A complex rune written out by the Research Table. Another researcher can study it to copy the rune into their own
 * codex (the scroll is used up); its author already knows it.
 */
public class RuneScrollItem extends Item {
    public RuneScrollItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(ComplexRune rune, int stars, Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.RUNE.get(), rune);
        stack.set(ModDataComponents.STARS.get(), Math.max(1, Math.min(3, stars)));
        return stack;
    }

    public static int stars(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STARS.get(), 1);
    }

    public static @Nullable ComplexRune rune(ItemStack stack) {
        return stack.get(ModDataComponents.RUNE.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        ComplexRune rune = rune(stack);
        return rune == null ? super.getName(stack)
                : Component.translatable("item.hextale.rune_scroll.named", Component.translatable(rune.translationKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        ComplexRune rune = rune(stack);
        if (rune == null) {
            tooltip.accept(Component.translatable("item.hextale.rune_scroll.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.accept(Component.translatable("rune.hextale.formula", Component.translatable(rune.action().translationKey()),
                Component.translatable(rune.target().translationKey())).withStyle(ChatFormatting.DARK_GRAY));
        int stars = stars(stack);
        tooltip.accept(Component.translatable(rune.category().translationKey()).withColor(rune.color())
                .append(Component.literal("  " + "★".repeat(stars)).withStyle(ChatFormatting.GOLD))
                .append(Component.literal("★".repeat(3 - stars)).withStyle(ChatFormatting.DARK_GRAY)));
        tooltip.accept(Component.translatable(rune.translationKey() + ".effect").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(rune.translationKey() + ".use").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        tooltip.accept(Component.translatable("item.hextale.rune_scroll.study").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ComplexRune rune = rune(stack);
        if (rune == null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            Component name = Component.translatable(rune.translationKey()).withColor(rune.color());
            if (Research.of(serverPlayer).knows(rune)) {
                serverPlayer.sendOverlayMessage(Component.translatable("item.hextale.rune_scroll.known", name));
                return InteractionResult.FAIL;
            }
            Research.learn(serverPlayer, rune, 1, 0);
            stack.consume(1, player);
            serverPlayer.sendOverlayMessage(Component.translatable("item.hextale.rune_scroll.studied", name));
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.9F);
        }
        return InteractionResult.SUCCESS;
    }
}
