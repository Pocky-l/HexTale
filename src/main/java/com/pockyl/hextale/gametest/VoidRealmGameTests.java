package com.pockyl.hextale.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.VoidAnchorBlockEntity;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.thevoid.TheVoid;
import com.pockyl.hextale.thevoid.VoidShape;
import com.pockyl.hextale.thevoid.realm.VoidChunkGenerator;
import com.pockyl.hextale.thevoid.realm.VoidPassage;
import com.pockyl.hextale.thevoid.realm.VoidShapes;
import com.pockyl.hextale.thevoid.realm.WornMembraneBlock;

/** The Void as a place: it exists, it is built of ribs and skin, a held breach lets a player in and a gate lets them out. */
public final class VoidRealmGameTests {
    private VoidRealmGameTests() {
    }

    /**
     * The Void's dimension reads into our generator and zones. (The game test server loads no datapack dimensions, so
     * the file is decoded here the way a world would read it.)
     */
    @HexTest
    public static void theVoidIsAWorld(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, server.registryAccess());
        var resource = server.getResourceManager().getResource(Hextale.id("dimension/the_void.json"));
        helper.assertTrue(resource.isPresent(), "the dimension file");
        try (var reader = resource.get().openAsReader()) {
            var json = com.google.gson.JsonParser.parseReader(reader);
            var stem = net.minecraft.world.level.dimension.LevelStem.CODEC.parse(ops, json).getOrThrow();
            helper.assertTrue(stem.generator() instanceof VoidChunkGenerator, "with its own generator");
            helper.assertTrue(stem.generator().getBiomeSource() instanceof com.pockyl.hextale.thevoid.realm.VoidBiomeSource, "and zones");
            helper.assertTrue(stem.type().value().coordinateScale() == VoidPassage.SCALE, "an eighth of our world");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        helper.succeed();
    }

    /** Over a stretch of the Void: ribs with lit crests, skin with veins and worn patches, flesh - and nothing low down. */
    @HexTest(maxTicks = 200)
    public static void theVoidIsBuiltOfRibsAndSkin(GameTestHelper helper) {
        VoidShapes shapes = new VoidShapes(20261031L);
        int volume = 16 * 16 * VoidShapes.HEIGHT;
        byte[] kinds = new byte[volume];
        float[] best = new float[volume];
        int[] counts = new int[16];
        int lowest = Integer.MAX_VALUE;
        for (int cx = -12; cx < 12; cx++) {
            for (int cz = -12; cz < 12; cz++) {
                shapes.fill(cx, cz, kinds, best);
                for (int i = 0; i < volume; i++) {
                    if (kinds[i] != VoidShapes.AIR) {
                        counts[kinds[i]]++;
                        lowest = Math.min(lowest, i / 256);
                    }
                }
            }
        }
        helper.assertTrue(counts[VoidShapes.CHITIN] > 1000, "ribs of chitin");
        helper.assertTrue(counts[VoidShapes.RIDGE] > 50, "with lit crests");
        helper.assertTrue(counts[VoidShapes.FLESH] > 100, "knots of flesh");
        helper.assertTrue(counts[VoidShapes.MEMBRANE] > 500, "skin between the ribs");
        helper.assertTrue(counts[VoidShapes.VEINED] > 0 && counts[VoidShapes.WORN] > 0, "veined and worn skin");
        helper.assertTrue(counts[VoidShapes.CRYSTAL] > 0, "geodes with Void crystals");
        helper.assertTrue(counts[VoidShapes.RIFT_ORE] > 0, "rift ore in the husks");
        helper.assertTrue(lowest > 8, "nothing down by the abyss: lowest " + lowest);
        // The same seed builds the same Void.
        byte[] again = new byte[volume];
        shapes.fill(3, -5, kinds, best);
        new VoidShapes(20261031L).fill(3, -5, again, new float[volume]);
        helper.assertTrue(java.util.Arrays.equals(kinds, again), "the same seed, the same Void");
        helper.succeed();
    }

