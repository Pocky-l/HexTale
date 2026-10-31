package com.pockyl.hextale.runework;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * The hextech axe, shovel and sword: tools of their kind carrying up to four runes like the pickaxe, listed in the
 * tooltip like enchantments. Their effects live in {@link ToolEffects}.
 */
public class HextechToolItem extends Item {
    public HextechToolItem(Properties properties) {
        super(properties);
    }

    /** Right click with the Blink rune: a step through the shadow to the back of the creature in sight, and a blow. */
    @Override
    public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                                                     net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != net.minecraft.world.InteractionHand.MAIN_HAND || WeaponRunes.strength(stack, ToolRune.BLINK) <= 0) {
            return super.use(level, player, hand);
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            return net.minecraft.world.InteractionResult.FAIL;
        }
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            return WeaponRunes.blink(server, player, stack) ? net.minecraft.world.InteractionResult.SUCCESS_SERVER
                    : net.minecraft.world.InteractionResult.FAIL;
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    /** Mending: points of wear are often shrugged off and mended. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        RuneHolder holder = RuneHolder.of(stack);
        if (holder == null || entity == null) {
            return amount;
        }
        ToolRune.Tool tool = ToolEffects.tool(holder);
        int strength = tool == null ? 0 : ToolEffects.strength(stack, tool, ToolRune.MENDING);
        return ToolEffects.mend(stack, amount, entity, strength);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        listRunes(stack, PickaxeRunes.of(stack), tooltip);
    }

    /** The runes like enchantments: name in the rune's colour and stars, then how many places are taken. */
    static void listRunes(ItemStack stack, PickaxeRunes runes, Consumer<Component> tooltip) {
        for (PickaxeRunes.Entry entry : runes.entries()) {
            tooltip.accept(Component.translatable(entry.rune().translationKey()).withColor(entry.rune().color())
                    .append(Component.literal(" " + "★".repeat(entry.stars())).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("★".repeat(3 - entry.stars())).withStyle(ChatFormatting.DARK_GRAY)));
        }
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", runes.count(), RuneTier.slots(stack))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** The axe strips logs, the shovel makes paths (26.3: their block_transformer component). */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return com.pockyl.hextale.port.PortTransformers.useOn(context);
    }
}
