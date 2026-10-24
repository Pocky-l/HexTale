package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModSounds;

import java.util.function.Consumer;

/**
 * Creative helpers for testing the guide: the Scroll of Omniscience teaches everything (every rune with three stars,
 * every item with essences, every item quest), the Scroll of Oblivion forgets everything. Not craftable; they stay in
 * the hand after use.
 */
public class KnowledgeScrollItem extends Item {
    private final boolean teaches;

    public KnowledgeScrollItem(boolean teaches, Properties properties) {
        super(properties);
        this.teaches = teaches;
    }

    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            if (teaches) {
                Research.learnEverything(server);
            } else {
                Research.forgetEverything(server);
            }
            server.displayClientMessage(Component.translatable(teaches ? "item.hextale.scroll_of_omniscience.used"
                    : "item.hextale.scroll_of_oblivion.used").withStyle(teaches ? ChatFormatting.GOLD : ChatFormatting.DARK_PURPLE), true);
            level.playSound(null, player.blockPosition(), ModSounds.SCROLL_KNOWLEDGE.get(), SoundSource.PLAYERS, 0.8F, teaches ? 1.1F : 0.7F);
        }
        player.getCooldowns().addCooldown(player.getItemInHand(hand).getItem(), 20);
        return InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.Interactions.holder(usePort(level, player, hand), player.getItemInHand(hand));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return teaches;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.creative_only").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
