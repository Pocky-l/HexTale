package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.OpenStickerPayload;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.rune.Essence;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * An essence sticker: a paper label with one essence on it. Right click in the air or on a block that takes no sticker
 * opens the essence picker; right click on an Essence Jar sticks it on (the jar then takes only that essence), on a
 * siphon or relay pylon it makes the pylon carry only that essence. Blank until an essence is chosen.
 */
public class EssenceStickerItem extends Item {
    public EssenceStickerItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static Essence essence(ItemStack stack) {
        return stack.get(ModDataComponents.STICKER.get());
    }

    public static ItemStack of(Item item, @Nullable Essence essence) {
        ItemStack stack = new ItemStack(item);
        if (essence != null) {
            stack.set(ModDataComponents.STICKER.get(), essence);
        }
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            ModNetwork.sendToPlayer(server, new OpenStickerPayload(hand));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        return player == null ? InteractionResult.PASS : use(context.getLevel(), player, context.getHand());
    }

    @Override
    public Component getName(ItemStack stack) {
        Essence essence = essence(stack);
        return essence == null ? super.getName(stack)
                : Component.translatable("item.hextale.essence_sticker.of", Component.translatable(essence.translationKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(essence(stack) == null ? "item.hextale.essence_sticker.blank" : "item.hextale.essence_sticker.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
