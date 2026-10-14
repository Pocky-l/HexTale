package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.OpenDiaryPayload;
import com.pockyl.hextale.registry.ModDataComponents;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * The researcher's diary. It is signed by the first player who opens it; everything in it is that player's knowledge,
 * kept on the player, so a lost diary is simply crafted again.
 */
public class ResearchersDiaryItem extends Item {
    public ResearchersDiaryItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static DiaryOwner owner(ItemStack stack) {
        return stack.get(ModDataComponents.DIARY_OWNER.get());
    }

    /** Unsigned diaries can be used by anyone (and get signed by them); signed ones only by their owner. */
    public static boolean canResearchWith(ItemStack diary, Player player) {
        DiaryOwner owner = owner(diary);
        return owner == null || owner.is(player);
    }

    /** Signs an unsigned diary; returns whether it was signed now. */
    public static boolean sign(ItemStack diary, ServerPlayer player) {
        if (owner(diary) != null) {
            return false;
        }
        diary.set(ModDataComponents.DIARY_OWNER.get(), DiaryOwner.of(player));
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        DiaryOwner owner = owner(stack);
        return owner == null ? super.getName(stack) : Component.translatable("item.hextale.researchers_diary.owned", owner.name());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DiaryOwner owner = owner(stack);
        tooltip.add((owner == null ? Component.translatable("item.hextale.researchers_diary.unsigned")
                : Component.translatable("item.hextale.researchers_diary.signed", owner.name())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.hextale.researchers_diary.read").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (sign(stack, serverPlayer)) {
                level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 0.8F, 1.2F);
            }
            DiaryOwner owner = owner(stack);
            if (owner != null && !owner.is(player)) {
                serverPlayer.displayClientMessage(Component.translatable("item.hextale.researchers_diary.foreign", owner.name()), true);
                return InteractionResultHolder.fail(stack);
            }
            ModNetwork.sendToPlayer(serverPlayer, new OpenDiaryPayload(owner == null ? player.getGameProfile().getName() : owner.name()));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
