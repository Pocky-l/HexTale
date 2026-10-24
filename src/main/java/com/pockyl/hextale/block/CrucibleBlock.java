package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;

import org.jspecify.annotations.Nullable;

/**
 * The Crucible: a cauldron woken with hextech dust. A water bucket (or water bottles) fills it, a fire, campfire, magma
 * or lava below heats it. Once it boils, bottles and jars of essence pour into it and items sink and dissolve into their
 * essences (put in by hand one at a time, or dropped in by the stack); a recipe's catalyst takes its essences and rises
 * as the result instead. An empty bucket takes the water back out, losing what is in it;
 * sneak-use with an empty hand boils it all away in a cloud of steam. A plain use says how the water stands.
 */
public class CrucibleBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape INSIDE = Block.box(3, 4, 3, 13, 16, 13);
    private static final VoxelShape SHAPE = Shapes.join(Shapes.or(Block.box(1, 2, 1, 15, 15, 15), Block.box(2, 0, 2, 14, 2, 14)),
            INSIDE, BooleanOp.ONLY_FIRST);

    public CrucibleBlock(Properties properties) {
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        boolean creative = player.getAbilities().instabuild;
        if (stack.is(Items.WATER_BUCKET)) {
            if (crucible.fillWater(CrucibleBlockEntity.BUCKET, true) == 0) {
                return say(level, player, "full");
            }
            if (!level.isClientSide()) {
                crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
                if (!creative) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.BUCKET)) {
            if (crucible.water() + crucible.essence().total() < CrucibleBlockEntity.BUCKET) {
                return say(level, player, "not_a_bucket");
            }
            if (!level.isClientSide() && crucible.takeBucket() && !creative) {
                give(player, hand, stack, new ItemStack(Items.WATER_BUCKET));
            }
            return InteractionResult.SUCCESS;
        }
        if (isWaterBottle(stack)) {
            if (crucible.fillWater(CrucibleBlockEntity.WATER_BOTTLE, true) == 0) {
                return say(level, player, "full");
            }
            if (!level.isClientSide()) {
                crucible.fillWater(CrucibleBlockEntity.WATER_BOTTLE, false);
                if (!creative) {
                    give(player, hand, stack, new ItemStack(Items.GLASS_BOTTLE));
                }
            }
            return InteractionResult.SUCCESS;
        }
        boolean bottle = stack.is(ModItems.ESSENCE_FLASK.get());
        boolean jar = stack.is(ModItems.ESSENCE_TANK.get()) && !EssenceFlaskItem.contents(stack).isEmpty();
        if (bottle || jar) {
            if (!crucible.boiling()) {
                return say(level, player, crucible.liquid() <= 0 ? "no_water" : "not_boiling");
            }
            if (!level.isClientSide()) {
                EssenceMix held = EssenceFlaskItem.contents(stack);
                EssenceMix taken = crucible.pour(held);
                if (!creative) {
                    EssenceMix left = held.minus(taken);
                    if (bottle) {
                        give(player, hand, stack, EssenceFlaskItem.filled(left));
                    } else if (left.isEmpty()) {
                        stack.remove(ModDataComponents.ESSENCE.get());
                    } else {
                        stack.set(ModDataComponents.ESSENCE.get(), left);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.GLASS_BOTTLE) && crucible.brew() != null) {
            if (!level.isClientSide()) {
                ItemStack vial = crucible.bottle();
                if (!vial.isEmpty()) {
                    give(player, hand, stack, vial);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (crucible.liquid() <= 0) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            if (crucible.take(stack, 1, true, player) == 0) {
                // Neither a catalyst the water can pay for, nor anything boiling water could dissolve.
                return say(level, player, crucible.boiling() ? "no_essences" : "not_boiling");
            }
            if (!creative) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Sneaking boils the water away; otherwise the crucible says how its water stands. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (player.isSecondaryUseActive()) {
                crucible.boilAway();
            } else {
                player.displayClientMessage(status(crucible), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** "Boiling - water 750 mB, essence 32 mB", or what it lacks. */
    public static Component status(CrucibleBlockEntity crucible) {
        if (crucible.liquid() <= 0) {
            return Component.translatable("block.hextale.crucible.say.no_water");
        }
        Component phase = crucible.phase().describe();
        if (crucible.heat() <= 0 && !crucible.boiling()) {
            return Component.translatable("block.hextale.crucible.status.cold", phase, crucible.water());
        }
        if (crucible.brew() != null) {
            return Component.translatable("block.hextale.crucible.status.brew", phase,
                    Component.translatable(crucible.brew().translationKey()).withColor(crucible.brew().color()), crucible.doses());
        }
        return Component.translatable("block.hextale.crucible.status", phase, crucible.water(), crucible.essence().total());
    }

    private static InteractionResult say(Level level, Player player, String key) {
        if (!level.isClientSide()) {
            player.displayClientMessage(Component.translatable("block.hextale.crucible.say." + key), true);
        }
        return InteractionResult.FAIL;
    }

    private static void give(Player player, InteractionHand hand, ItemStack held, ItemStack result) {
        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(hand, result);
        } else if (!player.getInventory().add(result)) {
            player.drop(result, false, Prediction.SERVER_ONLY);
        }
    }

    public static boolean isWaterBottle(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return stack.is(Items.POTION) && contents != null && contents.is(Potions.WATER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INSIDE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
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
        return new CrucibleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.CRUCIBLE.get(), CrucibleBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.CRUCIBLE.get(), CrucibleBlockEntity::serverTick);
    }
}
