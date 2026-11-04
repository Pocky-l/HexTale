package com.pockyl.hextale.garden;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;

/**
 * Crystal Cane: glassy cane that grows clear crystal pods on its joints now and then (faster on a dosed bed). A use
 * picks the pods - a crystal essence no stone gives so cheaply - and the cane grows new ones.
 */
public class CrystalCaneBlock extends VegetationBlock implements BonemealableBlock {
    public static final BooleanProperty PODDED = BooleanProperty.create("podded");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);
    /** 1 in this many random ticks grows the pods. */
    private static final int POD_CHANCE = 6;
    private static final int POD_COLOR = 0xBFEFFF;

    public CrystalCaneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PODDED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PODDED);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(com.pockyl.hextale.registry.ModTags.SUPPORTS_CRYSTAL_CANE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(PODDED);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PODDED) && random.nextInt(POD_CHANCE) == 0
                && net.neoforged.neoforge.common.CommonHooks.canCropGrow(level, pos, state, true)) {
            level.setBlock(pos, state.setValue(PODDED, true), Block.UPDATE_CLIENTS);
            net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    /** A use picks the pods: one or two of them, a soft clink of glass. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(PODDED)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            Block.popResource(server, pos, new ItemStack(ModItems.CRYSTAL_POD.get(), 1 + server.getRandom().nextInt(2)));
            server.setBlock(pos, state.setValue(PODDED, false), Block.UPDATE_CLIENTS);
            server.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.5F, 1.4F);
            server.sendParticles(ModParticles.glint(POD_COLOR), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.25, 0.3, 0.25, 0.02);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PODDED) && random.nextInt(8) == 0) {
            level.addParticle(ModParticles.glint(POD_COLOR), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.2 + random.nextDouble() * 0.7,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.01, 0);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return !state.getValue(PODDED);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(PODDED, true), Block.UPDATE_CLIENTS);
    }

    @Override
    protected com.mojang.serialization.MapCodec<CrystalCaneBlock> codec() {
        return simpleCodec(CrystalCaneBlock::new);
    }
}
