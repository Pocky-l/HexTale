package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;

import java.util.ArrayList;
import java.util.List;

/**
 * A rune drawn in chalk on the floor (Rune Chalk). It does nothing alone; around a Hextech Altar the marks quicken the
 * ritual - each mark within {@link #RADIUS} blocks adds {@link #SPEED_PER_MARK} of a second step per tick, up to
 * {@link #MAX_MARKS} - and they glow gold while it runs. Three glyphs, turned four ways; no drops, a touch wipes it.
 */
public class ChalkMarkBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty GLYPH = IntegerProperty.create("glyph", 0, 2);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int RADIUS = 4;
    public static final int MAX_MARKS = 10;
    public static final float SPEED_PER_MARK = 0.04F;
    private static final VoxelShape SHAPE = com.pockyl.hextale.port.PortShapes.column(16.0, 0.0, 0.5);
    private static final int GOLD = 0xF2D27A;

    public ChalkMarkBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GLYPH, 0).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GLYPH, LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /** Lit marks breathe gold motes up off the floor. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(3) == 0) {
            level.addParticle(ModParticles.glint(GOLD), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.05,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.03, 0);
        }
    }

    /** Marks counted for the altar at {@code altar} (the nearest first, at most {@link #MAX_MARKS}). */
    public static List<BlockPos> around(Level level, BlockPos altar) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(altar.offset(-RADIUS, -1, -RADIUS), altar.offset(RADIUS, 1, RADIUS))) {
            if (level.getBlockState(pos).is(ModBlocks.CHALK_MARK.get())) {
                found.add(pos.immutable());
            }
        }
        found.sort((a, b) -> Double.compare(a.distSqr(altar), b.distSqr(altar)));
        return found.size() > MAX_MARKS ? found.subList(0, MAX_MARKS) : found;
    }

    /** The share of a second ritual step per tick that the marks around the altar give. */
    public static float speed(Level level, BlockPos altar) {
        return around(level, altar).size() * SPEED_PER_MARK;
    }

    /** Lights or dims every mark around the altar (a ritual starts or ends). */
    public static void light(ServerLevel level, BlockPos altar, boolean lit) {
        for (BlockPos pos : BlockPos.betweenClosed(altar.offset(-RADIUS, -1, -RADIUS), altar.offset(RADIUS, 1, RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.CHALK_MARK.get()) && state.getValue(LIT) != lit) {
                level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected com.mojang.serialization.MapCodec<ChalkMarkBlock> codec() {
        return simpleCodec(ChalkMarkBlock::new);
    }
}
