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

import com.pockyl.hextale.rune.ComplexRune;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * A hextech pickaxe: a pickaxe of its tier carrying up to four runes, listed in the tooltip like enchantments - name and
 * stars, never what they do.
 */
public class HextechPickaxeItem extends Item {
    private final HextechPickaxe kind;

    public HextechPickaxeItem(HextechPickaxe kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public HextechPickaxe kind() {
        return kind;
    }

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof HextechPickaxeItem;
    }

    /** Tempering: points of wear are often shrugged off. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<ItemStack> onBroken) {
        int strength = PickaxeRunes.of(stack).strength(PickaxeRune.TEMPERING);
        if (strength == 0 || entity == null || entity.level().isClientSide()) {
            return amount;
        }
        float chance = PickaxeEffects.temperChance(strength);
        int left = amount;
        for (int i = 0; i < amount; i++) {
            if (entity.getRandom().nextFloat() < chance) {
                left--;
            }
        }
        return left;
    }

    /** Right click: the pickaxe's right-click rune, if it carries one; then it rests a moment. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PickaxeRune power = PickaxeEffects.power(stack);
        if (hand != InteractionHand.MAIN_HAND || power == null) {
            return super.use(level, player, hand);
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            return PickaxeEffects.use(server, player, stack, power) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        PickaxeRunes runes = PickaxeRunes.of(stack);
        for (PickaxeRunes.Entry entry : runes.entries()) {
            ComplexRune rune = entry.rune();
            tooltip.accept(Component.translatable(rune.translationKey()).withColor(rune.color())
                    .append(Component.literal(" " + "★".repeat(entry.stars())).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("★".repeat(3 - entry.stars())).withStyle(ChatFormatting.DARK_GRAY)));
        }
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", runes.count(), PickaxeRunes.SLOTS)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
