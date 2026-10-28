package com.pockyl.hextale.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.block.EssenceMachine;

import org.jspecify.annotations.Nullable;

/**
 * A block of a formed structure. It draws nothing (the controller's renderer draws the whole structure), takes its shape
 * from the structure, passes clicks on to the controller and lets pipes join it on every side. Mined, it drops what it
 * was and the structure comes apart.
 */
public class MultiblockPartBlock extends BaseEntityBlock implements EssenceMachine, net.minecraft.world.WorldlyContainerHolder {
    public MultiblockPartBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part
                && level.getBlockEntity(part.controller()) instanceof MultiblockCore core && core.formed() != null) {
            Multiblock multiblock = Multiblocks.byId(core.formed().multiblock());
            if (multiblock != null) {
                MultiblockPattern.Placement placement = core.formed().placement();
                BlockPos offset = pos.subtract(part.controller()).rotate(Multiblocks.inverse(placement.rotation()));
                return multiblock.partShape(offset);
            }
        }
        return Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** See-through: it neither shades the blocks around it nor keeps the light of the sky from what lies below. */
    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter portLevel, BlockPos portPos) {
        return true;
    }

    /** Pipes may join the structure anywhere; what they bring goes to the controller. */
    @Override
    public boolean isPort(BlockState state, Direction face) {
        return true;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        BlockPos controller = controller(level, pos);
        if (controller == null) {
            return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return com.pockyl.hextale.port.Interactions.itemOn(level.getBlockState(controller).useItemOn(stack, level, player, hand, hitResult.withPosition(controller)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos controller = controller(level, pos);
        if (controller == null) {
            return InteractionResult.PASS;
        }
        return level.getBlockState(controller).useWithoutItem(level, player, hitResult.withPosition(controller));
    }

    /**
     * Hoppers reach a structure that shares its items ({@link Multiblock#sharesItems}) through any of its blocks: the
     * controller's container, with the faces of the block they touch.
     */
    @Override
    public net.minecraft.world.@Nullable WorldlyContainer getContainer(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part ? part.sharedContainer() : null;
    }

    @Nullable
    private static BlockPos controller(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part ? part.controller() : null;
    }

    /** Mined by a player: drops what the block was, as if that block had been mined. */
    @Override
    public void playerDestroy(Level level, net.minecraft.world.entity.player.Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                              ItemStack destroyedWith) {
        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        if (blockEntity instanceof MultiblockPartBlockEntity part) {
            Block.dropResources(part.original(), level, pos, null, player, destroyedWith);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part ? new ItemStack(part.original().getBlock()) : ItemStack.EMPTY;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiblockPartBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<MultiblockPartBlock> codec() {
        return simpleCodec(MultiblockPartBlock::new);
    }
}
