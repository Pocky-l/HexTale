package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;

/**
 * A hex wisp: a mote of cold hextech light a pickaxe with the Eternal Flame rune leaves in the dark where it dug. Its
 * look - a flickering core in a soft halo with sparks circling it, clinging under the ceiling - is drawn by
 * {@code HexWispRenderer}; the block entity carries nothing. It lights the tunnel for good. A tool passes through it, so
 * digging on never puts it out; an empty hand does at a touch. Nothing walks into it; blocks and fluids take its place.
 */
public class HexWispBlock extends Block implements EntityBlock {
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
        // Only an empty hand can aim at it: a pickaxe digging on through the tunnel looks past it.
        return context.isHoldingItem(Items.AIR) ? SHAPE : Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Glow(pos, state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Wisps left before they had a block entity get one, and clients see it once the block is set again.
        if (level.getChunkAt(pos).getBlockEntity(pos, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) == null) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Now and then a spark drifts up off the flame; the flame itself is the renderer's.
        if (random.nextInt(3) == 0) {
            level.addParticle(ModParticles.glint(random.nextBoolean() ? BLUE : WHITE), pos.getX() + 0.35 + random.nextDouble() * 0.3,
                    pos.getY() + 0.45 + random.nextDouble() * 0.2, pos.getZ() + 0.35 + random.nextDouble() * 0.3,
                    (random.nextDouble() - 0.5) * 0.006, 0.012, (random.nextDouble() - 0.5) * 0.006);
        }
    }

    /** The wisp's block entity: only there so the renderer draws it. */
    public static class Glow extends BlockEntity {
        /** Client: when the renderer first saw it, for the flame growing in. */
        public float clientBorn = -1;

        public Glow(BlockPos pos, BlockState state) {
            super(ModBlockEntities.HEX_WISP.get(), pos, state);
        }
    }
}
