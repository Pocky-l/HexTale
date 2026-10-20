package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import org.jspecify.annotations.Nullable;

/**
 * The Pink Brazier: a riveted iron bowl on three legs with shimmer burning in it. A vial of shimmer feeds it by hand; an
 * empty hand reads how long it burns and how far its light reaches. Its flame is cold: it never burns anyone.
 */
public class PinkBrazierBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 11.0);
    private static final int FLAME = 0xFF7AD9;
    private static final int FADE = 0xB04CF0;

    public PinkBrazierBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (PinkBrazierBlockEntity.burnTicks(stack) <= 0 || !(level.getBlockEntity(pos) instanceof PinkBrazierBlockEntity brazier)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            int taken = brazier.feed(stack);
            if (taken <= 0) {
                player.sendOverlayMessage(Component.translatable("block.hextale.pink_brazier.say.full"));
                return InteractionResult.FAIL;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(taken);
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6F, 1.1F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PinkBrazierBlockEntity brazier) {
            player.sendOverlayMessage(brazier.status());
        }
        return InteractionResult.SUCCESS;
    }

    /** The pink flame licks over the bowl's rim, sparks and motes rise, and it breathes now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.75;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 2; i++) {
            level.addParticle(new DustColorTransitionOptions(FLAME, FADE, 0.9F), x + (random.nextDouble() - 0.5) * 0.45,
                    y + random.nextDouble() * 0.2, z + (random.nextDouble() - 0.5) * 0.45, 0, 0.04, 0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ModParticles.SHIMMER_MOTE.get(), x + (random.nextDouble() - 0.5) * 0.4, y + 0.4,
                    z + (random.nextDouble() - 0.5) * 0.4, 0, 0.03, 0);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x + (random.nextDouble() - 0.5) * 0.3, y + 0.1,
                    z + (random.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
        }
        if (random.nextInt(80) == 0) {
            level.playLocalSound(pos, ModSounds.PINK_BRAZIER_BREATH.get(), SoundSource.BLOCKS, 0.4F, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PinkBrazierBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.PINK_BRAZIER.get(), PinkBrazierBlockEntity::serverTick);
    }
}
