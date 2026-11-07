package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.essence.JarReach;

/**
 * A Storage Cube: more room for a Hex Vault. A cube counts on the gate's platform or against it, and then it lights up. Coming or
 * going, a cube tells the vaults around, nothing looks for it.
 */
public class StorageCubeBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    /** The box round a cube, over the reach of {@link JarReach#within}, that holds every vault it can belong to. */
    private static final int LOOK = 5;

    public StorageCubeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapes.of(state);
    }

    /** Lights the cube at {@code pos} or puts it out. */
    public static void light(Level level, BlockPos pos, boolean on) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof StorageCubeBlock && state.getValue(ACTIVE) != on) {
            level.setBlock(pos, state.setValue(ACTIVE, on), Block.UPDATE_CLIENTS);
        }
    }

    private static void tellVaults(ServerLevel level, BlockPos pos) {
        for (HexVaultBlockEntity vault : JarReach.within(level, pos.offset(-LOOK, -LOOK, -LOOK), pos.offset(LOOK, LOOK, LOOK), HexVaultBlockEntity.class)) {
            vault.recount();
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            tellVaults(server, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        tellVaults(level, pos);
    }
}
