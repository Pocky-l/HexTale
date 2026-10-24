package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.block.HexPylonBlockEntity;
import com.pockyl.hextale.menu.ItemFilterMenu;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.transport.ItemFilter;

import java.util.function.Consumer;

/**
 * A Pylon Filter: right click (in the air or on any block) opens its settings; right click on a pylon sets it into the
 * pylon (a filter already there comes back to the player), sneak + right click on a pylon takes the pylon's filter out.
 */
public class PylonFilterItem extends Item {
    public PylonFilterItem(Properties properties) {
        super(properties);
    }

    /** Into the player's inventory, or at their feet when it is full. */
    public static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider((id, inventory, who) -> new ItemFilterMenu(id, inventory, hand),
                    Component.translatable("item.hextale.pylon_filter")), buf -> buf.writeEnum(hand));
        }
        return InteractionResult.SUCCESS;
    }

    /** On a pylon: sneak takes its filter out (setting one in is the pylon's own use); anywhere else: the settings. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()
                && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof HexPylonBlockEntity pylon) {
            if (!context.getLevel().isClientSide()) {
                ItemStack out = pylon.takeFilter();
                if (out.isEmpty()) {
                    player.displayClientMessage(Component.translatable("item.hextale.pylon_filter.none"), true);
                } else {
                    com.pockyl.hextale.item.PylonFilterItem.give(player, out);
                    player.displayClientMessage(Component.translatable("item.hextale.pylon_filter.taken"), true);
                    context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.7F, 1.2F);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return player == null ? InteractionResult.PASS : use(context.getLevel(), player, context.getHand());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        ItemFilter filter = stack.get(ModDataComponents.ITEM_FILTER.get());
        if (filter != null && !filter.entries().isEmpty()) {
            tooltip.accept(Component.translatable(filter.whitelist() ? "gui.hextale.pylon_filter.whitelist" : "gui.hextale.pylon_filter.blacklist")
                    .withStyle(ChatFormatting.GOLD));
            for (ItemFilter.Entry entry : filter.entries()) {
                Component name = entry.item().getHoverName();
                tooltip.accept(filter.rule() == ItemFilter.Rule.NONE || !filter.whitelist() ? Component.literal("• ").append(name)
                        .withStyle(ChatFormatting.GRAY) : Component.literal("• ").append(name).append(" · " + entry.count()).withStyle(ChatFormatting.GRAY));
            }
            if (filter.rule() != ItemFilter.Rule.NONE && filter.whitelist()) {
                tooltip.accept(Component.translatable("gui.hextale.pylon_filter.rule." + filter.rule().getSerializedName()).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        tooltip.accept(Component.translatable("item.hextale.pylon_filter.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
