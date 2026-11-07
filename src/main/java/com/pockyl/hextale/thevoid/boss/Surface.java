package com.pockyl.hextale.thevoid.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The height of the ground anywhere the fight goes, found from a height near it rather than from the heightmap, so a fight
 * in a cave stays in the cave. Trees do not count: a mark on the ground should not climb into the leaves. Both sides use
 * it: the server for where a move lands, the client to lay its marks over the hills.
 */
public final class Surface {
    /** How far up and down the ground is looked for. */
    private static final int CLIMB = 24;
    private static final int DROP = 32;
    /** How far over the given height the search starts. */
    private static final int HEADROOM = 2;

    private Surface() {
    }

    /** Whether something can stand on this block, or swim at its top. */
    private static boolean ground(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty();
    }

    /** The top of the ground at (x, z) nearest under {@code near}; {@code near} itself over a drop with no bottom. */
    public static double height(BlockGetter level, double x, double z, double near) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(near) + HEADROOM, Mth.floor(z));
        int start = pos.getY();
        if (ground(level, pos)) {
            while (pos.getY() < start + CLIMB) {
                pos.move(0, 1, 0);
                if (!ground(level, pos)) {
                    return pos.getY();
                }
            }
            return near;
        }
        while (pos.getY() > start - DROP) {
            pos.move(0, -1, 0);
            if (ground(level, pos)) {
                return pos.getY() + 1;
            }
        }
        return near;
    }
}
