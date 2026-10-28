package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import com.pockyl.hextale.port.ResourceHandler;
import com.pockyl.hextale.port.FluidResource;
import com.pockyl.hextale.port.Transaction;

import com.pockyl.hextale.block.BigEssenceJarBlockEntity;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.essence.EssenceFluid;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.multiblock.Multiblocks;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModFluids;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.thevoid.Thinning;

/**
 * Pure essence as a fluid for other mods' tanks and pipes: a jar of one essence gives it, a mixture gives nothing, jars
 * take what their sticker allows, the creative jar gives every essence without end, aborted transfers change nothing.
 */
public final class EssenceFluidGameTests {
    private EssenceFluidGameTests() {
    }

    private static final BlockPos AT = new BlockPos(0, 2, 0);

    private static int insert(ResourceHandler<FluidResource> handler, FluidResource resource, int amount, boolean commit) {
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = handler.insert(resource, amount, transaction);
            if (commit) {
                transaction.commit();
            }
            return moved;
        }
    }

    private static int extract(ResourceHandler<FluidResource> handler, FluidResource resource, int amount, boolean commit) {
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = handler.extract(resource, amount, transaction);
            if (commit) {
                transaction.commit();
            }
            return moved;
        }
    }

    private static EssenceTankBlockEntity jar(GameTestHelper helper, EssenceMix contents) {
        helper.setBlock(AT.below(), Blocks.STONE);
        helper.setBlock(AT, ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity jar = helper.<EssenceTankBlockEntity>getBlockEntity(AT);
        jar.tank().set(contents);
        return jar;
    }

    private static ResourceHandler<FluidResource> handler(GameTestHelper helper, BlockPos at, Direction side) {
        ResourceHandler<FluidResource> handler = com.pockyl.hextale.port.ResourceHandler.ofFluidBlock(helper.getLevel(), helper.absolutePos(at), side);
        helper.assertTrue(handler != null, "a fluid handler at " + at);
        return handler;
    }

    /** A jar of fire alone gives fire essence fluid and loses what it gives; an aborted draw leaves it whole. */
    @HexTest
    public static void aPureJarGivesItsEssenceAsFluid(GameTestHelper helper) {
        FluidResource fire = EssenceFluid.resource(Essence.FIRE);
        FluidResource water = EssenceFluid.resource(Essence.WATER);
        EssenceTankBlockEntity jar = jar(helper, EssenceMix.pure(Essence.FIRE, 100));
        ResourceHandler<FluidResource> port = handler(helper, AT, Direction.NORTH);
        float thinning = Thinning.get(helper.getLevel(), helper.absolutePos(AT));
        helper.assertTrue(port.size() == 1 && port.getResource(0).equals(fire) && port.getAmountAsLong(0) == 100,
                "it shows 100 mB of fire essence");
        helper.assertTrue(fire.getFluid() == ModFluids.ESSENCE.get() && EssenceFluid.kind(fire) == Essence.FIRE, "the fluid carries its kind");
        helper.assertTrue("fluid_type.hextale.essence.fire".equals(EssenceFluid.stack(Essence.FIRE, 1).getDescriptionId()),
                "it is named after its essence");
        helper.assertTrue(extract(port, fire, 40, false) == 40 && jar.tank().amount() == 100, "an aborted draw leaves the jar whole");
        helper.assertTrue(extract(port, water, 40, true) == 0, "it holds no water");
        helper.assertTrue(extract(port, FluidResource.of(ModFluids.ESSENCE.get()), 40, true) == 0, "essence without a kind is nothing");
        helper.assertTrue(extract(port, fire, 40, true) == 40 && jar.tank().contents().equals(EssenceMix.pure(Essence.FIRE, 60)),
                "a committed draw takes 40 mB out of the jar");
        helper.assertTrue(extract(port, fire, 1000, true) == 60 && jar.tank().isEmpty(), "the rest comes out and the jar is empty");
        helper.assertTrue(port.getResource(0).isEmpty() && port.getAmountAsLong(0) == 0, "an empty jar shows nothing");
        helper.assertTrue(Thinning.get(helper.getLevel(), helper.absolutePos(AT)) == thinning, "moving essence wears no land thin");
        helper.succeed();
    }

    /** A mixture, or essence stained with the unknown, is never split: the jar shows nothing and gives nothing. */
    @HexTest
    public static void aMixedJarGivesNoFluid(GameTestHelper helper) {
        FluidResource fire = EssenceFluid.resource(Essence.FIRE);
        FluidResource water = EssenceFluid.resource(Essence.WATER);
        EssenceTankBlockEntity jar = jar(helper, EssenceMix.pure(Essence.FIRE, 50).plus(EssenceMix.pure(Essence.WATER, 50)));
        ResourceHandler<FluidResource> port = handler(helper, AT, Direction.UP);
        helper.assertTrue(port.getResource(0).isEmpty() && port.getAmountAsLong(0) == 0, "a mixture shows no fluid");
        helper.assertTrue(extract(port, fire, 10, true) == 0 && extract(port, water, 10, true) == 0, "nothing comes out of a mixture");
        jar.tank().set(EssenceMix.pure(Essence.FIRE, 50).plus(EssenceMix.unknown(5)));
        helper.assertTrue(extract(port, fire, 10, true) == 0, "nor out of essence with an unknown share");
        helper.assertTrue(jar.tank().amount() == 55, "the jar keeps all of it");
        helper.succeed();
    }

    /**
     * An empty jar takes any essence and mixes it like a pipe pouring it in; an aborted pour leaves nothing; it fills to
     * the brim only; essence without a kind and plain water are refused.
     */
    @HexTest
    public static void anEmptyJarTakesEssenceFluid(GameTestHelper helper) {
        FluidResource fire = EssenceFluid.resource(Essence.FIRE);
        FluidResource water = EssenceFluid.resource(Essence.WATER);
        EssenceTankBlockEntity jar = jar(helper, EssenceMix.EMPTY);
        ResourceHandler<FluidResource> port = handler(helper, AT, Direction.EAST);
        helper.assertTrue(insert(port, water, 100, false) == 100 && jar.tank().isEmpty(), "an aborted pour leaves the jar empty");
        helper.assertTrue(insert(port, water, 100, true) == 100 && jar.tank().contents().equals(EssenceMix.pure(Essence.WATER, 100)),
                "a committed pour fills it");
        helper.assertTrue(insert(port, fire, 50, true) == 50
                && jar.tank().contents().equals(EssenceMix.pure(Essence.WATER, 100).plus(EssenceMix.pure(Essence.FIRE, 50))),
                "another essence mixes in, like from a pipe");
        helper.assertTrue(port.getResource(0).isEmpty(), "and the mixture is no fluid any more");
        helper.assertTrue(insert(port, fire, 1000, true) == EssenceTankBlockEntity.CAPACITY - 150, "it fills to the brim only");
        helper.assertTrue(insert(port, FluidResource.of(ModFluids.ESSENCE.get()), 10, true) == 0, "essence without a kind is refused");
        jar.tank().set(EssenceMix.EMPTY);
        helper.assertTrue(insert(port, FluidResource.of(Fluids.WATER), 10, true) == 0, "plain water is no essence");
        // Several moves in one transaction count together and land only on its commit.
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertTrue(port.insert(fire, 30, transaction) == 30 && port.getAmountAsLong(0) == 30, "the pour is seen inside");
            helper.assertTrue(port.extract(fire, 10, transaction) == 10 && port.getAmountAsLong(0) == 20, "and so is the draw");
            helper.assertTrue(jar.tank().isEmpty(), "the jar itself is untouched until the commit");
            transaction.commit();
        }
        helper.assertTrue(jar.tank().contents().equals(EssenceMix.pure(Essence.FIRE, 20)), "the commit leaves 20 mB of fire");
        helper.succeed();
    }

    /** A jar with a fire sticker takes fire essence and refuses water essence. */
    @HexTest
    public static void aStickeredJarTakesOnlyItsEssenceFluid(GameTestHelper helper) {
        FluidResource fire = EssenceFluid.resource(Essence.FIRE);
        FluidResource water = EssenceFluid.resource(Essence.WATER);
        EssenceTankBlockEntity jar = jar(helper, EssenceMix.EMPTY);
        jar.setSticker(Essence.FIRE);
        ResourceHandler<FluidResource> port = handler(helper, AT, Direction.WEST);
        helper.assertTrue(!port.isValid(0, water) && port.isValid(0, fire), "it names fire as the only valid fluid");
        helper.assertTrue(insert(port, water, 50, true) == 0 && jar.tank().isEmpty(), "water essence is refused");
        helper.assertTrue(insert(port, fire, 50, true) == 50 && jar.tank().contents().equals(EssenceMix.pure(Essence.FIRE, 50)),
                "fire essence goes in");
        helper.succeed();
    }

    /** The Creative Essence Jar shows every essence, gives any of them without end and takes nothing in. */
    @HexTest
    public static void theCreativeJarGivesEndlessFluid(GameTestHelper helper) {
        FluidResource fire = EssenceFluid.resource(Essence.FIRE);
        FluidResource water = EssenceFluid.resource(Essence.WATER);
        helper.setBlock(AT.below(), Blocks.STONE);
        helper.setBlock(AT, ModBlocks.CREATIVE_ESSENCE_TANK.get());
        EssenceTankBlockEntity jar = helper.<EssenceTankBlockEntity>getBlockEntity(AT);
        ResourceHandler<FluidResource> port = handler(helper, AT, Direction.DOWN);
        helper.assertTrue(port.size() == Essence.COUNT, "one slot per essence");
        helper.assertTrue(port.getResource(Essence.MAGIC.ordinal()).equals(EssenceFluid.resource(Essence.MAGIC)), "magic among them");
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(extract(port, fire, 1000, true) == EssenceTankBlockEntity.CAPACITY, "fire again and again");
        }
        helper.assertTrue(extract(port, EssenceFluid.resource(Essence.CIRCUIT), 16, true) == 16, "any essence");
        helper.assertTrue(insert(port, water, 50, true) == 0, "it takes nothing in");
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(jar.tank().contents().equals(EssenceTankBlockEntity.CREATIVE), "it stays full of every essence");
            helper.succeed();
        });
    }

    /** The Big Essence Jar takes and gives essence fluid through any block of it, the controller and the parts alike. */
    @HexTest(padding = 3)
    public static void theBigJarTakesFluidThroughEveryBlock(GameTestHelper helper) {
        FluidResource fire = EssenceFluid.resource(Essence.FIRE);
        BlockPos corner = new BlockPos(0, 1, 0);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 3; z++) {
                    boolean middle = x == 1 && z == 1 && (y == 1 || y == 2);
                    helper.setBlock(corner.offset(x, y, z), middle ? Blocks.AIR : y == 3 ? Blocks.OAK_SLAB : Blocks.GLASS);
                }
            }
        }
        var found = Multiblocks.find(helper.getLevel(), helper.absolutePos(corner.offset(2, 3, 0)), new ItemStack(ModItems.HEXTECH_DUST.get()));
        helper.assertTrue(found.isPresent(), "the big jar is found");
        Multiblocks.form(helper.getLevel(), found.get().getKey(), found.get().getValue(), null);
        BigEssenceJarBlockEntity jar = helper.<BigEssenceJarBlockEntity>getBlockEntity(corner.offset(1, 0, 1));
        ResourceHandler<FluidResource> side = handler(helper, corner.offset(0, 2, 1), Direction.WEST);
        helper.assertTrue(insert(side, fire, 1000, true) == 1000 && jar.tank().amount() == 1000, "a side block takes fire essence");
        ResourceHandler<FluidResource> bottom = handler(helper, corner.offset(1, 0, 1), Direction.DOWN);
        helper.assertTrue(bottom.getCapacityAsLong(0, fire) == BigEssenceJarBlockEntity.CAPACITY, "it holds sixteen jars");
        helper.assertTrue(extract(bottom, fire, 400, true) == 400 && jar.tank().amount() == 600, "the controller gives it back");
        helper.succeed();
    }
}
