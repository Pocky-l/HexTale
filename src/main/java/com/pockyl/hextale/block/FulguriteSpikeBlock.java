package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * A fulgurite spike: a glassy tube of fused earth standing up out of the ground where a bolt went in, knobbed and
 * branching like the real thing. A spike is a column of parts - a thick base, middles, a thin tip - that set themselves
 * by what is above and below (like pointed dripstone); it needs ground or another spike under it.
 */
public class FulguriteSpikeBlock extends Block {
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final VoxelShape BASE = Block.column(10.0, 0.0, 16.0);
    private static final VoxelShape MIDDLE = Block.column(7.0, 0.0, 16.0);
    private static final VoxelShape TIP = Block.column(5.0, 0.0, 12.0);

    public FulguriteSpikeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.TIP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = switch (state.getValue(PART)) {
            case BASE -> BASE;
            case MIDDLE -> MIDDLE;
            case TIP -> TIP;
        };
        return shape.move(state.getOffset(pos));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState under = level.getBlockState(below);
        return under.is(this) || under.isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? shaped(state, context.getLevel(), context.getClickedPos()) : null;
    }

    /** The part a spike block is by its neighbours: a tip with nothing of the spike above, a base on the ground. */
    public BlockState shaped(BlockState state, BlockGetter level, BlockPos pos) {
        boolean above = level.getBlockState(pos.above()).is(this);
        boolean below = level.getBlockState(pos.below()).is(this);
        return state.setValue(PART, !above ? Part.TIP : below ? Part.MIDDLE : Part.BASE);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return direction.getAxis().isVertical() ? shaped(state, level, pos) : state;
    }

    public enum Part implements StringRepresentable {
        BASE("base"),
        MIDDLE("middle"),
        TIP("tip");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
