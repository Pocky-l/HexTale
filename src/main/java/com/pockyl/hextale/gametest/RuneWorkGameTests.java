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
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechPickaxe;
import com.pockyl.hextale.runework.PickaxeEffects;

import java.util.List;
import java.util.Map;

/** Rune works: the Hextech Altar's rituals, the runes they need on steles, and the hextech pickaxes. */
public final class RuneWorkGameTests {
    private static final BlockPos ALTAR = new BlockPos(3, 2, 3);
    private static final List<BlockPos> PEDESTALS = List.of(new BlockPos(3, 2, 1), new BlockPos(5, 2, 3), new BlockPos(3, 2, 5),
            new BlockPos(1, 2, 3));
    private static final BlockPos STELE = new BlockPos(1, 2, 1);
    private static final BlockPos TANK = new BlockPos(6, 2, 6);
    private static final ComplexRune SMELTING = ComplexRunes.byKey("fire_earth");
    /** The Smelter's Pickaxe recipe's essences, its rune's (magma) included. */
    private static final EssenceMix SMELTER_ESSENCE = EssenceMix.of(Map.of(Essence.FORGE, 16, Essence.ORE, 12, Essence.MAGMA, 16));

    private RuneWorkGameTests() {
    }

    private static ItemStack pickaxe(HextechPickaxe kind, int stars) {
        ItemStack tool = new ItemStack(ModItems.pickaxe(kind).get());
        tool.set(ModDataComponents.STARS.get(), stars);
        return tool;
    }

    private static List<ItemStack> smelterItems() {
        return List.of(new ItemStack(ModItems.HEXTECH_GEMSTONE.get()), new ItemStack(ModItems.BRASS_INGOT.get()),
                new ItemStack(ModItems.BRASS_INGOT.get()), new ItemStack(Items.FURNACE));
    }

    /** Breaks the block at {@code pos} with the tool as a player (drops through the pickaxe's abilities). */
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

    private static void stele(GameTestHelper helper, ComplexRune rune, int stars) {
        helper.setBlock(STELE, ModBlocks.RUNE_STELE.get());
        helper.getBlockEntity(STELE, RuneSteleBlockEntity.class).choose(rune, stars);
    }

