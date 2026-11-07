package com.pockyl.hextale.block;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.CarboyContents;
import com.pockyl.hextale.chem.ChemVialItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
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
 * The Chem Carboy: a two-block glass flask in an iron cradle (the cell above holds a stand-in of {@link ShapeParts}) with
 * one brew in it, up to {@value CarboyContents#CAPACITY} doses. Vials poured on it fill it and give their bottles back,
 * an empty bottle takes a vial at the tap, and devices around it draw from it ({@code CarboyReach}). Broken, it keeps its
 * brew.
 */
public class ChemCarboyBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public ChemCarboyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The neck needs the cell above. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
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

    /** Vials pour in; an empty bottle fills at the tap. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ChemCarboyBlockEntity carboy)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        Brew brew = ChemVialItem.brew(stack);
        if (brew != null) {
            if (!level.isClientSide()) {
                pour(stack, brew, carboy, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.GLASS_BOTTLE)) {
            if (!level.isClientSide()) {
                ItemStack vial = carboy.bottle();
                if (vial.isEmpty()) {
                    player.sendOverlayMessage(Component.translatable("block.hextale.chem_carboy.empty"));
                } else {
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, vial));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** The whole stack in hand goes in as far as it fits, and the bottles come back. */
    protected void pour(ItemStack stack, Brew brew, ChemCarboyBlockEntity carboy, Player player) {
        int vials = carboy.fill(brew, stack.getCount());
        if (vials <= 0) {
            player.sendOverlayMessage(carboy.contents().isEmpty() || carboy.contents().brew() == brew
                    ? Component.translatable("block.hextale.chem_carboy.full")
                    : Component.translatable("block.hextale.chem_carboy.other_brew", Component.translatable(carboy.contents().brew().translationKey())));
            return;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(vials);
            player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE, vials));
        }
        BlockPos pos = carboy.getBlockPos();
        carboy.getLevel().playSound(null, pos.above(), SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.4F, 0.9F);
    }

    /** An empty hand: what it holds. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ChemCarboyBlockEntity carboy) {
            player.sendOverlayMessage(status(carboy.contents()));
        }
        return InteractionResult.SUCCESS;
    }

    /** One line: the brew and how many doses of it are left. */
    public static Component status(CarboyContents contents) {
        return contents.isEmpty() ? Component.translatable("block.hextale.chem_carboy.status.empty")
                : Component.translatable("block.hextale.chem_carboy.status", Component.translatable(contents.brew().translationKey())
                .withColor(contents.brew().color()), contents.doses(), CarboyContents.CAPACITY);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Any brew at all gives at least 1; only a full carboy gives 15. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof ChemCarboyBlockEntity carboy) || carboy.contents().isEmpty()) {
            return 0;
        }
        return 1 + carboy.contents().doses() * 14 / CarboyContents.CAPACITY;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChemCarboyBlockEntity(pos, state);
    }

    @Override
    protected com.mojang.serialization.MapCodec<ChemCarboyBlock> codec() {
        return simpleCodec(ChemCarboyBlock::new);
    }
}
