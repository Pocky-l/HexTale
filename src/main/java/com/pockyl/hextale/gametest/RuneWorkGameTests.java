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
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechPickaxe;
import com.pockyl.hextale.runework.PickaxeGrindstone;
import com.pockyl.hextale.runework.PickaxeRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.RuneHolder;

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
    private static final EssenceMix PICKAXE_ESSENCE = EssenceMix.of(Map.of(Essence.METAL, 16, Essence.ORE, 12, Essence.MAGIC, 8));

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
        return List.of(new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), new ItemStack(ModItems.BRASS_INGOT.get()),
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

    /** A player standing by the altar, who has researched the topic (or nothing, for null). */
    @SuppressWarnings("removal")
    private static ServerPlayer researcher(GameTestHelper helper, com.pockyl.hextale.research.@org.jspecify.annotations.Nullable ResearchTopic topic) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        net.minecraft.world.phys.Vec3 at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(ALTAR.getX() + 0.5, ALTAR.getY(), ALTAR.getZ() + 2.5));
        player.snapTo(at.x, at.y, at.z, 0, 0);
        if (topic != null) {
            Research.research(player, topic);
        }
        return player;
    }

    /** A blank scroll on the altar with one rune on a stele becomes a scroll of that rune with the stele's stars. */
    @HexTest(maxTicks = 300)
    public static void theAltarWritesARuneOnABlankScroll(GameTestHelper helper) {
        EssenceMix cost = Ritual.writingCost(PickaxeRune.SMELTING.rune(), 2);
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(ModItems.BLANK_SCROLL.get()), List.of(), cost);
        stele(helper, PickaxeRune.SMELTING, 2);
        altar.startOrAbort(null);
        helper.assertTrue(altar.active() && altar.kind() == Ritual.Kind.WRITE, "the writing starts");
        helper.succeedWhen(() -> {
            helper.assertTrue(com.pockyl.hextale.item.RuneScrollItem.rune(altar.center()) == PickaxeRune.SMELTING.rune()
                    && com.pockyl.hextale.item.RuneScrollItem.stars(altar.center()) == 2, "a two-star scroll of Smelting");
            helper.assertTrue(helper.getBlockEntity(TANK, EssenceTankBlockEntity.class).tank().isEmpty(), "the rune's essence was drawn twice");
        });
    }

    /** A ritual with a gemstone in the centre ends with the altar itself turned into a Hexcore. */
    @HexTest(maxTicks = 700)
    public static void theAltarWakesIntoAHexcore(GameTestHelper helper) {
        List<ItemStack> items = List.of(new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), new ItemStack(ModItems.HEXTECH_CRYSTAL.get()),
                new ItemStack(ModItems.HEXSTEEL_INGOT.get()), new ItemStack(ModItems.HEXSTEEL_INGOT.get()));
        EssenceMix essence = EssenceMix.of(Map.of(Essence.MAGIC, 32, Essence.CIRCUIT, 24, Essence.CRYSTAL, 16));
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(ModItems.HEXTECH_GEMSTONE.get()), items, essence);
        List<ItemStack> more = List.of(new ItemStack(ModItems.CRYSTAL_BATTERY.get()), new ItemStack(ModItems.REGENERATIVE_SLIME.get()),
                new ItemStack(Items.LIGHTNING_ROD.weathering().unaffected()));
        List<BlockPos> corners = List.of(new BlockPos(5, 2, 1), new BlockPos(5, 2, 5), new BlockPos(1, 2, 5));
        for (int i = 0; i < corners.size(); i++) {
            helper.setBlock(corners.get(i), ModBlocks.PEDESTAL.get());
            helper.getBlockEntity(corners.get(i), PedestalBlockEntity.class).place(more.get(i));
        }
        researcher(helper, com.pockyl.hextale.research.ResearchTopics.HEXCORE);
        altar.startOrAbort(null);
        helper.assertTrue(altar.active(), "the awakening starts");
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(ModBlocks.HEXCORE.get(), ALTAR);
            HextechAltarBlockEntity core = helper.getBlockEntity(ALTAR, HextechAltarBlockEntity.class);
            helper.assertTrue(core instanceof com.pockyl.hextale.block.HexcoreBlockEntity && core.center().isEmpty() && !core.active(),
                    "a Hexcore stands where the altar was, with nothing left in it");
        });
    }

    /** Heavy rituals are more unstable on a Hexcore than light ones. */
    @HexTest
    public static void heavyRitualsAreUnstable(GameTestHelper helper) {
        helper.assertTrue(com.pockyl.hextale.block.HexcoreBlockEntity.instabilityOf(7, 72, 0)
                > com.pockyl.hextale.block.HexcoreBlockEntity.instabilityOf(4, 36, 0)
                && com.pockyl.hextale.block.HexcoreBlockEntity.instabilityOf(0, 24, 1) < 0.1F, "instability follows the ritual's weight");
        helper.succeed();
    }

    private static Ritual.Circle scan(GameTestHelper helper) {
        return Ritual.Circle.scan(helper.getLevel(), helper.absolutePos(ALTAR));
    }

    /** A stele offers only the runes that do something for the item on its altar; an empty altar takes any rune. */
    @HexTest
    public static void steleOffersOnlyRunesForTheAltarItem(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(ModItems.pickaxe(HextechPickaxe.HEXTECH).get()), List.of(), EssenceMix.EMPTY);
        stele(helper, PickaxeRune.SMELTING, 1);
        helper.assertTrue(Ritual.altarOf(helper.getLevel(), helper.absolutePos(STELE)) == altar, "the stele finds its altar");
        java.util.Set<com.pockyl.hextale.rune.ComplexRune> forPickaxe = Ritual.runesFor(altar.center(), false);
        for (PickaxeRune rune : PickaxeRune.values()) {
            helper.assertTrue(forPickaxe.contains(rune.rune()), "a pickaxe takes " + rune);
        }
        helper.assertFalse(forPickaxe.contains(ComplexRunes.byKey("fire_water")), "but not a rune no pickaxe takes");
        helper.assertTrue(Ritual.runesFor(new ItemStack(Items.IRON_PICKAXE), false).isEmpty(),
                "an iron pickaxe needs no rune to become a hextech one");
        List<Ritual.RuneUse> uses = Ritual.usesFor(altar.center(), false);
        for (int i = 0; i < PickaxeRune.values().length; i++) {
            helper.assertTrue(uses.get(i).rune() == PickaxeRune.values()[i].rune() && uses.get(i).holder() != null,
                    "the pickaxe's runes come in the book's order, got " + uses.get(i).rune() + " at " + i);
        }
        helper.assertTrue(uses.getFirst().cost(2).equals(PickaxeRune.values()[0].cost(2)), "the list shows what the altar draws for each rune");
        List<Ritual.RuneUse> scroll = Ritual.usesFor(new ItemStack(ModItems.BLANK_SCROLL.get()), false);
        helper.assertTrue(scroll.size() == ComplexRunes.COUNT && scroll.stream().allMatch(Ritual.RuneUse::writes),
                "any rune can be written on a blank scroll");
        helper.assertTrue(Ritual.runesFor(new ItemStack(Items.DIRT), false).isEmpty(), "dirt takes no rune");
        helper.assertTrue(Ritual.runesFor(ItemStack.EMPTY, false) == null, "an empty altar takes any rune");
        helper.succeed();
    }

    /** Steles of one circle: a rune once, no more runes than the item's slots, never two that clash. */
    @HexTest
    public static void stelesKeepTheCircleSensible(GameTestHelper helper) {
        ItemStack pylon = new ItemStack(ModItems.GATHERING_PYLON.get());
        var tempering = com.pockyl.hextale.runework.PylonRune.SHIFT.rune();
        var bellows = com.pockyl.hextale.runework.PylonRune.MOTOR.rune();
        var forge = com.pockyl.hextale.runework.PylonRune.LIFT.rune();
        var lens = com.pockyl.hextale.runework.PylonRune.LENS.rune();
        helper.assertTrue(Ritual.steleRefusal(pylon, List.of(), tempering) == null, "the first rune goes");
        helper.assertTrue(Ritual.steleRefusal(pylon, List.of(tempering), tempering) != null, "the same rune twice does not");
        helper.assertTrue(Ritual.steleRefusal(pylon, List.of(tempering, bellows), forge) == null, "the third fills the last slot");
        helper.assertTrue(Ritual.steleRefusal(pylon, List.of(tempering, bellows, forge), lens) != null, "a fourth has no slot");
        // A rune the pylon already carries takes no new slot: its stars may still be raised.
        ItemStack carrying = pylon.copy();
        RuneHolder.PYLON.set(carrying, PickaxeRunes.EMPTY.with(lens, 1));
        helper.assertTrue(Ritual.steleRefusal(carrying, List.of(tempering, bellows), lens) == null, "raising a carried rune goes");
        helper.assertTrue(Ritual.steleRefusal(carrying, List.of(tempering, bellows), forge) != null, "but the carried one counts");
        ItemStack pickaxe = new ItemStack(ModItems.pickaxe(HextechPickaxe.HEXTECH).get());
        helper.assertTrue(Ritual.steleRefusal(pickaxe, List.of(PickaxeRune.DRILL.rune()), PickaxeRune.EROSION.rune()) != null,
                "Drill and Erosion clash");
        helper.succeed();
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

    /** Every pickaxe rune is paid with its own essence (or its two primals), more for more stars and for deeper chapters. */
    @HexTest
    public static void everyPickaxeRuneHasItsEssence(GameTestHelper helper) {
        for (PickaxeRune rune : PickaxeRune.values()) {
            EssenceMix one = rune.cost(1);
            EssenceMix three = rune.cost(3);
            com.pockyl.hextale.rune.Essence pair = com.pockyl.hextale.rune.Essence.pair(rune.rune().action(), rune.rune().target());
            helper.assertTrue(pair != null ? one.pureEssence() == pair
                    : one.get(rune.rune().action()) > 0 && one.get(rune.rune().target()) > 0 && one.asMap().size() == 2,
                    rune + " costs the compound of its primals, or both primals");
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

    /**
     * The hextech axe, shovel and sword are charged from their iron tools, each needs its research, and each still works
     * as its kind of tool (the axe strips logs, the shovel digs dirt fast, the sword is a sword).
     */
    @HexTest
    public static void theHextechToolsHaveTheirRecipes(GameTestHelper helper) {
        Map<String, ItemStack> irons = Map.of("hextech_axe", new ItemStack(Items.IRON_AXE), "hextech_shovel", new ItemStack(Items.IRON_SHOVEL),
                "hextech_sword", new ItemStack(Items.IRON_SWORD));
        Map<String, ItemStack> tools = Map.of("hextech_axe", new ItemStack(ModItems.HEXTECH_AXE.get()),
                "hextech_shovel", new ItemStack(ModItems.HEXTECH_SHOVEL.get()), "hextech_sword", new ItemStack(ModItems.HEXTECH_SWORD.get()));
        for (String id : irons.keySet()) {
            InfusionRecipe recipe = InfusionRecipes.get(false).get(Hextale.id(id));
            helper.assertTrue(recipe != null, id + " has an altar recipe");
            helper.assertTrue(recipe.center().test(irons.get(id)), id + " is made from its iron tool");
            helper.assertTrue(recipe.result().item().value() == tools.get(id).getItem(), id + " makes itself");
            helper.assertTrue(ResearchTopics.of(tools.get(id).getItem()) != null, id + " needs its research");
        }
        helper.assertTrue(tools.get("hextech_axe").has(net.minecraft.core.component.DataComponents.BLOCK_TRANSFORMER), "the axe strips logs");
        helper.assertTrue(tools.get("hextech_shovel").getDestroySpeed(Blocks.DIRT.defaultBlockState()) > 6.0F, "the shovel digs dirt fast");
        helper.assertTrue(tools.get("hextech_sword").is(net.minecraft.tags.ItemTags.SWORDS), "the sword counts as a sword");
        helper.succeed();
    }

    /** An iron pickaxe, four items and three essences become a Hextech Pickaxe that keeps its enchantment. */
    @HexTest(maxTicks = 360)
    public static void theAltarMakesTheHextechPickaxe(GameTestHelper helper) {
        ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
        iron.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), 2);
        HextechAltarBlockEntity altar = circle(helper, iron, pickaxeItems(), PICKAXE_ESSENCE.plus(EssenceMix.pure(Essence.ORE, 10)));
        ServerPlayer smith = researcher(helper, null);
        smith.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        altar.startOrAbort(smith);
        helper.assertFalse(altar.active(), "a smith who has not researched the pickaxe starts nothing");
        Research.research(smith, ResearchTopics.HEXTECH_PICKAXE);
        altar.startOrAbort(smith);
        smith.discard();
        helper.assertTrue(altar.active() && altar.kind() == Ritual.Kind.INFUSE, "once researched, the ritual starts");
        helper.succeedWhen(() -> {
            helper.assertTrue(altar.center().is(ModItems.pickaxe(HextechPickaxe.HEXTECH).get()), "the centre became a Hextech Pickaxe");
            helper.assertTrue(altar.center().isEnchanted(), "it kept its enchantment");
            EssenceMix left = helper.getBlockEntity(TANK, EssenceTankBlockEntity.class).tank().contents();
            helper.assertTrue(left.get(Essence.ORE) == 10 && left.get(Essence.METAL) == 0, "the essences were drawn, left " + left);
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
                new ItemStack(ModItems.BRASS_INGOT.get()), new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), new ItemStack(Items.DIRT)),
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
                EssenceMix.of(Map.of(Essence.METAL, 16, Essence.ORE, 12)));
        // Started by redstone: a researcher standing by is enough.
        ServerPlayer smith = researcher(helper, ResearchTopics.HEXTECH_PICKAXE);
        altar.startOrAbort(null);
        smith.discard();
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
        // Creative mode knows nothing by itself: the maker must have researched the pickaxe.
        Research.research(player, ResearchTopics.HEXTECH_PICKAXE);
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
