package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
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
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.infusion.InfusionRecipe;
import com.pockyl.hextale.infusion.InfusionRecipes;
import com.pockyl.hextale.infusion.Ritual;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechTools;
import com.pockyl.hextale.runework.RuneSlots;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.runework.ToolRuneEffects;
import com.pockyl.hextale.runework.ToolRunes;

import java.util.List;
import java.util.Map;

/** Rune works: the Hextech Altar's rituals, the essence cell and the runes firing on hextech tools. */
public final class RuneWorkGameTests {
    private static final BlockPos ALTAR = new BlockPos(3, 2, 3);
    private static final List<BlockPos> PEDESTALS = List.of(new BlockPos(3, 2, 1), new BlockPos(5, 2, 3), new BlockPos(3, 2, 5),
            new BlockPos(1, 2, 3));
    private static final BlockPos STELE = new BlockPos(1, 2, 1);
    private static final BlockPos TANK = new BlockPos(6, 2, 6);

    private RuneWorkGameTests() {
    }

    private static ItemStack smeltingPickaxe(int fire) {
        ItemStack tool = new ItemStack(ModItems.HEXTECH_PICKAXE.get());
        HextechTools.setRunes(tool, RuneSlots.EMPTY.with(new RuneSlots.Engraving(0, ToolRunes.SMELTING, 2)));
        HextechTools.setCell(tool, EssenceMix.pure(Essence.FIRE, fire));
        return tool;
    }

