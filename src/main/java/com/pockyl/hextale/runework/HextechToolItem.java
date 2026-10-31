package com.pockyl.hextale.runework;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * The hextech axe, shovel and sword: tools of their kind carrying up to four runes like the pickaxe, listed in the
 * tooltip like enchantments. Their effects live in {@link ToolEffects}.
 *
 * <p>1.21.1: tools are {@link Tier} based, so each kind extends its vanilla tool class (stripping, paths, sweeping) as
 * the nested {@link Axe}, {@link Shovel} and {@link Sword}; this class itself is the plain rune-carrying item the Strike
 * Gauntlet builds on. All of them share the rune behaviour below.
 */
public class HextechToolItem extends Item {
    public HextechToolItem(Properties properties) {
        super(properties);
    }

    /** Right click with the Blink rune: a step through the shadow to the back of the creature in sight, and a blow. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResultHolder<ItemStack> blink = blink(level, player, hand);
        return blink != null ? blink : super.use(level, player, hand);
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return wear(stack, amount, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        listRunes(stack, PickaxeRunes.of(stack), tooltipLines::add);
    }

    public static class Axe extends AxeItem {
        public Axe(Tier material, Properties properties) {
            super(material, properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            InteractionResultHolder<ItemStack> blink = blink(level, player, hand);
            return blink != null ? blink : super.use(level, player, hand);
        }

        @Override
        public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
            return wear(stack, amount, entity);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
            listRunes(stack, PickaxeRunes.of(stack), tooltipLines::add);
        }
    }

    public static class Shovel extends ShovelItem {
        public Shovel(Tier material, Properties properties) {
            super(material, properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            InteractionResultHolder<ItemStack> blink = blink(level, player, hand);
            return blink != null ? blink : super.use(level, player, hand);
        }

        @Override
        public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
            return wear(stack, amount, entity);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
            listRunes(stack, PickaxeRunes.of(stack), tooltipLines::add);
        }
    }

    public static class Sword extends SwordItem {
        public Sword(Tier material, Properties properties) {
            super(material, properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            InteractionResultHolder<ItemStack> blink = blink(level, player, hand);
            return blink != null ? blink : super.use(level, player, hand);
        }

        @Override
        public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
            return wear(stack, amount, entity);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
            listRunes(stack, PickaxeRunes.of(stack), tooltipLines::add);
        }
    }

    /**
     * Right click with the Blink rune: a step through the shadow to the back of the creature in sight, and a blow.
     * Null when the tool has no Blink (the vanilla use goes on).
     */
    static @Nullable InteractionResultHolder<ItemStack> blink(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || WeaponRunes.strength(stack, ToolRune.BLINK) <= 0) {
            return null;
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel server) {
            return WeaponRunes.blink(server, player, stack) ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.success(stack);
    }

    /** Mending: points of wear are often shrugged off and mended. */
    static int wear(ItemStack stack, int amount, @Nullable LivingEntity entity) {
        RuneHolder holder = RuneHolder.of(stack);
        if (holder == null || entity == null) {
            return amount;
        }
        ToolRune.Tool tool = ToolEffects.tool(holder);
        int strength = tool == null ? 0 : ToolEffects.strength(stack, tool, ToolRune.MENDING);
        return ToolEffects.mend(stack, amount, entity, strength);
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
}
