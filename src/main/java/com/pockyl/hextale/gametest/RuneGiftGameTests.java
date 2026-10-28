package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.HexFurnaceBlockEntity;
import com.pockyl.hextale.farm.Felling;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.runework.FurnaceRune;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.RuneHolder;
import com.pockyl.hextale.runework.ToolEffects;
import com.pockyl.hextale.runework.ToolRune;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The runes every device and tool takes: each holder knows its runes, the furnace melts old tools back into ingots, the
 * axe fells a whole tree and the shovel digs an area, the sawmill takes bee nests whole; rune signs stay sparse.
 */
public final class RuneGiftGameTests {
    private static final BlockPos AT = new BlockPos(0, 1, 0);

    private RuneGiftGameTests() {
    }

    /** Every holder is found from its items, takes each codex rune once, and every rune can tell its number and cost. */
    @HexTest
    public static void everyHolderKnowsItsRunes(GameTestHelper helper) {
        for (RuneHolder holder : RuneHolder.values()) {
            for (ItemStack item : holder.items()) {
                helper.assertTrue(RuneHolder.of(item) == holder, item + " is a " + holder);
            }
            Set<String> seen = new HashSet<>();
            for (HolderRune rune : holder.holderRunes()) {
                helper.assertTrue(seen.add(rune.rune().key()), holder + " takes " + rune.rune().key() + " once");
                for (int strength = 1; strength <= 4; strength++) {
                    helper.assertTrue(rune.amountText(strength) != null, rune + " has a number at " + strength);
                }
                helper.assertFalse(rune.cost(1).isEmpty(), rune + " costs essence");
            }
            // Numbers only (reach, pace, load) are the base layer; at least three runes change what the holder gives.
            long behaviour = holder.holderRunes().stream().filter(rune -> !java.util.Set.of("SHIFT", "MOTOR", "LIFT").contains(rune.name())).count();
            helper.assertTrue(behaviour >= 3, holder + " has " + behaviour + " runes that change what it gives");
            helper.assertFalse(holder.takes(com.pockyl.hextale.rune.ComplexRunes.byKey("water_light")), holder + " takes no Lens");
        }
        helper.assertTrue(ToolEffects.saplingOf(Blocks.OAK_LOG.defaultBlockState()) == Items.OAK_SAPLING, "oak logs grow from oak saplings");
        helper.assertTrue(ToolEffects.saplingOf(Blocks.CRIMSON_STEM.defaultBlockState()) == Items.CRIMSON_FUNGUS, "stems from fungi");
        helper.assertTrue(ToolEffects.saplingOf(Blocks.MANGROVE_LOG.defaultBlockState()) == Items.MANGROVE_PROPAGULE, "mangroves from propagules");
        helper.assertTrue(ToolEffects.saplingOf(ModBlocks.MAGIC_LOG.get().defaultBlockState()) == ModItems.MAGIC_SAPLING.get(),
                "the magic tree from its sprout");
        helper.succeed();
    }

    /** A proc firing every tick shows one rune sign, not a sign per tick; another rune or place still shows one. */
    @HexTest
    public static void runeProcSignsAreThrottled(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // The middle of a 4-block cell (the throttle counts cells), so a step up stays in it.
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        Vec3 at = new Vec3((origin.getX() >> 2 << 2) + 1.5, (origin.getY() >> 2 << 2) + 1.5, (origin.getZ() >> 2 << 2) + 1.5);
        ComplexRune haste = ToolRune.HASTE.rune();
        // The cell rule alone (no player near: a mock player of another test may stand close by).
        helper.assertTrue(ToolEffects.procSignDue(level, at, haste, null), "the first proc sounds");
        helper.assertFalse(ToolEffects.procSignDue(level, at, haste, null), "the same proc a tick later stays quiet");
        helper.assertTrue(ToolEffects.procSignDue(level, at, ToolRune.MENDING.rune(), null), "another rune there sounds");
        helper.assertFalse(ToolEffects.procSignDue(level, at.add(0, 1, 0), haste, null), "nor right beside it (a miner's next block)");
        helper.assertTrue(ToolEffects.procSignDue(level, at.add(8, 0, 0), haste, null), "the same rune elsewhere sounds");
        // A player's budget: one sign near them, whatever the rune and place, until the gap passes.
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        Vec3 far = at.add(0, 0, 16);
        helper.assertTrue(ToolEffects.procSignDue(level, far, haste, player), "a sign near the player");
        helper.assertFalse(ToolEffects.procSignDue(level, far.add(8, 0, 0), ToolRune.SPOILS.rune(), player),
                "a second sign near the same player waits, other rune and place or not");
        helper.runAfterDelay(Math.max(ToolEffects.PROC_SIGN_GAP, ToolEffects.PLAYER_SIGN_GAP) + 1, () -> {
            helper.assertTrue(ToolEffects.procSignDue(level, at, haste, null), "after the gap it sounds again");
            helper.assertTrue(ToolEffects.procSignDue(level, far.add(8, 0, 0), ToolRune.SPOILS.rune(), player), "and the player's budget refills");
            helper.succeed();
        });
    }