    /** Breaks iron ore with the tool (as a player) and returns the items it dropped. */
    private static List<ItemEntity> mine(GameTestHelper helper, ItemStack tool) {
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockState ore = Blocks.IRON_ORE.defaultBlockState();
        helper.getLevel().setBlockAndUpdate(pos, ore);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Block.dropResources(ore, helper.getLevel(), pos, null, player, tool);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
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

    @HexTest
    public static void smeltingRuneSmeltsDrops(GameTestHelper helper) {
        ItemStack tool = smeltingPickaxe(10);
        List<ItemEntity> drops = mine(helper, tool);
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(Items.IRON_INGOT)), "raw iron comes out as an ingot");
        helper.assertTrue(drops.stream().noneMatch(drop -> drop.getItem().is(Items.RAW_IRON)), "no raw iron left");
        helper.assertTrue(HextechTools.cell(tool).get(Essence.FIRE) == 9, "one mB of fire burned per item");
        helper.succeed();
    }

    @HexTest
    public static void smeltingSleepsWithoutFire(GameTestHelper helper) {
        List<ItemEntity> drops = mine(helper, smeltingPickaxe(0));
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(Items.RAW_IRON)), "without fire the drop stays raw");
        helper.succeed();
    }

    @HexTest
    public static void cellFillsFromTanks(GameTestHelper helper) {
        ItemStack tool = new ItemStack(ModItems.HEXTECH_PICKAXE.get());
        EssenceTank tank = new EssenceTank(256);
        tank.set(EssenceMix.pure(Essence.FIRE, 100));
        helper.assertTrue(HextechTools.fillFrom(tool, tank) == HextechTools.CELL, "the cell fills up to its size");
        helper.assertTrue(tank.amount() == 100 - HextechTools.CELL, "the tank gives what the cell took");
        EssenceTank murky = new EssenceTank(256);
        murky.set(EssenceMix.unknown(50));
        helper.assertTrue(HextechTools.fillFrom(new ItemStack(ModItems.HEXTECH_PICKAXE.get()), murky) < 0, "unknown essence is refused");
        helper.assertTrue(HextechTools.spend(tool, Essence.FIRE, 4) && HextechTools.cell(tool).get(Essence.FIRE) == HextechTools.CELL - 4,
                "a firing burns its essence");
        helper.assertFalse(HextechTools.spend(tool, Essence.WATER, 1), "nothing burns without that essence");
        helper.succeed();
    }

    /** Every rune of the codex does something on a tool and can be burned into one at the altar. */
    @HexTest
    public static void everyRuneWorksOnTools(GameTestHelper helper) {
        helper.assertTrue(ToolRuneEffects.withoutEffect().isEmpty(), "runes without a tool effect: " + ToolRuneEffects.withoutEffect());
        for (ComplexRune rune : ComplexRunes.all()) {
            helper.assertTrue(ToolRunes.worksOnTools(rune), rune.key() + " works on tools");
            helper.assertTrue(InfusionRecipes.engraving(false, rune) != null, rune.key() + " has an engraving");
        }
        helper.succeed();
    }

    /** Cache: what the tool breaks goes straight into the pocket; Drill takes the blocks around too. */
    @HexTest
    public static void drillAndCacheMineIntoThePocket(GameTestHelper helper) {
        ItemStack tool = new ItemStack(ModItems.HEXTECH_PICKAXE.get());
        HextechTools.setRunes(tool, RuneSlots.EMPTY.with(new RuneSlots.Engraving(0, ComplexRunes.byKey("metal_earth"), 1))
                .with(new RuneSlots.Engraving(1, ComplexRunes.byKey("earth_shadow"), 1)));
        HextechTools.setCell(tool, EssenceMix.of(Map.of(Essence.METAL, 30, Essence.EARTH, 20)));
        BlockPos centre = helper.absolutePos(new BlockPos(2, 3, 2));
        for (BlockPos at : BlockPos.betweenClosed(centre.offset(-1, 0, -1), centre.offset(1, 0, 1))) {
            helper.getLevel().setBlockAndUpdate(at, Blocks.STONE.defaultBlockState());
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(centre.getX() + 0.5, centre.getY() + 2, centre.getZ() + 0.5, 0, 90);
        BlockState stone = Blocks.STONE.defaultBlockState();
        helper.getLevel().removeBlock(centre, false);
        Block.dropResources(stone, helper.getLevel(), centre, null, player, tool);
        int left = 0;
        for (BlockPos at : BlockPos.betweenClosed(centre.offset(-1, 0, -1), centre.offset(1, 0, 1))) {
            left += helper.getLevel().getBlockState(at).isAir() ? 0 : 1;
        }
        helper.assertTrue(left == 0, "the 3x3 around is mined, " + left + " left");
        helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 9, "all nine cobblestones went into the pocket");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(centre).inflate(3)).isEmpty(), "nothing on the ground");
        helper.assertTrue(HextechTools.cell(tool).get(Essence.METAL) == 30 - 8, "one firing of Drill per extra block");
        helper.succeed();
    }

    /** Right click fires the rune's spell: Healing heals, pays, and the tool rests a moment. */
    @HexTest
    public static void useRunesFireOnRightClick(GameTestHelper helper) {
        ItemStack tool = new ItemStack(ModItems.HEXTECH_PICKAXE.get());
        HextechTools.setRunes(tool, RuneSlots.EMPTY.with(new RuneSlots.Engraving(0, ComplexRunes.byKey("water_life"), 2)));
        HextechTools.setCell(tool, EssenceMix.pure(Essence.WATER, 10));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setHealth(6);
        helper.assertTrue(ToolRuneEffects.use(helper.getLevel(), player, tool), "the rune fires");
        helper.assertTrue(player.getHealth() >= 10, "and heals");
        helper.assertTrue(HextechTools.cell(tool).get(Essence.WATER) == 10 - ToolRunes.cost(ComplexRunes.byKey("water_life")), "paid");
        helper.assertFalse(ToolRuneEffects.use(helper.getLevel(), player, tool), "not again right away");
        helper.succeed();
    }

    /** The altar recipes load from data, including the first infusion, Smelting's engraving and erasing. */
    @HexTest
    public static void altarRecipesLoad(GameTestHelper helper) {
        InfusionRecipe pickaxe = InfusionRecipes.get(false).get(Hextale.id("hextech_pickaxe"));
        helper.assertTrue(pickaxe != null && pickaxe.kind() == InfusionRecipe.Kind.INFUSE, "the pickaxe infusion is loaded");
        helper.assertTrue(pickaxe.pedestals().size() == 4 && pickaxe.essences().get(Essence.ORE) == 24, "with its items and essences");
        helper.assertTrue(InfusionRecipes.engraving(false, ToolRunes.SMELTING) != null, "Smelting can be engraved");
        helper.assertTrue(InfusionRecipes.erasing(false) != null, "runes can be ground off");
        for (Map.Entry<Identifier, InfusionRecipe> entry : InfusionRecipes.get(false).entrySet()) {
            if (entry.getValue().kind() == InfusionRecipe.Kind.ENGRAVE) {
                helper.assertTrue(ToolRunes.worksOnTools(entry.getValue().rune()), entry.getKey() + " engraves a rune that works on tools");
            }
        }
        helper.succeed();
    }

    /** An iron pickaxe, four items and three essences become a Hextech Pickaxe that keeps its enchantments. */
    @HexTest(maxTicks = 260)
    public static void altarInfusesTheHextechPickaxe(GameTestHelper helper) {
        ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
        iron.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.EFFICIENCY), 2);
        EssenceMix essence = EssenceMix.of(Map.of(Essence.ORE, 40, Essence.FORGE, 30, Essence.ENERGY, 20));
        HextechAltarBlockEntity altar = circle(helper, iron, List.of(new ItemStack(ModItems.BRASS_INGOT.get()), new ItemStack(Items.AMETHYST_SHARD),
                new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), new ItemStack(ModItems.BRASS_INGOT.get())), essence);
        altar.startOrAbort(null);
        helper.assertTrue(altar.active() && altar.kind() == Ritual.Kind.INFUSE, "the ritual starts");
        helper.succeedWhen(() -> {
            helper.assertTrue(altar.center().is(ModItems.HEXTECH_PICKAXE.get()), "the centre became a hextech pickaxe");
            helper.assertTrue(altar.center().isEnchanted(), "it kept its enchantment");
            for (BlockPos pos : PEDESTALS) {
                helper.assertTrue(helper.getBlockEntity(pos, PedestalBlockEntity.class).item().isEmpty(), "every pedestal item was drawn");
            }
            EssenceMix left = helper.getBlockEntity(TANK, EssenceTankBlockEntity.class).tank().contents();
            helper.assertTrue(left.get(Essence.ORE) == 16 && left.get(Essence.FORGE) == 14 && left.get(Essence.ENERGY) == 12,
                    "the essences were drawn from the tank, left " + left);
        });
    }

    /** Anything extra on a pedestal spoils the match: nothing starts. */
    @HexTest
    public static void anExtraItemSpoilsTheCircle(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(Items.IRON_PICKAXE), List.of(new ItemStack(ModItems.BRASS_INGOT.get()),
                new ItemStack(ModItems.BRASS_INGOT.get()), new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), new ItemStack(Items.DIRT)),
                EssenceMix.pure(Essence.METAL, 100));
        altar.startOrAbort(null);
        helper.assertFalse(altar.active(), "a circle with a wrong item does not start");
        helper.succeed();
    }

    /** A stele with Smelting, a furnace and coal and fire + earth engrave Smelting into the pickaxe with the stele's stars. */
    @HexTest(maxTicks = 200)
    public static void altarEngravesTheSteleRune(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(ModItems.HEXTECH_PICKAXE.get()),
                List.of(new ItemStack(Items.FURNACE), new ItemStack(Items.COAL)), EssenceMix.of(Map.of(Essence.FIRE, 50, Essence.MAGMA, 50)));
        helper.setBlock(STELE, ModBlocks.RUNE_STELE.get());
        helper.getBlockEntity(STELE, RuneSteleBlockEntity.class).choose(ToolRunes.SMELTING, 3);
        altar.startOrAbort(null);
        helper.assertTrue(altar.kind() == Ritual.Kind.ENGRAVE, "the ritual engraves");
        helper.succeedWhen(() -> {
            RuneSlots.Engraving engraving = HextechTools.runes(altar.center()).find(ToolRunes.SMELTING);
            helper.assertTrue(engraving != null && engraving.slot() == 0 && engraving.stars() == 3, "Smelting sits in slot I with three stars");
        });
    }

    /** A pickaxe with Smelting and nothing else around fills its cell with fire from the tanks. */
    @HexTest(maxTicks = 120)
    public static void altarChargesATool(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, smeltingPickaxe(0), List.of(), EssenceMix.of(Map.of(Essence.FIRE, 100, Essence.WATER, 50)));
        altar.startOrAbort(null);
        helper.assertTrue(altar.kind() == Ritual.Kind.CHARGE, "the ritual charges");
        helper.succeedWhen(() -> {
            helper.assertFalse(altar.active(), "charging is done");
            EssenceMix cell = HextechTools.cell(altar.center());
            helper.assertTrue(cell.get(Essence.FIRE) == HextechTools.CELL && cell.get(Essence.WATER) == 0,
                    "the cell took only the fire its rune burns, got " + cell);
        });
    }

    /** With an essence missing the ritual stalls instead of finishing. */
    @HexTest(maxTicks = 200)
    public static void aRitualShortOfEssenceStalls(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(Items.IRON_PICKAXE), List.of(new ItemStack(ModItems.BRASS_INGOT.get()),
                new ItemStack(ModItems.BRASS_INGOT.get()), new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), new ItemStack(Items.AMETHYST_SHARD)),
                EssenceMix.of(Map.of(Essence.ORE, 40, Essence.FORGE, 30)));
        altar.startOrAbort(null);
        helper.runAfterDelay(180, () -> {
            helper.assertTrue(altar.active() && altar.stalled(), "without energy essence the ritual waits");
            helper.assertTrue(altar.center().is(Items.IRON_PICKAXE), "and the centre is untouched");
            helper.succeed();
        });
    }

    /** The player's way: a Creative Essence Tank, sneak + empty hand on the altar, and wait for the result. */
    @HexTest(maxTicks = 260)
    public static void aPlayerStartsARitualWithCreativeTanks(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(Items.IRON_PICKAXE), List.of(new ItemStack(ModItems.BRASS_INGOT.get()),
                new ItemStack(Items.AMETHYST_SHARD), new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), new ItemStack(ModItems.BRASS_INGOT.get())),
                EssenceMix.EMPTY);
        BlockPos creative = new BlockPos(6, 2, 0);
        helper.setBlock(creative, ModBlocks.CREATIVE_ESSENCE_TANK.get());
        helper.getBlockEntity(creative, EssenceTankBlockEntity.class).tank().set(EssenceTankBlockEntity.CREATIVE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        BlockPos abs = helper.absolutePos(ALTAR);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false);
        helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(altar.active(), "sneak + empty hand starts the ritual");
        helper.succeedWhen(() -> helper.assertTrue(altar.center().is(ModItems.HEXTECH_PICKAXE.get()), "the ritual finishes"));
    }

    /** A stele's rune is burned into the pickaxe the moment it is made; the rune's items join the pickaxe's on the pedestals. */
    @HexTest(maxTicks = 300)
    public static void theSteleRuneGoesIntoTheNewPickaxe(GameTestHelper helper) {
        HextechAltarBlockEntity altar = circle(helper, new ItemStack(Items.IRON_PICKAXE), List.of(new ItemStack(ModItems.BRASS_INGOT.get()),
                new ItemStack(Items.AMETHYST_SHARD), new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), new ItemStack(ModItems.BRASS_INGOT.get())),
                EssenceMix.of(Map.of(Essence.ORE, 40, Essence.FORGE, 30, Essence.ENERGY, 20, Essence.FIRE, 40, Essence.MAGMA, 30)));
        BlockPos[] extra = {new BlockPos(5, 2, 1), new BlockPos(1, 2, 5)};
        ItemStack[] items = {new ItemStack(Items.FURNACE), new ItemStack(Items.COAL)};
        for (int i = 0; i < extra.length; i++) {
            helper.setBlock(extra[i], ModBlocks.PEDESTAL.get());
            helper.getBlockEntity(extra[i], PedestalBlockEntity.class).place(items[i]);
        }
        helper.setBlock(STELE, ModBlocks.RUNE_STELE.get());
        helper.getBlockEntity(STELE, RuneSteleBlockEntity.class).choose(ToolRunes.SMELTING, 2);
        altar.startOrAbort(null);
        helper.assertTrue(altar.kind() == Ritual.Kind.INFUSE, "the infusion starts with the rune's items");
        helper.succeedWhen(() -> {
            helper.assertTrue(altar.center().is(ModItems.HEXTECH_PICKAXE.get()), "the pickaxe is made");
            helper.assertTrue(HextechTools.runes(altar.center()).has(ToolRunes.SMELTING), "already carrying Smelting");
        });
    }
}
