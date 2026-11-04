package com.pockyl.hextale.block;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
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

import org.jspecify.annotations.Nullable;

/**
 * The Chem Vat Valve: a riveted iron hatch with a wheel valve. Alone it is only a valve; built into the front of a Chem
 * Vat and sealed with acid, it becomes the vat's controller ({@link ChemVatBlockEntity}): vials and the metal to temper go
 * in through it (or through the grate on top), the tempered metal comes out of it. Its renderer draws the rest of the vat
 * and the bath inside.
 */
public class ChemVatBlock extends BaseEntityBlock implements WorldlyContainerHolder {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Part of a sealed vat (the valve alone is a plain block). */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** The fire under the bath burns right now. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public ChemVatBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, LIT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Vials are poured in, fuel goes under the bath, metal to temper drops in; anything else does what it would anyway. */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!state.getValue(FORMED) || !(level.getBlockEntity(pos) instanceof ChemVatBlockEntity vat) || !vat.accepts(stack)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            int taken = vat.insert(stack);
            if (taken <= 0) {
                player.displayClientMessage(Component.translatable("block.hextale.chem_vat.full"), true);
                return net.minecraft.world.ItemInteractionResult.FAIL;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(taken);
            }
            level.playSound(null, pos, com.pockyl.hextale.chem.ChemVialItem.brew(stack) != null ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY,
                    SoundSource.BLOCKS, 0.6F, 0.9F);
        }
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    /** An empty hand takes the tempered metal out; a valve that is not part of a vat says so. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!state.getValue(FORMED)) {
            player.displayClientMessage(Component.translatable("block.hextale.chem_vat.alone"), true);
        } else if (level.getBlockEntity(pos) instanceof ChemVatBlockEntity vat) {
            ItemStack out = vat.takeOutput();
            if (out.isEmpty()) {
                player.displayClientMessage(status(vat), true);
            } else {
                if (!player.getInventory().add(out)) {
                    player.drop(out, false);
                }
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.6F, 1.1F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    /** While the bath works: fumes over the grate, green and violet bubbles bursting, a hiss now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FORMED) || !state.getValue(LIT)) {
            return;
        }
        Vec3 grate = Vec3.atCenterOf(ChemVatBlockEntity.heart(pos, state.getValue(FACING)).above());
        int colour = random.nextBoolean() ? Brew.ACID.color() : Brew.REFINED_SHIMMER.color();
        level.addParticle(ModParticles.vapour(colour), grate.x + random.nextGaussian() * 0.2, grate.y + 0.55,
                grate.z + random.nextGaussian() * 0.2, 0, 0.03, 0);
        if (random.nextInt(30) == 0) {
            level.playLocalSound(grate.x, grate.y, grate.z, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.15F, 1.6F, false);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChemVatBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.CHEM_VAT.get(), ChemVatBlockEntity::serverTick);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return state.getValue(FORMED) && level.getBlockEntity(pos) instanceof ChemVatBlockEntity vat ? vat : null;
    }

    /** One status line for Jade, the goggles and an empty-hand click. */
    public static Component status(ChemVatBlockEntity vat) {
        return vat.status();
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<ChemVatBlock> codec() {
        return simpleCodec(ChemVatBlock::new);
    }
}
