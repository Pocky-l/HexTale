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

import com.pockyl.hextale.menu.VaultMenu;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.storage.VaultLink;

import java.util.UUID;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * A Hex Terminal for the road: the same screen and the same toll, the jars it pays from are the ones round its holder, so
 * far from them it runs at the slow rate. Used with a link crystal in the other hand it ties itself to that crystal's vault.
 */
public class PocketTerminalItem extends Item {
    public PocketTerminalItem(Properties properties) {
        super(properties);
    }

    /** The vault this stack is tied to, if it is a Pocket Terminal that has one. */
    @Nullable
    public static UUID vault(ItemStack stack) {
        VaultLink link = stack.is(ModItems.POCKET_TERMINAL.get()) ? stack.get(ModDataComponents.VAULT_LINK.get()) : null;
        return link == null ? null : link.vault();
    }

    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        VaultLink offered = other.is(ModItems.RIFT_LINK_CRYSTAL.get()) ? other.get(ModDataComponents.VAULT_LINK.get()) : null;
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResult.SUCCESS;
        }
        if (offered != null) {
            stack.set(ModDataComponents.VAULT_LINK.get(), offered);
            other.consume(1, player);
            level.playSound(null, player.blockPosition(), ModSounds.RIFT_LINK.get(), SoundSource.PLAYERS, 0.5F, 1.4F);
            player.displayClientMessage(Component.translatable("block.hextale.hex_terminal.linked"), true);
            return InteractionResult.SUCCESS;
        }
        UUID vault = vault(stack);
        if (vault == null) {
            player.displayClientMessage(Component.translatable("block.hextale.hex_terminal.not_linked"), true);
        } else if (VaultMenu.open(server, vault, VaultMenu.Kind.POCKET, player.blockPosition(), hand, stack.getHoverName())) {
            level.playSound(null, player.blockPosition(), ModSounds.VAULT_TERMINAL.get(), SoundSource.PLAYERS, 0.3F, 1.2F);
        } else {
            player.displayClientMessage(Component.translatable("block.hextale.hex_terminal.vault_gone"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.Interactions.holder(usePort(level, player, hand), player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        VaultLink link = stack.get(ModDataComponents.VAULT_LINK.get());
        if (link != null) {
            tooltip.accept(Component.translatable("item.hextale.rift_link_crystal.gate", link.pos().getX(), link.pos().getY(), link.pos().getZ())
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        tooltip.accept(Component.translatable("item.hextale.pocket_terminal.hint").withStyle(ChatFormatting.GRAY));
    }
}
