package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Where things are on the two-block desk. Points are in pixels of the main block's model as built facing north
 * (x = 16 - u, where u runs along the desk from the viewer's left end, 0, to the right end, 32; the side block covers
 * x from -16 to 0). The block models come from {@code tools/model-gen/hextale.py}; keep these numbers in sync with it.
 */
public final class TableGeometry {
    /** Centre of the floating orb, inside the gimbal rings that hang from the brass arm. */
    public static final Vec3 ORB = new Vec3(-4.5, 20.6, 7.5);
    /** Centre of the parchment on the writing pad. */
    public static final Vec3 PARCHMENT = new Vec3(-4.5, 14.25, 7.5);
    /** The loupe arm's swivel post on the desk's right front corner (its foot), and where the lens hangs while it looks. */
    public static final Vec3 LOUPE_PIVOT = new Vec3(-14, 14, 2.5);
    public static final Vec3 LOUPE_LOOK = new Vec3(-6.0, 19.5, 2.8);
    /** Centre of the open diary on the slanted book rest. */
    public static final Vec3 DIARY = new Vec3(8.5, 17.2, 12.5);

    /** Collision and outline boxes in design space: {u0, y0, z0, u1, y1, z1}, z = 0 at the front. */
    private static final double[][] BOXES = {
            {0, 12, 1, 32, 14, 15},               // top
            {0, 0, 1, 4, 12, 5},                  // legs
            {28, 0, 1, 32, 12, 5},
            {0, 0, 11, 4, 12, 15},
            {28, 0, 11, 32, 12, 15},
            {4, 0, 4, 14, 12, 15},                // drawer cabinet
            {18, 0, 4, 28, 12, 15},               // bookshelf
            {2, 14, 9, 13, 16, 16},               // book rest
            {2, 14, 2, 7, 16, 7},                 // inkwell
            {18, 14, 12, 23, 16, 15},             // orb arm base
    };
    private static final Map<Direction, VoxelShape> MAIN_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> SIDE_SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            MAIN_SHAPES.put(facing, shape(16, facing));
            SIDE_SHAPES.put(facing, shape(32, facing));
        }
    }

    private TableGeometry() {
    }

    public static VoxelShape shape(TablePart part, Direction facing) {
        return (part == TablePart.MAIN ? MAIN_SHAPES : SIDE_SHAPES).get(facing);
    }

    /** The boxes of one half: x = offset - u, clipped to the block. */
    private static VoxelShape shape(double offset, Direction facing) {
        return shape(BOXES, offset, facing);
    }

    /**
     * The shape of one half of any two-block desk built like this one (the Lab Bench too): {@code boxes} in design space
     * ({u0, y0, z0, u1, y1, z1}, u along the desk from the viewer's left end), {@code part} the half.
     */
    public static VoxelShape shape(double[][] boxes, TablePart part, Direction facing) {
        return shape(boxes, part == TablePart.MAIN ? 16 : 32, facing);
    }

    private static VoxelShape shape(double[][] boxes, double offset, Direction facing) {
        VoxelShape shape = Shapes.empty();
        for (double[] b : boxes) {
            double x0 = Math.max(0, offset - b[3]);
            double x1 = Math.min(16, offset - b[0]);
            if (x1 <= x0) {
                continue;
            }
            double[] from = rotate(x0, b[2], facing);
            double[] to = rotate(x1, b[5], facing);
            shape = Shapes.or(shape, Block.box(Math.min(from[0], to[0]), b[1], Math.min(from[1], to[1]),
                    Math.max(from[0], to[0]), b[4], Math.max(from[1], to[1])));
        }
        return shape.optimize();
    }

    /** World position of a point given in main block model pixels. */
    public static Vec3 toWorld(BlockPos mainPos, Direction facing, Vec3 local) {
        double[] rotated = rotate(local.x, local.z, facing);
        return new Vec3(mainPos.getX() + rotated[0] / 16, mainPos.getY() + local.y / 16, mainPos.getZ() + rotated[1] / 16);
    }

    /** Both blocks and the space above them, where the renderer draws the orb and its rings. */
    public static AABB renderBounds(BlockPos mainPos, Direction facing) {
        BlockPos side = mainPos.relative(TablePart.MAIN.toOther(facing));
        return new AABB(mainPos).minmax(new AABB(side)).expandTowards(0, 1, 0);
    }

    /** Rotates a model point (pixels, around the block centre) like a blockstate "y" rotation of a north-facing model. */
    private static double[] rotate(double x, double z, Direction facing) {
        double dx = x - 8;
        double dz = z - 8;
        return switch (facing) {
            case EAST -> new double[]{8 - dz, 8 + dx};
            case SOUTH -> new double[]{8 - dx, 8 - dz};
            case WEST -> new double[]{8 + dz, 8 - dx};
            default -> new double[]{x, z};
        };
    }
}
