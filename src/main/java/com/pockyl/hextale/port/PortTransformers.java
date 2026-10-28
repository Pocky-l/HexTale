package com.pockyl.hextale.port;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;

import org.jspecify.annotations.Nullable;

/**
 * Right-click block conversions of tools that are not AxeItem / ShovelItem on 26.1.2: stripping, scraping and unwaxing
 * for items in {@code #minecraft:axes}, path making for {@code #minecraft:shovels} (the same steps as the vanilla items).
 * port-of: net.minecraft.core.component.DataComponents.BLOCK_TRANSFORMER
 */
public final class PortTransformers {
    private PortTransformers() {
    }

    /** Whether the stack converts blocks like an axe or a shovel. */
    public static boolean transforms(ItemStack stack) {
        return stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS);
    }

    public static InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (stack.is(ItemTags.AXES)) {
            if (convert(context, ItemAbilities.AXE_STRIP, SoundEvents.AXE_STRIP, -1)
                    || convert(context, ItemAbilities.AXE_SCRAPE, SoundEvents.AXE_SCRAPE, 3005)
                    || convert(context, ItemAbilities.AXE_WAX_OFF, SoundEvents.AXE_WAX_OFF, 3004)) {
                return InteractionResult.SUCCESS;
            }
        } else if (stack.is(ItemTags.SHOVELS) && context.getClickedFace() != Direction.DOWN
                && context.getLevel().getBlockState(context.getClickedPos().above()).isAir()
                && convert(context, ItemAbilities.SHOVEL_FLATTEN, SoundEvents.SHOVEL_FLATTEN, -1)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static boolean convert(UseOnContext context, ItemAbility ability, SoundEvent sound, int levelEvent) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        @Nullable Player player = context.getPlayer();
        BlockState changed = level.getBlockState(pos).getToolModifiedState(context, ability, false);
        if (changed == null) {
            return false;
        }
        level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (levelEvent >= 0) {
            level.levelEvent(player, levelEvent, pos, 0);
        }
        if (level.isClientSide()) {
            return true;
        }
        ItemStack stack = context.getItemInHand();
        if (player instanceof ServerPlayer server) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(server, pos, stack);
        }
        level.setBlock(pos, changed, 11);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, changed));
        if (player != null) {
            stack.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
        }
        return true;
    }
}
