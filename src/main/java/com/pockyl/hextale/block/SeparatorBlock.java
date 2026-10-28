package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;

import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The Separator: a separating flask over a dish of blaze powder that draws the essence chosen on its screen out of a
 * mixture, burning the powder. What each side does is set per side (see {@link Side}); the top is the gas vent. A new
 * separator is shut all round: a side opens by itself when something it works with is set right beside it (see
 * {@link #opensAs}) and shuts again when that is taken away. While it works ({@link #LIT}) the powder glows, small
 * flames lick the flask and the gas of separation smokes out of the vent.
 */
public class SeparatorBlock extends BaseEntityBlock implements WorldlyContainerHolder, EssenceMachine {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** What each side shows: the joint of the side's mode (see {@link PortMode}), pipe or not, so a change shows at
     * once. The top is the gas vent and the bottom stands on the floor: no pipes there. */
    public static final Map<Direction, EnumProperty<PortMode>> LOOKS = looks();
    /** Burning blaze powder: the separator is drawing an essence out right now. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SeparatorBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false);
        for (EnumProperty<PortMode> look : LOOKS.values()) {
            state = state.setValue(look, PortMode.NONE);
        }
        registerDefaultState(state);
    }

    private static Map<Direction, EnumProperty<PortMode>> looks() {
        Map<Direction, EnumProperty<PortMode>> looks = new EnumMap<>(Direction.class);
        for (Direction face : Direction.Plane.HORIZONTAL) {
            looks.put(face, EnumProperty.create(face.getSerializedName(), PortMode.class));
        }
        return looks;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
        LOOKS.values().forEach(builder::add);
    }

    /** The sides of a separator as seen by someone standing in front of it. */
    public enum Side {
        FRONT, BACK, LEFT, RIGHT;

        public Direction face(Direction facing) {
            return switch (this) {
                case FRONT -> facing;
                case BACK -> facing.getOpposite();
                // Standing in front and looking at the separator, its left is the viewer's left.
                case LEFT -> facing.getClockWise();
                case RIGHT -> facing.getCounterClockWise();
            };
        }

        @Nullable
        public static Side of(Direction face, Direction facing) {
            for (Side side : values()) {
                if (side.face(facing) == face) {
                    return side;
                }
            }
            return null;
        }
    }

    /** The four sides unless closed; what a side does is set in the separator's screen, or by sneak-using that side of
     * the block. */
    @Override
    public boolean isPort(BlockState state, Direction face) {
        return face.getAxis().isHorizontal() && state.getValue(LOOKS.get(face)) != PortMode.NONE;
    }

    /** The state with the look of {@code face} set to the face's mode. */
    public static BlockState withLook(BlockState state, Direction face, BlockState neighbour, BlockGetter level, BlockPos pos) {
        EnumProperty<PortMode> look = LOOKS.get(face);
        if (look == null) {
            return state;
        }
        Side side = Side.of(face, state.getValue(FACING));
        PortMode mode = level.getBlockEntity(pos) instanceof SeparatorBlockEntity separator ? separator.mode(side) : PortMode.NONE;
        return state.setValue(look, mode);
    }

    /** The state with every face's look set. */
    public static BlockState withLooks(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction face : LOOKS.keySet()) {
            state = withLook(state, face, level.getBlockState(pos.relative(face)), level, pos);
        }
        return state;
    }

    /**
     * What a shut side opens as when this is set right beside it: a jar with essence in it, or an alembic, feeds the
     * separator (inlet); an empty jar takes what it lets out (the concentrate first, then the rest); a pipe brings the
     * mixture in - its way can be changed on the block, the screen or the pipe. Null for anything the separator does
     * not work with: a side beside it shuts.
     */
    @Nullable
    static PortMode opensAs(SeparatorBlockEntity separator, BlockState neighbour, BlockGetter level, BlockPos at) {
        if (neighbour.getBlock() instanceof BrassPipeBlock || neighbour.getBlock() instanceof AlembicBlock) {
            return PortMode.INLET;
        }
        EssenceTankBlockEntity jar = EssenceTankBlockEntity.at(level, at);
        if (jar != null) {
            return jar.tank().isEmpty() ? separator.freeOutlet() : PortMode.INLET;
        }
        return neighbour.getBlock() instanceof EssenceTankBlock ? separator.freeOutlet() : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (!level.isClientSide() && LOOKS.containsKey(direction) && level.getBlockEntity(pos) instanceof SeparatorBlockEntity separator) {
            // Decided a tick later, once a placed jar has its contents.
            separator.notice(direction);
            level.scheduleTick(pos, this, 1);
        }
        return withLook(state, direction, neighbour, level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
        super.onPlace(state, level, pos, old, movedByPiston);
        if (!old.is(this) && !level.isClientSide()) {
            // Whatever already stands around a new separator opens its sides as if it were set beside it now.
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof SeparatorBlockEntity separator)) {
            return;
        }
        for (Direction face : separator.takeNoticed()) {
            Side side = Side.of(face, state.getValue(FACING));
            BlockPos at = pos.relative(face);
            separator.meet(side, opensAs(separator, level.getBlockState(at), level, at));
        }
        BlockState looks = withLooks(state, level, pos);
        if (looks != state) {
            level.setBlock(pos, looks, Block.UPDATE_ALL);
        }
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return withLooks(defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()),
                context.getLevel(), context.getClickedPos());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SeparatorBlockEntity separator) {
            Side side = Side.of(hitResult.getDirection(), state.getValue(FACING));
            if (player.isShiftKeyDown() && side != null) {
                // Sneak-using a side changes what its pipe does, right on the block.
                separator.cycle(side);
                player.displayClientMessage(Component.translatable("gui.hextale.separator.side." + side.name().toLowerCase(Locale.ROOT))
                        .append(": ").append(Component.translatable("gui.hextale.port." + separator.mode(side).getSerializedName())), true);
                level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.6F, 1.2F);
            } else {
                player.openMenu(separator, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        for (Direction face : Direction.Plane.HORIZONTAL) {
            rotated = rotated.setValue(LOOKS.get(rotation.rotate(face)), state.getValue(LOOKS.get(face)));
        }
        return rotated;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    /** The fire of the powder under the flask's tip, and now and then the gas of separation out of the vent. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        if (random.nextInt(2) == 0) {
            level.addParticle(ModParticles.flame(0xFFB040), x + (random.nextDouble() - 0.5) * 0.2, pos.getY() + 3.1 / 16,
                    z + (random.nextDouble() - 0.5) * 0.2, 0, 0.004, 0);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ModParticles.vapour(ModParticles.SMOKE), x, pos.getY() + 1.02, z, (random.nextDouble() - 0.5) * 0.01, 0.03,
                    (random.nextDouble() - 0.5) * 0.01);
        }
    }

    /** The parts of the model that look the same whichever way it faces: base, dish, flask, ring, neck and vent. */
    private static final VoxelShape BODY = Shapes.or(
            Block.box(3, 0, 2, 13, 1, 14), Block.box(2, 0, 3, 14, 1, 13),
            Block.box(5, 1, 5, 11, 2, 11),
            Block.box(7, 2, 7, 9, 4, 9),
            Block.box(5, 4, 5, 11, 5, 11),
            Block.box(4, 5, 4, 12, 6, 12),
            Block.box(4, 6, 3, 12, 11, 13), Block.box(3, 6, 4, 13, 11, 12),
            Block.box(5, 11, 5, 11, 12, 11),
            Block.box(6, 12, 6, 10, 14, 10),
            Block.box(7, 14, 7, 9, 15, 9),
            Block.box(6, 15, 6, 10, 16, 10));
    private static final Map<BlockState, VoxelShape> SHAPES = new java.util.concurrent.ConcurrentHashMap<>();

    /** The block's outline and collision: the flask on its stand and the joints of the open sides, not a full cube. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.computeIfAbsent(state, SeparatorBlock::shapeOf);
    }

    private static VoxelShape shapeOf(BlockState state) {
        // The stand's rod sits at the back left of a separator facing north.
        VoxelShape shape = Shapes.or(BODY, turned(3, 1, 12, 4, 11, 13, state.getValue(FACING)));
        for (Map.Entry<Direction, EnumProperty<PortMode>> look : LOOKS.entrySet()) {
            Direction face = look.getKey();
            // Joint boxes as on the north face, like the joint models.
            shape = switch (state.getValue(look.getValue())) {
                case NONE -> shape;
                case INLET -> Shapes.or(shape, turned(7, 7, 0, 9, 9, 3, face));
                case PRODUCT -> Shapes.or(shape, turned(7, 7, 1, 9, 9, 3, face), turned(6, 6, 0, 10, 10, 1, face));
                case WASTE -> Shapes.or(shape, turned(7, 7, 1, 9, 9, 3, face), turned(6, 6, 0, 10, 10, 1, face),
                        turned(7, 10, 0, 9, 11, 1, face));
            };
        }
        return shape;
    }

    /** A box given as it lies for the north face (pixels), turned about the block's middle to {@code face}. */
    private static VoxelShape turned(double x0, double y0, double z0, double x1, double y1, double z1, Direction face) {
        return switch (face) {
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SeparatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.SEPARATOR.get(), SeparatorBlockEntity::serverTick);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SeparatorBlockEntity separator ? separator : null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<SeparatorBlock> codec() {
        return simpleCodec(SeparatorBlock::new);
    }
}
