package com.pockyl.hextale.garden;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.pockyl.hextale.registry.ModParticles;

import org.jspecify.annotations.Nullable;

/**
 * The Chem Planter: a riveted iron box with green glass sides and soil on top. A plant set on it grows as on wet
 * farmland, and the brew in its tank (growth tonic or raw shimmer poured from vials, sap from a bottle or through pipes)
 * is spent a dose at a time to make it grow a stage more. The tank shows through the glass. It takes runes.
 */
public class ChemPlanterBlock extends BaseEntityBlock implements com.pockyl.hextale.block.EssenceMachine {
    public ChemPlanterBlock(Properties properties) {
        super(properties);
    }

    /** Sap comes in by pipe through the sides and the bottom. */
    @Override
    public boolean isPort(BlockState state, Direction face) {
        return face != Direction.UP;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ChemPlanterBlockEntity planter)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return com.pockyl.hextale.port.Interactions.itemOn(GardenPouring.pourInto(planter, stack, level, player, hand));
    }

    /** Says what is in the tank. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ChemPlanterBlockEntity planter)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.displayClientMessage(planter.status(), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Crops, stems and the garden's own plants take root in its soil. */
    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant) {
        return facing == Direction.UP && SteepedSoilBlock.grows(plant) ? TriState.TRUE : TriState.DEFAULT;
    }

    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** A dose in the tank glints on the soil now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0 && level.getBlockEntity(pos) instanceof ChemPlanterBlockEntity planter && planter.charges() > 0) {
            level.addParticle(ModParticles.glint(planter.dose().color()), pos.getX() + 0.15 + random.nextDouble() * 0.7, pos.getY() + 1.02,
                    pos.getZ() + 0.15 + random.nextDouble() * 0.7, 0, 0.01, 0);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChemPlanterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.@Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, com.pockyl.hextale.registry.ModBlockEntities.CHEM_PLANTER.get(), ChemPlanterBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<ChemPlanterBlock> codec() {
        return simpleCodec(ChemPlanterBlock::new);
    }
}
