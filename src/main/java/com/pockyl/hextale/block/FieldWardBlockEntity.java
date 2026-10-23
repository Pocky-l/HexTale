package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.farm.ReachIndex;
import com.pockyl.hextale.registry.ModBlockEntities;

/**
 * A Field Ward only has to be known: it joins the {@link #INDEX} while it is loaded, and every natural monster spawn
 * and every trampling hoof asks the index whether a ward watches that spot ({@link com.pockyl.hextale.farm.WardEvents}).
 * It never ticks.
 */
public class FieldWardBlockEntity extends BlockEntity {
    /** Blocks to each side, up and down, that the eye watches. */
    public static final int RADIUS = 16;
    public static final ReachIndex<FieldWardBlockEntity> INDEX = new ReachIndex<>(FieldWardBlockEntity.class, RADIUS,
            FieldWardBlockEntity::watches);

    public FieldWardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIELD_WARD.get(), pos, state);
    }

    /** A cylinder of {@value #RADIUS} blocks round the post, as high and as deep. */
    public boolean watches(Vec3 at) {
        double dx = at.x - (worldPosition.getX() + 0.5);
        double dz = at.z - (worldPosition.getZ() + 0.5);
        return dx * dx + dz * dz <= RADIUS * RADIUS && Math.abs(at.y - worldPosition.getY()) <= RADIUS;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            INDEX.add(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            INDEX.remove(level, worldPosition);
        }
        super.setRemoved();
    }
}