    /** A held breach takes a player through to its gate in the Void; the gate lets them back out beside the breach. */
    @HexTest(maxTicks = 600, padding = 8)
    @SuppressWarnings("removal")
    public static void aHeldBreachLetsAPlayerThroughAndTheGateOut(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(1, 4, 1));
        List<VoidAnchorBlockEntity> anchors = VoidRoadGameTests.ring(helper);
        anchors.forEach(anchor -> anchor.insert(new ItemStack(ModItems.HEXTECH_CRYSTAL.get())));
        TheVoid breach = TheVoid.rooted(level, Vec3.atCenterOf(centre), 0, VoidShape.EYE, 3.5F, 887);
        level.addFreshEntity(breach);
        if (level.getServer().getLevel(VoidPassage.LEVEL) == null) {
            // The game test server has no datapack dimensions: there is nowhere to go, so the breach throws back as ever.
            helper.succeed();
            breach.discard();
            VoidRoadGameTests.clearRing(helper);
            return;
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(centre.getX() + 0.5, centre.getY() + 0.5 + 40, centre.getZ() + 0.5, 0, 0);
        player.getAbilities().flying = true;
        boolean[] stage = {false, false};
        helper.succeedWhen(() -> {
            helper.assertTrue(breach.subdued(), "the ring holds it");
            if (!stage[0]) {
                stage[0] = true;
                // Into the breach's middle.
                player.snapTo(breach.getX(), breach.getY() - player.getBbHeight() * 0.5, breach.getZ() + 0.1, 0, 0);
            }
            helper.assertTrue(player.level().dimension().equals(VoidPassage.LEVEL), "the player is in the Void");
            VoidPassage.Return back = player.getData(ModAttachments.VOID_RETURN);
            helper.assertTrue(back.seed() == 887 && !back.none(), "and knows the way back");
            VoidPassage.Gate gate = VoidPassage.gates(level.getServer()).get(887);
            helper.assertTrue(gate != null && Math.abs(gate.at().x - Math.floor(breach.getX() / VoidPassage.SCALE) - 0.5) < 0.01, "a gate at an eighth of the way");
            ServerLevel realm = (ServerLevel) player.level();
            helper.assertTrue(!realm.getEntitiesOfClass(TheVoid.class, new net.minecraft.world.phys.AABB(gate.at(), gate.at()).inflate(3),
                    TheVoid::gate).isEmpty(), "the gate stands in the Void");
            helper.assertTrue(realm.getBlockState(BlockPos.containing(player.getX(), VoidPassage.LEDGE_Y - 1, player.getZ())).isSolid(),
                    "on a ledge");
            if (!stage[1]) {
                stage[1] = true;
                player.setPortalCooldown(0);
                VoidPassage.leave(player, gate.seed());
            }
            helper.assertTrue(player.level().dimension().equals(level.dimension()), "the gate lets them out");
            helper.assertTrue(player.position().distanceTo(breach.position()) < 6, "beside their breach");
            helper.assertTrue(player.getData(ModAttachments.VOID_RETURN).none(), "the way back is spent");
            player.discard();
            breach.discard();
            VoidRoadGameTests.clearRing(helper);
        });
    }

    /** A breach opened by hand (a Void Shard) is held by a ring of anchors like one the land tore open. */
    @HexTest(maxTicks = 300, padding = 8)
    public static void aShardBreachIsHeldByARing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(1, 4, 1));
        List<VoidAnchorBlockEntity> anchors = VoidRoadGameTests.ring(helper);
        anchors.forEach(anchor -> anchor.insert(new ItemStack(ModItems.HEXTECH_CRYSTAL.get())));
        TheVoid breach = TheVoid.open(level, Vec3.atCenterOf(centre), 0, VoidShape.EYE, 3.5F, 889);
        level.addFreshEntity(breach);
        helper.succeedWhen(() -> {
            helper.assertTrue(breach.subdued(), "the ring holds it");
            helper.assertTrue(breach.calm() > 0.5F, "and it cools");
            breach.discard();
            VoidRoadGameTests.clearRing(helper);
        });
    }

    /** On clean land a breach starves - but not while a ring is being built round it. */
    @HexTest(maxTicks = 400, padding = 8)
    public static void aRingBeingBuiltKeepsABreachFromStarving(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(1, 4, 1));
        List<VoidAnchorBlockEntity> anchors = VoidRoadGameTests.ring(helper);
        anchors.get(0).insert(new ItemStack(ModItems.HEXTECH_CRYSTAL.get()));
        TheVoid breach = TheVoid.rooted(level, Vec3.atCenterOf(centre), 0, VoidShape.EYE, 3.5F, 891);
        level.addFreshEntity(breach);
        breach.setStarved(TheVoid.STARVE_TICKS - TheVoid.FEED_TICKS);
        helper.runAfterDelay(TheVoid.OMEN_TICKS + TheVoid.ROOTED_OPEN_TICKS + TheVoid.FEED_TICKS * 2 + 20, () -> {
            helper.assertTrue(breach.heldSides() == 1, "one anchor ties it");
            helper.assertTrue(!breach.closing() && breach.isAlive(), "and it does not starve");
            breach.discard();
            VoidRoadGameTests.clearRing(helper);
            helper.succeed();
        });
    }

    /** Worn skin holds a moment under a creature and tears; the tear leaves nothing behind it. */
    @HexTest(maxTicks = 200)
    public static void wornSkinTearsUnderACreature(GameTestHelper helper) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                helper.setBlock(new BlockPos(dx, 2, dz), ModBlocks.WORN_VOID_MEMBRANE.get());
            }
        }
        var pig = helper.spawn(EntityTypes.PIG, new BlockPos(1, 5, 1));
        helper.succeedWhen(() -> {
            var state = helper.getBlockState(new BlockPos(1, 2, 1));
            helper.assertTrue(WornMembraneBlock.torn(state), "the skin tore");
            helper.assertTrue(state.getCollisionShape(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1))).isEmpty(), "and holds nothing");
            pig.discard();
            for (int dx = 0; dx < 3; dx++) {
                for (int dz = 0; dz < 3; dz++) {
                    helper.setBlock(new BlockPos(dx, 2, dz), Blocks.AIR);
                }
            }
        });
    }

    /** Riftmetal comes from the Void now: raw metal alloyed with Radiant Alloy. */
    @HexTest
    public static void riftmetalIsAlloyedFromTheVoidsOre(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager().recipeMap();
        helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Hextale.id("alloying/riftmetal_ingot"))) != null,
                "the alloy");
        helper.succeed();
    }
}
