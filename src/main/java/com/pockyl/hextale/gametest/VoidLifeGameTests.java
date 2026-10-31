package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModEffects;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.thevoid.VoidMite;
import com.pockyl.hextale.thevoid.Voidling;
import com.pockyl.hextale.thevoid.realm.HuskCrustBlock;
import com.pockyl.hextale.thevoid.realm.PitMaw;
import com.pockyl.hextale.thevoid.realm.RibCrawler;
import com.pockyl.hextale.thevoid.realm.VoidChunkGenerator;
import com.pockyl.hextale.thevoid.realm.VoidSacs;
import com.pockyl.hextale.thevoid.realm.VoidShapes;
import com.pockyl.hextale.thevoid.realm.VoidZones;

/** What lives and waits in the Void: its nurseries, ruins and crusts, and the creatures and traps themselves. */
public final class VoidLifeGameTests {
    private VoidLifeGameTests() {
    }

    /** A mock player in survival (the framework's own is always creative, and the Void's creatures leave those alone). */
    static ServerPlayer survivor(GameTestHelper helper) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-survivor"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public net.minecraft.world.level.GameType gameMode() {
                return net.minecraft.world.level.GameType.SURVIVAL;
            }
        };
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }

    /** The generator grows nurseries full of sacs in the Husk Drift, swallowed ruins with a chest on the shoals, crusts. */
    @HexTest(maxTicks = 1200)
    public static void theVoidGrowsNurseriesRuinsAndCrusts(GameTestHelper helper) {
        VoidShapes shapes = new VoidShapes(20261031L);
        int sacs = 0, pearls = 0, chests = 0, crusts = 0, gnawed = 0;
        boolean nurseryFound = false;
        // A nursery somewhere in the first rings round a vortex.
        search:
        for (int cx = -96; cx < 96; cx++) {
            for (int cz = -96; cz < 96; cz++) {
                if (!shapes.nurseriesIn(cx, cz).isEmpty()) {
                    nurseryFound = true;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            for (BlockState state : VoidChunkGenerator.chunkStates(shapes, cx + dx, cz + dz)) {
                                sacs += state.is(ModBlocks.VOID_SAC.get()) ? 1 : 0;
                                pearls += state.is(ModBlocks.PEARL_SAC.get()) || state.is(ModBlocks.MIMIC_SAC.get()) ? 1 : 0;
                            }
                        }
                    }
                    break search;
                }
            }
        }
        helper.assertTrue(nurseryFound, "a nursery in the Drift");
        helper.assertTrue(sacs > 40, "its walls hung with sacs: " + sacs);
        helper.assertTrue(pearls > 3, "pearl sacs on its mound: " + pearls);
        // The shoals: ruins and crusts.
        int found = 0;
        for (int cx = -96; cx < 96 && found < 200; cx++) {
            for (int cz = -96; cz < 96 && found < 200; cz++) {
                if (VoidZones.zone((cx << 4) + 8, (cz << 4) + 8) != VoidZones.Zone.SHOALS) {
                    continue;
                }
                found++;
                for (BlockState state : VoidChunkGenerator.chunkStates(shapes, cx, cz)) {
                    chests += state.is(Blocks.CHEST) ? 1 : 0;
                    crusts += state.is(ModBlocks.HUSK_CRUST.get()) ? 1 : 0;
                    gnawed += state.is(ModBlocks.GNAWED_PLANKS.get()) || state.is(ModBlocks.GNAWED_STONE_BRICKS.get()) ? 1 : 0;
                }
            }
        }
        helper.assertTrue(found > 20, "shoals to look at: " + found);
        helper.assertTrue(chests > 0 && gnawed > 20, "swallowed ruins with a chest: " + chests + " chests, " + gnawed + " gnawed blocks");
        helper.assertTrue(crusts > 0, "crust over hollows");
        helper.succeed();
    }

    /** Currents run between the plates over the gaps; a point on one's line is caught by it, one far off is not. */
    @HexTest(maxTicks = 400)
    public static void currentsRunBetweenThePlates(GameTestHelper helper) {
        VoidShapes shapes = new VoidShapes(20261031L);
        int count = 0, breaking = 0;
        for (int rx = -3; rx <= 3; rx++) {
            for (int rz = -3; rz <= 3; rz++) {
                for (var current : shapes.currents(rx, rz)) {
                    count++;
                    breaking += current.breaking() ? 1 : 0;
                    var middle = current.point(current.points() / 2);
                    helper.assertTrue(current.nearest(middle, com.pockyl.hextale.thevoid.realm.VoidCurrents.RADIUS)[0] >= 0, "its own line catches");
                    helper.assertTrue(current.nearest(middle.add(0, 10, 0), com.pockyl.hextale.thevoid.realm.VoidCurrents.RADIUS)[0] < 0,
                            "far above it does not");
                    helper.assertTrue(middle.y > current.point(0).y, "it arcs up over the gap");
                }
            }
        }
        helper.assertTrue(count > 20, "currents in the Void: " + count);
        helper.assertTrue(breaking > 0 && breaking < count, "some of them break: " + breaking);
        helper.succeed();
    }

    /** A creature on husk crust: it cracks, and the patch falls in. */
    @HexTest(maxTicks = 200)
    public static void huskCrustGivesWayUnderACreature(GameTestHelper helper) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                helper.setBlock(new BlockPos(dx, 2, dz), ModBlocks.HUSK_CRUST.get());
            }
        }
        var pig = helper.spawn(EntityType.PIG, new BlockPos(1, 4, 1));
        helper.succeedWhen(() -> {
            boolean fallen = false;
            for (int dx = 0; dx < 3; dx++) {
                for (int dz = 0; dz < 3; dz++) {
                    fallen |= HuskCrustBlock.fallen(helper.getBlockState(new BlockPos(dx, 2, dz)));
                }
            }
            helper.assertTrue(fallen, "the crust fell in (centre " + helper.getBlockState(new BlockPos(1, 2, 1)) + ", pig at "
                    + helper.relativeVec(pig.position()) + ", on ground " + pig.onGround() + ")");
            pig.discard();
            for (int dx = 0; dx < 3; dx++) {
                for (int dz = 0; dz < 3; dz++) {
                    helper.setBlock(new BlockPos(dx, 2, dz), Blocks.AIR);
                }
            }
        });
    }

    /** Touching a mimic: it bursts into mites. */
    @HexTest(maxTicks = 100)
    public static void aMimicSacBurstsIntoMites(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at.below(), Blocks.STONE);
        helper.setBlock(at, ModBlocks.MIMIC_SAC.get().defaultBlockState().setValue(VoidSacs.FACING, Direction.UP));
        ServerPlayer player = survivor(helper);
        player.snapTo(helper.absoluteVec(net.minecraft.world.phys.Vec3.atCenterOf(at)).add(0, 0, -2));
        helper.getBlockState(at).attack(helper.getLevel(), helper.absolutePos(at), player);
        helper.assertBlockNotPresent(ModBlocks.MIMIC_SAC.get(), at);
        AABB round = new AABB(helper.absolutePos(at)).inflate(3);
        var mites = helper.getLevel().getEntitiesOfClass(VoidMite.class, round);
        helper.assertTrue(mites.size() >= 3, "mites spilled out: " + mites.size());
        mites.forEach(net.minecraft.world.entity.Entity::discard);
        player.discard();
        helper.setBlock(at.below(), Blocks.AIR);
        helper.succeed();
    }

    /** A player stepping onto a Pit Maw: it tears open and pulls. */
    @HexTest(maxTicks = 200, padding = 6)
    public static void aPitMawOpensUnderAPlayer(GameTestHelper helper) {
        PitMaw maw = helper.spawn(ModEntities.PIT_MAW.get(), new BlockPos(2, 2, 2));
        ServerPlayer player = survivor(helper);
        player.snapTo(maw.getX(), maw.getY() + maw.getBbHeight(), maw.getZ() + 0.3, 0, 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(maw.isPulling() || maw.open(0) > 0, "it opened");
            maw.discard();
            player.discard();
        });
    }

    /** The Seer's mark: Void creatures round the marked player turn on them. */
    @HexTest(maxTicks = 100, padding = 6)
    public static void theVoidMarkCallsTheVoidsCreatures(GameTestHelper helper) {
        Voidling voidling = helper.spawn(ModEntities.VOIDLING.get(), new BlockPos(3, 2, 3));
        ServerPlayer player = survivor(helper);
        player.snapTo(helper.absoluteVec(net.minecraft.world.phys.Vec3.atCenterOf(new BlockPos(1, 2, 1))));
        ModEffects.VOID_MARKED.value().applyEffectTick(helper.getLevel(), player, 0);
        helper.assertTrue(voidling.getTarget() == player, "the voidling comes for the marked one");
        voidling.discard();
        player.discard();
        helper.succeed();
    }

    /** A Rib Crawler hanging over a player: it clicks, lets go and lands. */
    @HexTest(maxTicks = 240, padding = 8)
    public static void aRibCrawlerDropsOnWhoPassesBelow(GameTestHelper helper) {
        BlockPos ceiling = new BlockPos(2, 9, 2);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(ceiling.offset(dx, 0, dz), Blocks.STONE);
            }
        }
        RibCrawler crawler = helper.spawn(ModEntities.RIB_CRAWLER.get(), ceiling.below());
        crawler.setClinging(true);
        crawler.snapTo(crawler.getX(), helper.absolutePos(ceiling).getY() - crawler.getBbHeight(), crawler.getZ(), 0, 0);
        ServerPlayer player = survivor(helper);
        player.snapTo(crawler.getX() + 1, helper.absolutePos(new BlockPos(2, 2, 2)).getY(), crawler.getZ(), 0, 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(!crawler.isClinging(), "it let go (alive " + crawler.isAlive() + ", at " + helper.relativeVec(crawler.position())
                    + ", player at " + helper.relativeVec(player.position()) + ", sees " + crawler.hasLineOfSight(player) + ")");
            helper.assertTrue(crawler.onGround(), "and landed");
            crawler.discard();
            player.discard();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    helper.setBlock(ceiling.offset(dx, 0, dz), Blocks.AIR);
                }
            }
        });
    }
}
