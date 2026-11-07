package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.HerdkeeperBlock;
import com.pockyl.hextale.block.HerdkeeperBlockEntity;
import com.pockyl.hextale.block.ShapePartBlock;
import com.pockyl.hextale.block.ShapeParts;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.farm.Herding;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HerdkeeperRune;
import com.pockyl.hextale.runework.PickaxeRunes;

import java.util.ArrayList;
import java.util.List;

/** The Herdkeeper: produce from an animal's own loot for life essence, with no harm, no breeding and a rest per animal. */
public final class HerdkeeperGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);
    private static final BlockPos JAR = BASE.offset(0, 0, -3);
    private static final BlockPos CHEST = BASE.west();
    private static final int ROOM = 12;

    private HerdkeeperGameTests() {
    }

    private static void clear(GameTestHelper helper) {
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                helper.setBlock(BASE.offset(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= 3; y++) {
                    helper.setBlock(BASE.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BASE)).inflate(12)).forEach(ItemEntity::discard);
    }

    /** A Herdkeeper with a chest beside it and a jar of life essence holding {@code essence} mB (none when 0). */
    private static HerdkeeperBlockEntity keeper(GameTestHelper helper, int essence) {
        clear(helper);
        if (essence > 0) {
            helper.setBlock(JAR, ModBlocks.ESSENCE_TANK.get());
            helper.<EssenceTankBlockEntity>getBlockEntity(JAR).tank().set(EssenceMix.pure(Essence.LIFE, essence));
        }
        helper.setBlock(CHEST, Blocks.CHEST);
        helper.setBlock(BASE, ModBlocks.HERDKEEPER.get());
        return helper.<HerdkeeperBlockEntity>getBlockEntity(BASE);
    }

    private static int jar(GameTestHelper helper) {
        return helper.<EssenceTankBlockEntity>getBlockEntity(JAR).tank().amount();
    }

    private static int chest(GameTestHelper helper, Item item) {
        return helper.<ChestBlockEntity>getBlockEntity(CHEST).countItem(item);
    }

    private static <T extends Mob> T animal(GameTestHelper helper, EntityType<T> type, BlockPos at) {
        return helper.spawnWithNoFreeWill(type, at);
    }

    /** A cow gives its beef into the chest for one payment, and stands there as it stood: same health, no love, same age. */
    @HexTest(maxTicks = 200, padding = ROOM)
    public static void aCowGivesBeefUnhurt(GameTestHelper helper) {
        keeper(helper, 10);
        Animal cow = animal(helper, EntityType.COW, BASE.offset(3, 0, 0));
        int age = cow.getAge();
        helper.succeedWhen(() -> {
            helper.assertTrue(chest(helper, Items.BEEF) >= 1, "the beef is in the chest");
            helper.assertTrue(jar(helper) == 10 - HerdkeeperBlockEntity.MB_PER_TEND, "one tend was paid");
            helper.assertTrue(cow.isAlive() && cow.getHealth() == cow.getMaxHealth(), "the cow is not hurt");
            helper.assertFalse(cow.isInLove(), "and does not breed");
            helper.assertTrue(cow.getAge() == age, "nor age");
        });
    }

    /** Each animal gives its own loot, and what it gives alive: the chicken's egg comes with every roll. */
    @HexTest(padding = ROOM)
    public static void everyAnimalGivesItsOwnLoot(GameTestHelper helper) {
        clear(helper);
        Mob chicken = animal(helper, EntityType.CHICKEN, BASE);
        Mob cow = animal(helper, EntityType.COW, BASE.east(3));
        Mob sheep = animal(helper, EntityType.SHEEP, BASE.west(3));
        List<ItemStack> fromChicken = new ArrayList<>();
        List<ItemStack> fromCow = new ArrayList<>();
        List<ItemStack> fromSheep = new ArrayList<>();
        for (int roll = 0; roll < 30; roll++) {
            List<ItemStack> eggs = Herding.gather(helper.getLevel(), chicken);
            helper.assertTrue(eggs.stream().anyMatch(stack -> stack.is(Items.EGG)), "a chicken lays with every roll");
            fromChicken.addAll(eggs);
            fromCow.addAll(Herding.gather(helper.getLevel(), cow));
            fromSheep.addAll(Herding.gather(helper.getLevel(), sheep));
        }
        helper.assertTrue(fromChicken.stream().anyMatch(stack -> stack.is(Items.FEATHER)), "feathers");
        helper.assertTrue(fromChicken.stream().anyMatch(stack -> stack.is(Items.CHICKEN)), "chicken");
        helper.assertTrue(fromCow.stream().anyMatch(stack -> stack.is(Items.LEATHER)), "leather");
        helper.assertTrue(fromCow.stream().anyMatch(stack -> stack.is(Items.BEEF)), "beef");
        helper.assertTrue(fromSheep.stream().anyMatch(stack -> stack.is(Items.MUTTON)), "mutton");
        helper.assertTrue(chicken.isAlive() && cow.isAlive() && sheep.isAlive(), "all still alive");
        helper.succeed();
    }

    /** Grown passive animals only: no calf, no monster, no villager, no golem. */
    @HexTest(padding = ROOM)
    public static void onlyGrownPassiveAnimalsAreTended(GameTestHelper helper) {
        clear(helper);
        Animal calf = animal(helper, EntityType.COW, BASE);
        calf.setAge(-24000);
        helper.assertTrue(Herding.tendable(animal(helper, EntityType.COW, BASE.east())), "a cow");
        helper.assertTrue(Herding.tendable(animal(helper, EntityType.CHICKEN, BASE.east(2))), "a chicken");
        helper.assertFalse(Herding.tendable(calf), "a calf has nothing to give");
        helper.assertFalse(Herding.tendable(animal(helper, EntityType.ZOMBIE, BASE.west())), "a zombie");
        helper.assertFalse(Herding.tendable(animal(helper, EntityType.HOGLIN, BASE.west(2))), "a hoglin is an animal but no friend");
        helper.assertFalse(Herding.tendable(animal(helper, EntityType.VILLAGER, BASE.west(3))), "a villager");
        helper.assertFalse(Herding.tendable(animal(helper, EntityType.WANDERING_TRADER, BASE.west(4))), "a trader");
        helper.assertFalse(Herding.tendable(animal(helper, EntityType.IRON_GOLEM, BASE.north(4))), "a golem");
        helper.succeed();
    }

    /** A tended cow rests: no second roll for its keeper or for another one beside it, until the rest is over. */
    @HexTest(maxTicks = 700, padding = ROOM)
    public static void aTendedAnimalRests(GameTestHelper helper) {
        keeper(helper, 10);
        Animal cow = animal(helper, EntityType.COW, BASE.offset(3, 0, 0));
        int paid = 10 - HerdkeeperBlockEntity.MB_PER_TEND;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(jar(helper) == paid, "the first tend was paid"))
                .thenExecute(() -> helper.setBlock(BASE.south(3), ModBlocks.HERDKEEPER.get()))
                .thenIdle(200)
                .thenExecute(() -> {
                    helper.assertTrue(jar(helper) == paid, "no second tend while it rests, not even by a second keeper");
                    helper.assertTrue(Herding.resting(cow, helper.getLevel().getGameTime()), "the cow rests");
                    Herding.rest(cow, 0);
                })
                .thenWaitUntil(() -> helper.assertTrue(jar(helper) == paid - HerdkeeperBlockEntity.MB_PER_TEND, "rested, it gives again"))
                .thenSucceed();
    }

    /** Three cows give three times: each is paid for and each fills the chest. */
    @HexTest(maxTicks = 400, padding = ROOM)
    public static void moreAnimalsGiveMore(GameTestHelper helper) {
        keeper(helper, 10);
        animal(helper, EntityType.COW, BASE.offset(3, 0, 0));
        animal(helper, EntityType.COW, BASE.offset(3, 0, 3));
        animal(helper, EntityType.COW, BASE.offset(-3, 0, 3));
        helper.succeedWhen(() -> {
            helper.assertTrue(jar(helper) == 10 - 3 * HerdkeeperBlockEntity.MB_PER_TEND, "three tends were paid");
            helper.assertTrue(chest(helper, Items.BEEF) >= 3, "and at least three beef came in");
        });
    }

    /** Without essence nothing is gathered and the cow keeps its turn. */
    @HexTest(maxTicks = 200, padding = ROOM)
    public static void noEssenceGivesNothing(GameTestHelper helper) {
        HerdkeeperBlockEntity keeper = keeper(helper, 0);
        Animal cow = animal(helper, EntityType.COW, BASE.offset(3, 0, 0));
        helper.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    helper.assertTrue(keeper.hungry(), "it says it is out of essence");
                    helper.assertTrue(chest(helper, Items.BEEF) == 0 && chest(helper, Items.LEATHER) == 0, "the chest stays empty");
                    helper.assertFalse(Herding.resting(cow, helper.getLevel().getGameTime()), "the cow keeps its turn");
                    helper.assertFalse(helper.getBlockState(BASE).getValue(HerdkeeperBlock.ACTIVE), "the bell hangs still");
                })
                .thenSucceed();
    }

    /** With no container beside it the produce pops out of the trough, and the bell swings meanwhile. */
    @HexTest(maxTicks = 200, padding = ROOM)
    public static void withoutAContainerProducePopsOut(GameTestHelper helper) {
        keeper(helper, 10);
        helper.setBlock(CHEST, Blocks.AIR);
        animal(helper, EntityType.COW, BASE.offset(3, 0, 0));
        AABB around = new AABB(helper.absolutePos(BASE)).inflate(3);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(BASE).getValue(HerdkeeperBlock.ACTIVE), "the bell swings");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, item -> item.getItem().is(Items.BEEF)).size() >= 1, "the beef lies by the trough");
        });
    }

    /** A heap nobody takes away stops it before it costs anything. */
    @HexTest(maxTicks = 200, padding = ROOM)
    public static void aHeapedTroughStopsIt(GameTestHelper helper) {
        HerdkeeperBlockEntity keeper = keeper(helper, 10);
        animal(helper, EntityType.COW, BASE.offset(3, 0, 0));
        BlockPos at = helper.absolutePos(BASE.south());
        ItemEntity heap = new ItemEntity(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, new ItemStack(Items.WHEAT_SEEDS, HerdkeeperBlockEntity.TROUGH_LIMIT));
        heap.setNeverPickUp();
        heap.setUnlimitedLifetime();
        helper.getLevel().addFreshEntity(heap);
        helper.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    helper.assertTrue(keeper.full(), "it says the trough is heaped");
                    helper.assertTrue(jar(helper) == 10, "nothing was spent");
                })
                .thenSucceed();
    }

    /** Shift reaches a cow past the plain radius, Spring shortens its rest. */
    @HexTest(maxTicks = 200, padding = ROOM)
    public static void herdkeeperRunesReachFurtherAndRestLess(GameTestHelper helper) {
        HerdkeeperBlockEntity keeper = keeper(helper, 10);
        keeper.setRunes(PickaxeRunes.EMPTY.with(HerdkeeperRune.SHIFT.rune(), 1).with(HerdkeeperRune.SPRING.rune(), 1));
        helper.assertTrue(keeper.radius() == 8 && HerdkeeperRune.radius(0) == 6, "Shift widens the pasture");
        Animal cow = animal(helper, EntityType.COW, BASE.offset(8, 0, 0));
        long start = helper.getLevel().getGameTime();
        helper.succeedWhen(() -> {
            helper.assertTrue(chest(helper, Items.BEEF) >= 1, "the far cow was reached");
            long rest = cow.getData(ModAttachments.HERD_REST.get()) - start;
            helper.assertTrue(rest <= HerdkeeperRune.rest(1) + 200 && rest < HerdkeeperRune.rest(0), "Spring shortened its rest");
        });
    }

    /** Made only after the Lab Bench study. */
    @HexTest
    public static void theHerdkeeperWaitsForItsResearch(GameTestHelper helper) {
        var player = MockPlayers.inLevel(helper);
        ItemStack keeper = new ItemStack(ModItems.HERDKEEPER.get());
        var knowledge = Research.of(player);
        helper.assertTrue(ResearchTopics.HERDKEEPER.lab(), "worked out at the Lab Bench");
        helper.assertFalse(ResearchTopics.mayMake(player, keeper, knowledge), "locked before its research");
        Research.research(player, ResearchTopics.HERDKEEPER);
        helper.assertTrue(ResearchTopics.mayMake(player, keeper, knowledge), "open after it");
        helper.succeed();
    }

    /** The crook reaches into the cell above, and a stand-in there speaks for the device. */
    @HexTest(padding = ROOM)
    public static void theHerdkeeperStandsTwoBlocksTall(GameTestHelper helper) {
        clear(helper);
        helper.setBlock(BASE, ModBlocks.HERDKEEPER.get());
        BlockPos device = helper.absolutePos(BASE);
        ShapeParts.refresh(helper.getLevel(), device, null);
        var part = helper.getBlockState(BASE.above());
        helper.assertTrue(part.getBlock() instanceof ShapePartBlock, "a stand-in holds the cell above");
        helper.assertTrue(ShapePartBlock.device(part, device.above()).equals(device), "and points at the device");
        helper.succeed();
    }
}
