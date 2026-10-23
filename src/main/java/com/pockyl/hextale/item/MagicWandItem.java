package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.RuneHolder;
import com.pockyl.hextale.runework.ToolEffects;
import com.pockyl.hextale.runework.WandRune;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * The Magic Wand: a rod of magic wood with hextech dust in its tip. A touch wakes a block the way a pinch of dust does
 * (without spending dust, see {@code BlockAwakening}), and only the wand starts a ritual at the Hextech Altar. Every
 * touch wears it a little. Takes runes ({@link WandRune}).
 */
public class MagicWandItem extends Item {
    /** Wear of waking a block; starting a ritual wears one point. */
    public static final int AWAKEN_WEAR = 2;

    public MagicWandItem(Properties properties) {
        super(properties);
    }

    public static boolean is(ItemStack stack) {
        return stack.is(ModItems.MAGIC_WAND.get());
    }

    public static int strength(ItemStack wand, WandRune rune) {
        return RuneHolder.WAND.runes(wand).strength(rune.rune());
    }

    /** Wears the wand by {@code points} in the hand of {@code user} (Dam may spare it). */
    public static void wear(ItemStack wand, int points, LivingEntity user, EquipmentSlot slot) {
        int dam = strength(wand, WandRune.DAM);
        if (dam > 0 && user.getRandom().nextFloat() < HolderRune.at(WandRune.THRIFT, dam)) {
            if (user.level() instanceof ServerLevel level) {
                ToolEffects.puff(level, user.position().add(0, 1, 0), WandRune.DAM.rune(), 4, 0.3);
            }
            return;
        }
        wand.hurtAndBreak(points, user, slot);
    }

    /** Mending: points of wear are often shrugged off and mended. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<ItemStack> onBroken) {
        return entity == null ? amount : ToolEffects.mend(stack, amount, entity, strength(stack, WandRune.MENDING));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hextale.magic_wand.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", RuneHolder.WAND.runes(stack).count(), WandRune.SLOTS)
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.hextale.magic_wand.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
