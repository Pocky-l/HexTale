package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A hex wisp: a mote of cold hextech light a pickaxe with the Eternal Flame rune leaves in the dark where it dug. It has
 * no body - only a slow swirl of blue motes - lights the tunnel for good and goes out at a touch. Nothing walks into it;
 * blocks and fluids take its place.
 */
public class HexWispBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    private static final int BLUE = 0x5CC8F2;
    private static final int WHITE = 0xCFF2FF;

    public HexWispBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double angle = (level.getGameTime() % 80) / 80.0 * Math.PI * 2;
        for (int i = 0; i < 2; i++) {
            double a = angle + i * Math.PI;
            double x = pos.getX() + 0.5 + Math.cos(a) * 0.15;
            double z = pos.getZ() + 0.5 + Math.sin(a) * 0.15;
            level.addParticle(new DustParticleOptions(i == 0 ? BLUE : WHITE, 0.6F), x, pos.getY() + 0.45 + random.nextDouble() * 0.1, z, 0, 0.01, 0);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.01, 0.01, (random.nextDouble() - 0.5) * 0.01);
        }
    }
}
