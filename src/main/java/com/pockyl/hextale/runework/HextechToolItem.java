package com.pockyl.hextale.runework;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.pockyl.hextale.rune.ComplexRune;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * A hextech tool (pickaxe, shovel...): a plain tool of its kind plus rune slots and an essence cell. The essence cell
 * is drawn in the tooltip by the client; the runes are listed here.
 */
public class HextechToolItem extends Item {
    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V"};

    public HextechToolItem(Properties properties) {
        super(properties);
    }

    /** Chance a Tempering rune of strength 1-4 saves a point of wear. */
    private static final float[] TEMPERED = {0.4F, 0.6F, 0.8F, 0.9F};

    /** Tempering: each point of wear may be shrugged off, paid with the rune's essence. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<ItemStack> onBroken) {
        RuneSlots.Engraving tempering = HextechTools.engraving(stack, ToolRunes.TEMPERING);
        if (tempering == null || entity == null || entity.level().isClientSide()) {
            return amount;
        }
        float chance = TEMPERED[Math.min(4, HextechTools.strength(stack, tempering)) - 1];
        int left = amount;
        for (int i = 0; i < amount; i++) {
            if (entity.getRandom().nextFloat() < chance && HextechTools.payFiring(stack, tempering, entity.getRandom())) {
                left--;
            }
        }
        return left;
    }

    /** Right click: the runes that fire on use (spells at what the holder looks at). */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !ToolRuneEffects.usable(stack)) {
            return super.use(level, player, hand);
        }
        if (level instanceof ServerLevel server) {
            return ToolRuneEffects.use(server, player, stack) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        RuneSlots runes = HextechTools.runes(stack);
        if (runes.isEmpty()) {
            tooltip.accept(Component.translatable("item.hextale.hextech_tool.no_runes").withStyle(ChatFormatting.DARK_GRAY));
        }
        for (RuneSlots.Engraving engraving : runes.engravings()) {
            ComplexRune rune = engraving.rune();
            int stars = engraving.stars();
            tooltip.accept(Component.literal(NUMERALS[engraving.slot()] + " ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.translatable(rune.translationKey()).withColor(rune.color()))
                    .append(Component.literal(" " + "★".repeat(stars)).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("★".repeat(3 - stars)).withStyle(ChatFormatting.DARK_GRAY)));
        }
    }
}
