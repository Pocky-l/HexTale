package com.pockyl.hextale.runework;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.registry.ModTags;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The Rift Staff, forged on the Hexcore round the Prosector's probe. A left click casts a rift bolt whenever the staff is
 * ready (the attack indicator); every third cast is the lance that pierces everything in a line. Held up with the right
 * click it raises a ward of hexagons that stops what comes from the front until it has taken too much and breaks; a
 * lowered ward heals. Every blow the ward stops counts towards the lance. The logic lives in {@link RiftStaff}.
 */
public class RiftStaffItem extends Item {
    /** It is a level 4 thing: as many rune places as riftforged gear. */
    public static final int RUNE_PLACES = 4;
    public static final int DURABILITY = 3000;
    /** Ticks between casts (the attack indicator fills in this time). */
    public static final int CAST_TICKS = 14;
    /** The ward's colour on the item bar, and the item bar's when the ward is whole. */
    private static final int WARD_BAR = 0x9A7CF0;

    public RiftStaffItem(Properties properties) {
        super(properties);
    }

    public static Properties properties(Properties properties) {
        return properties.durability(DURABILITY).rarity(Rarity.EPIC).fireResistant()
                .repairable(ModTags.REPAIRS_RIFTMETAL)
                .attributes(attributes())
                .component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(0.1F, 1.0F,
                        List.of(new BlocksAttacks.DamageReduction(RiftStaff.WARD_ANGLE, Optional.empty(), 0.0F, 1.0F)),
                        new BlocksAttacks.ItemDamageFunction(2.0F, 1.0F, 0.0F), Optional.empty(),
                        Optional.of(ModSounds.STAFF_WARD_HIT), Optional.of(ModSounds.STAFF_WARD_BREAK)));
    }

    /** No blow of its own (a fist's), and the rhythm of the casts as the attack speed. */
    public static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, 20.0 / CAST_TICKS - 4, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (RiftStaff.wardLeft(stack) <= 0) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (getUseDuration(stack, entity) - ticksRemaining == 1 && level instanceof ServerLevel server) {
            RiftStaff.raised(server, entity);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (owner instanceof LivingEntity living) {
            RiftStaff.rest(level, living, stack);
        }
    }

    @Override
    public boolean canDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // The charge, the ward and the wear change while it is held: only a new item lifts it anew.
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    // The bar shows the ward while it is not whole, else the wear.

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STAFF_WARD.get(), 0F) > 0 || super.isBarVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (stack.getOrDefault(ModDataComponents.STAFF_WARD.get(), 0F) > 0) {
            return Math.round(13.0F * RiftStaff.wardLeft(stack) / RiftStaff.wardMax(stack));
        }
        return super.getBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (stack.getOrDefault(ModDataComponents.STAFF_WARD.get(), 0F) > 0) {
            float left = RiftStaff.wardLeft(stack) / RiftStaff.wardMax(stack);
            return left > 0 ? WARD_BAR : 0x5A4A70;
        }
        return super.getBarColor(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hextale.rift_staff.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.rift_staff.ward", Mth.ceil(RiftStaff.wardLeft(stack)), Mth.ceil(RiftStaff.wardMax(stack)))
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable("item.hextale.rift_staff.controls").withStyle(ChatFormatting.DARK_GRAY));
    }
}
