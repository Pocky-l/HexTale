package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import com.pockyl.hextale.Config;
import com.pockyl.hextale.entity.DustCharge;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.runework.HextechPickaxe;
import com.pockyl.hextale.runework.PickaxeRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.thevoid.Thinning;
import com.pockyl.hextale.world.WorldEdits;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Claims on a server: a stand-in claim mod (a listener refusing every break and placement at the protected positions)
 * must stop the mod's own block changes - a Dust Charge, a pickaxe's area mining, the blight - and the config keeps the
 * blight off the blocks altogether.
 */
public final class ProtectionGameTests {
    /** Positions the stand-in claim mod protects; empty outside these tests. */
    private static final Set<BlockPos> PROTECTED = new HashSet<>();
    /** Who the last refused change was asked for. */
    private static UUID refusedFor;
    private static boolean listening;

    private ProtectionGameTests() {
    }

    private static void listen() {
        if (listening) {
            return;
        }
        listening = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BreakBlockEvent.class, event -> {
            if (PROTECTED.contains(event.getPos())) {
                refusedFor = event.getPlayer().getUUID();
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityPlaceEvent.class, event -> {
            if (PROTECTED.contains(event.getPos())) {
                refusedFor = event.getEntity() == null ? null : event.getEntity().getUUID();
                event.setCanceled(true);
            }
        });
    }

    /** Runs {@code body} with {@code positions} protected; the protection is lifted whatever happens. */
    private static void protecting(List<BlockPos> positions, Runnable body) {
        listen();
        refusedFor = null;
        PROTECTED.addAll(positions);
        try {
            body.run();
        } finally {
            PROTECTED.clear();
        }
    }

    @HexTest(padding = 4)
    public static void aClaimKeepsItsRockFromADustCharge(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(2, 3, 2));
        BlockPos claimed = centre.east();
        BlockPos free = centre.west();
        level.setBlockAndUpdate(claimed, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(free, Blocks.STONE.defaultBlockState());
        ServerPlayer thrower = helper.makeMockServerPlayerInLevel();
        DustCharge charge = new DustCharge(ModEntities.DUST_CHARGE.get(), level);
        List<BlockPos> cracked = new ArrayList<>();
        protecting(List.of(claimed), () -> cracked.addAll(DustCharge.burst(level, Vec3.atCenterOf(centre), charge, thrower)));
        helper.assertTrue(level.getBlockState(claimed).is(Blocks.STONE), "the claimed stone stays");
        helper.assertTrue(cracked.contains(free) && !cracked.contains(claimed), "only the free stone cracks: " + cracked);
        helper.assertTrue(thrower.getUUID().equals(refusedFor), "the thrower was asked for, got " + refusedFor);
        level.getEntitiesOfClass(ItemEntity.class, new AABB(centre).inflate(4)).forEach(ItemEntity::discard);
        level.setBlockAndUpdate(claimed, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /** Drill takes the 3x3 around the broken block - all but the claimed neighbours. */
    @HexTest
    public static void aClaimStopsAreaMining(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        BlockPos claimedA = new BlockPos(0, 1, 0);
        BlockPos claimedB = new BlockPos(2, 1, 1);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        protecting(List.of(helper.absolutePos(claimedA), helper.absolutePos(claimedB)),
                () -> mine(helper, player, pickaxe(PickaxeRune.DRILL), new BlockPos(1, 1, 1)));
        helper.assertBlockPresent(Blocks.STONE, claimedA);
        helper.assertBlockPresent(Blocks.STONE, claimedB);
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 1, 2));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(0, 1, 1));
        helper.assertTrue(player.getUUID().equals(refusedFor), "the miner was asked for, got " + refusedFor);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(4))
                .forEach(ItemEntity::discard);
        helper.succeed();
    }

    @HexTest
    public static void aClaimKeepsTheBlightOut(GameTestHelper helper) {
        BlockPos grass = new BlockPos(0, 1, 0);
        BlockPos stone = new BlockPos(2, 1, 0);
        helper.setBlock(grass, Blocks.GRASS_BLOCK);
        helper.setBlock(grass.above(), Blocks.SHORT_GRASS);
        helper.setBlock(stone, Blocks.STONE);
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(grass);
        BlockPos rock = helper.absolutePos(stone);
        protecting(List.of(at), () -> {
            Thinning.blightColumn(level, at.getX(), at.getZ(), level.getRandom());
            Thinning.blightColumn(level, rock.getX(), rock.getZ(), level.getRandom());
        });
        helper.assertBlockPresent(Blocks.GRASS_BLOCK, grass);
        helper.assertBlockPresent(Blocks.SHORT_GRASS, grass.above());
        helper.assertBlockPresent(ModBlocks.VOID_ROCK.get(), stone);
        helper.assertTrue(WorldEdits.PROFILE.id().equals(refusedFor), "the world's own [HexTale] was asked for, got " + refusedFor);
        helper.succeed();
    }

    @HexTest
    public static void theBlightConfigKeepsTheGround(GameTestHelper helper) {
        BlockPos grass = new BlockPos(0, 1, 0);
        helper.setBlock(grass, Blocks.GRASS_BLOCK);
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(grass);
        Config.BLIGHT_CHANGES_BLOCKS.set(false);
        try {
            Thinning.blightColumn(level, at.getX(), at.getZ(), level.getRandom());
        } finally {
            Config.BLIGHT_CHANGES_BLOCKS.set(true);
        }
        helper.assertBlockPresent(Blocks.GRASS_BLOCK, grass);
        Thinning.blightColumn(level, at.getX(), at.getZ(), level.getRandom());
        helper.assertBlockPresent(ModBlocks.VOID_SOIL.get(), grass);
        helper.succeed();
    }

    /** A placed device remembers its placer, and acts in their name; an unknown one acts as the world's own. */
    @HexTest
    public static void devicesActInTheNameOfWhoPlacedThem(GameTestHelper helper) {
        BlockPos reaper = new BlockPos(0, 1, 0);
        helper.setBlock(reaper, ModBlocks.WHIRL_REAPER.get());
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(reaper);
        var device = level.getBlockEntity(at);
        helper.assertTrue(device != null, "the reaper has a block entity");
        helper.assertTrue(WorldEdits.PROFILE.id().equals(WorldEdits.owner(level, device).getUUID()),
                "nobody placed it yet: the world's own");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, at);
        NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(snapshot, level.getBlockState(at.below()), player));
        Player owner = WorldEdits.owner(level, device);
        helper.assertTrue(player.getUUID().equals(owner.getUUID()), "placed by the player: acts as them, got " + owner.getUUID());
        helper.assertTrue(owner instanceof net.neoforged.neoforge.common.util.FakePlayer, "as a fake player with their UUID");
        helper.succeed();
    }

    private static ItemStack pickaxe(PickaxeRune rune) {
        ItemStack tool = new ItemStack(ModItems.pickaxe(HextechPickaxe.HEXTECH).get());
        PickaxeRunes.set(tool, PickaxeRunes.EMPTY.with(rune, 1));
        return tool;
    }

    /** Breaks the block at {@code pos} with the tool as a player standing over it (as in the rune work tests). */
    private static void mine(GameTestHelper helper, ServerPlayer player, ItemStack tool, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(abs);
        player.snapTo(abs.getX() + 0.5, abs.getY() + 1, abs.getZ() + 0.5, 0, 90);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        Block.dropResources(state, helper.getLevel(), abs, null, player, tool);
        helper.getLevel().removeBlock(abs, false);
    }
}
