package com.pockyl.hextale.gametest;

import com.pockyl.hextale.GearConfig;
import com.pockyl.hextale.block.ChemBurnerBlock;
import com.pockyl.hextale.block.ChemBurnerBlockEntity;
import com.pockyl.hextale.block.ChemCarboyBlock;
import com.pockyl.hextale.block.ChemCarboyBlockEntity;
import com.pockyl.hextale.block.ChemStillBlockEntity;
import com.pockyl.hextale.block.ChemTankBlockEntity;
import com.pockyl.hextale.block.CrucibleBlockEntity;
import com.pockyl.hextale.block.PinkBrazierBlock;
import com.pockyl.hextale.block.PinkBrazierBlockEntity;
import com.pockyl.hextale.block.ShapePartBlock;
import com.pockyl.hextale.block.ShapeParts;
import com.pockyl.hextale.block.VialRackBlock;
import com.pockyl.hextale.block.VialRackBlockEntity;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.CarboyContents;
import com.pockyl.hextale.chem.ChemFuel;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Chem Carboy: one brew in two blocks, filled by hand, hopper and crucible, emptied at the tap, drawn from by the
 * devices within reach, read by a comparator, carried off with its brew. The Vial Rack: eight places, each clicked by itself.
 */
public final class ChemCarboyGameTests {
    private static final BlockPos AT = new BlockPos(2, 1, 2);

    private ChemCarboyGameTests() {
    }

