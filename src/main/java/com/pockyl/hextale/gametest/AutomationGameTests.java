package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import com.pockyl.hextale.port.ResourceHandler;
import com.pockyl.hextale.port.FluidResource;
import com.pockyl.hextale.port.ItemResource;
import com.pockyl.hextale.port.Transaction;

import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.block.CrucibleBlockEntity;
import com.pockyl.hextale.block.CrystalBatteryBlockEntity;
import com.pockyl.hextale.block.HexWorkbenchBlockEntity;
import com.pockyl.hextale.block.LapidaryBlockEntity;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.transport.ExperienceOutlet;

/**
 * Other mods' pipes, tanks and hoppers through the capabilities: water goes into the crucible and the alembic, crystals
 * into the battery and the workbench socket, items into a boiling crucible, crystals and discs into the lapidary.
 */
public final class AutomationGameTests {
    private AutomationGameTests() {
    }

    private static final BlockPos AT = new BlockPos(0, 2, 0);

    private static <T> int insert(ResourceHandler<T> handler, T resource, int amount,
                                                                                             boolean commit) {
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = handler.insert(resource, amount, transaction);
            if (commit) {
                transaction.commit();
            }
            return moved;
        }
    }

    private static <T> int extract(ResourceHandler<T> handler, T resource, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = handler.extract(resource, amount, transaction);
            transaction.commit();
            return moved;
        }
    }

    /** A pipe fills the crucible up to the brim and no further; an aborted transfer leaves nothing; lava is refused. */
    @HexTest
    public static void waterThroughTheFluidCapabilityFillsTheCrucible(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        CrucibleBlockEntity crucible = helper.<CrucibleBlockEntity>getBlockEntity(AT);
        ResourceHandler<FluidResource> inlet = com.pockyl.hextale.port.ResourceHandler.ofFluidBlock(level, pos, Direction.NORTH);
        helper.assertTrue(inlet != null, "the crucible has a fluid handler");
        FluidResource water = FluidResource.of(Fluids.WATER);
        helper.assertTrue(insert(inlet, water, 1000, false) == 1000 && crucible.water() == 0, "an aborted transfer leaves no water");
        helper.assertTrue(insert(inlet, water, 1000, true) == 1000 && crucible.water() == 1000, "a committed one does");
        helper.assertTrue(insert(inlet, water, 1000, true) == CrucibleBlockEntity.CAPACITY - 1000, "pipes fill it to the brim only");
        helper.assertTrue(crucible.water() == CrucibleBlockEntity.CAPACITY, "it is brim-full");
        helper.assertTrue(insert(inlet, FluidResource.of(Fluids.LAVA), 100, true) == 0, "lava is no water");
        helper.assertTrue(extract(inlet, water, 100) == 0, "nothing comes out");
        helper.succeed();
    }

    /** The alembic takes piped water through its sides only, up to its flask. */
    @HexTest
    public static void theAlembicTakesPipedWaterThroughItsSides(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.ALEMBIC.get());
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        helper.assertTrue(com.pockyl.hextale.port.ResourceHandler.ofFluidBlock(level, pos, Direction.UP) == null, "no water from above");
        ResourceHandler<FluidResource> inlet = com.pockyl.hextale.port.ResourceHandler.ofFluidBlock(level, pos, Direction.EAST);
        helper.assertTrue(inlet != null, "a side takes water");
        int put = insert(inlet, FluidResource.of(Fluids.WATER), 2500, true);
        AlembicBlockEntity alembic = helper.<AlembicBlockEntity>getBlockEntity(AT);
        helper.assertTrue(put == AlembicBlockEntity.WATER_CAPACITY && alembic.water() == AlembicBlockEntity.WATER_CAPACITY,
                "it fills its flask and no more (" + put + ")");
        helper.succeed();
    }

    /** Hoppers feed the battery crystals and the workbench socket; neither gives anything back. */
    @HexTest
    public static void crystalsGoInThroughTheItemCapability(GameTestHelper helper) {
        BlockPos benchAt = AT.east(2);
        helper.setBlock(AT, ModBlocks.CRYSTAL_BATTERY.get());
        helper.setBlock(benchAt, ModBlocks.HEX_WORKBENCH.get());
        ServerLevel level = helper.getLevel();
        ResourceHandler<ItemResource> battery = com.pockyl.hextale.port.ResourceHandler.ofBlock(level, helper.absolutePos(AT), Direction.UP);
        helper.assertTrue(battery != null, "the battery has an item handler");
        ItemResource orb = ItemResource.of(ModItems.UNSTABLE_HEXTECH.get());
        helper.assertTrue(insert(battery, ItemResource.of(Items.STONE), 4, true) == 0, "stone is no crystal");
        helper.assertTrue(insert(battery, orb, 3, false) == 3, "it would take three orbs");
        CrystalBatteryBlockEntity store = helper.<CrystalBatteryBlockEntity>getBlockEntity(AT);
        helper.assertTrue(store.charge() == 0, "an aborted transfer soaks nothing");
        helper.assertTrue(insert(battery, orb, 3, true) == 3, "it takes three orbs");
        helper.assertTrue(store.charge() >= 3 * HexWorkbenchBlockEntity.UNSTABLE_CHARGE, "they soak into charge (" + store.charge() + ")");
        int room = (store.capacity() - store.charge()) / HexWorkbenchBlockEntity.UNSTABLE_CHARGE;
        helper.assertTrue(insert(battery, orb, room + 5, true) == room, "no more than there is room for");

        ResourceHandler<ItemResource> socket = com.pockyl.hextale.port.ResourceHandler.ofBlock(level, helper.absolutePos(benchAt), Direction.NORTH);
        helper.assertTrue(socket != null && socket.size() == 1, "the workbench shows only its socket");
        helper.assertTrue(insert(socket, ItemResource.of(Items.STICK), 1, true) == 0, "a stick does not fit the socket");
        helper.assertTrue(insert(socket, orb, 2, true) == 2, "orbs go into the socket");
        HexWorkbenchBlockEntity bench = helper.<HexWorkbenchBlockEntity>getBlockEntity(benchAt);
        helper.assertTrue(bench.getItem(HexWorkbenchBlockEntity.CRYSTAL).is(ModItems.UNSTABLE_HEXTECH.get())
                && bench.getItem(0).isEmpty(), "they lie in the socket, the grid stays empty");
        helper.assertTrue(extract(socket, orb, 2) == 0, "nothing comes back out");
        helper.succeed();
    }

    /** Lapis piped into boiling water dissolves like lapis dropped in; nothing goes in from below. */
    @HexTest(maxTicks = 700)
    public static void pipedItemsSinkIntoBoilingWater(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.LAVA);
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.<CrucibleBlockEntity>getBlockEntity(AT);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        helper.assertTrue(com.pockyl.hextale.port.ResourceHandler.ofBlock(level, pos, Direction.DOWN) == null, "nothing from below");
        boolean[] piped = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            int magic = CrucibleBlockEntity.essencesOf(new ItemStack(Items.LAPIS_LAZULI)).get(Essence.MAGIC);
            if (!piped[0]) {
                ResourceHandler<ItemResource> intake = com.pockyl.hextale.port.ResourceHandler.ofBlock(level, pos, Direction.UP);
                helper.assertTrue(intake != null, "the crucible has an item handler");
                helper.assertTrue(insert(intake, ItemResource.of(Items.LAPIS_LAZULI), 2, true) == 2, "it takes the lapis");
                piped[0] = true;
            }
            helper.assertTrue(magic > 0 && crucible.essence().get(Essence.MAGIC) == 2 * magic, "the piped lapis dissolved");
        });
    }

    /** A crystal from above and a disc from the side make a fresh cut a worker can grind at once. */
    @HexTest
    public static void theLapidaryIsStockedThroughItsFaces(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.HEX_LAPIDARY.get());
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        ResourceHandler<ItemResource> top = com.pockyl.hextale.port.ResourceHandler.ofBlock(level, pos, Direction.UP);
        ResourceHandler<ItemResource> side = com.pockyl.hextale.port.ResourceHandler.ofBlock(level, pos, Direction.NORTH);
        helper.assertTrue(top != null && side != null, "the lapidary has item handlers");
        helper.assertTrue(insert(top, ItemResource.of(ModItems.UNSTABLE_HEXTECH.get()), 4, true) == 4, "orbs go in from above");
        helper.assertTrue(insert(side, ItemResource.of(ModItems.CUTTING_DISC.get()), 1, true) == 1, "a disc goes in from the side");
        LapidaryBlockEntity lapidary = helper.<LapidaryBlockEntity>getBlockEntity(AT);
        helper.assertTrue(lapidary.blocked() == null && lapidary.cut() != null, "it is ready to cut");
        lapidary.stroke(helper.makeMockServerPlayerInLevel(), 0, 10);
        ResourceHandler<ItemResource> bottom = com.pockyl.hextale.port.ResourceHandler.ofBlock(level, pos, Direction.DOWN);
        helper.assertTrue(extract(bottom, ItemResource.of(ModItems.UNSTABLE_HEXTECH.get()), 4) == 0, "the clamp gives nothing back");
        helper.succeed();
    }

    /** Without a mod that adds liquid experience the condenser offers no fluid at all. */
    @HexTest
    public static void theCondenserOffersLiquidExperienceOnlyWhenItExists(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.XP_CONDENSER.get());
        boolean offered = com.pockyl.hextale.port.ResourceHandler.ofFluidBlock(helper.getLevel(), helper.absolutePos(AT), Direction.NORTH) != null;
        helper.assertTrue(offered == (ExperienceOutlet.fluid() != null), "a fluid handler only with liquid experience");
        helper.succeed();
    }
}
