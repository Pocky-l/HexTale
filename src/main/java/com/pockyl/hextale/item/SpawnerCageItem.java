package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import java.util.function.Consumer;

/**
 * The Spawner Cage: a hextech cage that folds a monster spawner into itself - everything it spawns and how - and
 * unfolds it once, against any block. The cage is spent when it unfolds.
 */
public class SpawnerCageItem extends Item {
    public SpawnerCageItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        CompoundTag caged = stack.get(ModDataComponents.CAGED_SPAWNER.get());
        if (caged == null) {
            BlockPos pos = context.getClickedPos();
            if (!level.getBlockState(pos).is(Blocks.SPAWNER) || level.getBlockEntity(pos) == null) {
                return InteractionResult.PASS;
            }
            if (!mayChange(level, player, pos, context)) {
                return InteractionResult.FAIL;
            }
            ItemStack full = stack.copyWithCount(1);
            full.set(ModDataComponents.CAGED_SPAWNER.get(), fold(level, pos));
            stack.shrink(1);
            if (player != null) {
                if (stack.isEmpty()) {
                    player.setItemInHand(context.getHand(), full);
                } else if (!player.getInventory().add(full)) {
                    player.drop(full, false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        BlockPos at = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(at).canBeReplaced() || !mayChange(level, player, at, context)) {
            return InteractionResult.FAIL;
        }
        unfold(level, at, caged);
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    /** Whether {@code player} may change the block at {@code pos}: not in adventure mode, not in a protected spot. */
    private static boolean mayChange(ServerLevel level, @org.jspecify.annotations.Nullable Player player, BlockPos pos, UseOnContext context) {
        return player == null || player.mayBuild() && level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand());
    }

    /** Takes the spawner at {@code pos} out of the world, returning everything it is. */
    public static CompoundTag fold(ServerLevel level, BlockPos pos) {
        BlockEntity spawner = level.getBlockEntity(pos);
        CompoundTag tag = spawner == null ? new CompoundTag() : spawner.saveWithoutMetadata(level.registryAccess());
        level.removeBlockEntity(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        effects(level, pos, 0.8F);
        return tag;
    }

    /** Sets a spawner made of {@code caged} at {@code pos}. */
    public static void unfold(ServerLevel level, BlockPos pos, CompoundTag caged) {
        level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity spawner = level.getBlockEntity(pos);
        if (spawner != null) {
            spawner.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), caged));
            spawner.setChanged();
            level.sendBlockUpdated(pos, spawner.getBlockState(), spawner.getBlockState(), Block.UPDATE_ALL);
        }
        effects(level, pos, 1.2F);
    }

    private static void effects(ServerLevel level, BlockPos pos, float pitch) {
        level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5, 0, 1.0, 1, 0, 1);
        level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.2);
        level.playSound(null, pos, ModSounds.HEART_UNFOLD.get(), SoundSource.BLOCKS, 0.7F, pitch);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(stack.has(ModDataComponents.CAGED_SPAWNER.get())
                ? "item.hextale.spawner_cage.full_hint" : "item.hextale.spawner_cage.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.spawner_cage.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
