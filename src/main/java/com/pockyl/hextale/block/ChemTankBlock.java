package com.pockyl.hextale.block;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemFuel;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.chem.TankFuel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * A Chem Tank set down: fuel vials poured on it fill it (hoppers too), and a researcher wearing a tank who uses it with
 * an empty hand gets their own tank topped up from it through the hose. Broken, it keeps its fuel.
 */
public class ChemTankBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public ChemTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapes.of(state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    /** A fuel vial pours in when the tank takes that fuel. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        Brew brew = ChemVialItem.brew(stack);
        if (brew == null || ChemFuel.of(brew) == null || !(level.getBlockEntity(pos) instanceof ChemTankBlockEntity tank)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            int vials = Math.min(stack.getCount(), ChemTank.room(tank.contents(), brew));
            if (vials <= 0) {
                player.sendOverlayMessage(Component.translatable(tank.contents().isEmpty() ? "block.hextale.chem_tank.full"
                        : "block.hextale.chem_tank.other_fuel", Component.translatable(tank.contents().fuel().brew().translationKey())));
                return InteractionResult.FAIL;
            }
            tank.setContents(ChemTank.poured(tank.contents(), brew, vials));
            if (!player.getAbilities().instabuild) {
                stack.shrink(vials);
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6F, 0.9F);
        }
        return InteractionResult.SUCCESS;
    }

    /** An empty hand: pumps into the tank the player wears, or says what the tank holds. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof ChemTankBlockEntity tank)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack worn = ChemTank.worn(player);
        if (worn.isEmpty()) {
            player.sendOverlayMessage(status(tank.contents()));
            return InteractionResult.SUCCESS;
        }
        TankFuel[] after = ChemTank.pump(tank.contents(), ChemTank.contents(worn));
        if (after[1].equals(ChemTank.contents(worn))) {
            player.sendOverlayMessage(Component.translatable(tank.contents().isEmpty() ? "block.hextale.chem_tank.empty"
                    : "block.hextale.chem_tank.cannot_pump"));
            return InteractionResult.SUCCESS;
        }
        tank.setContents(after[0]);
        ChemTank.setContents(worn, after[1]);
        tank.pumped(player);
        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.5F, 0.8F);
        player.sendOverlayMessage(Component.translatable("block.hextale.chem_tank.pumped",
                Component.translatable(after[1].fuel().brew().translationKey()), after[1].vials()));
        return InteractionResult.SUCCESS;
    }

    /** One line: the fuel and how many vials of it are left. */
    public static Component status(TankFuel contents) {
        return contents.isEmpty() ? Component.translatable("item.hextale.chem_tank.empty")
                : Component.translatable("item.hextale.chem_tank.contents", Component.translatable(contents.fuel().brew().translationKey()),
                contents.vials());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChemTankBlockEntity(pos, state);
    }
}
