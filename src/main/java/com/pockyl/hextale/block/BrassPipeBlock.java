package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;

import java.util.EnumMap;
import java.util.Map;

/**
 * A brass pipe for essence. Pipes carry no liquid themselves: a machine pushing into a pipe reaches every vessel the
 * connected pipes touch (see {@code EssenceNetwork}). A side joins another pipe, or a machine where that machine has a
 * port (its model then shows the joint too).
 * <p>
 * Sneak-use with an empty hand sets the side looked at (the arm clicked, or the face of the core): towards a pipe it
 * shuts or opens the joint, towards a vessel it cycles both ways -> in only -> out only -> shut. An "out only" side
 * pumps a jar out into the pipes by itself.
 */
public class BrassPipeBlock extends Block {
    public static final Map<Direction, EnumProperty<PipeSide>> SIDES = sides();
    /** Ticks between two pulls of a side that pumps a jar out, and the millibuckets of one pull. */
    public static final int PUMP_EVERY = EssenceTankBlockEntity.FLOW_EVERY;
    public static final int PUMP_FLOW = EssenceTankBlockEntity.FLOW;
    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);

    public BrassPipeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (EnumProperty<PipeSide> side : SIDES.values()) {
            state = state.setValue(side, PipeSide.NONE);
        }
        registerDefaultState(state);
    }

    private static Map<Direction, EnumProperty<PipeSide>> sides() {
        Map<Direction, EnumProperty<PipeSide>> sides = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            sides.put(direction, EnumProperty.create(direction.getSerializedName(), PipeSide.class));
        }
        return sides;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIDES.values().toArray(EnumProperty[]::new));
    }

    /** What the pipe in this state does towards {@code direction}; NONE for anything that is not a pipe. */
    public static PipeSide side(BlockState state, Direction direction) {
        return state.getBlock() instanceof BrassPipeBlock ? state.getValue(SIDES.get(direction)) : PipeSide.NONE;
    }

    /** Whether this is a pipe joined towards {@code direction} (a machine there shows its joint). */
    public static boolean joins(BlockState state, Direction direction) {
        return side(state, direction).open();
    }

    /** Whether a pipe joins the neighbour in {@code direction}: another pipe, or a machine's port facing it. */
    public static boolean connects(BlockState neighbour, Direction direction) {
        return neighbour.getBlock() instanceof BrassPipeBlock
                || neighbour.getBlock() instanceof EssenceMachine machine && machine.isPort(neighbour, direction.getOpposite());
    }

    /** What a side becomes next to this neighbour: a shut side stays shut, a set vessel side keeps its way. */
    private static PipeSide sideFor(PipeSide current, Direction direction, BlockState neighbour) {
        if (neighbour.getBlock() instanceof BrassPipeBlock) {
            // Two pipes shut a joint together; a pipe set beside a shut one stays apart from it.
            return current == PipeSide.CLOSED || side(neighbour, direction.getOpposite()) == PipeSide.CLOSED ? PipeSide.CLOSED : PipeSide.PIPE;
        }
        if (connects(neighbour, direction)) {
            return current == PipeSide.IN || current == PipeSide.OUT || current == PipeSide.CLOSED ? current : PipeSide.PIPE;
        }
        return PipeSide.NONE;
    }

    /** The state with every side set for its neighbour (hand-set sides kept where they still apply). */
    private static BlockState joined(LevelReader level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            EnumProperty<PipeSide> property = SIDES.get(direction);
            state = state.setValue(property, sideFor(state.getValue(property), direction, level.getBlockState(pos.relative(direction))));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return joined(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        PipeSide side = sideFor(state.getValue(SIDES.get(direction)), direction, neighbour);
        if (side == PipeSide.OUT) {
            ticks.scheduleTick(pos, this, PUMP_EVERY);
        }
        return state.setValue(SIDES.get(direction), side);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
        // Called for every new state of a pipe (placed, joined, a side set by hand): networks are walked again.
        EssenceNetwork.invalidate();
        if (!old.is(this) && !level.isClientSide()) {
            // Set by a command or a structure, not by a player: join the neighbours the way placing it would.
            BlockState joined = joined(level, pos, state);
            if (joined != state) {
                level.setBlock(pos, joined, Block.UPDATE_ALL);
                return;
            }
        }
        if (pumps(state)) {
            level.scheduleTick(pos, this, PUMP_EVERY);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        EssenceNetwork.invalidate();
    }

    private static boolean pumps(BlockState state) {
        for (EnumProperty<PipeSide> side : SIDES.values()) {
            if (state.getValue(side) == PipeSide.OUT) {
                return true;
            }
        }
        return false;
    }

    /** Every out-only side towards a jar pulls from it and pushes into the pipes; the tick runs while one is left. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!pumps(state)) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (state.getValue(SIDES.get(direction)) == PipeSide.OUT) {
                pump(level, pos, direction);
            }
        }
        level.scheduleTick(pos, this, PUMP_EVERY);
    }

    private static void pump(ServerLevel level, BlockPos pos, Direction direction) {
        BlockPos at = pos.relative(direction);
        EssenceTankBlockEntity jar = EssenceTankBlockEntity.at(level, at);
        if (jar == null || jar.tank().isEmpty()) {
            return;
        }
        EssenceMix offer = jar.tank().drain(PUMP_FLOW, true);
        int moved = EssenceNetwork.pushOutOf(level, at, direction.getOpposite(), offer, jar);
        if (moved > 0) {
            jar.tank().remove(offer.part(moved));
            jar.setChanged();
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        Direction direction = aimedSide(state, pos, hitResult);
        PipeSide current = state.getValue(SIDES.get(direction));
        BlockPos neighbourPos = pos.relative(direction);
        BlockState neighbour = level.getBlockState(neighbourPos);
        if (current == PipeSide.NONE && !connects(neighbour, direction)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("block.hextale.brass_pipe.nothing"), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean towardsPipe = neighbour.getBlock() instanceof BrassPipeBlock;
        PipeSide next = current.next(towardsPipe);
        if (towardsPipe) {
            // The pipe beside first, quietly, so its shape update does not shut this side again.
            level.setBlock(neighbourPos, neighbour.setValue(SIDES.get(direction.getOpposite()), next),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        level.setBlock(pos, state.setValue(SIDES.get(direction), next), Block.UPDATE_ALL);
        if (next == PipeSide.OUT) {
            level.scheduleTick(pos, this, PUMP_EVERY);
        }
        level.playSound(null, pos, next == PipeSide.CLOSED ? SoundEvents.COPPER_TRAPDOOR_CLOSE : SoundEvents.COPPER_TRAPDOOR_OPEN,
                SoundSource.BLOCKS, 0.6F, 1.3F);
        player.displayClientMessage(Component.translatable("block.hextale.brass_pipe.side." + next.getSerializedName()
                + (towardsPipe ? ".pipe" : ""), neighbour.getBlock().getName()), true);
        return InteractionResult.SUCCESS;
    }

    /** The side the player aims at: the arm hit, else the face of the core that was clicked. */
    public static Direction aimedSide(BlockState state, BlockPos pos, BlockHitResult hitResult) {
        Vec3 local = hitResult.getLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        double reach = 2.0 / 16 + 1.0E-4;
        Direction best = hitResult.getDirection();
        double far = reach;
        for (Direction direction : Direction.values()) {
            double along = local.x * direction.getStepX() + local.y * direction.getStepY() + local.z * direction.getStepZ();
            if (along > far) {
                far = along;
                best = direction;
            }
        }
        return best;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction direction : Direction.values()) {
            PipeSide side = state.getValue(SIDES.get(direction));
            if (side == PipeSide.NONE) {
                continue;
            }
            // A shut side is a short capped stub that ends 2 px before the block edge.
            int edge = side == PipeSide.CLOSED ? 2 : 0;
            shape = Shapes.or(shape, Block.box(
                    direction.getStepX() < 0 ? edge : direction.getStepX() > 0 ? 10 : 6,
                    direction.getStepY() < 0 ? edge : direction.getStepY() > 0 ? 10 : 6,
                    direction.getStepZ() < 0 ? edge : direction.getStepZ() > 0 ? 10 : 6,
                    direction.getStepX() < 0 ? 6 : direction.getStepX() > 0 ? 16 - edge : 10,
                    direction.getStepY() < 0 ? 6 : direction.getStepY() > 0 ? 16 - edge : 10,
                    direction.getStepZ() < 0 ? 6 : direction.getStepZ() > 0 ? 16 - edge : 10));
        }
        return shape;
    }
}
