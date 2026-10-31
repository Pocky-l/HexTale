package com.pockyl.hextale.runework;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;


import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * A hextech pickaxe: a pickaxe of its tier carrying up to four runes, listed in the tooltip like enchantments - name and
 * stars, never what they do.
 */
public class HextechPickaxeItem extends PickaxeItem {
    private final HextechPickaxe kind;

    public HextechPickaxeItem(HextechPickaxe kind, net.minecraft.world.item.Tier material, Properties properties) {
        super(material, properties);
        this.kind = kind;
    }

    public HextechPickaxe kind() {
        return kind;
    }

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof HextechPickaxeItem;
    }

    /** Tempering: points of wear are often shrugged off; Mending shrugs off more and mends a point. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        if (entity == null || entity.level().isClientSide()) {
            return amount;
        }
        PickaxeRunes runes = PickaxeRunes.of(stack);
        int strength = runes.strength(PickaxeRune.TEMPERING);
        int left = amount;
        if (strength > 0) {
            float chance = PickaxeEffects.temperChance(strength);
            for (int i = 0; i < amount; i++) {
                if (entity.getRandom().nextFloat() < chance) {
                    left--;
                }
            }
        }
        return ToolEffects.mend(stack, left, entity, runes.strength(PickaxeRune.MENDING));
    }

    /** Right click: the pickaxe's right-click rune, if it carries one; then it rests a moment. */
    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PickaxeRune power = PickaxeEffects.power(stack);
        if (hand != InteractionHand.MAIN_HAND || power == null) {
            return super.use(level, player, hand).getResult();
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            return PickaxeEffects.use(server, player, stack, power) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
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
        HextechToolItem.listRunes(stack, PickaxeRunes.of(stack), tooltip);
    }
}