    private static ChemCarboyBlockEntity carboy(GameTestHelper helper, BlockPos pos, Brew brew, int doses) {
        helper.setBlock(pos, ModBlocks.CHEM_CARBOY.get());
        ShapeParts.refresh(helper.getLevel(), helper.absolutePos(pos), null);
        ChemCarboyBlockEntity carboy = helper.getBlockEntity(pos, ChemCarboyBlockEntity.class);
        if (doses > 0) {
            carboy.setContents(new CarboyContents(brew, doses));
        }
        return carboy;
    }

    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos pos, Vec3 hit, Direction side) {
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(hit, side, helper.absolutePos(pos), false));
    }

    private static void clickMiddle(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        click(helper, player, pos, Vec3.atCenterOf(helper.absolutePos(pos)), Direction.NORTH);
    }

    /** A player whose hands run out: the game test server starts everybody in creative. */
    private static ServerPlayer survivor(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.getAbilities().instabuild = false;
        return player;
    }

    private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getInventory().countItem(item);
    }

    @HexTest
    public static void aCarboyHoldsOneBrewAndGivesVialsAtTheTap(GameTestHelper helper) {
        ChemCarboyBlockEntity carboy = carboy(helper, AT, Brew.ACID, 0);
        helper.assertTrue(helper.getBlockState(AT.above()).getBlock() instanceof ShapePartBlock, "a stand-in keeps the cell above");
        helper.assertTrue(ChemCarboyBlockEntity.at(helper.getLevel(), helper.absolutePos(AT.above())) == carboy, "the upper half is the same carboy");
        ServerPlayer player = survivor(helper);

        player.setItemInHand(InteractionHand.MAIN_HAND, ChemVialItem.of(Brew.ACID, 3));
        clickMiddle(helper, player, AT);
        helper.assertTrue(carboy.contents().brew() == Brew.ACID && carboy.contents().doses() == 3, "three vials poured in");
        helper.assertTrue(player.getMainHandItem().isEmpty() || player.getMainHandItem().is(Items.GLASS_BOTTLE), "the vials left the hand");
        helper.assertTrue(count(player, Items.GLASS_BOTTLE) == 3, "the bottles came back");

        player.setItemInHand(InteractionHand.MAIN_HAND, ChemVialItem.of(Brew.GROWTH, 2));
        clickMiddle(helper, player, AT.above());
        helper.assertTrue(carboy.contents().brew() == Brew.ACID && carboy.contents().doses() == 3, "no second brew, also through the upper half");
        helper.assertTrue(ChemVialItem.brew(player.getMainHandItem()) == Brew.GROWTH && player.getMainHandItem().getCount() == 2, "it stays in hand");

        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
        clickMiddle(helper, player, AT);
        helper.assertTrue(carboy.contents().doses() == 2, "a bottle takes one dose");
        helper.assertTrue(count(player, Items.GLASS_BOTTLE) == 1, "one bottle was filled");
        helper.assertTrue(count(player, ModItems.CHEM_VIAL.get()) == 1, "and came out as a vial");

        carboy.setContents(new CarboyContents(Brew.ACID, 1));
        helper.assertTrue(carboy.bottle().getCount() == 1 && carboy.contents().isEmpty(), "the last dose empties it");
        helper.assertTrue(carboy.room(Brew.GROWTH) == CarboyContents.CAPACITY, "an empty carboy takes any brew again");
        helper.succeed();
    }

    @HexTest
    public static void theComparatorReadsTheLevelAndABrokenCarboyKeepsItsBrew(GameTestHelper helper) {
        ChemCarboyBlockEntity carboy = carboy(helper, AT, Brew.ACID, 0);
        BlockPos at = helper.absolutePos(AT);
        BlockState state = helper.getLevel().getBlockState(at);
        helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), at, Direction.NORTH) == 0, "empty gives nothing");
        carboy.setContents(new CarboyContents(Brew.ACID, 1));
        helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), at, Direction.NORTH) == 1, "one dose gives 1");
        carboy.setContents(new CarboyContents(Brew.ACID, CarboyContents.CAPACITY - 1));
        helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), at, Direction.NORTH) == 14, "not quite full stays below 15");
        carboy.setContents(new CarboyContents(Brew.ACID, CarboyContents.CAPACITY));
        helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), at, Direction.NORTH) == 15, "full gives 15");

        carboy.setContents(new CarboyContents(Brew.RAW_SHIMMER, 40));
        AABB around = new AABB(at).inflate(3);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).forEach(Entity::discard);
        helper.getLevel().destroyBlock(at, true);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(ModItems.CHEM_CARBOY.get()));
        helper.assertTrue(drops.size() == 1, "the carboy drops");
        ItemStack dropped = drops.getFirst().getItem();
        CarboyContents kept = dropped.getOrDefault(ModDataComponents.CHEM_CARBOY.get(), CarboyContents.EMPTY);
        helper.assertTrue(kept.brew() == Brew.RAW_SHIMMER && kept.doses() == 40, "with its brew");
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.AIR, AT.above()));
    }

    @HexTest
    public static void aCreativeCarboyStaysFullOfTheBrewAVialPicked(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.CREATIVE_CHEM_CARBOY.get());
        ChemCarboyBlockEntity carboy = helper.getBlockEntity(AT, ChemCarboyBlockEntity.class);
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, ChemVialItem.of(Brew.COOLANT, 1));
        clickMiddle(helper, player, AT);
        helper.assertTrue(carboy.contents().brew() == Brew.COOLANT && carboy.contents().doses() == CarboyContents.CAPACITY, "the vial picks the brew");
        helper.assertTrue(ChemVialItem.brew(carboy.bottle()) == Brew.COOLANT && carboy.contents().doses() == CarboyContents.CAPACITY, "it never runs down");
        helper.assertFalse(carboy.canPlaceItem(0, ChemVialItem.of(Brew.COOLANT)), "hoppers pour nothing in");
        helper.succeed();
    }

    @HexTest(maxTicks = 200)
    public static void hoppersPourVialsIntoBothHalves(GameTestHelper helper) {
        ChemCarboyBlockEntity carboy = carboy(helper, AT, Brew.ACID, 0);
        BlockPos top = AT.above(2);
        BlockPos side = AT.east();
        helper.setBlock(top, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(side, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));
        helper.getBlockEntity(top, HopperBlockEntity.class).setItem(0, ChemVialItem.of(Brew.ACID, 2));
        helper.getBlockEntity(side, HopperBlockEntity.class).setItem(0, ChemVialItem.of(Brew.ACID, 2));
        helper.getBlockEntity(side, HopperBlockEntity.class).setItem(1, new ItemStack(Items.DIRT));
        helper.succeedWhen(() -> {
            helper.assertTrue(carboy.contents().doses() == 4, "both hoppers emptied their vials into it");
            helper.assertTrue(helper.getBlockEntity(side, HopperBlockEntity.class).getItem(1).is(Items.DIRT), "dirt stays in the hopper");
        });
    }

    @HexTest(maxTicks = 700)
    public static void theCrucibleBesidePoursItsBrewIn(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.LAVA);
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        ChemCarboyBlockEntity carboy = carboy(helper, AT.east(), Brew.FUEL, 0);
        ServerPlayer player = MockPlayers.inLevel(helper);
        Research.research(player, ResearchTopics.COMBUSTIBLE_MIX);
        boolean[] brewed = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (!brewed[0]) {
                crucible.pour(EssenceMix.pure(Essence.FIRE, 8).plus(EssenceMix.pure(Essence.ASH, 4)));
                helper.assertTrue(crucible.take(new ItemStack(Items.COAL), 1, true, player) == 1, "the coal goes in");
                helper.assertTrue(crucible.brew() == Brew.FUEL, "the water turns into combustible mix");
                brewed[0] = true;
            }
            helper.assertTrue(crucible.brew() == null, "the brew left the crucible");
            helper.assertTrue(carboy.contents().brew() == Brew.FUEL && carboy.contents().doses() == 4, "all four doses are in the carboy");
        });
    }

    @HexTest(maxTicks = 300)
    public static void aBurnerTakesTheWeakestFuelInReachAndNothingBeyondIt(GameTestHelper helper) {
        BlockPos burnerAt = new BlockPos(1, 1, 1);
        helper.setBlock(burnerAt, ModBlocks.CHEM_BURNER.get());
        helper.setBlock(burnerAt.above(), ModBlocks.CRUCIBLE.get());
        helper.getBlockEntity(burnerAt.above(), CrucibleBlockEntity.class).fillWater(CrucibleBlockEntity.BUCKET, false);
        ChemCarboyBlockEntity near = carboy(helper, burnerAt.east(2), Brew.REFINED_SHIMMER, 5);
        ChemCarboyBlockEntity weak = carboy(helper, burnerAt.south(4), Brew.FUEL, 5);
        ChemBurnerBlockEntity burner = helper.getBlockEntity(burnerAt, ChemBurnerBlockEntity.class);
        helper.succeedWhen(() -> {
            helper.assertTrue(ChemBurnerBlock.isLit(helper.getBlockState(burnerAt)), "the burner lit from a carboy");
            helper.assertTrue(burner.burnFull() == Brew.FUEL.burnTicks(), "on combustible mix");
            helper.assertTrue(weak.contents().doses() == 4, "one dose went");
            helper.assertTrue(near.contents().doses() == 5, "the refined shimmer was left alone");
        });
    }

    @HexTest(maxTicks = 100)
    public static void aCarboyOutOfReachFeedsNothing(GameTestHelper helper) {
        BlockPos burnerAt = new BlockPos(1, 1, 1);
        helper.setBlock(burnerAt, ModBlocks.CHEM_BURNER.get());
        helper.setBlock(burnerAt.above(), ModBlocks.CRUCIBLE.get());
        helper.getBlockEntity(burnerAt.above(), CrucibleBlockEntity.class).fillWater(CrucibleBlockEntity.BUCKET, false);
        ChemCarboyBlockEntity far = carboy(helper, burnerAt.east(6), Brew.FUEL, 5);
        helper.runAfterDelay(50, () -> {
            helper.assertFalse(ChemBurnerBlock.isLit(helper.getBlockState(burnerAt)), "six blocks is too far");
            helper.assertTrue(far.contents().doses() == 5, "nothing was drawn");
            helper.succeed();
        });
    }

    @HexTest(maxTicks = 200)
    public static void theBrazierStillAndTankDrawTheirBrews(GameTestHelper helper) {
        BlockPos brazierAt = new BlockPos(1, 1, 1);
        BlockPos stillAt = new BlockPos(3, 1, 1);
        BlockPos tankAt = new BlockPos(5, 1, 1);
        helper.setBlock(brazierAt, ModBlocks.PINK_BRAZIER.get());
        helper.setBlock(stillAt, ModBlocks.CHEM_STILL.get());
        helper.setBlock(tankAt, ModBlocks.CHEM_TANK.get());
        ChemCarboyBlockEntity raw = carboy(helper, new BlockPos(3, 1, 3), Brew.RAW_SHIMMER, 20);
        PinkBrazierBlockEntity brazier = helper.getBlockEntity(brazierAt, PinkBrazierBlockEntity.class);
        ChemStillBlockEntity still = helper.getBlockEntity(stillAt, ChemStillBlockEntity.class);
        ChemTankBlockEntity tank = helper.getBlockEntity(tankAt, ChemTankBlockEntity.class);
        helper.assertTrue(still.feed(new ItemStack(ModItems.CRYSTAL_POD.get())) == 1, "the still gets its pod");
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(brazierAt).getValue(PinkBrazierBlock.LIT), "the brazier burns raw shimmer from the carboy");
            helper.assertTrue(ChemVialItem.brew(still.input()) == Brew.RAW_SHIMMER && still.input().getCount() == 2, "the still topped up its raw shimmer");
            helper.assertTrue(tank.contents().fuel() == ChemFuel.RAW_SHIMMER && tank.contents().vials() >= 2, "the tank fills itself");
            helper.assertTrue(raw.contents().doses() < 20 - 3, "and the doses came out of the carboy");
        });
        helper.assertTrue(brazier != null && GearConfig.TANK_VIALS.get() > 2, "the tank has room for two vials");
    }

    // ---- Vial Rack --------------------------------------------------------------------------------------------------------

    private static Vec3 model(BlockPos at, Direction facing, float x, float y, float z) {
        double mx = x / 16 - 0.5;
        double mz = z / 16 - 0.5;
        double wx = switch (facing) {
            case EAST -> -mz;
            case SOUTH -> -mx;
            case WEST -> mz;
            default -> mx;
        };
        double wz = switch (facing) {
            case EAST -> mx;
            case SOUTH -> -mz;
            case WEST -> -mx;
            default -> mz;
        };
        return new Vec3(at.getX() + 0.5 + wx, at.getY() + y / 16, at.getZ() + 0.5 + wz);
    }

    @HexTest
    public static void everyPlaceOfARackIsFoundFromEverySide(GameTestHelper helper) {
        BlockPos at = BlockPos.ZERO;
        float[] columns = {13, 10, 6, 3};
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState standing = ModBlocks.VIAL_RACK.get().defaultBlockState().setValue(VialRackBlock.FACING, facing);
            BlockState wall = standing.setValue(VialRackBlock.WALL, true);
            for (int column = 0; column < 4; column++) {
                helper.assertTrue(VialRackBlock.slotAt(standing, at, model(at, facing, columns[column], 4, 4), facing) == column,
                        "front row of a standing rack facing " + facing);
                helper.assertTrue(VialRackBlock.slotAt(standing, at, model(at, facing, columns[column], 9, 9), facing) == 4 + column,
                        "back row of a standing rack facing " + facing);
                helper.assertTrue(VialRackBlock.slotAt(standing, at, model(at, facing, columns[column], 5, 10), Direction.UP) == 4 + column,
                        "the step's top is the back row, facing " + facing);
                helper.assertTrue(VialRackBlock.slotAt(wall, at, model(at, facing, columns[column], 4, 12), facing) == column,
                        "lower shelf of a wall rack facing " + facing);
                helper.assertTrue(VialRackBlock.slotAt(wall, at, model(at, facing, columns[column], 12, 12), facing) == 4 + column,
                        "upper shelf of a wall rack facing " + facing);
            }
        }
        helper.succeed();
    }

    @HexTest
    public static void aRackTakesAndGivesVialsPlaceByPlace(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.VIAL_RACK.get());
        VialRackBlockEntity rack = helper.getBlockEntity(AT, VialRackBlockEntity.class);
        BlockPos at = helper.absolutePos(AT);
        ServerPlayer player = survivor(helper);

        player.setItemInHand(InteractionHand.MAIN_HAND, ChemVialItem.of(Brew.ACID, 2));
        click(helper, player, AT, model(at, Direction.NORTH, 13, 4, 4), Direction.NORTH);
        helper.assertTrue(ChemVialItem.brew(rack.getItem(0)) == Brew.ACID && rack.getItem(0).getCount() == 1, "one vial stands in the place aimed at");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "one left the hand");
        click(helper, player, AT, model(at, Direction.NORTH, 3, 9, 9), Direction.NORTH);
        helper.assertTrue(ChemVialItem.brew(rack.getItem(7)) == Brew.ACID, "the other goes to the far end of the back row");
        helper.assertTrue(rack.filled() == 2, "two vials stand");

        BlockState state = helper.getLevel().getBlockState(at);
        helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), at, Direction.NORTH) == 4, "a comparator counts them");

        helper.assertTrue(player.getMainHandItem().isEmpty(), "the hand is empty");
        click(helper, player, AT, model(at, Direction.NORTH, 13, 4, 4), Direction.NORTH);
        helper.assertTrue(rack.getItem(0).isEmpty() && count(player, ModItems.CHEM_VIAL.get()) == 1, "an empty hand takes the vial back");

        helper.assertFalse(rack.canPlaceItem(1, new ItemStack(Items.DIRT)), "only vials stand here");
        helper.assertFalse(rack.canPlaceItem(1, new ItemStack(ModItems.CHEM_VIAL.get())), "and only filled ones");
        helper.assertTrue(rack.canPlaceItem(1, ChemVialItem.of(Brew.GROWTH)), "of any brew");
        helper.assertFalse(rack.canPlaceItem(7, ChemVialItem.of(Brew.GROWTH)), "one to a place");

        AABB around = new AABB(at).inflate(3);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).forEach(Entity::discard);
        helper.getLevel().destroyBlock(at, true);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> ChemVialItem.brew(e.getItem()) == Brew.ACID).size() == 1,
                "a broken rack drops its vials");
        helper.succeed();
    }

    @HexTest(maxTicks = 40)
    public static void aWallRackFallsWithItsWall(GameTestHelper helper) {
        BlockPos wall = AT.south();
        helper.setBlock(wall, Blocks.STONE);
        helper.setBlock(AT, ModBlocks.VIAL_RACK.get().defaultBlockState().setValue(VialRackBlock.WALL, true));
        helper.getBlockEntity(AT, VialRackBlockEntity.class).setItem(0, ChemVialItem.of(Brew.GLOW));
        helper.runAfterDelay(2, () -> {
            helper.assertBlockPresent(ModBlocks.VIAL_RACK.get(), AT);
            helper.setBlock(wall, Blocks.AIR);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertBlockPresent(Blocks.AIR, AT);
            AABB around = new AABB(helper.absolutePos(AT)).inflate(3);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> ChemVialItem.brew(e.getItem()) == Brew.GLOW).size() == 1,
                    "its vial falls with it");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(ModItems.VIAL_RACK.get())).size() == 1,
                    "and so does the rack");
            helper.succeed();
        });
    }

    /** The placement rule: the neck needs the cell above, so a carboy under a ceiling is refused. */
    @HexTest
    public static void aCarboyNeedsTheCellAbove(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.STONE);
        helper.setBlock(AT.above(), Blocks.STONE);
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHEM_CARBOY.get()));
        click(helper, player, AT.below(), Vec3.atCenterOf(helper.absolutePos(AT.below())).add(0, 0.5, 0), Direction.UP);
        helper.assertFalse(helper.getBlockState(AT).getBlock() instanceof ChemCarboyBlock, "no room for the neck");
        helper.setBlock(AT.above(), Blocks.AIR);
        click(helper, player, AT.below(), Vec3.atCenterOf(helper.absolutePos(AT.below())).add(0, 0.5, 0), Direction.UP);
        helper.assertTrue(helper.getBlockState(AT).getBlock() instanceof ChemCarboyBlock, "with room it stands");
        helper.succeed();
    }
}
