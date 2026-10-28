package com.pockyl.hextale.port;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The shape helpers that Minecraft 26.x added to {@link Block} (sizes in pixels, centred on the block). */
public final class PortShapes {
    private PortShapes() {
    }

    public static VoxelShape cube(double size) {
        return cube(size, size, size);
    }

    public static VoxelShape cube(double sizeX, double sizeY, double sizeZ) {
        double halfY = sizeY / 2.0;
        return column(sizeX, sizeZ, 8.0 - halfY, 8.0 + halfY);
    }

    public static VoxelShape column(double sizeXZ, double minY, double maxY) {
        return column(sizeXZ, sizeXZ, minY, maxY);
    }

    public static VoxelShape column(double sizeX, double sizeZ, double minY, double maxY) {
        double halfX = sizeX / 2.0;
        double halfZ = sizeZ / 2.0;
        return Block.box(8.0 - halfX, minY, 8.0 - halfZ, 8.0 + halfX, maxY, 8.0 + halfZ);
    }

    public static VoxelShape boxZ(double sizeXY, double minZ, double maxZ) {
        return boxZ(sizeXY, sizeXY, minZ, maxZ);
    }

    public static VoxelShape boxZ(double sizeX, double sizeY, double minZ, double maxZ) {
        double halfY = sizeY / 2.0;
        return boxZ(sizeX, 8.0 - halfY, 8.0 + halfY, minZ, maxZ);
    }

    public static VoxelShape boxZ(double sizeX, double minY, double maxY, double minZ, double maxZ) {
        double halfX = sizeX / 2.0;
        return Block.box(8.0 - halfX, minY, minZ, 8.0 + halfX, maxY, maxZ);
    }

    /**
     * Whether {@code entity} is in a block's inside shape (26.x calls entityInside only then; 1.21.1 for any entity in
     * the block's space).
     */
    public static boolean inside(net.minecraft.world.phys.shapes.VoxelShape shape, net.minecraft.core.BlockPos pos,
                                 net.minecraft.world.entity.Entity entity) {
        return net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape.move(pos.getX(), pos.getY(), pos.getZ()),
                net.minecraft.world.phys.shapes.Shapes.create(entity.getBoundingBox()), net.minecraft.world.phys.shapes.BooleanOp.AND);
    }
}
