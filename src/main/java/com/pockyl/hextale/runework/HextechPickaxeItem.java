package com.pockyl.hextale.runework;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/** A hextech pickaxe: a pickaxe of its tier with the fixed abilities of its {@link HextechPickaxe}, as strong as its stars. */
public class HextechPickaxeItem extends Item {
    private final HextechPickaxe kind;

    public HextechPickaxeItem(HextechPickaxe kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public HextechPickaxe kind() {
        return kind;
    }

    @Nullable
    public static HextechPickaxe kind(ItemStack stack) {
        return stack.getItem() instanceof HextechPickaxeItem pickaxe ? pickaxe.kind : null;
    }

    /** Tempered pickaxes often shrug off wear. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<ItemStack> onBroken) {
        if (!kind.has(HextechPickaxe.Ability.TEMPER) || entity == null || entity.level().isClientSide()) {
            return amount;
        }
        float chance = PickaxeEffects.temperChance(HextechPickaxe.stars(stack));
        int left = amount;
        for (int i = 0; i < amount; i++) {
            if (entity.getRandom().nextFloat() < chance) {
                left--;
            }
        }
        return left;
    }

    /** Right click: the pickaxe's active ability (ore sight, tunnel, quake), then it rests a moment. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || kind.active() == null) {
            return super.use(level, player, hand);
        }
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            return PickaxeEffects.use(server, player, stack, kind) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        int stars = HextechPickaxe.stars(stack);
        tooltip.accept(Component.literal("★".repeat(stars)).withStyle(ChatFormatting.GOLD)
                .append(Component.literal("★".repeat(3 - stars)).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable("item.hextale.hextech_pickaxe.speed", PickaxeEffects.times(HextechPickaxe.speed(stars)))
                        .withStyle(ChatFormatting.GRAY)));
        for (Component line : PickaxeEffects.describe(kind, stars)) {
            tooltip.accept(line);
        }
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.stars").withStyle(ChatFormatting.DARK_GRAY));
    }
}
