package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import org.jspecify.annotations.Nullable;

/**
 * The Chem Burner: a squat riveted iron stove with a green glass window, a copper fuel tank on its back and a grate on
 * top for the machine it heats. A vial of combustible mix feeds it by hand, a bottle of essence pours into its tank;
 * pipes join it at the back and the sides. An empty hand reads how much fuel it has.
 */
public class ChemBurnerBlock extends BaseEntityBlock implements EssenceMachine {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    static final int FLAME = 0x8CFF5A;
    static final int SMOKE = 0x3A4A34;
    static final int PINK_FLAME = 0xFF6FD8;
    static final int PINK_SMOKE = 0x4A2F4A;

    public ChemBurnerBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false);
        for (Direction face : Direction.Plane.HORIZONTAL) {
            state = state.setValue(PORTS.get(face), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
        for (Direction face : Direction.Plane.HORIZONTAL) {
            builder.add(PORTS.get(face));
        }
    }

    /** Whether this block is a burning Chem Burner. */
    public static boolean isLit(BlockState state) {
        return state.is(ModBlocks.CHEM_BURNER.get()) && state.getValue(LIT);
    }

    /** The window at the front shows the flame; essence comes in at the back and the sides. */
    @Override
    public boolean isPort(BlockState state, Direction face) {
        return face.getAxis().isHorizontal() && face != state.getValue(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return EssenceMachine.connectAll(context.getLevel(), context.getClickedPos(), state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return EssenceMachine.connect(state, direction, neighbour);
    }

    /** A vial of combustible mix goes in; a bottle of essence pours into the tank. */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ChemBurnerBlockEntity burner)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        boolean creative = player.getAbilities().instabuild;
        if (ChemBurnerBlockEntity.burnTicks(stack) > 0) {
            if (!level.isClientSide()) {
                int taken = burner.feed(stack);
                if (taken <= 0) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("block.hextale.chem_burner.say.full"), true);
                    return net.minecraft.world.ItemInteractionResult.FAIL;
                }
                if (!creative) {
                    stack.shrink(taken);
                }
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.BLOCKS, 0.4F, 1.3F);
            }
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        if (stack.is(ModItems.ESSENCE_FLASK.get())) {
            EssenceMix held = EssenceFlaskItem.contents(stack);
            if (held.isEmpty()) {
                return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide()) {
                EssenceMix taken = burner.pour(held);
                if (taken.isEmpty()) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("block.hextale.chem_burner.say.full"), true);
                    return net.minecraft.world.ItemInteractionResult.FAIL;
                }
                if (!creative) {
                    ItemStack left = EssenceFlaskItem.filled(held.minus(taken));
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, left);
                    } else if (!player.getInventory().add(left)) {
                        player.drop(left, false);
                    }
                }
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6F, 0.9F);
            }
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        return com.pockyl.hextale.port.Interactions.itemOn(stack.is(Items.FLINT_AND_STEEL) ? net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION : net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ChemBurnerBlockEntity burner) {
            player.displayClientMessage(burner.status(), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Behind the front window, where the flame burns. */
    static Vec3 window(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return Vec3.atBottomCenterOf(pos).add(facing.getStepX() * 0.52, 0.32, facing.getStepZ() * 0.52);
    }

    /** The green flame licks behind the window, smoke rises from the flue, and it roars low now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        Vec3 window = Vec3.atBottomCenterOf(pos).add(facing.getStepX() * 0.52, 0.32, facing.getStepZ() * 0.52);
        Direction side = facing.getClockWise();
        double along = (random.nextDouble() - 0.5) * 0.4;
        boolean pink = level.getBlockEntity(pos) instanceof ChemBurnerBlockEntity burner && burner.pink();
        level.addParticle(ModParticles.flame(pink ? PINK_FLAME : FLAME),
                window.x + side.getStepX() * along, window.y + random.nextDouble() * 0.15, window.z + side.getStepZ() * along,
                0, 0.02, 0);
        if (random.nextInt(3) == 0) {
            // The flue rises at the back corner, beside the machine on top.
            Direction back = facing.getOpposite();
            Vec3 flue = Vec3.atBottomCenterOf(pos).add(back.getStepX() * 0.34 + side.getStepX() * 0.3, 1.05,
                    back.getStepZ() * 0.34 + side.getStepZ() * 0.3);
            level.addParticle(ModParticles.vapour(pink ? PINK_SMOKE : SMOKE), flue.x, flue.y, flue.z, 0, 0.03, 0);
        }
        if (pink && random.nextInt(4) == 0) {
            level.addParticle(ModParticles.SHIMMER_MOTE.get(), window.x, window.y + 0.1, window.z, 0, 0.02, 0);
        }
        if (random.nextInt(120) == 0) {
            level.playLocalSound(pos, ModSounds.CHEM_BURNER_ROAR.get(), SoundSource.BLOCKS, 0.35F + random.nextFloat() * 0.1F,
                    0.85F + random.nextFloat() * 0.2F, false);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapes.of(state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return EssenceMachine.rotatePorts(state.setValue(FACING, rotation.rotate(state.getValue(FACING))), rotation);
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
        return new ChemBurnerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.CHEM_BURNER.get(), ChemBurnerBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<ChemBurnerBlock> codec() {
        return simpleCodec(ChemBurnerBlock::new);
    }
}
