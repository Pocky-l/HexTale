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
        return helper.getBlockEntity(pos, HexPylonBlockEntity.class);
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
        Container chest = (Container) helper.getBlockEntity(BASE, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
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
        FurnaceBlockEntity furnace = helper.getBlockEntity(BASE, FurnaceBlockEntity.class);
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
        Container from = (Container) helper.getBlockEntity(BASE, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
        from.setItem(0, new ItemStack(Items.COAL, 8));
        from.setItem(1, new ItemStack(Items.RAW_IRON, 8));
        HexPylonBlockEntity pylon = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        pylon.toggleLink(link(helper, furnacePos, Direction.UP));
        FurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos, FurnaceBlockEntity.class);
        helper.succeedWhen(() -> {
            helper.assertTrue(count(from, Items.COAL) == 0 && count(from, Items.RAW_IRON) == 0, "coal and ore were sent");
            helper.assertTrue(furnace.getItem(1).is(Items.COAL), "coal went into the fuel slot");
            helper.assertTrue(furnace.getItem(0).is(Items.RAW_IRON), "ore went into the input");
        });
    }

    /** A sending pylon on a chest moves its contents into the linked chest. */
    @HexTest(maxTicks = 200)
    public static void sendingPylonEmptiesIntoItsLink(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, Blocks.CHEST);
        BlockPos target = BASE.offset(4, 0, 0);
        helper.setBlock(target, Blocks.CHEST);
        Container from = (Container) helper.getBlockEntity(BASE, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
        Container to = (Container) helper.getBlockEntity(target, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
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
        FurnaceBlockEntity furnace = helper.getBlockEntity(BASE, FurnaceBlockEntity.class);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 8));
        furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 5));
        Container to = (Container) helper.getBlockEntity(target, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
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
        Container from = (Container) helper.getBlockEntity(BASE, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
        from.setItem(0, new ItemStack(Items.IRON_INGOT, 6));
        from.setItem(1, new ItemStack(Items.COBBLESTONE, 6));
        BlockPos relayPos = BASE.offset(5, 0, 0);
        BlockPos farChest = BASE.offset(10, 0, 0);
        helper.setBlock(farChest, Blocks.CHEST);
        HexPylonBlockEntity relay = place(helper, ModBlocks.RELAY_PYLON.get(), relayPos);
        relay.toggleLink(link(helper, farChest, Direction.UP));
        HexPylonBlockEntity sender = place(helper, ModBlocks.SENDING_PYLON.get(), BASE.above());
        sender.toggleLink(link(helper, relayPos, Direction.UP));
        Container far = (Container) helper.getBlockEntity(farChest, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
        helper.succeedWhen(() -> {
            helper.assertTrue(count(far, Items.IRON_INGOT) == 6 && count(far, Items.COBBLESTONE) == 6, "everything went through the relay");
            helper.assertTrue(sender.lastFlights().getFirst().path().contains(relay.gem()), "the flight passes the relay's gem");
        });
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
    @HexTest(maxTicks = 300)
    public static void siphonPoursEssenceAlongItsLinks(GameTestHelper helper) {
        clear(helper, 6);
        helper.setBlock(BASE, ModBlocks.ESSENCE_TANK.get());
        BlockPos near = BASE.offset(3, 0, 0);
        BlockPos relayPos = BASE.offset(0, 0, 4);
        BlockPos far = BASE.offset(0, 0, 8);
        helper.setBlock(near, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(far, ModBlocks.ESSENCE_TANK.get());
        com.pockyl.hextale.block.EssenceTankBlockEntity source = helper.getBlockEntity(BASE, com.pockyl.hextale.block.EssenceTankBlockEntity.class);
        com.pockyl.hextale.block.EssenceTankBlockEntity first = helper.getBlockEntity(near, com.pockyl.hextale.block.EssenceTankBlockEntity.class);
        com.pockyl.hextale.block.EssenceTankBlockEntity last = helper.getBlockEntity(far, com.pockyl.hextale.block.EssenceTankBlockEntity.class);
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
        HexPylonBlockEntity relay = helper.getBlockEntity(relayPos, HexPylonBlockEntity.class);
        helper.assertTrue(PylonLinking.takes(level, relay, helper.absolutePos(jar), Direction.UP)
                && PylonLinking.takes(level, relay, helper.absolutePos(chest), Direction.UP), "a relay links to a jar and to a chest");
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
        Container chest = (Container) helper.getBlockEntity(BASE, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
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
        Container from = (Container) helper.getBlockEntity(BASE, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
        Container to = (Container) helper.getBlockEntity(target, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
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
        Container from = (Container) helper.getBlockEntity(BASE, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
        Container to = (Container) helper.getBlockEntity(target, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
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
        Container from = (Container) helper.getBlockEntity(BASE, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
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
        Container iron = (Container) helper.getBlockEntity(ironChest, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
        Container rest = (Container) helper.getBlockEntity(restChest, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
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
        var source = helper.getBlockEntity(BASE, com.pockyl.hextale.block.EssenceTankBlockEntity.class);
        var fire = helper.getBlockEntity(fireAt, com.pockyl.hextale.block.EssenceTankBlockEntity.class);
        var plain = helper.getBlockEntity(plainAt, com.pockyl.hextale.block.EssenceTankBlockEntity.class);
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
        var fire = helper.getBlockEntity(fireAt, com.pockyl.hextale.block.EssenceTankBlockEntity.class);
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
}
