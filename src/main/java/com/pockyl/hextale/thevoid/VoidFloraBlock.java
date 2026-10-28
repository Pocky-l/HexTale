package com.pockyl.hextale.thevoid;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModTags;

/**
 * Growth of the Void on blighted land: glassy coral, black roots, a glowing bulb. It takes root only on the soil and
 * rock the Void has touched, and now and then breathes a dark mote or shows a magenta glint.
 */
public class VoidFloraBlock extends VegetationBlock {
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 13.0);
    private static final int MOTE = 0x2A1C34;
    private static final int GLINT = 0xD0469E;
    private final boolean glows;

    public VoidFloraBlock(boolean glows, BlockBehaviour.Properties properties) {
        super(properties);
        this.glows = glows;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModTags.VOID_GROUND);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(glows ? 4 : 10) != 0) {
            return;
        }
        double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
        double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
        if (glows) {
            level.addParticle(ModParticles.glint(GLINT), x, pos.getY() + 0.5 + random.nextDouble() * 0.3, z, 0, 0.006, 0);
        } else {
            level.addParticle(ModParticles.wisp(MOTE), x, pos.getY() + 0.3, z, 0, 0.012, 0);
        }
    }

    @Override
    protected com.mojang.serialization.MapCodec<VoidFloraBlock> codec() {
        throw new UnsupportedOperationException("VoidFloraBlock has no type codec");
    }
}