    /** Reforging: an old iron sword melts into ingots (two at two stars), not a nugget. */
    @HexTest(maxTicks = 400)
    public static void theFurnaceReforgesOldTools(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.HEX_FURNACE.get());
        HexFurnaceBlockEntity furnace = helper.getBlockEntity(AT, HexFurnaceBlockEntity.class);
        furnace.setRunes(PickaxeRunes.EMPTY.with(FurnaceRune.REFORGING.rune(), 2));
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.setDamageValue(100);
        furnace.setItem(0, sword);
        furnace.setItem(HexFurnaceBlockEntity.FUEL, new ItemStack(Items.COAL));
        helper.succeedWhen(() -> {
            ItemStack out = furnace.getItem(HexFurnaceBlockEntity.OUTPUT);
            helper.assertTrue(out.is(Items.IRON_INGOT) && out.getCount() == 2, "the sword came back as two ingots, got " + out);
        });
    }

    /** A hextech tool with these runes at one star. */
    private static ItemStack tool(ItemStack stack, ToolRune... runes) {
        PickaxeRunes on = PickaxeRunes.EMPTY;
        for (ToolRune rune : runes) {
            on = on.with(rune.rune(), 1);
        }
        PickaxeRunes.set(stack, on);
        return stack;
    }

    /** Breaks the block as a player standing over it, looking down, with the tool in hand (drops through its runes). */
    @SuppressWarnings("removal")
    private static void dig(GameTestHelper helper, ItemStack tool, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(abs);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(abs.getX() + 0.5, abs.getY() + 1, abs.getZ() + 0.5, 0, 90);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        Block.dropResources(state, helper.getLevel(), abs, null, player, tool);
        helper.getLevel().removeBlock(abs, false);
    }

    /** Gravity: cutting a tree's lowest log brings down the logs above and the crown; a log wall without leaves stays. */
    @HexTest(maxTicks = 40)
    public static void theAxeFellsAWholeTree(GameTestHelper helper) {
        BlockPos foot = AT.offset(1, 0, 1);
        helper.setBlock(foot.below(), Blocks.DIRT);
        for (int y = 0; y < 4; y++) {
            helper.setBlock(foot.above(y), Blocks.OAK_LOG);
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            helper.setBlock(foot.above(3).relative(side), Blocks.OAK_LEAVES);
        }
        helper.setBlock(foot.above(4), Blocks.OAK_LEAVES);
        dig(helper, tool(new ItemStack(ModItems.HEXTECH_AXE.get()), ToolRune.GRAVITY), foot);
        for (int y = 1; y < 4; y++) {
            helper.assertBlockPresent(Blocks.AIR, foot.above(y));
        }
        helper.assertBlockPresent(Blocks.AIR, foot.above(4));

        BlockPos wall = AT.offset(-2, 0, 1);
        for (int y = 0; y < 3; y++) {
            helper.setBlock(wall.above(y), Blocks.OAK_LOG);
        }
        dig(helper, tool(new ItemStack(ModItems.HEXTECH_AXE.get()), ToolRune.GRAVITY), wall);
        helper.assertBlockPresent(Blocks.OAK_LOG, wall.above());
        helper.succeed();
    }

    /** Erosion: the shovel digs the soil around the struck block, never the stone among it. */
    @HexTest(maxTicks = 40)
    public static void theShovelDigsAnArea(GameTestHelper helper) {
        BlockPos centre = AT.offset(0, 0, -3);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                helper.setBlock(centre.offset(x, 0, z), x == 1 && z == 1 ? Blocks.STONE : Blocks.DIRT);
                helper.setBlock(centre.offset(x, 1, z), Blocks.AIR);
            }
        }
        dig(helper, tool(new ItemStack(ModItems.HEXTECH_SHOVEL.get()), ToolRune.EROSION), centre);
        helper.assertBlockPresent(Blocks.AIR, centre.offset(-1, 0, -1));
        helper.assertBlockPresent(Blocks.AIR, centre.offset(1, 0, 0));
        helper.assertBlockPresent(Blocks.STONE, centre.offset(1, 0, 1));
        helper.succeed();
    }

    /** Hush: a bee nest on a felled tree's log comes down whole. */
    @HexTest(maxTicks = 40)
    public static void theSawmillTakesBeeNestsWhole(GameTestHelper helper) {
        BlockPos log = AT.offset(3, 0, 3);
        BlockPos nest = log.east();
        helper.setBlock(log, Blocks.OAK_LOG);
        helper.setBlock(nest, Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 5));
        BlockPos abs = helper.absolutePos(log);
        Felling.Tree tree = new Felling.Tree(abs, List.of(abs), List.of(abs), List.of());
        List<ItemStack> nests = Felling.takeNests(helper.getLevel(), tree);
        helper.assertTrue(nests.size() == 1 && nests.getFirst().is(Items.BEE_NEST), "the nest came down as an item, got " + nests);
        helper.assertBlockPresent(Blocks.AIR, nest);
        helper.succeed();
    }
}
