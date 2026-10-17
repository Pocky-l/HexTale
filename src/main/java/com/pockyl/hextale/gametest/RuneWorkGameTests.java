package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.GrindstoneEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.HextechAltarBlockEntity;
import com.pockyl.hextale.block.PedestalBlockEntity;
import com.pockyl.hextale.block.RuneSteleBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.infusion.InfusionRecipe;
import com.pockyl.hextale.infusion.InfusionRecipes;
import com.pockyl.hextale.infusion.Ritual;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechPickaxe;
import com.pockyl.hextale.runework.PickaxeGrindstone;
import com.pockyl.hextale.runework.PickaxeRune;
import com.pockyl.hextale.runework.PickaxeRunes;

import java.util.List;
import java.util.Map;

/** Rune works: the Hextech Altar's rituals, the hextech pickaxes and the runes burned into them. */
public final class RuneWorkGameTests {
    private static final BlockPos ALTAR = new BlockPos(3, 2, 3);
    private static final List<BlockPos> PEDESTALS = List.of(new BlockPos(3, 2, 1), new BlockPos(5, 2, 3), new BlockPos(3, 2, 5),
            new BlockPos(1, 2, 3));
    private static final BlockPos STELE = new BlockPos(1, 2, 1);
    private static final BlockPos TANK = new BlockPos(6, 2, 6);
    /** The Hextech Pickaxe recipe's essences. */
    private static final EssenceMix PICKAXE_ESSENCE = EssenceMix.of(Map.of(Essence.FORGE, 16, Essence.ORE, 12, Essence.ENERGY, 8));

    private RuneWorkGameTests() {
    }

    private static ItemStack pickaxe(PickaxeRune... runes) {
        ItemStack tool = new ItemStack(ModItems.pickaxe(HextechPickaxe.HEXTECH).get());
        PickaxeRunes on = PickaxeRunes.EMPTY;
        for (PickaxeRune rune : runes) {
            on = on.with(rune, 1);
        }
        PickaxeRunes.set(tool, on);
        return tool;
    }

    private static List<ItemStack> pickaxeItems() {
        return List.of(new ItemStack(ModItems.HEXTECH_GEMSTONE.get()), new ItemStack(ModItems.BRASS_INGOT.get()),
                new ItemStack(ModItems.BRASS_INGOT.get()), new ItemStack(Items.AMETHYST_SHARD));
    }

