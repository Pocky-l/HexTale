package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.thevoid.TheVoid;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The Void Anchor: a standing stone of nullstone with a window through it, a brass collar round its waist, and a hextech
 * crystal hanging in the window ({@link VoidAnchorBlockEntity}). Six of them round a breach, one on every side, hold it.
 * A hextech crystal in hand goes in; an empty hand takes the crystal out, or tells what it does when it holds none.
 */
public class VoidAnchorBlock extends BaseEntityBlock {

    public VoidAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!DeviceCrystals.fits(stack) || !(level.getBlockEntity(pos) instanceof VoidAnchorBlockEntity anchor)
                || !anchor.crystal().isEmpty()) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            anchor.insert(stack);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    /**
     * A clouded crystal pressed to an anchor of a held breach, sneaking: the crystal goes into the breach and it folds into
     * its heart. Called by the crystal ({@code CloudedCrystalItem}): a sneaking player with an item in hand never reaches
     * the block's own use.
     */
    public static InteractionResult fold(ItemStack stack, Level level, VoidAnchorBlockEntity anchor, Player player) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        TheVoid breach = anchor.heldBreach(server);
        if (breach == null || !breach.subdued()) {
            player.displayClientMessage(Component.translatable("block.hextale.void_anchor.fold.not_held"), true);
            return InteractionResult.FAIL;
        }
        if (!breach.startFold(server)) {
            player.displayClientMessage(Component.translatable("block.hextale.void_anchor.fold.busy"), true);
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        net.minecraft.world.phys.Vec3 from = VoidAnchorBlockEntity.crystalAt(anchor.getBlockPos());
        net.minecraft.world.phys.Vec3 to = breach.position();
        for (int i = 1; i < 10; i++) {
            net.minecraft.world.phys.Vec3 at = from.lerp(to, i / 10.0);
            server.sendParticles(ModParticles.glint(TheVoid.COLD), at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof VoidAnchorBlockEntity anchor) {
            if (anchor.crystal().isEmpty()) {
                player.displayClientMessage(status(anchor), true);
            } else {
                player.getInventory().placeItemBackInInventory(anchor.take());
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** What it does now, in one line. */
    public static Component status(VoidAnchorBlockEntity anchor) {
        if (anchor.crystal().isEmpty()) {
            return Component.translatable("block.hextale.void_anchor.status.empty");
        }
        if (!anchor.holds()) {
            return Component.translatable("block.hextale.void_anchor.status.clouded");
        }
        String clouded = String.format(Locale.ROOT, "%.0f", anchor.clouding() * 100);
        if (!anchor.tied()) {
            return Component.translatable("block.hextale.void_anchor.status.free", clouded);
        }
        return Component.translatable(anchor.subdued() ? "block.hextale.void_anchor.status.held" : "block.hextale.void_anchor.status.fighting",
                clouded);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapes.of(state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** A comparator reads how clouded the crystal is (15 = clouded through, 0 = none in it). */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof VoidAnchorBlockEntity anchor) || anchor.crystal().isEmpty()) {
            return 0;
        }
        return 1 + Math.round(anchor.clouding() * 14);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VoidAnchorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.VOID_ANCHOR.get(),
                level.isClientSide() ? VoidAnchorBlockEntity::clientTick : VoidAnchorBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<VoidAnchorBlock> codec() {
        return simpleCodec(VoidAnchorBlock::new);
    }
}
