package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.CrystalBatteryBlockEntity;
import com.pockyl.hextale.block.HexWorkbenchBlockEntity;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.runework.BatteryRune;
import com.pockyl.hextale.runework.BulwarkHeart;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.RuneHolder;
import com.pockyl.hextale.runework.WorkbenchRune;

import java.util.List;

/**
 * The Awakened Workbench after the Crystal Battery: it holds runes and its charge when it is broken, a comparator reads
 * it, every bench feeds a Bulwark heart, Chain Lightning shares charge between benches. The retired battery gives back
 * what is in it.
 */
public final class WorkbenchGameTests {
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    private WorkbenchGameTests() {
    }

    private static HexWorkbenchBlockEntity bench(GameTestHelper helper, BlockPos at, WorkbenchRune rune, int stars) {
        helper.setBlock(at, ModBlocks.HEX_WORKBENCH.get());
        HexWorkbenchBlockEntity bench = helper.getBlockEntity(at, HexWorkbenchBlockEntity.class);
        if (rune != null) {
            bench.setRunes(PickaxeRunes.EMPTY.with(rune.rune(), stars));
        }
        return bench;
    }

    private static List<ItemEntity> dropped(GameTestHelper helper, BlockPos at) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(at)).inflate(2));
    }

    private static ServerPlayer wearer(GameTestHelper helper, double x, double z) {
        ServerPlayer wearer = MockPlayers.inLevel(helper);
        Vec3 at = helper.absoluteVec(new Vec3(x, 1, z));
        wearer.snapTo(at.x, at.y, at.z, 0, 0);
        wearer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.BULWARK_CHESTPLATE.get()));
        return wearer;
    }

    private static int heart(ServerPlayer wearer) {
        return BulwarkHeart.of(wearer.getItemBySlot(EquipmentSlot.CHEST)).charge();
    }

    /** Cache: the socket soaks in past the plain pool, and the comparator reads the share of the bigger one. */
    @HexTest(maxTicks = 200)
    public static void aCacheRaisesThePoolAndAComparatorReadsIt(GameTestHelper helper) {
        HexWorkbenchBlockEntity bench = bench(helper, AT, WorkbenchRune.CACHE, 3);
        helper.assertTrue(bench.capacity() == 600, "three stars hold what a battery held, got " + bench.capacity());
        helper.assertTrue(helper.getBlockState(AT).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(AT), Direction.NORTH) == 0,
                "an empty bench gives no signal");
        bench.setItem(HexWorkbenchBlockEntity.CRYSTAL, new ItemStack(ModItems.UNSTABLE_HEXTECH.get(), 64));
        helper.succeedWhen(() -> {
            helper.assertTrue(bench.charge() > HexWorkbenchBlockEntity.MAX_CHARGE, "the socket soaks past the plain pool, got " + bench.charge());
            int signal = helper.getBlockState(AT).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(AT), Direction.NORTH);
            helper.assertTrue(signal > 0 && signal < 15, "the comparator reads a part of the pool, got " + signal);
            helper.assertTrue(bench.receive(1000) > 0 && bench.signal() == 15, "a full bench gives the full signal");
        });
    }

    /** Runes and charge stay with a bench that is broken and placed again. */
    @HexTest
    public static void aBrokenBenchKeepsItsRunesAndCharge(GameTestHelper helper) {
        HexWorkbenchBlockEntity bench = bench(helper, AT, WorkbenchRune.CACHE, 2);
        bench.receive(200);
        helper.getLevel().destroyBlock(helper.absolutePos(AT), true);
        ItemStack item = dropped(helper, AT).stream().map(ItemEntity::getItem).filter(stack -> stack.is(ModItems.HEX_WORKBENCH.get())).findFirst()
                .orElseThrow(() -> helper.assertionException("the bench dropped"));
        helper.assertTrue(RuneHolder.of(item) == RuneHolder.WORKBENCH, "the altar takes it as a workbench");
        helper.assertTrue(RuneHolder.WORKBENCH.runes(item).strength(WorkbenchRune.CACHE.rune()) == 2, "the runes stay on the item");
        helper.assertTrue(item.getOrDefault(ModDataComponents.WORKBENCH_CHARGE.get(), 0) == 200, "the charge stays on the item");
        HexWorkbenchBlockEntity again = bench(helper, AT, null, 0);
        again.applyComponentsFromItemStack(item);
        helper.assertTrue(again.charge() == 200 && again.capacity() == 300, "placed again it has them: " + again.charge() + " / " + again.capacity());
        helper.succeed();
    }

    /** Thrift: some crystals give more than they hold, whether they soak in by themselves or are used up by a craft. */
    @HexTest
    public static void thriftGetsMoreOutOfCrystals(GameTestHelper helper) {
        HexWorkbenchBlockEntity bench = bench(helper, AT, WorkbenchRune.THRIFT, 3);
        bench.setItem(HexWorkbenchBlockEntity.CRYSTAL, new ItemStack(ModItems.UNSTABLE_HEXTECH.get(), 64));
        helper.assertTrue(bench.spend(64 * HexWorkbenchBlockEntity.UNSTABLE_CHARGE), "the socket pays");
        helper.assertTrue(bench.chargeAvailable() > 0, "something is left over what the crystals hold");
        helper.succeed();
    }

    /** A bench with no rune at all feeds a heart in reach, and a full heart costs the orbs it costs by hand. */
    @HexTest(maxTicks = 500)
    public static void everyBenchFeedsABulwarkHeart(GameTestHelper helper) {
        int orbsByHand = BulwarkHeart.MAX / BulwarkHeart.PER_CRYSTAL;
        int chargeOfThoseOrbs = orbsByHand * HexWorkbenchBlockEntity.UNSTABLE_CHARGE;
        HexWorkbenchBlockEntity bench = bench(helper, AT, null, 0);
        bench.receive(HexWorkbenchBlockEntity.MAX_CHARGE);
        ServerPlayer wearer = wearer(helper, 3.5, 1.5);
        helper.succeedWhen(() -> {
            helper.assertTrue(heart(wearer) == BulwarkHeart.MAX, "the bench filled the heart");
            helper.assertTrue(HexWorkbenchBlockEntity.MAX_CHARGE - bench.charge() == chargeOfThoseOrbs,
                    "a full heart took the charge of the orbs a refill by hand takes");
        });
    }

    /** Chain Lightning reaches a heart the plain bench cannot. */
    @HexTest(maxTicks = 80)
    public static void chainLightningWidensTheReach(GameTestHelper helper) {
        HexWorkbenchBlockEntity bench = bench(helper, AT, null, 0);
        bench.receive(HexWorkbenchBlockEntity.MAX_CHARGE);
        ServerPlayer wearer = wearer(helper, 7.5, 1.5);
        helper.runAfterDelay(HexWorkbenchBlockEntity.FEED_TICKS + 5, () -> {
            helper.assertTrue(heart(wearer) == 0, "out of reach of a plain bench");
            bench.setRunes(PickaxeRunes.EMPTY.with(WorkbenchRune.CHAIN_LIGHTNING.rune(), 1));
            helper.assertTrue(bench.feedRate() > HexWorkbenchBlockEntity.FEED, "the rune feeds faster");
            helper.succeedWhen(() -> helper.assertTrue(heart(wearer) > 0, "the rune reached the heart"));
        });
    }

    /** A plain bench keeps its charge; with Chain Lightning it levels out with the benches around, nothing lost. */
    @HexTest(maxTicks = 200)
    public static void chainLightningSharesChargeBetweenBenches(GameTestHelper helper) {
        HexWorkbenchBlockEntity hub = bench(helper, AT, null, 0);
        HexWorkbenchBlockEntity other = bench(helper, AT.east(3), null, 0);
        hub.receive(HexWorkbenchBlockEntity.MAX_CHARGE);
        helper.runAfterDelay(HexWorkbenchBlockEntity.FEED_TICKS + 5, () -> {
            helper.assertTrue(other.charge() == 0, "a plain bench shares nothing");
            hub.setRunes(PickaxeRunes.EMPTY.with(WorkbenchRune.CHAIN_LIGHTNING.rune(), 1));
            helper.succeedWhen(() -> {
                helper.assertTrue(Math.abs(hub.charge() - other.charge()) <= 1, "they level out: " + hub.charge() + " and " + other.charge());
                helper.assertTrue(hub.charge() + other.charge() == HexWorkbenchBlockEntity.MAX_CHARGE, "no charge lost");
            });
        });
    }

    /** A battery of an old world gives back its runes as scrolls with their stars and its charge as orbs, rounded up. */
    @HexTest
    public static void aBrokenBatteryGivesBackItsRunesAndCharge(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.CRYSTAL_BATTERY.get());
        ItemStack old = new ItemStack(ModItems.CRYSTAL_BATTERY.get());
        RuneHolder.BATTERY.set(old, PickaxeRunes.EMPTY.with(BatteryRune.CACHE.rune(), 2).with(BatteryRune.SUNLIGHT_CELL.rune(), 3));
        old.set(ModDataComponents.BATTERY_CHARGE.get(), 321);
        CrystalBatteryBlockEntity battery = helper.getBlockEntity(AT, CrystalBatteryBlockEntity.class);
        battery.applyComponentsFromItemStack(old);
        helper.assertTrue(battery.charge() == 321, "the old block holds its charge");
        helper.getLevel().destroyBlock(helper.absolutePos(AT), true);
        List<ItemStack> drops = dropped(helper, AT).stream().map(ItemEntity::getItem).toList();
        int orbs = drops.stream().filter(stack -> stack.is(ModItems.UNSTABLE_HEXTECH.get())).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(orbs == 65, "321 charge is 65 orbs, got " + orbs);
        helper.assertTrue(drops.stream().anyMatch(stack -> RuneScrollItem.rune(stack) == BatteryRune.CACHE.rune() && RuneScrollItem.stars(stack) == 2),
                "Cache came back with its stars: " + drops);
        helper.assertTrue(drops.stream().anyMatch(stack -> RuneScrollItem.rune(stack) == BatteryRune.SUNLIGHT_CELL.rune() && RuneScrollItem.stars(stack) == 3),
                "Sunlight Cell came back with its stars: " + drops);
        ItemStack shell = drops.stream().filter(stack -> stack.is(ModItems.CRYSTAL_BATTERY.get())).findFirst()
                .orElseThrow(() -> helper.assertionException("the empty battery dropped"));
        helper.assertTrue(RuneHolder.BATTERY.runes(shell).isEmpty() && !shell.has(ModDataComponents.BATTERY_CHARGE.get()), "nothing is given back twice");
        helper.succeed();
    }

    /** The battery has no recipe and no step in the diary; its topic id stays for modpacks and its runes moved to the bench. */
    @HexTest
    public static void theBatteryIsRetired(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().getServer().getResourceManager().getResource(Hextale.id("recipe/infusion/crystal_battery.json")).isEmpty(),
                "no recipe makes it");
        helper.assertTrue(Guide.server().all().stream().noneMatch(step -> step.id().equals("crystal_battery") || step.topic() == ResearchTopics.CRYSTAL_BATTERY),
                "no step of the diary leads to it");
        helper.assertTrue(ResearchTopics.all().stream().anyMatch(topic -> topic.id().equals("crystal_battery")), "the topic id stays");
        helper.assertTrue(RuneHolder.BATTERY.retired() && !RuneHolder.WORKBENCH.retired(), "only the battery is retired");
        for (BatteryRune rune : BatteryRune.values()) {
            helper.assertTrue(RuneHolder.WORKBENCH.takes(rune.rune()), "the bench takes " + rune);
        }
        helper.succeed();
    }
}
