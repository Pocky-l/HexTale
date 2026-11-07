package com.pockyl.hextale.chem;

import com.pockyl.hextale.block.ChainAnchorBlockEntity;
import com.pockyl.hextale.block.ChainClawBlock;
import com.pockyl.hextale.block.ChainPostBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The Chain Launcher's rules in one place (owner, 2026-11-04): the hand-held launcher shoots a chain from where its
 * user stands to what they aim at. Where they stand, a Chain Post rises (or the post already standing there takes a
 * second line, so lines join into a network); a floor or a ceiling gets a post too, a wall gets a Chain Claw. A line is
 * 4-50 blocks long, at most 60 degrees steep and needs a clear path; it sags a little under its own weight and riders
 * follow that sag. Shared by the server (the shot) and the client (the hologram), so the hologram never promises a
 * line the server refuses.
 */
public final class Zipline {
    public static final double MIN_LENGTH = 4;
    public static final double MAX_LENGTH = 50;
    /** Steepest line, in degrees from the horizontal: steeper would be a lift (the Levitation Plate's job). */
    public static final double MAX_SLOPE = 60;
    /** How far the aiming ray looks for a target (a little past the longest line, to say "too far"). */
    public static final double AIM_REACH = 64;
    /** From the face a post stands on (or hangs from) to its chain eye: the neck of the bollard on top of the pole. */
    public static final double POST_EYE = 2.15;
    /** From the face the claw bites to its chain eye. */
    public static final double CLAW_EYE = 5 / 16.0;
    /** How far from the feet a standing post counts as "the post I stand at" (horizontal). */
    private static final double POST_NEAR = 1.4;
    /** How deep a line sags in its middle per block of length (50 blocks sag 1.25). */
    private static final double SAG_PER_BLOCK = 0.025;
    /** Blocks a tick the chain unreels during a shot. */
    public static final double SHOT_SPEED = 5;
    /** From the chain down to a rider's feet: they hang from the trolley's grip with raised arms. */
    public static final double HANG = 2.3;
    /** Blocks a tick: gravity's pull along the slope per tick, the speed limit, the motor's push from a standstill. */
    public static final double GRAVITY = 0.05;
    public static final double MAX_SPEED = 19 / 20.0;
    public static final double MOTOR_PUSH = 0.06;
    /** Speed the brake takes off per tick, steadily: about a second from full speed. */
    public static final double BRAKE = MAX_SPEED / 20;
    /** Blocks the motor drives a rider per action of tank fuel. */
    public static final double BLOCKS_PER_ACTION = 4;

    private Zipline() {
    }

    /** What the hologram says about a target, and what the server refuses with. */
    public enum Status {
        OK, NO_TARGET, NO_FOOTING, NO_GRIP, NO_ROOM, LINKED, TOO_CLOSE, TOO_FAR, TOO_STEEP, BLOCKED;

        public String key() {
            return "item.hextale.chain_launcher.aim." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** One line: from the owner's chain eye to the other end's, sagging {@code sag} blocks in the middle. */
    public record Line(Vec3 start, Vec3 end, double sag) {
        public static Line between(Vec3 start, Vec3 end) {
            return new Line(start, end, start.distanceTo(end) * SAG_PER_BLOCK);
        }

        public Vec3 point(double t) {
            return start.add(end.subtract(start).scale(t)).add(0, -4 * sag * t * (1 - t), 0);
        }

        /** The derivative along t (its length is the line's length at that point). */
        public Vec3 tangent(double t) {
            return end.subtract(start).add(0, -4 * sag * (1 - 2 * t), 0);
        }

        public double length() {
            return start.distanceTo(end);
        }

        /** The t of the point on the line nearest to {@code at} (coarse search, then a fine one). */
        public double nearest(Vec3 at) {
            int steps = Math.clamp((long) Math.ceil(length() * 2), 8, 400);
            double best = 0;
            double bestDistance = Double.MAX_VALUE;
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double d = point(t).distanceToSqr(at);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = t;
                }
            }
            double step = 1.0 / steps;
            for (int i = 0; i < 12; i++) {
                step /= 2;
                double lower = Math.max(0, best - step);
                double upper = Math.min(1, best + step);
                if (point(lower).distanceToSqr(at) < point(best).distanceToSqr(at)) {
                    best = lower;
                } else if (point(upper).distanceToSqr(at) < point(best).distanceToSqr(at)) {
                    best = upper;
                }
            }
            return best;
        }
    }

    /**
     * An end of a line. {@code face} is the outward normal of the face it sits on: UP a post standing on a floor, DOWN
     * a post hanging from a ceiling ({@code pos} = the post's half touching that face), a side = a claw bitten into a
     * wall ({@code pos} = the claw's cell). {@code existing}: the post is already there (the line joins it).
     */
    public record Anchor(BlockPos pos, Direction face, boolean existing) {
        public boolean post() {
            return face.getAxis() == Direction.Axis.Y;
        }

