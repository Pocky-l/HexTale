package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A hextech lamp: a refined gemstone floating in a brass cage, standing on a stone foot or hanging from the block above
 * (like a lantern). It shines at full light and now and then sheds a cold mote.
 */
public class HextechLampBlock extends LanternBlock {
    /** The cage is the same in both forms; standing adds a foot and a stem below and a finial on top, hanging a finial
     *  below and the rod it hangs by. */
    private static final VoxelShape CAGE = Block.column(8.0, 3.0, 13.0);
    private static final VoxelShape SHAPE_STANDING = Shapes.or(CAGE, Block.column(6.0, 0.0, 2.0), Block.column(2.0, 2.0, 3.0),
            Block.column(6.0, 13.0, 14.0), Block.column(2.0, 14.0, 15.0));
    private static final VoxelShape SHAPE_HANGING = Shapes.or(CAGE, Block.column(2.0, 1.0, 2.0), Block.column(6.0, 2.0, 3.0),
            Block.column(6.0, 13.0, 14.0), Block.column(2.0, 14.0, 16.0));

    public HextechLampBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? SHAPE_HANGING : SHAPE_STANDING;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
        level.addParticle(new DustParticleOptions(0x5CC8F2, 0.5F), x, pos.getY() + 0.5, z, 0, 0.02, 0);
    }
}
