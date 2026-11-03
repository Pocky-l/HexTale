package com.pockyl.hextale.garden;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModParticles;

import java.util.function.Supplier;

/**
 * A strain of the chemtech garden that grows like a crop in four stages (Cultivated Shimmerbloom, Ember Wheat): on
 * farmland, steeped soil or a Chem Planter. A ripe one may give off motes of its colour.
 */
public class GardenCropBlock extends CropBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape[] SHAPES = {Block.box(0, 0, 0, 16, 4, 16), Block.box(0, 0, 0, 16, 7, 16),
            Block.box(0, 0, 0, 16, 11, 16), Block.box(0, 0, 0, 16, 14, 16)};

    private final Supplier<? extends ItemLike> seed;
    private final int moteColor;

    public GardenCropBlock(Supplier<? extends ItemLike> seed, int moteColor, Properties properties) {
        super(properties);
        this.seed = seed;
        this.moteColor = moteColor;
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return 3;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }

    /** Four stages grow a little slower than wheat's eight, like beetroot. */
    @Override
    protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) {
            super.randomTick(state, level, pos, random);
        }
    }

    @Override
    protected int getBonemealAgeIncrease(Level level) {
        return super.getBonemealAgeIncrease(level) / 3;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) == 3 && random.nextInt(10) == 0) {
            level.addParticle(ModParticles.glint(moteColor), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.5 + random.nextDouble() * 0.4,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.015, 0);
        }
    }
}
