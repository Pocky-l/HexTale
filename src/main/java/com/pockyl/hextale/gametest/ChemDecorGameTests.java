package com.pockyl.hextale.gametest;

import com.pockyl.hextale.block.ChemLumenBlock;
import com.pockyl.hextale.block.GasCylinderBlock;
import com.pockyl.hextale.block.RustedCatwalkBlock;
import com.pockyl.hextale.block.RustedRailingBlock;
import com.pockyl.hextale.block.ValveWheelBlock;
import com.pockyl.hextale.block.ValveWheelBlockEntity;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchTopics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** The Chem Lumen and the decor set: how each is placed, what it hangs on and what it gives back. */
public final class ChemDecorGameTests {
    private static final BlockPos AT = new BlockPos(2, 2, 2);

    private ChemDecorGameTests() {
    }

    /** Uses the item in the player's hand on {@code side} of the block at {@code pos}, at {@code hit} within that block. */
    private static void place(GameTestHelper helper, ServerPlayer player, Item item, BlockPos pos, Direction side, Vec3 hit) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        BlockPos absolute = helper.absolutePos(pos);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atLowerCornerOf(absolute).add(hit), side, absolute, false));
    }

    /** On the top face of the block under {@code pos}, aimed at (x, z) of the cell. */
    private static void placeOnTop(GameTestHelper helper, ServerPlayer player, Item item, BlockPos pos, double x, double z) {
        place(helper, player, item, pos.below(), Direction.UP, new Vec3(x, 1, z));
    }

    private static int dropped(GameTestHelper helper, BlockPos pos, Item item) {
        AABB around = new AABB(helper.absolutePos(pos)).inflate(2);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(item)).stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static void clearDrops(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(3)).forEach(Entity::discard);
    }

    /** The lamp takes the face clicked, shines at 15 with nothing to burn, and falls with what holds it. */
    @HexTest
    public static void theLumenHangsWhereItIsPutAndShines(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        helper.setBlock(AT.below(), Blocks.STONE);
        placeOnTop(helper, player, ModItems.CHEM_LUMEN.get(), AT, 0.5, 0.5);
        BlockState floor = helper.getBlockState(AT);
        helper.assertTrue(floor.is(ModBlocks.CHEM_LUMEN.get()) && floor.getValue(ChemLumenBlock.FACE) == AttachFace.FLOOR, "stands on a floor");
        helper.assertTrue(floor.getLightEmission() == 15, "shines at 15");
        helper.assertTrue(!floor.hasBlockEntity(), "nothing to fuel");

        BlockPos wall = AT.east(2);
        helper.setBlock(wall, Blocks.STONE);
        place(helper, player, ModItems.CHEM_LUMEN.get(), wall, Direction.WEST, new Vec3(0, 0.5, 0.5));
        BlockState hung = helper.getBlockState(wall.west());
        helper.assertTrue(hung.is(ModBlocks.CHEM_LUMEN.get()) && hung.getValue(ChemLumenBlock.FACE) == AttachFace.WALL
                && hung.getValue(ChemLumenBlock.FACING) == Direction.WEST, "hangs on the wall, its back to it");

        BlockPos ceiling = AT.above(3);
        helper.setBlock(ceiling, Blocks.STONE);
        place(helper, player, ModItems.CHEM_LUMEN.get(), ceiling, Direction.DOWN, new Vec3(0.5, 0, 0.5));
        helper.assertTrue(helper.getBlockState(ceiling.below()).getValue(ChemLumenBlock.FACE) == AttachFace.CEILING, "hangs from a ceiling");

        clearDrops(helper, AT);
        helper.setBlock(wall, Blocks.AIR);
        helper.assertTrue(helper.getBlockState(wall.west()).isAir(), "falls with its wall");
        helper.setBlock(AT.below(), Blocks.AIR);
        helper.assertTrue(helper.getBlockState(AT).isAir(), "falls with its floor");
        helper.succeed();
    }

    /** Sneaking lays a cylinder down with its valve to the player; each band is its own item and block. */
    @HexTest
    public static void gasCylindersStandOrLie(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        helper.setBlock(AT.below(), Blocks.STONE);
        helper.setBlock(AT.below().east(), Blocks.STONE);
        placeOnTop(helper, player, ModItems.GAS_CYLINDER_RUST.get(), AT, 0.5, 0.5);
        helper.assertTrue(helper.getBlockState(AT).is(ModBlocks.GAS_CYLINDER_RUST.get()), "the rust band is its own block");
        helper.assertFalse(helper.getBlockState(AT).getValue(GasCylinderBlock.LYING), "stands by default");

        player.setShiftKeyDown(true);
        placeOnTop(helper, player, ModItems.GAS_CYLINDER_SHIMMER.get(), AT.east(), 0.5, 0.5);
        player.setShiftKeyDown(false);
        BlockState lying = helper.getBlockState(AT.east());
        helper.assertTrue(lying.is(ModBlocks.GAS_CYLINDER_SHIMMER.get()) && lying.getValue(GasCylinderBlock.LYING), "sneaking lays it down");
        helper.assertTrue(lying.getValue(GasCylinderBlock.FACING) == player.getDirection().getOpposite(), "the valve end looks at the player");
        helper.succeed();
    }

    /** A click gives the wheel a quarter turn, the count outlives a reload, and it powers nothing. */
    @HexTest
    public static void theValveWheelTurnsAndDoesNothingElse(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        BlockPos wall = AT.east();
        helper.setBlock(wall, Blocks.STONE);
        place(helper, player, ModItems.VALVE_WHEEL.get(), wall, Direction.WEST, new Vec3(0, 0.5, 0.5));
        BlockState state = helper.getBlockState(AT);
        helper.assertTrue(state.is(ModBlocks.VALVE_WHEEL.get()) && state.getValue(ValveWheelBlock.FACING) == Direction.WEST, "goes on the wall clicked");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockPos absolute = helper.absolutePos(AT);
        for (int i = 0; i < 2; i++) {
            player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(absolute), Direction.WEST, absolute, false));
        }
        ValveWheelBlockEntity wheel = helper.<ValveWheelBlockEntity>getBlockEntity(AT);
        helper.assertTrue(wheel.turns() == 2, "two clicks, two quarter turns: " + wheel.turns());
        helper.assertTrue(wheel.angle(0) == 180, "half way round: " + wheel.angle(0));

        ValveWheelBlockEntity loaded = new ValveWheelBlockEntity(absolute, state);
        var registries = helper.getLevel().registryAccess();
        loaded.loadCustomOnly(wheel.saveCustomOnly(registries), registries);
        helper.assertTrue(loaded.turns() == 2, "the turns are saved");
        helper.assertTrue(helper.getLevel().getSignal(absolute.west(), Direction.WEST) == 0 && !state.isSignalSource(), "it gives no signal");

        clearDrops(helper, AT);
        helper.setBlock(wall, Blocks.AIR);
        helper.assertTrue(helper.getBlockState(AT).isAir(), "falls with its wall");
        helper.succeed();
    }

    /** The slab rule: a deck on a floor lies low, one built out from a wall's upper half high; the bars follow the look. */
    @HexTest
    public static void catwalksFollowTheSlabRule(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        helper.setBlock(AT.below(), Blocks.STONE);
        placeOnTop(helper, player, ModItems.RUSTED_CATWALK.get(), AT, 0.5, 0.5);
        helper.assertTrue(helper.getBlockState(AT).getValue(RustedCatwalkBlock.HALF) == Half.BOTTOM, "lower half on a floor");
        helper.assertTrue(helper.getBlockState(AT).getValue(RustedCatwalkBlock.AXIS) == player.getDirection().getAxis(), "bars run along the look");

        BlockPos wall = AT.above(2);
        helper.setBlock(wall, Blocks.STONE);
        place(helper, player, ModItems.RUSTED_CATWALK.get(), wall, Direction.WEST, new Vec3(0, 0.75, 0.5));
        helper.assertTrue(helper.getBlockState(wall.west()).getValue(RustedCatwalkBlock.HALF) == Half.TOP, "upper half from a wall's upper half");
        helper.assertTrue(helper.getBlockState(wall.west()).getCollisionShape(helper.getLevel(), helper.absolutePos(wall.west())).max(Direction.Axis.Y) == 1,
                "the upper deck is flush with the cell's top");
        helper.succeed();
    }

    /** A free rail end always has a post, a run has none inside, and a corner is the corner piece's work. */
    @HexTest
    public static void railingPostsCloseEveryFreeEnd(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        BlockPos east = AT.east();
        helper.setBlock(AT.below(), Blocks.STONE);
        helper.setBlock(east.below(), Blocks.STONE);
        Item railing = ModItems.RUSTED_RAILING.get();
        placeOnTop(helper, player, railing, AT, 0.5, 0.1);
        BlockState alone = helper.getBlockState(AT);
        helper.assertTrue(alone.getValue(RustedRailingBlock.NORTH) && !alone.getValue(RustedRailingBlock.EAST), "the edge nearest the click");
        helper.assertTrue(alone.getValue(RustedRailingBlock.POST_NE) && alone.getValue(RustedRailingBlock.POST_NW), "both free ends have a post");

        placeOnTop(helper, player, railing, east, 0.5, 0.1);
        BlockState west = helper.getBlockState(AT);
        BlockState next = helper.getBlockState(east);
        helper.assertFalse(west.getValue(RustedRailingBlock.POST_NE), "the run goes on: the neighbour takes away the post");
        helper.assertFalse(next.getValue(RustedRailingBlock.POST_NW), "and it has none on that side");
        helper.assertTrue(west.getValue(RustedRailingBlock.POST_NW) && next.getValue(RustedRailingBlock.POST_NE), "the far ends keep theirs");

        placeOnTop(helper, player, railing, east, 0.9, 0.5);
        BlockState corner = helper.getBlockState(east);
        helper.assertTrue(corner.getValue(RustedRailingBlock.NORTH) && corner.getValue(RustedRailingBlock.EAST), "a second railing adds an edge");
        helper.assertFalse(corner.getValue(RustedRailingBlock.POST_NE), "the corner piece closes that corner");
        helper.assertTrue(corner.getValue(RustedRailingBlock.POST_SE), "the new rail's free end has its post");

        helper.assertTrue(corner.getCollisionShape(helper.getLevel(), helper.absolutePos(east)).max(Direction.Axis.Y) == 1.5, "as tall as a fence to walk into");
        helper.assertTrue(corner.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90).getValue(RustedRailingBlock.SOUTH), "turns with a structure");

        clearDrops(helper, AT);
        helper.getLevel().destroyBlock(helper.absolutePos(east), true);
        helper.assertTrue(dropped(helper, east, railing) == 2, "one railing per edge: " + dropped(helper, east, railing));
        BlockState left = helper.getBlockState(AT);
        helper.assertTrue(left.getValue(RustedRailingBlock.POST_NE), "the rail left behind gets its post back");
        helper.succeed();
    }

    /** Over a deck in the lower half a rail stands on the deck, and a step between the two heights has a post on either end. */
    @HexTest
    public static void aRailingStandsOnALowDeck(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        BlockPos east = AT.east();
        BlockState lowDeck = ModBlocks.RUSTED_CATWALK.get().defaultBlockState().setValue(RustedCatwalkBlock.HALF, Half.BOTTOM);
        BlockState highDeck = lowDeck.setValue(RustedCatwalkBlock.HALF, Half.TOP);
        helper.setBlock(AT.below(), lowDeck);
        helper.setBlock(east.below(), highDeck);
        Item railing = ModItems.RUSTED_RAILING.get();
        placeOnTop(helper, player, railing, AT, 0.5, 0.1);
        placeOnTop(helper, player, railing, east, 0.5, 0.1);
        BlockState low = helper.getBlockState(AT);
        BlockState high = helper.getBlockState(east);
        helper.assertTrue(low.getValue(RustedRailingBlock.LOW), "over the low deck the rail is low");
        helper.assertFalse(high.getValue(RustedRailingBlock.LOW), "a deck in the upper half needs no low rail");
        helper.assertTrue(low.getValue(RustedRailingBlock.POST_NE) && high.getValue(RustedRailingBlock.POST_NW), "the step has a post on either end");

        var at = helper.absolutePos(AT);
        helper.assertTrue(low.getCollisionShape(helper.getLevel(), at).max(Direction.Axis.Y) == 1, "as high over the deck as ever");
        helper.assertTrue(low.getShape(helper.getLevel(), at).min(Direction.Axis.Y) == -0.5, "its outline reaches down to the deck");
        helper.assertTrue(low.getShape(helper.getLevel(), at).max(Direction.Axis.Y) <= 0.5, "and ends 8 px lower");

        helper.setBlock(AT.below(), highDeck);
        BlockState raised = helper.getBlockState(AT);
        helper.assertFalse(raised.getValue(RustedRailingBlock.LOW), "the deck moved up: so does the rail");
        helper.assertFalse(raised.getValue(RustedRailingBlock.POST_NE), "and the run goes on without posts");
        helper.assertFalse(helper.getBlockState(east).getValue(RustedRailingBlock.POST_NW), "on either side");
        helper.assertTrue(raised.getCollisionShape(helper.getLevel(), at).max(Direction.Axis.Y) == 1.5, "as tall as a fence again");
        helper.setBlock(AT.below(), lowDeck);
        helper.assertTrue(helper.getBlockState(AT).getValue(RustedRailingBlock.LOW), "and back");
        helper.succeed();
    }

    /** A railing in hand never replaces an edge the cell already has. */
    @HexTest
    public static void aRailingAddsOnlyMissingEdges(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        helper.setBlock(AT.below(), Blocks.STONE);
        placeOnTop(helper, player, ModItems.RUSTED_RAILING.get(), AT, 0.5, 0.1);
        BlockState before = helper.getBlockState(AT);
        placeOnTop(helper, player, ModItems.RUSTED_RAILING.get(), AT, 0.5, 0.15);
        helper.assertTrue(helper.getBlockState(AT) == before, "the same edge twice changes nothing");
        helper.succeed();
    }

    /** The rest of the set: the ladder climbs, every piece asks for its research, and all of it can be mined by pickaxe. */
    @HexTest
    public static void theDecorSetIsResearchedAndMined(GameTestHelper helper) {
        helper.assertTrue(ModBlocks.RUSTED_LADDER.get().defaultBlockState().is(BlockTags.CLIMBABLE), "the ladder climbs");
        helper.assertTrue(ResearchTopics.of(ModItems.CHEM_LUMEN.get()) == ResearchTopics.CHEM_LUMEN, "the lamp has its research");
        for (Item item : List.of(ModItems.GAS_CYLINDER.get(), ModItems.GAS_CYLINDER_RUST.get(), ModItems.GAS_CYLINDER_SHIMMER.get(), ModItems.VALVE_WHEEL.get(),
                ModItems.RUSTED_CATWALK.get(), ModItems.RUSTED_RAILING.get(), ModItems.HAZARD_PLATING.get(), ModItems.RUSTED_LADDER.get())) {
            helper.assertTrue(ResearchTopics.of(item) == ResearchTopics.CHEM_FITTINGS, item + " opens with the fittings");
            helper.assertTrue(Block.byItem(item).defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE), item + " is mined by pickaxe");
        }
        helper.succeed();
    }
}
