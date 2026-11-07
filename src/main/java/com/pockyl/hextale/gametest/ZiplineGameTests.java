package com.pockyl.hextale.gametest;

import com.pockyl.hextale.block.ChainAnchorBlockEntity;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.TankFuel;
import com.pockyl.hextale.chem.Zipline;
import com.pockyl.hextale.entity.ZiplineTrolley;
import com.pockyl.hextale.item.ChainLauncherItem;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Chain Launcher: a line forms only within its rules (4-50 blocks, at most 60 degrees, a clear path, ground under
 * the shooter, room for the posts), a shot raises posts at both ends (a claw on a wall) and wears the launcher instead
 * of taking chains, a second shot from a post joins it, breaking an end takes its lines down; riders roll downhill for
 * free, climb only on tank fuel, run on through a junction, draw up over ground under the line and stop only in front
 * of a block too near the chain. Posts stop nobody, yet they can be aimed at and still stand in a chain's way.
 */
public final class ZiplineGameTests {
    private ZiplineGameTests() {
    }

    /** Where someone standing on a stone floor at {@code rel} has their feet (absolute). */
    private static Vec3 standOn(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel.below(), Blocks.STONE);
        return Vec3.atBottomCenterOf(helper.absolutePos(rel));
    }

    /** A hit on the given face of a stone block put at {@code rel}. */
    private static BlockHitResult stoneFace(GameTestHelper helper, BlockPos rel, Direction face) {
        helper.setBlock(rel, Blocks.STONE);
        BlockPos pos = helper.absolutePos(rel);
        return new BlockHitResult(Vec3.atCenterOf(pos).add(face.getUnitVec3().scale(0.5)), face, pos, false);
    }

    private static Zipline.Aim aim(GameTestHelper helper, Vec3 feet, BlockHitResult hit) {
        return Zipline.aim(helper.getLevel(), feet, true, hit);
    }

    private static Zipline.Status shoot(GameTestHelper helper, Player player, ItemStack launcher, Zipline.Aim aim) {
        player.setItemInHand(InteractionHand.MAIN_HAND, launcher);
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(launcher));
        return ChainLauncherItem.shoot(helper.getLevel(), player, launcher, InteractionHand.MAIN_HAND, aim);
    }

    @HexTest(padding = 13)
    public static void aLineFormsOnlyWithinItsRules(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 feet = standOn(helper, new BlockPos(1, 2, 1));
        BlockHitResult wall = stoneFace(helper, new BlockPos(1, 3, 11), Direction.NORTH);
        Zipline.Aim good = aim(helper, feet, wall);
        helper.assertTrue(good.ok(), "a wall 9 blocks off takes the line: " + good.status());
        helper.assertTrue(good.start() != null && good.start().post() && !good.start().existing(), "a new post where I stand");
        helper.assertTrue(good.target() != null && good.target().face() == Direction.NORTH, "a claw on the wall");

        Zipline.Aim floor = aim(helper, feet, stoneFace(helper, new BlockPos(8, 1, 8), Direction.UP));
        helper.assertTrue(floor.ok() && floor.target().face() == Direction.UP, "a post on a floor: " + floor.status());
        helper.setBlock(new BlockPos(8, 3, 8), Blocks.STONE);
        helper.assertTrue(aim(helper, feet, stoneFace(helper, new BlockPos(8, 1, 8), Direction.UP)).status() == Zipline.Status.NO_ROOM,
                "a post needs two free blocks");
        Zipline.Aim ceiling = aim(helper, feet, stoneFace(helper, new BlockPos(1, 9, 9), Direction.DOWN));
        helper.assertTrue(ceiling.ok() && ceiling.target().face() == Direction.DOWN, "a post hanging from a ceiling: " + ceiling.status());

        helper.assertTrue(Zipline.aim(level, feet, false, wall).status() == Zipline.Status.NO_FOOTING, "not from mid-air");
        helper.setBlock(new BlockPos(1, 2, 6), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 3, 6), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 4, 6), Blocks.STONE);
        helper.assertTrue(aim(helper, feet, wall).status() == Zipline.Status.BLOCKED, "a block in the way");
        helper.setBlock(new BlockPos(1, 2, 6), Blocks.AIR);
        helper.setBlock(new BlockPos(1, 3, 6), Blocks.AIR);
        helper.setBlock(new BlockPos(1, 4, 6), Blocks.AIR);
        helper.assertTrue(aim(helper, feet, stoneFace(helper, new BlockPos(1, 3, 3), Direction.NORTH)).status() == Zipline.Status.TOO_CLOSE,
                "too close");
        helper.assertTrue(aim(helper, feet, stoneFace(helper, new BlockPos(2, 12, 2), Direction.DOWN)).status() == Zipline.Status.TOO_STEEP,
                "straight up is a lift, not a zipline");
        helper.succeed();
    }

    @HexTest(padding = 13, maxTicks = 120)
    public static void aShotRaisesPostsWearsTheLauncherAndBreakingAnEndTakesItDown(GameTestHelper helper) {
        Vec3 feet = standOn(helper, new BlockPos(1, 2, 1));
        BlockHitResult floor = stoneFace(helper, new BlockPos(1, 1, 10), Direction.UP);
        ServerPlayer player = VoidLifeGameTests.survivor(helper);
        player.teleportTo(feet.x, feet.y, feet.z);
        ItemStack launcher = new ItemStack(ModItems.CHAIN_LAUNCHER.get());
        helper.assertTrue(shoot(helper, player, launcher, aim(helper, feet, floor)) == Zipline.Status.OK, "the shot goes off");
        helper.assertTrue(launcher.getDamageValue() == 1 && launcher.getMaxDamage() == ChainLauncherItem.SHOTS, "one line wore it once");
        BlockPos start = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos end = helper.absolutePos(new BlockPos(1, 2, 10));
        helper.assertTrue(helper.getLevel().getBlockState(start).is(ModBlocks.CHAIN_POST.get())
                && helper.getLevel().getBlockState(start.above()).is(ModBlocks.CHAIN_POST.get()), "a two-block post rose where I stood");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ChainAnchorBlockEntity.line(helper.getLevel(), start, end) != null
                        && helper.getLevel().getBlockState(end.above()).is(ModBlocks.CHAIN_POST.get()), "the far post stands, the line holds"))
                .thenExecute(() -> helper.getLevel().destroyBlock(end.above(), false))
                .thenWaitUntil(() -> helper.assertTrue(helper.getLevel().getBlockState(end).isAir() && helper.getLevel().getBlockState(start).isAir(),
                        "breaking the far post took the line and the lone start post down"))
                .thenSucceed();
    }

    @HexTest(padding = 13, maxTicks = 160)
    public static void shootingFromAPostJoinsIt(GameTestHelper helper) {
        Vec3 feet = standOn(helper, new BlockPos(6, 2, 6));
        ServerPlayer player = VoidLifeGameTests.survivor(helper);
        player.teleportTo(feet.x, feet.y, feet.z);
        ItemStack launcher = new ItemStack(ModItems.CHAIN_LAUNCHER.get());
        BlockPos hub = helper.absolutePos(new BlockPos(6, 2, 6));
        helper.assertTrue(shoot(helper, player, launcher, aim(helper, feet, stoneFace(helper, new BlockPos(6, 3, 14), Direction.NORTH)))
                == Zipline.Status.OK, "the first line");
        Vec3 beside = feet.add(0.6, 0, 0.3);
        Zipline.Aim second = aim(helper, beside, stoneFace(helper, new BlockPos(14, 3, 6), Direction.WEST));
        helper.assertTrue(second.start() != null && second.start().existing() && second.start().pos().equals(hub), "standing at the post starts from it");
        helper.assertTrue(shoot(helper, player, launcher, second) == Zipline.Status.OK, "the second line");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(helper.getLevel().getBlockEntity(hub) instanceof ChainAnchorBlockEntity anchor
                        && anchor.links().size() == 2 && anchor.holdsAny() && anchor.links().stream().noneMatch(ChainAnchorBlockEntity.Link::flying),
                        "one post holds both lines"))
                .thenExecute(() -> helper.assertTrue(helper.getLevel().getBlockState(hub.offset(1, 0, 0)).isAir(), "no second post"))
                .thenSucceed();
    }

    /** A line from a post on a tower top down to a low wall (a claw), shot by a creative player; returns the ends. */
    private static BlockPos[] downhillLine(GameTestHelper helper) {
        for (int y = 1; y <= 10; y++) {
            helper.setBlock(new BlockPos(1, y, 1), Blocks.STONE);
        }
        Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 11, 1)));
        BlockHitResult wall = stoneFace(helper, new BlockPos(1, 3, 14), Direction.NORTH);
        Zipline.Aim aim = aim(helper, feet, wall);
        Zipline.Status status = shoot(helper, MockPlayers.inLevel(helper), new ItemStack(ModItems.CHAIN_LAUNCHER.get()), aim);
        helper.assertTrue(status == Zipline.Status.OK, "the downhill line is good: " + status);
        return new BlockPos[] {aim.start().pos(), aim.target().pos()};
    }

    private static Zipline.Line line(GameTestHelper helper, BlockPos[] ends) {
        return ChainAnchorBlockEntity.line(helper.getLevel(), ends[0], ends[1]);
    }

    private static void hang(GameTestHelper helper, ServerPlayer rider, BlockPos[] ends, double t) {
        Vec3 at = line(helper, ends).point(t);
        rider.teleportTo(at.x, at.y - Zipline.HANG, at.z);
        helper.assertTrue(ZiplineTrolley.mount(helper.getLevel(), rider, ends[0], ends[1], t), "the rider hangs on");
    }

    @HexTest(padding = 15, maxTicks = 240)
    public static void ridersRollDownhillForFree(GameTestHelper helper) {
        BlockPos[] ends = downhillLine(helper);
        ServerPlayer rider = VoidLifeGameTests.survivor(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(line(helper, ends) != null, "the claw bites"))
                .thenExecute(() -> hang(helper, rider, ends, 0.15))
                .thenWaitUntil(() -> helper.assertTrue(!rider.isPassenger() && rider.position().distanceTo(Vec3.atCenterOf(ends[1])) < 3.5,
                        "rolled down to the claw and set down there: " + rider.position()))
                .thenSucceed();
    }

    @HexTest(padding = 15, maxTicks = 300)
    public static void climbingRunsOnTankFuel(GameTestHelper helper) {
        BlockPos[] ends = downhillLine(helper);
        ServerPlayer rider = VoidLifeGameTests.survivor(helper);
        ItemStack tank = new ItemStack(ModItems.CHEM_TANK.get());
        ChemTank.setContents(tank, ChemTank.poured(TankFuel.EMPTY, Brew.REFINED_SHIMMER, 3));
        int full = ChemTank.contents(tank).units();
        rider.setItemSlot(EquipmentSlot.CHEST, tank);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(line(helper, ends) != null, "the claw bites"))
                .thenExecute(() -> {
                    rider.setYRot(180);
                    rider.setXRot(0);
                    rider.setLastClientInput(new Input(true, false, false, false, false, false, false));
                    hang(helper, rider, ends, 0.85);
                })
                .thenWaitUntil(() -> helper.assertTrue(!rider.isPassenger() && rider.position().distanceTo(Vec3.atCenterOf(ends[0])) < 3.5,
                        "the motor drove up to the post: " + rider.position()))
                .thenExecute(() -> helper.assertTrue(ChemTank.contents(ChemTank.worn(rider)).units() < full, "the climb used fuel"))
                .thenSucceed();
    }

    @HexTest(padding = 15, maxTicks = 240)
    public static void withoutFuelTheMotorDoesNotClimb(GameTestHelper helper) {
        BlockPos[] ends = downhillLine(helper);
        ServerPlayer rider = VoidLifeGameTests.survivor(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(line(helper, ends) != null, "the claw bites"))
                .thenExecute(() -> {
                    rider.setYRot(180);
                    rider.setLastClientInput(new Input(true, false, false, false, false, false, false));
                    hang(helper, rider, ends, 0.6);
                })
                .thenExecuteAfter(80, () -> helper.assertTrue(!(rider.getVehicle() instanceof ZiplineTrolley trolley) || trolley.t() > 0.6,
                        "no tank, no climb: the clamp only holds it, it creeps down"))
                .thenExecute(() -> helper.assertTrue(rider.position().distanceTo(Vec3.atCenterOf(ends[1])) < 12, "still near the low end"))
                .thenSucceed();
    }

    /** Hangs the rider on at {@code t} already moving along the line at {@code speed} blocks a tick. */
    private static void hangMoving(GameTestHelper helper, ServerPlayer rider, BlockPos[] ends, double t, double speed) {
        Zipline.Line line = line(helper, ends);
        Vec3 at = line.point(t);
        rider.teleportTo(at.x, at.y - Zipline.HANG, at.z);
        rider.setKnownMovement(line.tangent(t).normalize().scale(speed));
        helper.assertTrue(ZiplineTrolley.mount(helper.getLevel(), rider, ends[0], ends[1], t), "the rider hangs on");
    }

    @HexTest(padding = 15, maxTicks = 120)
    public static void mountingKeepsThePlayersSpeedAlongTheLine(GameTestHelper helper) {
        BlockPos[] ends = downhillLine(helper);
        ServerPlayer rider = VoidLifeGameTests.survivor(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(line(helper, ends) != null, "the claw bites"))
                .thenExecute(() -> {
                    hangMoving(helper, rider, ends, 0.3, 0.5);
                    helper.assertTrue(rider.getVehicle() instanceof ZiplineTrolley trolley && Math.abs(trolley.speed() - 0.5) < 0.001,
                            "a running jump onto the line keeps its speed");
                })
                .thenSucceed();
    }

    @HexTest(padding = 15, maxTicks = 240)
    public static void aLastEndSlowsTheRideAndTheRiderGoesOn(GameTestHelper helper) {
        BlockPos[] ends = downhillLine(helper);
        ServerPlayer rider = VoidLifeGameTests.survivor(helper);
        double[] peak = {0};
        double[] last = {0};
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(line(helper, ends) != null, "the claw bites"))
                .thenExecute(() -> hang(helper, rider, ends, 0.15))
                .thenWaitUntil(() -> {
                    if (rider.getVehicle() instanceof ZiplineTrolley trolley) {
                        last[0] = Math.abs(trolley.speed());
                        peak[0] = Math.max(peak[0], last[0]);
                    }
                    helper.assertTrue(!rider.isPassenger(), "still riding");
                })
                .thenExecute(() -> {
                    helper.assertTrue(peak[0] > 0.6, "the ride picked up speed: " + peak[0]);
                    helper.assertTrue(last[0] < 0.35 && last[0] > 0.1, "it slowed for the end, and not to a dead stop: " + last[0]);
                })
                .thenExecute(() -> {
                    // A mock player has no connection to tick it, and the speed left is handed over in the player tick.
                    rider.doTick();
                    helper.assertTrue(rider.getDeltaMovement().length() > 0.03, "the rider goes on with what was left: " + rider.getDeltaMovement());
                })
                .thenSucceed();
    }

    @HexTest(padding = 15, maxTicks = 200)
    public static void theBrakeSlowsSteadily(GameTestHelper helper) {
        BlockPos[] ends = downhillLine(helper);
        ServerPlayer rider = VoidLifeGameTests.survivor(helper);
        double[] start = {0};
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(line(helper, ends) != null, "the claw bites"))
                .thenExecute(() -> {
                    rider.setYRot(0);
                    rider.setXRot(0);
                    rider.setLastClientInput(new Input(false, true, false, false, false, false, false));
                    hangMoving(helper, rider, ends, 0.1, Zipline.MAX_SPEED);
                    start[0] = ((ZiplineTrolley) rider.getVehicle()).speed();
                })
                .thenExecuteAfter(8, () -> {
                    double lost = start[0] - ((ZiplineTrolley) rider.getVehicle()).speed();
                    // An exponential brake of old would have taken about 80% by now.
                    helper.assertTrue(lost > 6 * Zipline.BRAKE && lost < 10 * Zipline.BRAKE, "a steady brake: lost " + lost + " in 8 ticks");
                })
                .thenExecuteAfter(16, () -> helper.assertTrue(Math.abs(((ZiplineTrolley) rider.getVehicle()).speed()) < 0.06,
                        "stopped in about a second"))
                .thenSucceed();
    }

    @HexTest(padding = 15, maxTicks = 200)
    public static void aBlockInTheWayStopsTheRide(GameTestHelper helper) {
        BlockPos[] ends = downhillLine(helper);
        ServerPlayer rider = VoidLifeGameTests.survivor(helper);
        int[] waited = {0};
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(line(helper, ends) != null, "the claw bites"))
                .thenExecute(() -> {
                    // Up to the chain itself: no drawing up gets past this.
                    BlockPos chain = BlockPos.containing(line(helper, ends).point(0.55));
                    helper.getLevel().setBlockAndUpdate(chain, Blocks.STONE.defaultBlockState());
                    helper.getLevel().setBlockAndUpdate(chain.below(), Blocks.STONE.defaultBlockState());
                    hang(helper, rider, ends, 0.15);
                })
                .thenWaitUntil(() -> helper.assertTrue(++waited[0] > 80, "riding a while"))
                .thenExecute(() -> helper.assertTrue(rider.getVehicle() instanceof ZiplineTrolley trolley && trolley.t() < 0.55 && trolley.speed() == 0,
                        "stopped in front of the block, still hanging"))
                .thenSucceed();
    }

    @HexTest(padding = 15, maxTicks = 400)
    public static void ridersDrawUpOverGroundUnderTheLine(GameTestHelper helper) {
        // A line down to a post on a floor: its low end runs closer to the floor than a rider hangs, over a step and a slab.
        for (int z = 0; z <= 15; z++) {
            helper.setBlock(new BlockPos(1, 1, z), Blocks.STONE);
        }
        for (int y = 2; y <= 4; y++) {
            helper.setBlock(new BlockPos(1, y, 1), Blocks.STONE);
        }
        Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 5, 1)));
        Zipline.Aim aim = aim(helper, feet, stoneFace(helper, new BlockPos(1, 1, 14), Direction.UP));
        helper.assertTrue(shoot(helper, MockPlayers.inLevel(helper), new ItemStack(ModItems.CHAIN_LAUNCHER.get()), aim) == Zipline.Status.OK,
                "the line: " + aim.status());
        BlockPos[] ends = {aim.start().pos(), aim.target().pos()};
        helper.setBlock(new BlockPos(1, 2, 10), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 2, 12), Blocks.SMOOTH_STONE_SLAB);
        ServerPlayer rider = VoidLifeGameTests.survivor(helper);
        double[] drawn = {0};
        boolean[] clipped = {false};
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(line(helper, ends) != null, "the far post stands"))
                .thenExecute(() -> hang(helper, rider, ends, 0.1))
                .thenWaitUntil(() -> {
                    if (rider.getVehicle() instanceof ZiplineTrolley trolley) {
                        drawn[0] = Math.max(drawn[0], trolley.lift());
                        clipped[0] |= !helper.getLevel().noBlockCollision(rider,
                                rider.getDimensions(rider.getPose()).makeBoundingBox(trolley.riderFeet()).deflate(0.05));
                    }
                    helper.assertTrue(!rider.isPassenger(), "still riding");
                })
                .thenExecute(() -> {
                    helper.assertTrue(!clipped[0], "the body never ran into the ground");
                    helper.assertTrue(drawn[0] > 0.2, "the rider drew up over the ground: " + drawn[0]);
                    helper.assertTrue(rider.position().distanceTo(Vec3.atCenterOf(ends[1])) < 3.5,
                            "rode past the step and the slab to the far post: " + helper.relativeVec(rider.position()));
                })
                .thenSucceed();
    }

    @HexTest(padding = 13, maxTicks = 120)
    public static void postsStopNobodyButCanBeAimedAt(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 feet = standOn(helper, new BlockPos(1, 2, 1));
        ServerPlayer shooter = VoidLifeGameTests.survivor(helper);
        shooter.teleportTo(feet.x, feet.y, feet.z);
        ItemStack launcher = new ItemStack(ModItems.CHAIN_LAUNCHER.get());
        Zipline.Aim first = aim(helper, feet, stoneFace(helper, new BlockPos(1, 1, 10), Direction.UP));
        helper.assertTrue(shoot(helper, shooter, launcher, first) == Zipline.Status.OK, "the first line");
        BlockPos[] ends = {first.start().pos(), first.target().pos()};
        BlockPos far = ends[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(line(helper, ends) != null, "the far post stands"))
                .thenExecute(() -> {
                    for (BlockPos half : new BlockPos[] {far, far.above()}) {
                        helper.assertTrue(level.getBlockState(half).getCollisionShape(level, half).isEmpty(), "nothing to bump into");
                        helper.assertTrue(!level.getBlockState(half).getShape(level, half).isEmpty(), "an outline to click and break");
                    }
                    helper.assertTrue(level.noBlockCollision(shooter, shooter.getDimensions(shooter.getPose())
                            .makeBoundingBox(Vec3.atBottomCenterOf(far))), "a player fits where the post stands");

                    // The aim ray still finds a post, though a collider ray goes through it.
                    Vec3 side = standOn(helper, new BlockPos(8, 2, 10));
                    shooter.teleportTo(side.x, side.y, side.z);
                    shooter.setOnGround(true);
                    shooter.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atBottomCenterOf(far).add(0, 1.2, 0));
                    Zipline.Aim atPost = Zipline.aim(level, shooter);
                    helper.assertTrue(atPost.target() != null && atPost.target().existing() && atPost.target().pos().equals(far),
                            "the aim ray finds the post: " + atPost.status());

                    // A post in a chain's path still blocks that chain.
                    Vec3 before = standOn(helper, new BlockPos(1, 2, 7));
                    helper.assertTrue(aim(helper, before, stoneFace(helper, new BlockPos(1, 3, 13), Direction.NORTH)).status() == Zipline.Status.BLOCKED,
                            "a chain does not run through a pole");
                })
                .thenSucceed();
    }

    @HexTest(padding = 13, maxTicks = 400)
    public static void aRideRunsOnThroughAJunction(GameTestHelper helper) {
        // Tower top -> a mid post on a lower tower -> a low wall further on, all in one straight line.
        for (int y = 1; y <= 12; y++) {
            helper.setBlock(new BlockPos(1, y, 1), Blocks.STONE);
        }
        for (int y = 1; y <= 7; y++) {
            helper.setBlock(new BlockPos(1, y, 8), Blocks.STONE);
        }
        Vec3 top = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 13, 1)));
        Vec3 mid = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 8, 8)));
        Player shooter = MockPlayers.inLevel(helper);
        ItemStack launcher = new ItemStack(ModItems.CHAIN_LAUNCHER.get());
        BlockHitResult midTop = new BlockHitResult(mid, Direction.UP, BlockPos.containing(mid).below(), false);
        Zipline.Aim first = aim(helper, top, midTop);
        helper.assertTrue(shoot(helper, shooter, launcher, first) == Zipline.Status.OK, "the first line: " + first.status());
        BlockPos[] ends = {first.start().pos(), first.target().pos()};
        BlockPos low = helper.absolutePos(new BlockPos(1, 3, 16));
        ServerPlayer rider = VoidLifeGameTests.survivor(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(line(helper, ends) != null, "the first line holds"))
                .thenExecute(() -> {
                    Zipline.Aim second = aim(helper, mid, stoneFace(helper, new BlockPos(1, 3, 17), Direction.NORTH));
                    helper.assertTrue(second.start() != null && second.start().existing(), "the second line starts at the mid post");
                    helper.assertTrue(shoot(helper, shooter, launcher, second) == Zipline.Status.OK, "the second line: " + second.status());
                })
                .thenWaitUntil(() -> helper.assertTrue(ChainAnchorBlockEntity.line(helper.getLevel(), ends[1], low) != null, "the second line holds"))
                .thenExecute(() -> hang(helper, rider, ends, 0.1))
                .thenWaitUntil(() -> helper.assertTrue(!rider.isPassenger() && rider.position().distanceTo(Vec3.atCenterOf(low)) < 3.5,
                        "ran on through the mid post down to the far wall: " + helper.relativeVec(rider.position())))
                .thenSucceed();
    }

}