        public Vec3 eye() {
            return switch (face) {
                case UP -> Vec3.atBottomCenterOf(pos).add(0, POST_EYE, 0);
                case DOWN -> Vec3.atBottomCenterOf(pos).add(0, 1 - POST_EYE, 0);
                default -> clawEye(pos, face);
            };
        }

        /** The cells this end takes: both halves of a post, the claw's own cell. */
        public List<BlockPos> cells() {
            return post() ? List.of(pos, pos.relative(face)) : List.of(pos);
        }

        /** The block it is fixed to. */
        public BlockPos support() {
            return pos.relative(face.getOpposite());
        }

        /** The end a placed post or claw block is. */
        public static @Nullable Anchor of(BlockGetter level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ChainPostBlock) {
                return new Anchor(ChainPostBlock.base(pos, state), ChainPostBlock.face(state), true);
            }
            if (state.getBlock() instanceof ChainClawBlock) {
                return new Anchor(pos, ChainClawBlock.normal(state), true);
            }
            return null;
        }
    }

    /** The result of aiming: both ends and the line between them, or why it cannot be. */
    public record Aim(Status status, @Nullable Line line, @Nullable Anchor start, @Nullable Anchor target, @Nullable Vec3 blockedAt) {
        static Aim refused(Status status, @Nullable Anchor start, @Nullable Anchor target) {
            return new Aim(status, null, start, target, null);
        }

        public boolean ok() {
            return status == Status.OK;
        }
    }

    /** The chain eye of a claw biting the face {@code normal} of the block behind {@code clawCell}. */
    public static Vec3 clawEye(BlockPos clawCell, Direction normal) {
        return Vec3.atCenterOf(clawCell).add(normal.getUnitVec3().scale(CLAW_EYE - 0.5));
    }

    /** What {@code player} aims at with a launcher now: the ray from their eyes, their feet as the start. */
    public static Aim aim(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 to = eye.add(player.getViewVector(1).scale(AIM_REACH));
        BlockHitResult solid = level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        BlockHitResult post = postOnRay(level, eye, solid.getLocation(), start(level, player.position(), player.onGround()));
        return aim(level, player.position(), player.onGround(), post != null ? post : solid);
    }

    /** The first Chain Post on the ray (posts have no collision, so a collider ray misses them), past the one shot from. */
    private static @Nullable BlockHitResult postOnRay(Level level, Vec3 from, Vec3 to, @Nullable Anchor start) {
        return BlockGetter.traverseBlocks(from, to, level, (getter, pos) -> {
            BlockState state = getter.getBlockState(pos);
            if (!(state.getBlock() instanceof ChainPostBlock)
                    || start != null && start.existing() && ChainPostBlock.base(pos, state).equals(start.pos())) {
                return null;
            }
            return state.getShape(getter, pos).clip(from, to, pos);
        }, getter -> null);
    }

    /** Judges a shot by someone standing at {@code feet} at what the aim ray hit. */
    public static Aim aim(Level level, Vec3 feet, boolean onGround, HitResult hit) {
        Anchor start = start(level, feet, onGround);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return Aim.refused(Status.NO_TARGET, start, null);
        }
        if (feet.distanceTo(blockHit.getLocation()) > MAX_LENGTH + 8) {
            return Aim.refused(Status.NO_TARGET, start, null);
        }
        Anchor target = target(level, blockHit);
        Status targetStatus = target == null ? Status.NO_GRIP : Status.OK;
        if (target != null && !target.existing() && !free(level, target)) {
            targetStatus = target.post() ? Status.NO_ROOM : Status.NO_GRIP;
        }
        if (target == null || targetStatus != Status.OK) {
            return Aim.refused(targetStatus, start, target);
        }
        if (start == null) {
            return Aim.refused(Status.NO_FOOTING, null, target);
        }
        if (start.existing() && target.existing() && ChainAnchorBlockEntity.linked(level, start.pos(), target.pos())) {
            return Aim.refused(Status.LINKED, start, target);
        }
        Line line = Line.between(start.eye(), target.eye());
        double length = line.length();
        Status status = Status.OK;
        Vec3 blocked = null;
        if (length < MIN_LENGTH || start.pos().equals(target.pos())) {
            status = Status.TOO_CLOSE;
        } else if (length > MAX_LENGTH) {
            status = Status.TOO_FAR;
        } else if (slope(line) > MAX_SLOPE) {
            status = Status.TOO_STEEP;
        } else {
            blocked = obstacle(level, line, start, target);
            if (blocked != null) {
                status = Status.BLOCKED;
            }
        }
        return new Aim(status, line, start, target, blocked);
    }

    /**
     * Where a shot by someone at {@code feet} starts: the post they stand at, else a new post on the ground under them,
     * else null (in the air, in water, no room for two blocks of pole).
     */
    public static @Nullable Anchor start(Level level, Vec3 feet, boolean onGround) {
        BlockPos at = BlockPos.containing(feet);
        Anchor best = null;
        double bestDistance = POST_NEAR * POST_NEAR;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-1, -2, -1), at.offset(1, 2, 1))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ChainPostBlock) || !ChainPostBlock.isBase(state) || ChainPostBlock.face(state) != Direction.UP) {
                continue;
            }
            Vec3 base = Vec3.atBottomCenterOf(pos);
            double dx = base.x - feet.x;
            double dz = base.z - feet.z;
            double d = dx * dx + dz * dz;
            if (d <= bestDistance && Math.abs(base.y - feet.y) < 1.2) {
                bestDistance = d;
                best = new Anchor(pos.immutable(), Direction.UP, true);
            }
        }
        if (best != null) {
            return best;
        }
        Anchor post = new Anchor(at, Direction.UP, false);
        BlockPos below = at.below();
        if (!onGround || !Block.canSupportCenter(level, below, Direction.UP) || !free(level, post)) {
            return null;
        }
        return post;
    }

    /** The end a hit would make: the post it hit, a new post on a floor or ceiling, a claw on a wall; null if none. */
    private static @Nullable Anchor target(Level level, BlockHitResult hit) {
        BlockPos support = hit.getBlockPos();
        Anchor existing = Anchor.of(level, support);
        if (existing != null) {
            return existing.post() ? existing : null;
        }
        Direction face = hit.getDirection();
        BlockPos cell = support.relative(face);
        boolean grips = face.getAxis() == Direction.Axis.Y ? Block.canSupportCenter(level, support, face)
                : level.getBlockState(support).isFaceSturdy(level, support, face);
        return grips ? new Anchor(cell, face, false) : null;
    }

    /** Whether a new end still fits where it was aimed: its block holds it and its cells are free. */
    public static boolean fits(Level level, Anchor anchor) {
        BlockPos support = anchor.support();
        boolean grips = anchor.post() ? Block.canSupportCenter(level, support, anchor.face())
                : level.getBlockState(support).isFaceSturdy(level, support, anchor.face());
        return grips && free(level, anchor);
    }

    /** Whether the cells a new end would take are free (air, plants, no fluid). */
    private static boolean free(Level level, Anchor anchor) {
        for (BlockPos cell : anchor.cells()) {
            BlockState state = level.getBlockState(cell);
            if (!level.isInWorldBounds(cell) || !state.canBeReplaced() || !state.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** The line's steepness in degrees (of its chord). */
    public static double slope(Line line) {
        Vec3 d = line.end().subtract(line.start());
        return Math.toDegrees(Math.atan2(Math.abs(d.y), Math.sqrt(d.x * d.x + d.z * d.z)));
    }

    /** The first point where the chain between these ends would run through a solid block, or null when clear. */
    public static @Nullable Vec3 obstacle(BlockGetter level, Line line, Anchor from, Anchor to) {
        List<BlockPos> ends = new ArrayList<>(from.cells());
        ends.addAll(to.cells());
        if (!to.post()) {
            ends.add(to.support());
        }
        return obstacle(level, line, ends);
    }

    private static @Nullable Vec3 obstacle(BlockGetter level, Line line, List<BlockPos> ends) {
        double length = line.length();
        int steps = Math.clamp((long) Math.ceil(length * 4), 4, 800);
        for (int i = 1; i < steps; i++) {
            Vec3 p = line.point(i / (double) steps);
            BlockPos pos = BlockPos.containing(p);
            if (ends.contains(pos)) {
                continue;
            }
            if (level instanceof Level world && !world.isLoaded(pos)) {
                return p;
            }
            BlockState state = level.getBlockState(pos);
            // Posts have no collision, but a chain still does not run through another line's pole.
            VoxelShape shape = state.getBlock() instanceof ChainPostBlock ? state.getShape(level, pos) : state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                // Every sample walks its stretch finely against the real boxes (slabs, fences, poles) instead of the whole cell.
                for (AABB box : shape.toAabbs()) {
                    AABB grown = box.move(pos).inflate(0.03);
                    for (int k = -2; k <= 2; k++) {
                        Vec3 q = line.point(Mth.clamp((i + k / 4.0) / steps, 0, 1));
                        if (grown.contains(q)) {
                            return q;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** The motor's top speed with this fuel in the tank, blocks a tick. */
    public static double motorSpeed(ChemFuel fuel) {
        return switch (fuel) {
            case EMBER_OIL -> 9 / 20.0;
            case RAW_SHIMMER -> 12 / 20.0;
            case REFINED_SHIMMER -> 15 / 20.0;
        };
    }

    /** Ticks the chain takes to unreel along a line of this length. */
    public static int flightTicks(double length) {
        return Math.max(3, Mth.ceil(length / SHOT_SPEED));
    }
}
