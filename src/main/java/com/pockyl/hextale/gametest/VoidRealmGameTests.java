package com.pockyl.hextale.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
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
import com.pockyl.hextale.thevoid.realm.SinewWillow;
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
        int[] counts = new int[32];
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
        helper.assertTrue(counts[VoidShapes.SOIL] > 5000, "plates with soil to walk on");
        helper.assertTrue(counts[VoidShapes.RIDGE] > 50, "with lit crests");
        helper.assertTrue(counts[VoidShapes.FLESH] > 100, "knots of flesh");
        helper.assertTrue(counts[VoidShapes.MEMBRANE] > 500, "skin between the ribs");
        helper.assertTrue(counts[VoidShapes.VEINED] > 0 && counts[VoidShapes.WORN] > 0, "veined and worn skin");
        helper.assertTrue(counts[VoidShapes.CRYSTAL] > 0, "geodes with Void crystals");
        helper.assertTrue(counts[VoidShapes.RIFT_ORE] > 0, "rift ore in the husks");
        helper.assertTrue(counts[VoidShapes.LOG_Y] > 100 && counts[VoidShapes.FRONDS] > 300, "sinew willows on the plates");
        helper.assertTrue(counts[VoidShapes.POD] > 0, "with ichor pods under their fronds");
        helper.assertTrue(lowest > 8, "nothing down by the abyss: lowest " + lowest);
        // The same seed builds the same Void.
        byte[] again = new byte[volume];
        shapes.fill(3, -5, kinds, best);
        new VoidShapes(20261031L).fill(3, -5, again, new float[volume]);
        helper.assertTrue(java.util.Arrays.equals(kinds, again), "the same seed, the same Void");
        helper.succeed();
    }

    /** A bucket of ichor is fuel, as lava is. */
    @HexTest
    public static void ichorBurnsInAFurnace(GameTestHelper helper) {
        helper.assertTrue(com.pockyl.hextale.port.PortFuel.burnTime(helper.getLevel(), new net.minecraft.world.item.ItemStack(com.pockyl.hextale.registry.ModItems.VOID_ICHOR_BUCKET.get())) > 0,
                "the ichor bucket burns");
        helper.succeed();
    }

    /** Wherever a gate lands, ground to stand on lies within the reach of its search (40 blocks). */
    @HexTest(maxTicks = 400)
    public static void everyGateFindsGroundNearby(GameTestHelper helper) {
        VoidShapes shapes = new VoidShapes(77L);
        int volume = 16 * 16 * VoidShapes.HEIGHT;
        byte[] kinds = new byte[volume];
        float[] best = new float[volume];
        java.util.Random random = new java.util.Random(5);
        for (int gate = 0; gate < 16; gate++) {
            int x = random.nextInt(8000) - 4000;
            int z = random.nextInt(8000) - 4000;
            boolean found = false;
            for (int cx = (x - 40) >> 4; cx <= (x + 40) >> 4 && !found; cx++) {
                for (int cz = (z - 40) >> 4; cz <= (z + 40) >> 4 && !found; cz++) {
                    shapes.fill(cx, cz, kinds, best);
                    for (int i = 0; i < 256 && !found; i++) {
                        int lx = i & 15;
                        int lz = i >> 4;
                        for (int y = VoidShapes.HEIGHT - 3; y > 30; y--) {
                            byte kind = kinds[VoidShapes.index(lx, y, lz)];
                            if (kind == VoidShapes.AIR) {
                                continue;
                            }
                            found = (kind == VoidShapes.SOIL || kind == VoidShapes.FLESH || kind == VoidShapes.CHITIN)
                                    && kinds[VoidShapes.index(lx, y + 1, lz)] == VoidShapes.AIR
                                    && kinds[VoidShapes.index(lx, y + 2, lz)] == VoidShapes.AIR;
                            break;
                        }
                    }
                }
            }
            helper.assertTrue(found, "ground near a gate at " + x + ", " + z);
        }
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
            helper.assertTrue(realm.getBlockState(player.blockPosition().below()).isSolid(), "on a ledge");
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

    /** A breach opened by hand keeps the land near it from tearing open another; once it is gone, the land may. */
    @HexTest
    public static void aShardBreachKeepsAnotherFromOpeningNear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(1, 4, 1));
        TheVoid breach = TheVoid.open(level, Vec3.atCenterOf(centre), 0, VoidShape.EYE, 3, 893);
        level.addFreshEntity(breach);
        com.pockyl.hextale.thevoid.Breaches.note(level, breach);
        helper.assertFalse(com.pockyl.hextale.thevoid.Breaches.mayOpenAt(level, centre.offset(200, 0, 0)), "no second breach near it");
        BlockPos site = breach.blockPosition();
        breach.discard();
        // Other tests' breaches may stand within reach, so only this one's place is checked.
        helper.assertFalse(com.pockyl.hextale.thevoid.Breaches.sites(level).sites().contains(site), "its place is free once it is gone");
        helper.succeed();
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
        var pig = helper.spawn(EntityType.PIG, new BlockPos(1, 5, 1));
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

    /** A Sinew Willow sprout on the Void's soil shoots up into a willow: a trunk, boughs, fronds. */
    @HexTest(maxTicks = 40, padding = 14)
    public static void aSinewWillowGrowsFromItsSprout(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                helper.setBlock(at.offset(dx, -1, dz), ModBlocks.VOID_SOIL.get());
            }
        }
        helper.setBlock(at, ModBlocks.SINEW_WILLOW_SPROUT.get());
        BlockPos sprout = helper.absolutePos(at);
        helper.assertTrue(SinewWillow.grow(helper.getLevel(), sprout, net.minecraft.util.RandomSource.create(5), false), "the sprout had room to grow");
        int wood = 0, fronds = 0;
        for (BlockPos pos : BlockPos.betweenClosed(sprout.offset(-14, -3, -14), sprout.offset(14, 24, 14))) {
            var state = helper.getLevel().getBlockState(pos);
            wood += state.is(ModBlocks.SINEW_WILLOW_LOG.get()) || state.is(ModBlocks.SINEW_WILLOW_WOOD.get()) ? 1 : 0;
            fronds += state.is(ModBlocks.SINEW_WILLOW_FRONDS.get()) ? 1 : 0;
        }
        helper.assertTrue(wood >= 20, "a trunk and boughs (" + wood + " wood)");
        helper.assertTrue(fronds >= 30, "a crown of fronds (" + fronds + ")");
        helper.assertFalse(helper.getLevel().getBlockState(sprout).is(ModBlocks.SINEW_WILLOW_SPROUT.get()), "the sprout became the tree");
        helper.succeed();
    }

    /** A ripe ichor pod is picked by hand and swells again; a pod falls when what it hangs from is gone. */
    @HexTest
    public static void aRipeIchorPodIsPicked(GameTestHelper helper) {
        BlockPos fronds = new BlockPos(1, 4, 1);
        BlockPos pod = fronds.below();
        helper.setBlock(fronds, SinewWillow.wildFronds());
        helper.setBlock(pod, ModBlocks.ICHOR_POD.get().defaultBlockState().setValue(SinewWillow.Pod.AGE, SinewWillow.Pod.RIPE));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absolute = helper.absolutePos(pod);
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(absolute), net.minecraft.core.Direction.DOWN, absolute, false);
        helper.getLevel().getBlockState(absolute).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(helper.getBlockState(pod).getValue(SinewWillow.Pod.AGE) == 0, "picked, the pod starts over");
        var dropped = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(absolute).inflate(3), item -> item.getItem().is(ModItems.ICHOR_POD.get()));
        helper.assertFalse(dropped.isEmpty(), "the pod drops into the hand");
        dropped.forEach(net.minecraft.world.entity.Entity::discard);
        helper.setBlock(fronds, Blocks.AIR);
        helper.assertBlockNotPresent(ModBlocks.ICHOR_POD.get(), pod);
        helper.succeed();
    }
}
