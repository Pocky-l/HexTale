package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.HexPylonBlockEntity;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.PylonRune;
import com.pockyl.hextale.transport.PylonFlight;
import com.pockyl.hextale.transport.PylonLink;
import com.pockyl.hextale.transport.PylonLinking;

/** Hex pylons: gathering into what they stand on, sending along links and through relays, reach and sleep. */
public final class PylonGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private PylonGameTests() {
    }

    /** Clears the area (blocks and loose items outside the 1x1x1 structure outlive their tests). */
    private static void clear(GameTestHelper helper, int reach) {
        for (int x = -reach; x <= reach + 12; x++) {
            for (int z = -reach; z <= reach; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(BASE.offset(x, y - 1, z), Blocks.AIR);
                }
            }
        }
        AABB area = new AABB(helper.absolutePos(BASE)).inflate(reach + 14, 6, reach + 2);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
    }

    private static HexPylonBlockEntity place(GameTestHelper helper, net.minecraft.world.level.block.Block pylon, BlockPos pos) {
        helper.setBlock(pos, pylon);
        return helper.<HexPylonBlockEntity>getBlockEntity(pos);
    }

    private static ItemEntity drop(GameTestHelper helper, Item item, int count, Vec3 at) {
        ItemEntity entity = helper.spawnItem(item, at);
        entity.setItem(new ItemStack(item, count));
        return entity;
    }

    private static int count(Container container, Item item) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(item)) {
                total += container.getItem(i).getCount();
            }
        }
        return total;
    }

    private static PylonLink link(GameTestHelper helper, BlockPos pos, Direction face) {
        return new PylonLink(helper.absolutePos(pos), face);
    }

    /** Items lying within the radius go into the chest under the pylon, a pile beyond it stays; the clients get flights. */
    @HexTest(maxTicks = 200)
    public static void gatheringPylonFillsTheChestBelow(GameTestHelper helper) {
        clear(helper, 10);
        helper.setBlock(BASE, Blocks.CHEST);
        HexPylonBlockEntity pylon = place(helper, ModBlocks.GATHERING_PYLON.get(), BASE.above());
        drop(helper, Items.COBBLESTONE, 5, new Vec3(6.5, 1.2, 1.5));
        drop(helper, Items.STICK, 3, new Vec3(1.5, 1.2, 5.5));
        ItemEntity far = drop(helper, Items.DIAMOND, 1, new Vec3(10.5, 1.2, 1.5));
        Container chest = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(BASE);
        helper.succeedWhen(() -> {
            helper.assertTrue(count(chest, Items.COBBLESTONE) == 5 && count(chest, Items.STICK) == 3, "the items around are in the chest");
            helper.assertTrue(far.isAlive() && count(chest, Items.DIAMOND) == 0, "an item beyond the radius stays where it lies");
            PylonFlight flight = pylon.lastFlights().isEmpty() ? null : pylon.lastFlights().getFirst();
            helper.assertTrue(flight != null && flight.gathered() && flight.path().size() == 3, "the move was shown as a flight");
        });
    }

    /** Set on a furnace, a gathering pylon feeds dropped ore into its input and coal into its fuel slot. */
    @HexTest(maxTicks = 200)
    public static void gatheringPylonFeedsAFurnaceFromTheTop(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, Blocks.FURNACE);
        place(helper, ModBlocks.GATHERING_PYLON.get(), BASE.above());
        drop(helper, Items.RAW_IRON, 4, new Vec3(2.5, 1.2, 2.5));
        drop(helper, Items.COAL, 3, new Vec3(1.5, 1.2, 3.5));
        FurnaceBlockEntity furnace = helper.<FurnaceBlockEntity>getBlockEntity(BASE);
        helper.succeedWhen(() -> {
            helper.assertTrue(furnace.getItem(0).is(Items.RAW_IRON), "the ore went into the input slot");
            helper.assertTrue(furnace.getItem(1).is(Items.COAL), "the coal went into the fuel slot");
        });
    }

    /** Coal and ore sent to a furnace sort themselves: ore into the input, coal into the fuel slot - whatever face was linked. */
    @HexTest(maxTicks = 200)
    public static void aFurnaceGetsFuelAndOreInTheirSlots(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, Blocks.CHEST);
        BlockPos furnacePos = BASE.offset(3, 0, 0);
        helper.setBlock(furnacePos, Blocks.FURNACE);
        Container from = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(BASE);
        from.setItem(0, new ItemStack(Items.COAL, 8));
        from.setItem(1, new ItemStack(Items.RAW_IRON, 8));
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        pylon.toggleLink(link(helper, furnacePos, Direction.UP));
        FurnaceBlockEntity furnace = helper.<FurnaceBlockEntity>getBlockEntity(furnacePos);
        helper.succeedWhen(() -> {
            helper.assertTrue(count(from, Items.COAL) == 0 && count(from, Items.RAW_IRON) == 0, "coal and ore were sent");
            helper.assertTrue(furnace.getItem(1).is(Items.COAL), "coal went into the fuel slot");
            helper.assertTrue(furnace.getItem(0).is(Items.RAW_IRON), "ore went into the input");
        });
    }

    /**
     * A pylon delivers on its owner's behalf: coal its owner has studied boils in the linked Alembic into its own
     * essences, an item they have not studied would boil into unknown essence.
     */
    @HexTest(maxTicks = 200)
    public static void anAlembicKnowsWhatThePylonOwnerStudied(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, Blocks.CHEST);
        BlockPos alembicPos = BASE.offset(3, 0, 0);
        helper.setBlock(alembicPos, ModBlocks.ALEMBIC.get());
        Container from = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(BASE);
        from.setItem(0, new ItemStack(Items.COAL, 4));
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        com.pockyl.hextale.knowledge.Research.of(owner).study(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Items.COAL));
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        pylon.setOwner(owner);
        helper.assertTrue(pylon.toggleLink(link(helper, alembicPos, Direction.WEST)), "linked to the alembic's side");
        com.pockyl.hextale.block.AlembicBlockEntity alembic = helper.<com.pockyl.hextale.block.AlembicBlockEntity>getBlockEntity(alembicPos);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            helper.assertTrue(alembic.receiveWater(side, 100, true) == 100, "the alembic takes water from the " + side);
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(alembic.input().is(Items.COAL), "the coal went into the alembic");
            helper.assertTrue(alembic.inputKnown(), "the owner has studied coal, so it boils into its essences");
        });
    }

    /**
     * A furnace whose input has room for only a few items gets nothing (no single items flown in to bounce back); once
     * there is room for a whole batch, exactly that batch comes.
     */
    @HexTest(maxTicks = 300)
    public static void sendingPylonWaitsForRoomForAWholeBatch(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, Blocks.CHEST);
        BlockPos furnacePos = BASE.offset(3, 0, 0);
        helper.setBlock(furnacePos, Blocks.FURNACE);
        Container from = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(BASE);
        from.setItem(0, new ItemStack(Items.RAW_IRON, 32));
        FurnaceBlockEntity furnace = helper.<FurnaceBlockEntity>getBlockEntity(furnacePos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 62));
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        int batch = pylon.amount();
        pylon.toggleLink(link(helper, furnacePos, Direction.UP));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(count(from, Items.RAW_IRON) == 32 && pylon.cargo().isEmpty(), "room for two: nothing is sent");
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, 64 - batch));
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(furnace.getItem(0).getCount() == 64, "a whole batch came once there was room for it");
                helper.assertTrue(count(from, Items.RAW_IRON) == 32 - batch, "and only that batch: nothing flew back, got "
                        + count(from, Items.RAW_IRON));
                helper.succeed();
            });
        });
    }

    /** Linked to two chests, a sending pylon takes them in turn: both get their share, not only the first. */
    @HexTest(maxTicks = 300)
    public static void sendingPylonTakesItsLinksInTurn(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, Blocks.CHEST);
        BlockPos east = BASE.offset(4, 0, 0);
        BlockPos south = BASE.offset(0, 0, 4);
        helper.setBlock(east, Blocks.CHEST);
        helper.setBlock(south, Blocks.CHEST);
        Container from = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(BASE);
        Container a = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(east);
        Container b = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(south);
        from.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        helper.assertTrue(pylon.toggleLink(link(helper, east, Direction.UP)) && pylon.toggleLink(link(helper, south, Direction.UP)), "linked");
        helper.succeedWhen(() -> {
            helper.assertTrue(count(from, Items.COBBLESTONE) == 0, "everything was sent");
            int first = count(a, Items.COBBLESTONE);
            int second = count(b, Items.COBBLESTONE);
            helper.assertTrue(first + second == 64 && Math.abs(first - second) <= 16, "both chests got a share: " + first + " and " + second);
        });
    }

    /** A sending pylon on a chest moves its contents into the linked chest. */
    @HexTest(maxTicks = 200)
    public static void sendingPylonEmptiesIntoItsLink(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, Blocks.CHEST);
        BlockPos target = BASE.offset(4, 0, 0);
        helper.setBlock(target, Blocks.CHEST);
        Container from = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(BASE);
        Container to = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(target);
        from.setItem(0, new ItemStack(Items.COBBLESTONE, 20));
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        helper.assertTrue(pylon.toggleLink(link(helper, target, Direction.UP)), "linked");
        helper.succeedWhen(() -> {
            helper.assertTrue(count(to, Items.COBBLESTONE) == 20 && count(from, Items.COBBLESTONE) == 0, "everything was sent");
            helper.assertTrue(!pylon.lastFlights().isEmpty() && !pylon.lastFlights().getFirst().gathered(), "the move was shown as a flight");
        });
    }

    /** A sending pylon on a furnace takes only what the furnace gives out at the bottom: the result, not the ore. */
    @HexTest(maxTicks = 200)
    public static void sendingPylonTakesOnlyTheFurnaceResult(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, Blocks.FURNACE);
        BlockPos target = BASE.offset(3, 0, 0);
        helper.setBlock(target, Blocks.CHEST);
        FurnaceBlockEntity furnace = helper.<FurnaceBlockEntity>getBlockEntity(BASE);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 8));
        furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 5));
        Container to = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(target);
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        pylon.toggleLink(link(helper, target, Direction.UP));
        helper.succeedWhen(() -> {
            helper.assertTrue(count(to, Items.IRON_INGOT) == 5, "the ingots were sent");
            helper.assertTrue(count(to, Items.RAW_IRON) == 0 && furnace.getItem(0).getCount() == 8, "the ore stayed in the furnace");
        });
    }

    /** A chain: the sender throws to a relay, the relay passes everything on to the chest it is linked to. */
    @HexTest(maxTicks = 300)
    public static void relaysPassItemsOn(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, Blocks.CHEST);
        Container from = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(BASE);
        from.setItem(0, new ItemStack(Items.IRON_INGOT, 6));
        from.setItem(1, new ItemStack(Items.COBBLESTONE, 6));
        BlockPos relayPos = BASE.offset(5, 0, 0);
        BlockPos farChest = BASE.offset(10, 0, 0);
        helper.setBlock(farChest, Blocks.CHEST);
        HexPylonBlockEntity relay = place(helper, ModBlocks.RELAY_PYLON.get(), relayPos);
        relay.toggleLink(link(helper, farChest, Direction.UP));
        HexPylonBlockEntity sender = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        sender.toggleLink(link(helper, relayPos, Direction.UP));
        Container far = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(farChest);
        helper.succeedWhen(() -> {
            helper.assertTrue(count(far, Items.IRON_INGOT) == 6 && count(far, Items.COBBLESTONE) == 6, "everything went through the relay");
            helper.assertTrue(sender.lastFlights().getFirst().path().contains(relay.gem()), "the flight passes the relay's gem");
        });
    }

    /** Smelting and Erosion change what arrives, piece by piece; what neither rune touches arrives as it left. */
    @HexTest(maxTicks = 20)
    public static void pylonRunesChangeWhatArrives(GameTestHelper helper) {
        clear(helper, 2);
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE);
        ItemStack runed = new ItemStack(ModBlocks.SENDING_PYLON.get());
        runed.set(ModDataComponents.PYLON_RUNES.get(), PickaxeRunes.EMPTY.with(PylonRune.SMELTING.rune(), 3).with(PylonRune.EROSION.rune(), 3));
        pylon.applyComponentsFromItemStack(runed);
        BlockPos target = helper.absolutePos(BASE.offset(2, 0, 0));
        java.util.List<ItemStack> ore = pylon.enrich(helper.getLevel(), new ItemStack(Items.RAW_IRON, 64), target);
        helper.assertTrue(ore.stream().mapToInt(ItemStack::getCount).sum() == 64 && ore.stream().anyMatch(s -> s.is(Items.IRON_INGOT))
                && ore.stream().anyMatch(s -> s.is(Items.RAW_IRON)), "some raw iron arrives smelted, the rest raw: " + ore);
        java.util.List<ItemStack> stone = pylon.enrich(helper.getLevel(), new ItemStack(Items.COBBLESTONE, 64), target);
        helper.assertTrue(stone.stream().mapToInt(ItemStack::getCount).sum() == 64 && stone.stream().anyMatch(s -> s.is(Items.GRAVEL)),
                "most cobblestone arrives as gravel: " + stone);
        java.util.List<ItemStack> dirt = pylon.enrich(helper.getLevel(), new ItemStack(Items.DIRT, 10), target);
        helper.assertTrue(dirt.size() == 1 && dirt.getFirst().is(Items.DIRT) && dirt.getFirst().getCount() == 10, "dirt arrives as dirt: " + dirt);
        helper.succeed();
    }

    /** A link reaches 8 blocks; Shift with two stars makes it 16. Four links at most. */
    @HexTest(maxTicks = 20)
    public static void reachAndLinkLimit(GameTestHelper helper) {
        clear(helper, 2);
        HexPylonBlockEntity pylon = place(helper, ModBlocks.RELAY_PYLON.get(), BASE);
        BlockPos far = helper.absolutePos(BASE.offset(12, 0, 0));
        helper.assertFalse(pylon.reaches(far), "12 blocks is too far without runes");
        ItemStack runed = new ItemStack(ModBlocks.RELAY_PYLON.get());
        runed.set(ModDataComponents.PYLON_RUNES.get(), PickaxeRunes.EMPTY.with(PylonRune.SHIFT.rune(), 2));
        pylon.applyComponentsFromItemStack(runed);
        helper.assertTrue(pylon.range() == 16 && pylon.reaches(far), "Shift with two stars reaches 16 blocks");
        for (int i = 0; i < HexPylonBlockEntity.MAX_LINKS; i++) {
            helper.assertTrue(pylon.toggleLink(new PylonLink(far.offset(0, 0, i), Direction.UP)), "link " + i);
        }
        helper.assertFalse(pylon.toggleLink(new PylonLink(far.offset(0, 0, 9), Direction.UP)), "no fifth link");
        helper.assertTrue(pylon.toggleLink(new PylonLink(far, Direction.UP)) && pylon.links().size() == 3, "the same target again unlinks");
        helper.succeed();
    }

    /** Linking by hand: click the pylon, then the furnace's side; the link remembers the face. Clicking twice gives up. */
    @HexTest(maxTicks = 20)
    public static void playersLinkByClicking(GameTestHelper helper) {
        clear(helper, 3);
        helper.setBlock(BASE, Blocks.CHEST);
        BlockPos furnacePos = BASE.offset(2, 0, 0);
        helper.setBlock(furnacePos, Blocks.FURNACE);
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        PylonLinking.start(player, pylon);
        helper.assertTrue(PylonLinking.linking(player), "linking started");
        PylonLinking.link(player, helper.absolutePos(furnacePos), Direction.EAST);
        helper.assertFalse(PylonLinking.linking(player), "the link is tied");
        helper.assertTrue(pylon.links().size() == 1 && pylon.links().getFirst().face() == Direction.EAST, "the furnace's side: fuel");
        PylonLinking.start(player, pylon);
        PylonLinking.start(player, pylon);
        helper.assertFalse(PylonLinking.linking(player), "a second click on the pylon gives up");
        helper.assertTrue(pylon.clearLinks() && pylon.links().isEmpty(), "all links can be taken away");
        helper.succeed();
    }

    /** A broken chest drops the links to it at once; the other link stays. */
    @HexTest(maxTicks = 20)
    public static void aBrokenTargetDropsItsLinks(GameTestHelper helper) {
        clear(helper, 4);
        helper.setBlock(BASE, Blocks.CHEST);
        BlockPos a = BASE.offset(3, 0, 0);
        BlockPos b = BASE.offset(0, 0, 3);
        helper.setBlock(a, Blocks.CHEST);
        helper.setBlock(b, Blocks.BARREL);
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        pylon.toggleLink(link(helper, a, Direction.UP));
        pylon.toggleLink(link(helper, b, Direction.UP));
        helper.destroyBlock(a);
        helper.assertTrue(pylon.links().size() == 1 && pylon.links().getFirst().pos().equals(helper.absolutePos(b)),
                "only the link to the broken chest went away");
        helper.succeed();
    }

    /** Sneak + right click on a linked block unties it from every pylon sending to it. */
    @HexTest(maxTicks = 20)
    public static void aBlockCanBeUntiedFromAllPylons(GameTestHelper helper) {
        clear(helper, 4);
        BlockPos target = BASE.offset(3, 0, 0);
        helper.setBlock(target, Blocks.CHEST);
        helper.setBlock(BASE, Blocks.CHEST);
        helper.setBlock(BASE.offset(0, 0, 2), Blocks.CHEST);
        HexPylonBlockEntity one = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        HexPylonBlockEntity two = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.offset(0, 1, 2));
        one.toggleLink(link(helper, target, Direction.UP));
        two.toggleLink(link(helper, target, Direction.UP));
        PylonLinking.untie(helper.makeMockServerPlayerInLevel(), helper.absolutePos(target));
        helper.assertTrue(one.links().isEmpty() && two.links().isEmpty(), "both pylons let go of it");
        helper.succeed();
    }

    /** While linking, a click on something that cannot take items gives the link up instead of getting in the way. */
    @HexTest(maxTicks = 20)
    public static void aClickElsewhereStopsLinking(GameTestHelper helper) {
        clear(helper, 3);
        helper.setBlock(BASE, Blocks.CHEST);
        BlockPos stone = BASE.offset(2, 0, 0);
        helper.setBlock(stone, Blocks.STONE);
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        PylonLinking.start(player, pylon);
        helper.assertFalse(PylonLinking.linkable(helper.getLevel(), helper.absolutePos(stone), Direction.UP), "stone takes no items");
        PylonLinking.giveUp(player);
        helper.assertFalse(PylonLinking.linking(player), "the link was given up");
        helper.assertTrue(pylon.links().isEmpty(), "nothing was linked");
        helper.succeed();
    }

    /** A siphon on a jar pours its essence into a linked jar and, through a relay, into a third one; the clients see streams. */
    @HexTest(maxTicks = 600)
    public static void siphonPoursEssenceAlongItsLinks(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, ModBlocks.ESSENCE_TANK.get());
        BlockPos near = BASE.offset(3, 0, 0);
        BlockPos relayPos = BASE.offset(0, 0, 4);
        BlockPos far = BASE.offset(0, 0, 8);
        helper.setBlock(near, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(far, ModBlocks.ESSENCE_TANK.get());
        com.pockyl.hextale.block.EssenceTankBlockEntity source = helper.<com.pockyl.hextale.block.EssenceTankBlockEntity>getBlockEntity(BASE);
        com.pockyl.hextale.block.EssenceTankBlockEntity first = helper.<com.pockyl.hextale.block.EssenceTankBlockEntity>getBlockEntity(near);
        com.pockyl.hextale.block.EssenceTankBlockEntity last = helper.<com.pockyl.hextale.block.EssenceTankBlockEntity>getBlockEntity(far);
        source.tank().set(com.pockyl.hextale.essence.EssenceMix.pure(com.pockyl.hextale.rune.Essence.WATER, 200));
        first.tank().set(com.pockyl.hextale.essence.EssenceMix.pure(com.pockyl.hextale.rune.Essence.WATER,
                com.pockyl.hextale.block.EssenceTankBlockEntity.CAPACITY - 40));
        HexPylonBlockEntity relay = place(helper, ModBlocks.RELAY_PYLON.get(), relayPos);
        relay.toggleLink(link(helper, far, Direction.UP));
        HexPylonBlockEntity siphon = place(helper, ModBlocks.SIPHON_PYLON.get(), BASE.above());
        helper.assertTrue(siphon.toggleLink(link(helper, near, Direction.UP)) && siphon.toggleLink(link(helper, relayPos, Direction.UP)), "linked");
        helper.succeedWhen(() -> {
            helper.assertTrue(first.tank().amount() == com.pockyl.hextale.block.EssenceTankBlockEntity.CAPACITY, "the first jar was filled up");
            helper.assertTrue(last.tank().amount() == 160 && source.tank().isEmpty(), "the rest went through the relay to the last jar");
            helper.assertTrue(!siphon.lastFlights().isEmpty() && siphon.lastFlights().getFirst().isEssence(), "the clients see a stream");
        });
    }

    /** An item pylon cannot link to a jar, a siphon cannot link to a chest; both can link to a relay, and a relay to either. */
    @HexTest(maxTicks = 20)
    public static void pylonsLinkOnlyToWhatTakesTheirLoad(GameTestHelper helper) {
        clear(helper, 4);
        helper.setBlock(BASE, Blocks.CHEST);
        BlockPos jar = BASE.offset(2, 0, 0);
        BlockPos chest = BASE.offset(0, 0, 2);
        BlockPos relayPos = BASE.offset(2, 0, 2);
        helper.setBlock(jar, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(chest, Blocks.CHEST);
        place(helper, ModBlocks.RELAY_PYLON.get(), relayPos);
        HexPylonBlockEntity sender = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        HexPylonBlockEntity siphon = place(helper, ModBlocks.SIPHON_PYLON.get(), jar.above());
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        helper.assertFalse(PylonLinking.takes(level, sender, helper.absolutePos(jar), Direction.UP), "items do not go into a jar");
        helper.assertFalse(PylonLinking.takes(level, siphon, helper.absolutePos(chest), Direction.UP), "essence does not go into a chest");
        helper.assertTrue(PylonLinking.takes(level, sender, helper.absolutePos(relayPos), Direction.UP)
                && PylonLinking.takes(level, siphon, helper.absolutePos(relayPos), Direction.UP), "both go to a relay");
        HexPylonBlockEntity relay = helper.<HexPylonBlockEntity>getBlockEntity(relayPos);
        helper.assertTrue(PylonLinking.takes(level, relay, helper.absolutePos(jar), Direction.UP)
                && PylonLinking.takes(level, relay, helper.absolutePos(chest), Direction.UP), "a relay links to a jar and to a chest");
        helper.succeed();
    }

    /** A filter laid into a filter's slot keeps only its picture: filters never nest into an item too big to send. */
    @HexTest
    public static void filtersNeverNest(GameTestHelper helper) {
        ItemStack inner = filter(only(Items.COBBLESTONE, 1));
        com.pockyl.hextale.transport.ItemFilter outer = com.pockyl.hextale.transport.ItemFilter.EMPTY.with(0, inner);
        helper.assertTrue(outer.entries().getFirst().item().is(com.pockyl.hextale.registry.ModItems.PYLON_FILTER.get())
                && !outer.entries().getFirst().item().has(ModDataComponents.ITEM_FILTER.get()), "the slot keeps a bare filter");
        helper.assertTrue(outer.match(inner) != null, "a set filter still matches its kind");
        helper.succeed();
    }

    private static ItemStack filter(com.pockyl.hextale.transport.ItemFilter settings) {
        ItemStack stack = new ItemStack(com.pockyl.hextale.registry.ModItems.PYLON_FILTER.get());
        stack.set(ModDataComponents.ITEM_FILTER.get(), settings);
        return stack;
    }

    private static com.pockyl.hextale.transport.ItemFilter only(net.minecraft.world.item.Item item, int count) {
        return com.pockyl.hextale.transport.ItemFilter.EMPTY.with(0, new ItemStack(item)).add(0, count - 1);
    }

    /** A whitelist on a gathering pylon: it picks up only cobblestone and leaves the dirt lying. */
    @HexTest(maxTicks = 200)
    public static void aWhitelistGathersOnlyItsItems(GameTestHelper helper) {
        clear(helper, 8);
        helper.setBlock(BASE, Blocks.CHEST);
        HexPylonBlockEntity pylon = place(helper, ModBlocks.GATHERING_PYLON.get(), BASE.above());
        pylon.setFilter(filter(only(Items.COBBLESTONE, 1)));
        drop(helper, Items.COBBLESTONE, 5, new Vec3(3.5, 1.2, 1.5));
        ItemEntity dirt = drop(helper, Items.DIRT, 5, new Vec3(1.5, 1.2, 3.5));
        Container chest = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(BASE);
        helper.succeedWhen(() -> {
            helper.assertTrue(count(chest, Items.COBBLESTONE) == 5, "the cobblestone was gathered");
            helper.assertTrue(dirt.isAlive() && count(chest, Items.DIRT) == 0, "the dirt was left lying");
        });
    }

    /** Numbers: a sending pylon keeping 5 cobblestone sends 15 of 20; then a blacklist sends all but cobblestone. */
    @HexTest(maxTicks = 300)
    public static void keepAndBlacklistRuleASendingPylon(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, Blocks.CHEST);
        BlockPos target = BASE.offset(3, 0, 0);
        helper.setBlock(target, Blocks.CHEST);
        Container from = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(BASE);
        Container to = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(target);
        from.setItem(0, new ItemStack(Items.COBBLESTONE, 20));
        from.setItem(1, new ItemStack(Items.DIRT, 10));
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        pylon.setFilter(filter(only(Items.COBBLESTONE, 5).withRule(com.pockyl.hextale.transport.ItemFilter.Rule.KEEP)));
        pylon.toggleLink(link(helper, target, Direction.UP));
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(count(to, Items.COBBLESTONE) == 15 && count(from, Items.COBBLESTONE) == 5, "five cobblestone stayed behind");
            helper.assertTrue(count(to, Items.DIRT) == 0, "dirt is not on the list");
            pylon.setFilter(filter(com.pockyl.hextale.transport.ItemFilter.EMPTY.with(0, new ItemStack(Items.COBBLESTONE)).withWhitelist(false)));
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(count(to, Items.DIRT) == 10 && count(from, Items.COBBLESTONE) == 5, "all but cobblestone went");
                helper.succeed();
            });
        });
    }

    /** Fill up to N: the target gets only 8 raw iron from a chest of 32. */
    @HexTest(maxTicks = 200)
    public static void fillUpToLimitsTheTarget(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, Blocks.CHEST);
        BlockPos target = BASE.offset(3, 0, 0);
        helper.setBlock(target, Blocks.CHEST);
        Container from = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(BASE);
        Container to = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(target);
        from.setItem(0, new ItemStack(Items.RAW_IRON, 32));
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        pylon.setFilter(filter(only(Items.RAW_IRON, 8).withRule(com.pockyl.hextale.transport.ItemFilter.Rule.LIMIT)));
        pylon.toggleLink(link(helper, target, Direction.UP));
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(count(to, Items.RAW_IRON) == 8 && count(from, Items.RAW_IRON) == 24, "the target was filled up to 8");
            helper.succeed();
        });
    }

    /** Sorting by relays: iron goes through the relay whose filter lets only iron pass, the rest takes the other link. */
    @HexTest(maxTicks = 300)
    public static void filteredRelaysSort(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, Blocks.CHEST);
        Container from = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(BASE);
        from.setItem(0, new ItemStack(Items.IRON_INGOT, 6));
        from.setItem(1, new ItemStack(Items.COBBLESTONE, 6));
        BlockPos relayPos = BASE.offset(5, 0, 0);
        BlockPos ironChest = BASE.offset(10, 0, 0);
        BlockPos restChest = BASE.offset(0, 0, 3);
        helper.setBlock(ironChest, Blocks.CHEST);
        helper.setBlock(restChest, Blocks.CHEST);
        HexPylonBlockEntity relay = place(helper, ModBlocks.RELAY_PYLON.get(), relayPos);
        relay.setFilter(filter(only(Items.IRON_INGOT, 1)));
        relay.toggleLink(link(helper, ironChest, Direction.UP));
        HexPylonBlockEntity sender = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        sender.toggleLink(link(helper, relayPos, Direction.UP));
        sender.toggleLink(link(helper, restChest, Direction.UP));
        Container iron = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(ironChest);
        Container rest = (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(restChest);
        helper.succeedWhen(() -> {
            helper.assertTrue(count(iron, Items.IRON_INGOT) == 6 && count(iron, Items.COBBLESTONE) == 0, "iron went through the relay");
            helper.assertTrue(count(rest, Items.COBBLESTONE) == 6 && count(rest, Items.IRON_INGOT) == 0, "the rest took the other link");
        });
    }

    /**
     * Pylons never split a mixture: a jar of fire and water sends nothing along a way with a fire sticker, but pours its
     * whole mixture, in its proportions, into an unlabelled jar; pure fire goes along the fire way.
     */
    @HexTest(maxTicks = 300)
    public static void stickersNeverSplitAMixture(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, ModBlocks.ESSENCE_TANK.get());
        BlockPos fireAt = BASE.offset(3, 0, 0);
        BlockPos plainAt = BASE.offset(0, 0, 3);
        helper.setBlock(fireAt, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(plainAt, ModBlocks.ESSENCE_TANK.get());
        var source = helper.<com.pockyl.hextale.block.EssenceTankBlockEntity>getBlockEntity(BASE);
        var fire = helper.<com.pockyl.hextale.block.EssenceTankBlockEntity>getBlockEntity(fireAt);
        var plain = helper.<com.pockyl.hextale.block.EssenceTankBlockEntity>getBlockEntity(plainAt);
        source.tank().set(com.pockyl.hextale.essence.EssenceMix.pure(com.pockyl.hextale.rune.Essence.FIRE, 40).plus(com.pockyl.hextale.essence.EssenceMix.pure(com.pockyl.hextale.rune.Essence.WATER, 40)));
        fire.setSticker(com.pockyl.hextale.rune.Essence.FIRE);
        HexPylonBlockEntity siphon = place(helper, ModBlocks.SIPHON_PYLON.get(), BASE.above());
        siphon.toggleLink(link(helper, fireAt, Direction.UP));
        siphon.toggleLink(link(helper, plainAt, Direction.UP));
        helper.succeedWhen(() -> {
            helper.assertTrue(source.tank().isEmpty(), "the mixture went out");
            helper.assertTrue(fire.tank().isEmpty(), "no fire was split off into the fire jar");
            helper.assertTrue(plain.tank().contents().get(com.pockyl.hextale.rune.Essence.FIRE) == 40 && plain.tank().contents().get(com.pockyl.hextale.rune.Essence.WATER) == 40,
                    "the plain jar got the whole mixture");
            source.tank().set(com.pockyl.hextale.essence.EssenceMix.pure(com.pockyl.hextale.rune.Essence.FIRE, 30));
            helper.runAfterDelay(100, () -> {
                helper.assertTrue(fire.tank().amount() == 30, "pure fire takes the fire way");
                helper.succeed();
            });
        });
    }

    /** A Creative Essence Jar holds every essence: a sticker on the way picks the one it gives, pure. */
    @HexTest(maxTicks = 200)
    public static void aCreativeJarGivesTheStickersEssence(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, ModBlocks.CREATIVE_ESSENCE_TANK.get());
        BlockPos fireAt = BASE.offset(3, 0, 0);
        helper.setBlock(fireAt, ModBlocks.ESSENCE_TANK.get());
        var fire = helper.<com.pockyl.hextale.block.EssenceTankBlockEntity>getBlockEntity(fireAt);
        fire.setSticker(com.pockyl.hextale.rune.Essence.FIRE);
        HexPylonBlockEntity siphon = place(helper, ModBlocks.SIPHON_PYLON.get(), BASE.above());
        siphon.toggleLink(link(helper, fireAt, Direction.UP));
        helper.succeedWhen(() -> helper.assertTrue(fire.tank().amount() > 0
                && fire.tank().contents().get(com.pockyl.hextale.rune.Essence.FIRE) == fire.tank().amount(), "pure fire came from the creative jar"));
    }

    /** The flights of a move survive the trip to the clients (the payload's codec), the longest way included. */
    @HexTest(maxTicks = 20)
    public static void flightsReachTheClient(GameTestHelper helper) {
        java.util.List<Vec3> path = new java.util.ArrayList<>();
        for (int i = 0; i < 4 + HexPylonBlockEntity.MAX_HOPS; i++) {
            path.add(new Vec3(i, 64 + i * 0.5, -i));
        }
        PylonFlight flight = new PylonFlight(new ItemStack(Items.IRON_INGOT), path, true);
        com.pockyl.hextale.network.PylonFlightsPayload payload = new com.pockyl.hextale.network.PylonFlightsPayload(
                helper.absolutePos(BASE), java.util.Collections.nCopies(HexPylonBlockEntity.SHOWN_FLIGHTS, flight));
        net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),
                helper.getLevel().registryAccess());
        com.pockyl.hextale.network.PylonFlightsPayload.STREAM_CODEC.encode(buf, payload);
        com.pockyl.hextale.network.PylonFlightsPayload back = com.pockyl.hextale.network.PylonFlightsPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(back.flights().size() == HexPylonBlockEntity.SHOWN_FLIGHTS, "every shown flight arrives");
        PylonFlight got = back.flights().getFirst();
        helper.assertTrue(got.icon().is(Items.IRON_INGOT) && got.gathered() && got.path().size() == path.size()
                && got.path().getLast().distanceTo(path.getLast()) < 1.0E-3, "the way arrives whole");
        // An essence stream carries no item: it must encode too (an empty stack once broke the connection).
        PylonFlight stream = PylonFlight.essence(path, 0x3366FF);
        net.minecraft.network.RegistryFriendlyByteBuf buf2 = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),
                helper.getLevel().registryAccess());
        com.pockyl.hextale.network.PylonFlightsPayload.STREAM_CODEC.encode(buf2,
                new com.pockyl.hextale.network.PylonFlightsPayload(helper.absolutePos(BASE), java.util.List.of(stream, flight)));
        com.pockyl.hextale.network.PylonFlightsPayload mixed = com.pockyl.hextale.network.PylonFlightsPayload.STREAM_CODEC.decode(buf2);
        helper.assertTrue(mixed.flights().getFirst().isEssence() && mixed.flights().getFirst().color() == 0x3366FF
                && mixed.flights().getFirst().icon().isEmpty() && mixed.flights().get(1).icon().is(Items.IRON_INGOT), "an essence stream arrives");
        helper.succeed();
    }

    /** With nothing to send, a pylon sleeps longer between looks. */
    @HexTest(maxTicks = 200)
    public static void anIdlePylonSleeps(GameTestHelper helper) {
        clear(helper, 2);
        helper.setBlock(BASE, Blocks.CHEST);
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        pylon.toggleLink(link(helper, BASE.offset(2, 0, 0), Direction.UP));
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(pylon.drowsiness() == 2, "it looks four times rarer");
            helper.succeed();
        });
    }

    private static Container chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        return (Container) helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(pos);
    }

    /**
     * A sorting matrix sends every item home: cobblestone to the chest that already keeps some, raw iron to a linked
     * furnace's input, coal to the furnace already burning coal; a chest that carries a pylon is no home, an empty chest
     * gets nothing, and what has no home stays in the dump chest.
     */
    @HexTest(maxTicks = 300)
    public static void aSortingMatrixSendsItemsHome(GameTestHelper helper) {
        clear(helper, 8);
        Container dump = chest(helper, BASE);
        dump.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        dump.setItem(1, new ItemStack(Items.RAW_IRON, 4));
        dump.setItem(2, new ItemStack(Items.COAL, 3));
        dump.setItem(3, new ItemStack(Items.DIAMOND, 2));
        dump.setItem(4, new ItemStack(Items.STICK, 5));
        Container cobble = chest(helper, BASE.offset(3, 0, 0));
        cobble.setItem(5, new ItemStack(Items.COBBLESTONE, 1));
        Container empty = chest(helper, BASE.offset(0, 0, 3));
        // A diamond lies in a chest under a pylon: that chest feeds the pylon, it is no store.
        Container fed = chest(helper, BASE.offset(-3, 0, 0));
        fed.setItem(0, new ItemStack(Items.DIAMOND, 1));
        place(helper, ModBlocks.SENDING_PYLON.get(), BASE.offset(-3, 1, 0));
        BlockPos furnacePos = BASE.offset(3, 0, 3);
        helper.setBlock(furnacePos, Blocks.FURNACE);
        FurnaceBlockEntity furnace = helper.<FurnaceBlockEntity>getBlockEntity(furnacePos);
        furnace.setItem(1, new ItemStack(Items.COAL, 1));
        HexPylonBlockEntity matrix = place(helper, ModBlocks.SORTING_MATRIX.get(), BASE.above());
        matrix.toggleLink(link(helper, furnacePos, Direction.UP));
        helper.succeedWhen(() -> {
            helper.assertTrue(count(cobble, Items.COBBLESTONE) == 11, "the cobblestone went to the chest that keeps it");
            // The furnace lights up with the coal and starts smelting: count what left the dump chest.
            helper.assertTrue(count(dump, Items.RAW_IRON) == 0 && furnace.getItem(0).is(Items.RAW_IRON), "the ore went into the furnace");
            helper.assertTrue(count(dump, Items.COAL) == 0 && furnace.getItem(1).is(Items.COAL), "the furnace burning coal got the coal");
            helper.assertTrue(count(dump, Items.DIAMOND) == 2 && count(fed, Items.DIAMOND) == 1, "a chest under a pylon is no home");
            helper.assertTrue(count(dump, Items.STICK) == 5 && count(empty, Items.STICK) == 0, "what has no home stays in the dump chest");
            helper.assertTrue(matrix.cargo().isEmpty(), "nothing is still on its way");
        });
    }

    /**
     * A relay with a filter naming sticks makes the empty chest behind it their home; a sending pylon linked to the matrix
     * ties to its dump chest and feeds it.
     */
    @HexTest(maxTicks = 300)
    public static void aFilteredRelayIsAHomeAndPylonsFeedTheMatrix(GameTestHelper helper) {
        clear(helper, 8);
        Container dump = chest(helper, BASE);
        BlockPos relayPos = BASE.offset(0, 0, 4);
        Container sticks = chest(helper, BASE.offset(4, 0, 4));
        HexPylonBlockEntity relay = place(helper, ModBlocks.RELAY_PYLON.get(), relayPos);
        relay.setFilter(filter(only(Items.STICK, 1)));
        relay.toggleLink(link(helper, BASE.offset(4, 0, 4), Direction.UP));
        HexPylonBlockEntity matrix = place(helper, ModBlocks.SORTING_MATRIX.get(), BASE.above());
        matrix.toggleLink(link(helper, relayPos, Direction.UP));
        // A sending pylon two blocks away, linked by clicks to the matrix: the link ties to the matrix's chest.
        Container source = chest(helper, BASE.offset(-3, 0, 0));
        source.setItem(0, new ItemStack(Items.STICK, 6));
        HexPylonBlockEntity sender = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.offset(-3, 1, 0));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        PylonLinking.start(player, sender);
        PylonLinking.link(player, helper.absolutePos(BASE.above()), Direction.UP);
        helper.assertTrue(sender.links().size() == 1 && sender.links().getFirst().pos().equals(helper.absolutePos(BASE)),
                "a link to the matrix goes into its chest");
        helper.succeedWhen(() -> {
            helper.assertTrue(count(sticks, Items.STICK) == 6 && count(dump, Items.STICK) == 0 && count(source, Items.STICK) == 0,
                    "the sticks went through the matrix to the chest behind the filtered relay");
        });
    }
}
