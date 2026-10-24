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
import net.minecraft.world.level.Level;

import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchTopic;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * A finished research written out at the Research Table. Reading the notes (they are used up) teaches the topic: from
 * then on the researcher may craft its items or make them at the altar. Any researcher can read them, so a research done
 * once can be handed to a friend.
 */
public class ResearchNotesItem extends Item {
    public ResearchNotesItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(ResearchTopic topic) {
        ItemStack stack = new ItemStack(ModItems.RESEARCH_NOTES.get());
        stack.set(ModDataComponents.TOPIC.get(), topic);
        return stack;
    }

    /** Notes written at a table: they keep the drawing, so the parchment shows it while they lie in the output slot. */
    public static ItemStack of(ResearchTopic topic, int attempt, int[] tree) {
        ItemStack stack = of(topic);
        stack.set(ModDataComponents.NOTES_DRAWING.get(), NotesDrawing.of(attempt, tree));
        return stack;
    }

    public static @Nullable ResearchTopic topic(ItemStack stack) {
        return stack.get(ModDataComponents.TOPIC.get());
    }

    /** The drawing these notes were written from, or null (not notes, or notes without one). */
    public static @Nullable NotesDrawing drawing(ItemStack stack) {
        return stack.is(ModItems.RESEARCH_NOTES.get()) && topic(stack) != null ? stack.get(ModDataComponents.NOTES_DRAWING.get()) : null;
    }

    @Override
    public Component getName(ItemStack stack) {
        ResearchTopic topic = topic(stack);
        return topic == null ? super.getName(stack)
                : Component.translatable("item.hextale.research_notes.named", Component.translatable(topic.translationKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        ResearchTopic topic = topic(stack);
        if (topic == null) {
            return;
        }
        for (var item : topic.items()) {
            tooltip.accept(Component.literal("• ").append(Component.translatable(item.get().asItem().getDescriptionId())).withColor(topic.color()));
        }
        tooltip.accept(Component.translatable("item.hextale.research_notes.read").withStyle(ChatFormatting.DARK_GRAY));
    }

    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResearchTopic topic = topic(stack);
        if (topic == null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            Component name = Component.translatable(topic.translationKey()).withColor(topic.color());
            if (!Research.research(serverPlayer, topic)) {
                serverPlayer.displayClientMessage(Component.translatable("item.hextale.research_notes.known", name), true);
                return InteractionResult.FAIL;
            }
            stack.consume(1, player);
            serverPlayer.displayClientMessage(Component.translatable("item.hextale.research_notes.learned", name), true);
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.9F);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.Interactions.holder(usePort(level, player, hand), player.getItemInHand(hand));
    }
}
