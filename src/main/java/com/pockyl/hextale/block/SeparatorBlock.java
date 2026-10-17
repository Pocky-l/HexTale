package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.ScheduledTickAccess;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The Separator: draws the essence chosen on its screen out of a mixture, burning blaze powder. Its front (facing the player who placed it) lets the
 * rest of the mixture out, its top the drawn-out concentrate; pipes bring the mixture in at the back and the sides.
 */
public class SeparatorBlock extends BaseEntityBlock implements WorldlyContainerHolder, EssenceMachine {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** What each side shows: the joint of the side's mode (see {@link PortMode}), pipe or not, so a change shows at
     * once. The top is the gas vent and the bottom stands on the floor: no pipes there. */
    public static final Map<Direction, EnumProperty<PortMode>> LOOKS = looks();

    public SeparatorBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
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
        builder.add(FACING);
        LOOKS.values().forEach(builder::add);
    }

    /** The sides of a separator as seen by someone standing in front of it. */
    public enum Side {
        FRONT, BACK, LEFT, RIGHT;

        /** The modes of a new separator: the mixture in at the back and the left, the concentrate out at the right, the
         * rest out at the front. */
        public PortMode defaultMode() {
            return switch (this) {
                case FRONT -> PortMode.WASTE;
                case RIGHT -> PortMode.PRODUCT;
                default -> PortMode.INLET;
            };
        }

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
        PortMode mode = level.getBlockEntity(pos) instanceof SeparatorBlockEntity separator ? separator.mode(side) : side.defaultMode();
        return state.setValue(look, mode);
    }

    /** The state with every face's look set. */
    public static BlockState withLooks(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction face : LOOKS.keySet()) {
            state = withLook(state, face, level.getBlockState(pos.relative(face)), level, pos);
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return withLook(state, direction, neighbour, level, pos);
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
                player.sendOverlayMessage(Component.translatable("gui.hextale.separator.side." + side.name().toLowerCase(Locale.ROOT))
                        .append(": ").append(Component.translatable("gui.hextale.port." + separator.mode(side).getSerializedName())));
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
}