    @HexTest
    public static void theSmeltersPickaxeSmeltsDrops(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.IRON_ORE);
        mine(helper, pickaxe(HextechPickaxe.SMELTING, 1), new BlockPos(0, 1, 0));
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(0, 1, 0))).inflate(2));
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(Items.IRON_INGOT)), "raw iron comes out as an ingot");
        helper.assertTrue(drops.stream().noneMatch(drop -> drop.getItem().is(Items.RAW_IRON)), "no raw iron left");
        helper.succeed();
    }

    /** The Drill takes the 3x3 around the broken block (struck from above: the layer). */
    @HexTest
    public static void theDrillDigsAnArea(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        mine(helper, pickaxe(HextechPickaxe.DRILL, 1), new BlockPos(1, 1, 1));
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 1, z));
            }
        }
        helper.succeed();
    }

    /** The Vein takes the ore touching the broken one, not the stone around. */
    @HexTest
    public static void theVeinTakesTheWholeVein(GameTestHelper helper) {
        for (int x = 0; x < 4; x++) {
            helper.setBlock(new BlockPos(x, 1, 0), Blocks.IRON_ORE);
        }
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.STONE);
        mine(helper, pickaxe(HextechPickaxe.VEIN, 1), new BlockPos(0, 1, 0));
        for (int x = 1; x < 4; x++) {
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 1, 0));
        }
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(0, 1, 1));
        helper.succeed();
    }

    /** Stars matter: a three-star pickaxe digs over twice as fast and every ability is stronger. */
    @HexTest
    public static void starsMakeAPickaxeStronger(GameTestHelper helper) {
        helper.assertTrue(HextechPickaxe.speed(3) >= HextechPickaxe.speed(1) * 2, "three stars dig over twice as fast");
        helper.assertTrue(PickaxeEffects.temperChance(3) > PickaxeEffects.temperChance(1), "tempering grows with the stars");
        helper.assertTrue(!PickaxeEffects.describe(HextechPickaxe.DRILL, 1).get(0).getString()
                .equals(PickaxeEffects.describe(HextechPickaxe.DRILL, 3).get(0).getString()), "the drill digs wider with three stars");
        helper.assertTrue(HextechPickaxe.stars(new ItemStack(ModItems.pickaxe(HextechPickaxe.CORE).get())) == 1, "no stars = one star");
        helper.succeed();
    }

    /** Every pickaxe has its recipe: as many runes as its tier, each rune paid with its own essence. */
    @HexTest
    public static void everyPickaxeHasItsRecipe(GameTestHelper helper) {
        for (HextechPickaxe kind : HextechPickaxe.values()) {
            InfusionRecipe recipe = InfusionRecipes.get(false).get(Hextale.id(kind.id()));
            helper.assertTrue(recipe != null, kind.id() + " has an altar recipe");
            helper.assertTrue(recipe.runes().size() == kind.tier(), kind.id() + " needs " + kind.tier() + " runes, has " + recipe.runes());
            helper.assertTrue(recipe.result().item().value() == ModItems.pickaxe(kind).get(), kind.id() + " makes itself");
            for (ComplexRune rune : recipe.runes()) {
                EssenceMix cost = InfusionRecipe.runeEssence(rune);
                helper.assertTrue(recipe.totalEssences().get(cost.pureEssence()) >= cost.total(), kind.id() + " pays " + rune.key() + "'s essence");
            }
        }
        helper.succeed();
    }

    /** Without its rune on a stele the pickaxe cannot be made, and the circle says which rune it waits for. */
    @HexTest
    public static void aPickaxeNeedsItsRunes(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(Items.IRON_PICKAXE), smelterItems(), SMELTER_ESSENCE);
        altar.startOrAbort(null);
        helper.assertFalse(altar.active(), "no rune, no ritual");
        Ritual.Hint hint = Ritual.hint(altar.center(), Ritual.Circle.scan(helper.getLevel(), helper.absolutePos(ALTAR)), false);
        helper.assertTrue(hint.runes().contains(SMELTING) && hint.missing().isEmpty() && hint.extra().isEmpty(),
                "the circle only lacks Smelting, got " + hint);
        helper.succeed();
    }

    /** The circle names what it lacks: missing pedestal items, items the ritual does not take, and runes. */
    @HexTest
    public static void theCircleShowsWhatItLacks(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(Items.IRON_PICKAXE),
                List.of(new ItemStack(ModItems.BRASS_INGOT.get()), new ItemStack(Items.DIRT)), EssenceMix.EMPTY);
        Ritual.Hint hint = Ritual.hint(altar.center(), Ritual.Circle.scan(helper.getLevel(), helper.absolutePos(ALTAR)), false);
        helper.assertTrue(hint.missing().size() == 3, "a gemstone, a brass ingot and the pickaxe's own item are missing, got " + hint.missing());
        helper.assertTrue(hint.extra().size() == 1 && hint.extra().getFirst().equals(helper.absolutePos(PEDESTALS.get(1))), "the dirt is extra");
        helper.assertTrue(hint.runes().size() == 1, "and one rune");
        helper.succeed();
    }

    /** With Smelting on a two-star stele, an iron pickaxe becomes a two-star Smelter's Pickaxe that keeps its enchantment. */
    @HexTest(maxTicks = 360)
    public static void theAltarMakesAPickaxeWithTheRunesStars(GameTestHelper helper) {
        ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
        iron.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), 2);
        HextechAltarBlockEntity altar = circle(helper, iron, smelterItems(), SMELTER_ESSENCE.plus(EssenceMix.pure(Essence.ORE, 10)));
        stele(helper, SMELTING, 2);
        altar.startOrAbort(null);
        helper.assertTrue(altar.active() && altar.kind() == Ritual.Kind.INFUSE, "the ritual starts");
        helper.succeedWhen(() -> {
            ItemStack made = altar.center();
            helper.assertTrue(made.is(ModItems.pickaxe(HextechPickaxe.SMELTING).get()), "the centre became a Smelter's Pickaxe");
            helper.assertTrue(HextechPickaxe.stars(made) == 2, "with the rune's two stars");
            helper.assertTrue(made.isEnchanted(), "it kept its enchantment");
            for (BlockPos pos : PEDESTALS) {
                helper.assertTrue(helper.getBlockEntity(pos, PedestalBlockEntity.class).item().isEmpty(), "every pedestal item was drawn");
            }
            EssenceMix left = helper.getBlockEntity(TANK, EssenceTankBlockEntity.class).tank().contents();
            helper.assertTrue(left.get(Essence.ORE) == 10 && left.get(Essence.MAGMA) == 0, "the essences were drawn, the rune's too, left " + left);
        });
    }

    /** Anything extra on a pedestal spoils the match: nothing starts. */
    @HexTest
    public static void anExtraItemSpoilsTheCircle(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(Items.IRON_PICKAXE), List.of(new ItemStack(ModItems.BRASS_INGOT.get()),
                new ItemStack(ModItems.BRASS_INGOT.get()), new ItemStack(ModItems.HEXTECH_GEMSTONE.get()), new ItemStack(Items.DIRT)),
                SMELTER_ESSENCE);
        stele(helper, SMELTING, 1);
        altar.startOrAbort(null);
        helper.assertFalse(altar.active(), "a circle with a wrong item does not start");
        helper.succeed();
    }

    /** With an essence missing (here the rune's own) the ritual stalls instead of finishing. */
    @HexTest(maxTicks = 360)
    public static void aRitualShortOfEssenceStalls(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(Items.IRON_PICKAXE), smelterItems(),
                EssenceMix.of(Map.of(Essence.FORGE, 16, Essence.ORE, 12)));
        stele(helper, SMELTING, 1);
        altar.startOrAbort(null);
        helper.runAfterDelay(300, () -> {
            helper.assertTrue(altar.active() && altar.stalled(), "without the rune's magma essence the ritual waits");
            helper.assertTrue(altar.center().is(Items.IRON_PICKAXE), "and the centre is untouched");
            helper.succeed();
        });
    }

    /** The player's way: a Creative Essence Jar, sneak + empty hand on the altar, and wait for the result. */
    @HexTest(maxTicks = 320)
    public static void aPlayerStartsARitualWithCreativeTanks(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(Items.IRON_PICKAXE), smelterItems(), EssenceMix.EMPTY);
        stele(helper, SMELTING, 3);
        BlockPos creative = new BlockPos(6, 2, 0);
        helper.setBlock(creative, ModBlocks.CREATIVE_ESSENCE_TANK.get());
        helper.getBlockEntity(creative, EssenceTankBlockEntity.class).tank().set(EssenceTankBlockEntity.CREATIVE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        BlockPos abs = helper.absolutePos(ALTAR);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs),
                net.minecraft.core.Direction.UP, abs, false);
        helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(altar.active(), "sneak + empty hand starts the ritual");
        helper.succeedWhen(() -> helper.assertTrue(altar.center().is(ModItems.pickaxe(HextechPickaxe.SMELTING).get())
                && HextechPickaxe.stars(altar.center()) == 3, "the ritual finishes with a three-star pickaxe"));
    }
}