    /** Breaks the block at {@code pos} with the tool as a player standing over it (drops through the pickaxe's runes). */
    private static void mine(GameTestHelper helper, ItemStack tool, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(abs);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        // Standing over the block, looking straight down at it: the pickaxe strikes its top face.
        player.snapTo(abs.getX() + 0.5, abs.getY() + 1, abs.getZ() + 0.5, 0, 90);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tool);
        Block.dropResources(state, helper.getLevel(), abs, null, player, tool);
        helper.getLevel().removeBlock(abs, false);
    }

    /** An altar with the given centre item, pedestal items and a tank of essence around it. */
    private static HextechAltarBlockEntity circle(GameTestHelper helper, ItemStack center, List<ItemStack> items, EssenceMix essence) {
        // Blocks outside the 1x1x1 test structure outlive their test: clear what other rituals may have left in reach.
        for (int x = ALTAR.getX() - Ritual.TANK_RADIUS; x <= ALTAR.getX() + Ritual.TANK_RADIUS; x++) {
            for (int z = ALTAR.getZ() - Ritual.TANK_RADIUS; z <= ALTAR.getZ() + Ritual.TANK_RADIUS; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        helper.setBlock(ALTAR, ModBlocks.HEXTECH_ALTAR.get());
        HextechAltarBlockEntity altar = helper.getBlockEntity(ALTAR, HextechAltarBlockEntity.class);
        altar.place(center);
        for (int i = 0; i < PEDESTALS.size(); i++) {
            helper.setBlock(PEDESTALS.get(i), ModBlocks.PEDESTAL.get());
            if (i < items.size()) {
                helper.getBlockEntity(PEDESTALS.get(i), PedestalBlockEntity.class).place(items.get(i));
            }
        }
        helper.setBlock(TANK, ModBlocks.ESSENCE_TANK.get());
        helper.getBlockEntity(TANK, EssenceTankBlockEntity.class).tank().set(essence);
        return altar;
    }

    private static void stele(GameTestHelper helper, PickaxeRune rune, int stars) {
        helper.setBlock(STELE, ModBlocks.RUNE_STELE.get());
        helper.getBlockEntity(STELE, RuneSteleBlockEntity.class).choose(rune.rune(), stars);
    }

    private static Ritual.Circle scan(GameTestHelper helper) {
        return Ritual.Circle.scan(helper.getLevel(), helper.absolutePos(ALTAR));
    }

    // ---- What the runes do ------------------------------------------------------------------------------------------

    @HexTest
    public static void smeltingSmeltsDrops(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.IRON_ORE);
        mine(helper, pickaxe(PickaxeRune.SMELTING), new BlockPos(0, 1, 0));
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(0, 1, 0))).inflate(2));
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(Items.IRON_INGOT)), "raw iron comes out as an ingot");
        helper.assertTrue(drops.stream().noneMatch(drop -> drop.getItem().is(Items.RAW_IRON)), "no raw iron left");
        helper.succeed();
    }

    /** Drill takes the 3x3 around the broken block (struck from above: the layer). */
    @HexTest
    public static void drillDigsAnArea(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        mine(helper, pickaxe(PickaxeRune.DRILL), new BlockPos(1, 1, 1));
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 1, z));
            }
        }
        helper.succeed();
    }

    /** Erosion takes the ore touching the broken one, not the stone around. */
    @HexTest
    public static void erosionTakesTheWholeVein(GameTestHelper helper) {
        for (int x = 0; x < 4; x++) {
            helper.setBlock(new BlockPos(x, 1, 0), Blocks.IRON_ORE);
        }
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.STONE);
        mine(helper, pickaxe(PickaxeRune.EROSION), new BlockPos(0, 1, 0));
        for (int x = 1; x < 4; x++) {
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 1, 0));
        }
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(0, 1, 1));
        helper.succeed();
    }

    /** A rune works with its stars; a Lens on the same pickaxe adds one, up to four. */
    @HexTest
    public static void aLensStrengthensTheOtherRunes(GameTestHelper helper) {
        PickaxeRunes runes = PickaxeRunes.EMPTY.with(PickaxeRune.DRILL, 3);
        helper.assertTrue(runes.strength(PickaxeRune.DRILL) == 3, "drill at three stars");
        runes = runes.with(PickaxeRune.LENS, 1);
        helper.assertTrue(runes.strength(PickaxeRune.DRILL) == 4, "a lens makes it four");
        helper.assertTrue(runes.strength(PickaxeRune.LENS) == 1, "the lens does not focus itself");
        helper.assertTrue(runes.strength(PickaxeRune.MAGNET) == 0, "a rune the pickaxe lacks has no strength");
        helper.succeed();
    }

    /** Every pickaxe rune is paid with its own essence, more for more stars and for deeper chapters. */
    @HexTest
    public static void everyPickaxeRuneHasItsEssence(GameTestHelper helper) {
        for (PickaxeRune rune : PickaxeRune.values()) {
            EssenceMix one = rune.cost(1);
            EssenceMix three = rune.cost(3);
            helper.assertTrue(one.pureEssence() != null && one.pureEssence().tier() == 1, rune + " costs the compound of its primals");
            helper.assertTrue(three.total() == one.total() * 3, rune + " costs three times as much at three stars");
        }
        helper.succeed();
    }

    // ---- The altar --------------------------------------------------------------------------------------------------

    /** The three pickaxes have their recipes, each made from the one before (the first from an iron pickaxe). */
    @HexTest
    public static void thePickaxeRecipesLoad(GameTestHelper helper) {
        for (HextechPickaxe kind : HextechPickaxe.values()) {
            InfusionRecipe recipe = InfusionRecipes.get(false).get(Hextale.id(kind.id()));
            helper.assertTrue(recipe != null, kind.id() + " has an altar recipe");
            helper.assertTrue(recipe.result().item().value() == ModItems.pickaxe(kind).get(), kind.id() + " makes itself");
            helper.assertTrue(recipe.keepComponents(), kind.id() + " keeps runes and enchantments");
        }
        helper.assertTrue(InfusionRecipes.get(false).get(Hextale.id("refined_hextech_pickaxe")).center()
                .test(new ItemStack(ModItems.pickaxe(HextechPickaxe.HEXTECH).get())), "the refined pickaxe is made from the plain one");
        helper.succeed();
    }

    /** An iron pickaxe, four items and three essences become a Hextech Pickaxe that keeps its enchantment. */
    @HexTest(maxTicks = 360)
    public static void theAltarMakesTheHextechPickaxe(GameTestHelper helper) {
        ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
        iron.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), 2);
        HextechAltarBlockEntity altar = circle(helper, iron, pickaxeItems(), PICKAXE_ESSENCE.plus(EssenceMix.pure(Essence.ORE, 10))
                .plus(InfusionRecipe.runeEssence(PickaxeRune.SMELTING.rune())));
        // The pickaxe is forged under the rune of Smelting.
        stele(helper, PickaxeRune.SMELTING, 1);
        altar.startOrAbort(null);
        helper.assertTrue(altar.active() && altar.kind() == Ritual.Kind.INFUSE, "the ritual starts");
        helper.succeedWhen(() -> {
            helper.assertTrue(altar.center().is(ModItems.pickaxe(HextechPickaxe.HEXTECH).get()), "the centre became a Hextech Pickaxe");
            helper.assertTrue(altar.center().isEnchanted(), "it kept its enchantment");
            EssenceMix left = helper.getBlockEntity(TANK, EssenceTankBlockEntity.class).tank().contents();
            helper.assertTrue(left.get(Essence.ORE) == 10 && left.get(Essence.FORGE) == 0, "the essences were drawn, left " + left);
        });
    }

    /** A pickaxe alone on the altar takes the stele's rune with the stele's stars, paid only with the rune's essence. */
    @HexTest(maxTicks = 300)
    public static void theAltarBurnsARuneIntoAPickaxe(GameTestHelper helper) {
        EssenceMix cost = PickaxeRune.SMELTING.cost(2);
        HextechAltarBlockEntity altar = circle(helper, pickaxe(), List.of(), cost.plus(EssenceMix.pure(Essence.ORE, 10)));
        stele(helper, PickaxeRune.SMELTING, 2);
        altar.startOrAbort(null);
        helper.assertTrue(altar.active() && altar.kind() == Ritual.Kind.ENGRAVE, "the rune burning starts");
        helper.succeedWhen(() -> {
            helper.assertTrue(PickaxeRunes.of(altar.center()).stars(PickaxeRune.SMELTING) == 2, "Smelting with two stars");
            EssenceMix left = helper.getBlockEntity(TANK, EssenceTankBlockEntity.class).tank().contents();
            helper.assertTrue(left.get(cost.pureEssence()) == 0 && left.get(Essence.ORE) == 10, "only the rune's essence was drawn, left " + left);
        });
    }

    /** More stars raise a rune the pickaxe has; clashing runes, a fifth rune, or non-pickaxe runes are refused. */
    @HexTest
    public static void runeBurningFollowsItsRules(GameTestHelper helper) {
        circle(helper, pickaxe(PickaxeRune.EROSION), List.of(), EssenceMix.EMPTY);
        stele(helper, PickaxeRune.DRILL, 1);
        helper.assertTrue("clash".equals(Ritual.engraving(pickaxe(PickaxeRune.EROSION), scan(helper)).refusal()), "drill and erosion clash");
        stele(helper, PickaxeRune.QUAKE, 1);
        helper.assertTrue("clash".equals(Ritual.engraving(pickaxe(PickaxeRune.INSIGHT), scan(helper)).refusal()), "one right-click rune only");
        stele(helper, PickaxeRune.MAGNET, 1);
        ItemStack full = pickaxe(PickaxeRune.SMELTING, PickaxeRune.TEMPERING, PickaxeRune.FORGE, PickaxeRune.MOTOR);
        helper.assertTrue("full".equals(Ritual.engraving(full, scan(helper)).refusal()), "four runes are all a pickaxe holds");
        stele(helper, PickaxeRune.SMELTING, 3);
        Ritual.Engraving raise = Ritual.engraving(full, scan(helper));
        helper.assertTrue(raise.possible() && raise.after().stars(PickaxeRune.SMELTING) == 3, "more stars raise a rune it already has");
        helper.assertTrue(raise.cost().total() == PickaxeRune.SMELTING.cost(3).total(), "paid for the stars it gets");
        stele(helper, PickaxeRune.SMELTING, 1);
        helper.assertTrue("nothing_new".equals(Ritual.engraving(full, scan(helper)).refusal()), "fewer stars change nothing");
        helper.getBlockEntity(STELE, RuneSteleBlockEntity.class).choose(ComplexRunes.byKey("fire_water"), 1);
        helper.assertTrue("not_pickaxe".equals(Ritual.engraving(pickaxe(), scan(helper)).refusal()), "steam does not take to a pickaxe");
        helper.succeed();
    }

    /** Anything extra on a pedestal spoils the match: nothing starts. */
    @HexTest
    public static void anExtraItemSpoilsTheCircle(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(Items.IRON_PICKAXE), List.of(new ItemStack(ModItems.BRASS_INGOT.get()),
                new ItemStack(ModItems.BRASS_INGOT.get()), new ItemStack(ModItems.HEXTECH_GEMSTONE.get()), new ItemStack(Items.DIRT)),
                PICKAXE_ESSENCE);
        altar.startOrAbort(null);
        helper.assertFalse(altar.active(), "a circle with a wrong item does not start");
        Ritual.Hint hint = Ritual.hint(altar.center(), scan(helper), false);
        helper.assertTrue(hint.missing().size() == 1 && hint.extra().size() == 1, "the shard is missing and the dirt is extra, got " + hint);
        helper.succeed();
    }

    /** With an essence missing the ritual stalls instead of finishing. */
    @HexTest(maxTicks = 360)
    public static void aRitualShortOfEssenceStalls(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(Items.IRON_PICKAXE), pickaxeItems(),
                EssenceMix.of(Map.of(Essence.FORGE, 16, Essence.ORE, 12)));
        stele(helper, PickaxeRune.SMELTING, 1);
        altar.startOrAbort(null);
        helper.runAfterDelay(300, () -> {
            helper.assertTrue(altar.active() && altar.stalled(), "without energy essence the ritual waits");
            helper.assertTrue(altar.center().is(Items.IRON_PICKAXE), "and the centre is untouched");
            helper.succeed();
        });
    }

    /** The player's way: a Creative Essence Jar, sneak + empty hand on the altar, and wait for the result. */
    @HexTest(maxTicks = 320)
    public static void aPlayerStartsARitualWithCreativeTanks(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(Items.IRON_PICKAXE), pickaxeItems(), EssenceMix.EMPTY);
        BlockPos creative = new BlockPos(6, 2, 0);
        helper.setBlock(creative, ModBlocks.CREATIVE_ESSENCE_TANK.get());
        helper.getBlockEntity(creative, EssenceTankBlockEntity.class).tank().set(EssenceTankBlockEntity.CREATIVE);
        stele(helper, PickaxeRune.SMELTING, 1);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        BlockPos abs = helper.absolutePos(ALTAR);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs),
                net.minecraft.core.Direction.UP, abs, false);
        helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(altar.active(), "sneak + empty hand starts the ritual");
        helper.succeedWhen(() -> helper.assertTrue(altar.center().is(ModItems.pickaxe(HextechPickaxe.HEXTECH).get()), "the ritual finishes"));
    }

    /** A grindstone takes the runes off a pickaxe (and only them). */
    @HexTest
    public static void aGrindstoneTakesTheRunesOff(GameTestHelper helper) {
        GrindstoneEvent.OnPlaceItem event = new GrindstoneEvent.OnPlaceItem(pickaxe(PickaxeRune.SMELTING, PickaxeRune.DRILL), ItemStack.EMPTY, 0);
        PickaxeGrindstone.onPlace(event);
        helper.assertTrue(HextechPickaxe.HEXTECH == ((com.pockyl.hextale.runework.HextechPickaxeItem) event.getOutput().getItem()).kind()
                && PickaxeRunes.of(event.getOutput()).isEmpty(), "the pickaxe comes out without runes");
        helper.succeed();
    }
}
