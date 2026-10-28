package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Glass woven with a rune of barrier that only some may pass. Ward Glass stops players and lets creatures (and dropped
 * items) through; Veil Glass lets players through and stops creatures. Anything without a body to judge (a projectile,
 * the world's own checks) meets solid glass.
 */
public class SieveGlassBlock extends TransparentBlock {
    /** Whether it stops players (Ward Glass) rather than creatures (Veil Glass). */
    private final boolean stopsPlayers;

    public SieveGlassBlock(boolean stopsPlayers, Properties properties) {
        super(properties);
        this.stopsPlayers = stopsPlayers;
    }

    /** Whether it is Ward Glass (stops players) rather than Veil Glass (stops creatures). */
    public boolean stopsPlayers() {
        return stopsPlayers;
    }

    /** Whether this glass lets {@code entity} through. */
    public boolean passes(Entity entity) {
        return entity instanceof Player != stopsPlayers;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() != null && passes(entityContext.getEntity())) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    /** Creatures plan their way through Ward Glass and around Veil Glass. */
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return stopsPlayers && type == PathComputationType.LAND;
    }
}
