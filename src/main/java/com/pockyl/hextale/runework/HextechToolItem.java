package com.pockyl.hextale.runework;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * The hextech axe, shovel and sword: tools of their kind carrying up to four runes like the pickaxe, listed in the
 * tooltip like enchantments. Their effects live in {@link ToolEffects}. On 1.21.1 each kind extends its vanilla tool
 * class (stripping, paths, sweeping) and shares the rune behaviour below.
 */
public final class HextechToolItem {
    private HextechToolItem() {
    }

    public static class Axe extends AxeItem {
        public Axe(Tier material, Properties properties) {
            super(material, properties);
        }

        @Override
        public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
            return wear(stack, amount, entity);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
            listRunes(PickaxeRunes.of(stack), tooltipLines::add);
        }
    }

    public static class Shovel extends ShovelItem {
        public Shovel(Tier material, Properties properties) {
            super(material, properties);
        }

        @Override
        public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
            return wear(stack, amount, entity);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
            listRunes(PickaxeRunes.of(stack), tooltipLines::add);
        }
    }

    public static class Sword extends SwordItem {
        public Sword(Tier material, Properties properties) {
            super(material, properties);
        }

        @Override
        public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
            return wear(stack, amount, entity);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
            listRunes(PickaxeRunes.of(stack), tooltipLines::add);
        }
    }

    /** Mending: points of wear are often shrugged off and mended. */
    private static int wear(ItemStack stack, int amount, @Nullable LivingEntity entity) {
        RuneHolder holder = RuneHolder.of(stack);
        if (holder == null || entity == null) {
            return amount;
        }
        ToolRune.Tool tool = ToolEffects.tool(holder);
        int strength = tool == null ? 0 : ToolEffects.strength(stack, tool, ToolRune.MENDING);
        return ToolEffects.mend(stack, amount, entity, strength);
    }

    /** The runes like enchantments: name in the rune's colour and stars, then how many places are taken. */
    static void listRunes(PickaxeRunes runes, Consumer<Component> tooltip) {
        for (PickaxeRunes.Entry entry : runes.entries()) {
            tooltip.accept(Component.translatable(entry.rune().translationKey()).withColor(entry.rune().color())
                    .append(Component.literal(" " + "★".repeat(entry.stars())).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("★".repeat(3 - entry.stars())).withStyle(ChatFormatting.DARK_GRAY)));
        }
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", runes.count(), PickaxeRunes.SLOTS)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
