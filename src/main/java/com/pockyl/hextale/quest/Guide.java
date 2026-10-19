package com.pockyl.hextale.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechPickaxe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * The researcher's guide: everything the mod asks of a researcher, as a tree of steps gathered by theme ({@link Region}):
 * the workshop, the essence works, the rune lore, the hextech circle, the pylons and the forge, with the devices still
 * being worked out (WIP) hanging off the steps they will grow from. A step opens when all its parents are done, and
 * parents are only what the step really needs. Devices and tools past the essence works carry a research topic: they
 * are researched at the table before they can be made. Runes are not on the guide: their scrolls are found in the world
 * and the book lists them on a page of their own. The order is the order things can really be made in:
 * {@code QuestGameTests.guideOrderIsLogical} checks that every mod item a step's recipe needs comes from a step before it.
 */
public final class Guide {
    /** Map pixels from a step to its children along the region's direction. */
    public static final int STEP = 62;
    /** Map pixels between neighbouring branches across the region's direction. */
    private static final int LANE = 50;
    /** A new region's branch keeps this far from every step laid out before it. */
    private static final int CLEAR = 90;
    /** Open sky around the landmark: nothing else comes closer. */
    private static final int ROOM = 112;
    /** No two steps closer than this, whatever the branches want. */
    private static final int SEPARATE = 46;

    private static final List<Quest> ALL = new ArrayList<>();
    private static final Map<String, Quest> BY_ID = new HashMap<>();

    // ---- The workshop: raw hextech and its dust, the woken workbench and furnace, brass, the table and the diary -------

    public static final Quest UNSTABLE_HEXTECH = item(Region.WORKSHOP, "unstable_hextech", ModItems.UNSTABLE_HEXTECH, Quest.Aura.STORM,
            QuestDisplay.scheme("hextale:unstable_hextech", "minecraft:lightning_rod", "minecraft:amethyst_block"));
    // A thrown orb shatters into dust; dust wakes the crafting table and the furnace.
    public static final Quest HEXTECH_DUST = item(Region.WORKSHOP, "hextech_dust", ModItems.HEXTECH_DUST, Quest.Aura.STORM,
            QuestDisplay.scheme("hextale:hextech_dust", "hextale:unstable_hextech"), UNSTABLE_HEXTECH);
    public static final Quest HEX_WORKBENCH = item(Region.WORKSHOP, "hex_workbench", ModItems.HEX_WORKBENCH, Quest.Aura.GOLD,
            QuestDisplay.scheme("hextale:hex_workbench", "hextale:hextech_dust", "minecraft:crafting_table"), HEXTECH_DUST);
    public static final Quest HEX_FURNACE = item(Region.FORGE, "hex_furnace", ModItems.HEX_FURNACE, Quest.Aura.FLAME,
            QuestDisplay.scheme("hextale:hex_furnace", "hextale:hextech_dust", "minecraft:furnace"), HEXTECH_DUST);
    // Zinc is mined: sphalerite lies in stone and deepslate like copper, a little rarer.
    public static final Quest SPHALERITE = add(Region.WORKSHOP, "sphalerite", Quest.Kind.ITEM, List.of(),
            () -> new ItemStack(ModItems.SPHALERITE_ORE.get()), Quests.has(ModItems.RAW_ZINC), null, null, Quest.Aura.NONE);
    public static final Quest ZINC = item(Region.WORKSHOP, "zinc", ModItems.ZINC_INGOT, Quest.Aura.NONE,
            QuestDisplay.scheme("hextale:zinc_ingot", "hextale:raw_zinc", "minecraft:furnace"), SPHALERITE);
    // Brass is the first alloy of the Hex Furnace, free to melt.
    public static final Quest BRASS = item(Region.WORKSHOP, "brass", ModItems.BRASS_INGOT, Quest.Aura.NONE,
            QuestDisplay.alloy("hextale:brass_ingot"), HEX_FURNACE, ZINC);
    public static final Quest RESEARCH_TABLE = item(Region.WORKSHOP, "research_table", ModItems.RESEARCH_TABLE, Quest.Aura.GOLD,
            QuestDisplay.hexCrafting("hextale:research_table"), BRASS, HEX_WORKBENCH);
    public static final Quest RESEARCHERS_DIARY = item(Region.WORKSHOP, "researchers_diary", ModItems.RESEARCHERS_DIARY, Quest.Aura.NONE,
            QuestDisplay.shapeless("hextale:researchers_diary", 1, "minecraft:book", "minecraft:amethyst_shard", "minecraft:feather"));
    public static final Quest ITEM_STUDY = add(Region.WORKSHOP, "item_study", Quest.Kind.ITEM, List.of(RESEARCH_TABLE, RESEARCHERS_DIARY),
            () -> new ItemStack(Items.PAPER), Quests.studied(1), QuestDisplay.essences(), null, Quest.Aura.NONE);
    public static final Quest RESEARCHERS_GOGGLES = item(Region.WORKSHOP, "researchers_goggles", ModItems.RESEARCHERS_GOGGLES, Quest.Aura.NONE,
            QuestDisplay.hexCrafting("hextale:researchers_goggles"), BRASS, HEX_WORKBENCH);

    // ---- The essence works: boiling, bottling, separating, storing ---------------------------------------------------

    public static final Quest ALEMBIC = item(Region.ESSENCES, "alembic", ModItems.ALEMBIC, Quest.Aura.GOLD,
            QuestDisplay.hexCrafting("hextale:alembic"), BRASS, HEX_WORKBENCH);
    public static final Quest ESSENCE_BOTTLE = add(Region.ESSENCES, "essence_bottle", Quest.Kind.ITEM, List.of(ALEMBIC),
            () -> EssenceFlaskItem.filled(Quests.SAMPLE_MIX), Quests.has(ModItems.ESSENCE_FLASK),
            QuestDisplay.scheme("hextale:essence_flask", "minecraft:glass_bottle", "hextale:alembic"), null, Quest.Aura.NONE);
    public static final Quest BRASS_PIPE = item(Region.ESSENCES, "brass_pipe", ModItems.BRASS_PIPE, Quest.Aura.NONE,
            QuestDisplay.hexCrafting("hextale:brass_pipe"), ALEMBIC);
    public static final Quest PUMP = item(Region.ESSENCES, "pump", ModItems.PUMP, Quest.Aura.NONE,
            QuestDisplay.hexCrafting("hextale:pump"), BRASS_PIPE);
    public static final Quest SEPARATOR = item(Region.ESSENCES, "separator", ModItems.SEPARATOR, Quest.Aura.NONE,
            QuestDisplay.hexCrafting("hextale:separator"), ESSENCE_BOTTLE);
    public static final Quest ESSENCE_TANK = item(Region.ESSENCES, "essence_tank", ModItems.ESSENCE_TANK, Quest.Aura.NONE,
            QuestDisplay.hexCrafting("hextale:essence_tank"), ESSENCE_BOTTLE);
    public static final Quest MAGIC_STONE = item(Region.ESSENCES, "magic_stone", ModItems.MAGIC_STONE, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:magic_stone", 8, new String[]{"SSS", "SBS", "SSS"},
                    "S=#hextale:soaks_essence", "B=hextale:essence_flask"), ESSENCE_BOTTLE);
    public static final Quest CONCENTRATE = add(Region.ESSENCES, "concentrate", Quest.Kind.ITEM, List.of(SEPARATOR),
            () -> EssenceFlaskItem.filled(EssenceMix.pure(Essence.WATER, EssenceFlaskItem.CAPACITY)), Quests::concentrate,
            QuestDisplay.reagents(), null, Quest.Aura.NONE);

    // The table's ink comes from jars of concentrate: the workshop and the essence works meet here, and research begins.
    public static final Quest INK = add(Region.WORKSHOP, "ink", Quest.Kind.ITEM, List.of(ITEM_STUDY, ESSENCE_TANK, CONCENTRATE),
            () -> new ItemStack(Items.INK_SAC), Quests::inkAtTable, null, null, Quest.Aura.GOLD);

    // Past the ink every device is researched before it is made.
    public static final Quest DRAIN_VALVE = researched(Region.ESSENCES, "drain_valve", ModItems.DRAIN_VALVE, ResearchTopics.DRAIN_VALVE,
            Quest.Aura.NONE, QuestDisplay.hexCrafting("hextale:drain_valve"), SEPARATOR, BRASS_PIPE, INK);
    // The first multiblock: built of glass and slabs, woken with a hextech crystal (Multiblocks completes it).
    public static final Quest BIG_ESSENCE_JAR = add(Region.ESSENCES, "big_essence_jar", Quest.Kind.ITEM, List.of(ESSENCE_TANK, UNSTABLE_HEXTECH, INK),
            () -> new ItemStack(ModItems.ESSENCE_TANK.get()), player -> false, QuestDisplay.multiblock("big_essence_jar"),
            ResearchTopics.BIG_ESSENCE_JAR, Quest.Aura.GOLD);

    // ---- Rune lore: scrolls found in the world, copied at the altar --------------------------------------------------

    public static final Quest RUNE_SCROLL = add(Region.RUNES, "rune_scroll", Quest.Kind.ITEM, List.of(RESEARCHERS_DIARY),
            () -> new ItemStack(ModItems.RUNE_SCROLL.get()), Quests.knowledge(knowledge -> knowledge.runeCount() > 0), null, null,
            Quest.Aura.GOLD);
    public static final Quest BLANK_SCROLL = item(Region.RUNES, "blank_scroll", ModItems.BLANK_SCROLL, Quest.Aura.NONE,
            QuestDisplay.shapeless("hextale:blank_scroll", 1, "minecraft:paper", "minecraft:paper", "minecraft:paper"), RUNE_SCROLL);

    // ---- The hextech circle: the altar, its pedestals and steles, refined hextech ------------------------------------

    public static final Quest HEXTECH_ALTAR = researched(Region.CIRCLE, "hextech_altar", ModItems.HEXTECH_ALTAR, ResearchTopics.CIRCLE,
            Quest.Aura.GOLD, QuestDisplay.hexCrafting("hextale:hextech_altar"), MAGIC_STONE, UNSTABLE_HEXTECH, INK);
    public static final Quest PEDESTAL = researched(Region.CIRCLE, "pedestal", ModItems.PEDESTAL, ResearchTopics.CIRCLE, Quest.Aura.NONE,
            QuestDisplay.hexCrafting("hextale:pedestal"), HEXTECH_ALTAR);
    public static final Quest RUNE_STELE = researched(Region.CIRCLE, "rune_stele", ModItems.RUNE_STELE, ResearchTopics.CIRCLE, Quest.Aura.NONE,
            QuestDisplay.hexCrafting("hextale:rune_stele"), HEXTECH_ALTAR);
    // Crystal cutting: the lapidary from the workbench, its disc from the altar (a ritual draws essence from jars around it).
    public static final Quest HEX_LAPIDARY = researched(Region.CIRCLE, "hex_lapidary", ModItems.HEX_LAPIDARY, ResearchTopics.LAPIDARY,
            Quest.Aura.HEXTECH, QuestDisplay.hexCrafting("hextale:hex_lapidary").then(QuestDisplay.infusion("hextale:cutting_disc")),
            PEDESTAL, ESSENCE_TANK);
    public static final Quest HEXTECH_CRYSTAL = item(Region.CIRCLE, "hextech_crystal", ModItems.HEXTECH_CRYSTAL, Quest.Aura.HEXTECH,
            QuestDisplay.scheme("hextale:hextech_crystal", "hextale:unstable_hextech", "hextale:cutting_disc"), HEX_LAPIDARY);
    public static final Quest HEXTECH_LAMP = researched(Region.CIRCLE, "hextech_lamp", ModItems.HEXTECH_LAMP, ResearchTopics.HEXTECH_LAMP,
            Quest.Aura.HEXTECH, QuestDisplay.hexCrafting("hextale:hextech_lamp"), HEXTECH_CRYSTAL);

    // Copying a rune: a blank scroll on the altar, the rune on a stele (HextechAltarBlockEntity completes it).
    public static final Quest WRITTEN_SCROLL = add(Region.RUNES, "written_scroll", Quest.Kind.ITEM, List.of(BLANK_SCROLL, RUNE_STELE),
            () -> new ItemStack(ModItems.RUNE_SCROLL.get()), player -> false,
            QuestDisplay.scheme("hextale:rune_scroll", "hextale:blank_scroll", "hextale:rune_stele"), null, Quest.Aura.HEXTECH);

    // ---- Hex pylons: items carried by hextech, gathered off the ground, thrown to links, passed on by relays ----------

    public static final Quest GATHERING_PYLON = researched(Region.PYLONS, "gathering_pylon", ModItems.GATHERING_PYLON,
            ResearchTopics.GATHERING_PYLON, Quest.Aura.HEXTECH, QuestDisplay.infusion("hextale:gathering_pylon"), HEXTECH_CRYSTAL);
    public static final Quest SENDING_PYLON = researched(Region.PYLONS, "sending_pylon", ModItems.SENDING_PYLON, ResearchTopics.SENDING_PYLON,
            Quest.Aura.GOLD, QuestDisplay.infusion("hextale:sending_pylon"), GATHERING_PYLON);
    public static final Quest RELAY_PYLON = researched(Region.PYLONS, "relay_pylon", ModItems.RELAY_PYLON, ResearchTopics.RELAY_PYLON,
            Quest.Aura.HEXTECH, QuestDisplay.infusion("hextale:relay_pylon"), SENDING_PYLON);
    // The sigils that steer the pylons are written at the altar.
    public static final Quest PYLON_FILTER = item(Region.PYLONS, "pylon_filter", ModItems.PYLON_FILTER, Quest.Aura.HEXTECH,
            QuestDisplay.infusion("hextale:pylon_filter"), GATHERING_PYLON);
    public static final Quest SIPHON_PYLON = researched(Region.PYLONS, "siphon_pylon", ModItems.SIPHON_PYLON, ResearchTopics.SIPHON_PYLON,
            Quest.Aura.HEXTECH, QuestDisplay.infusion("hextale:siphon_pylon"), SENDING_PYLON, ESSENCE_TANK, BRASS_PIPE);
    public static final Quest ESSENCE_STICKER = item(Region.PYLONS, "essence_sticker", ModItems.ESSENCE_STICKER, Quest.Aura.HEXTECH,
            QuestDisplay.infusion("hextale:essence_sticker"), SIPHON_PYLON);
    public static final Quest PYLON_RUNE = add(Region.PYLONS, "pylon_rune", Quest.Kind.ITEM, List.of(GATHERING_PYLON, RUNE_STELE, RUNE_SCROLL),
            () -> new ItemStack(ModItems.RUNE_STELE.get()), Quests.pylonRunes(1), QuestDisplay.pylonRunes(), null, Quest.Aura.NONE);

    // ---- The forge: alloys, the master disc and stabilised gemstones, hextech pickaxes ----------------------------------

    public static final Quest HEXSTEEL = researched(Region.FORGE, "hexsteel", ModItems.HEXSTEEL_INGOT, ResearchTopics.HEXSTEEL,
            Quest.Aura.FLAME, QuestDisplay.alloy("hextale:hexsteel_ingot"), HEX_FURNACE, HEXTECH_DUST, INK);
    public static final Quest MASTER_CUTTING_DISC = researched(Region.FORGE, "master_cutting_disc", ModItems.MASTER_CUTTING_DISC,
            ResearchTopics.MASTER_DISC, Quest.Aura.HEXTECH, QuestDisplay.infusion("hextale:master_cutting_disc"), HEXSTEEL, HEXTECH_CRYSTAL);
    public static final Quest HEXTECH_GEMSTONE = item(Region.FORGE, "hextech_gemstone", ModItems.HEXTECH_GEMSTONE, Quest.Aura.GOLD,
            QuestDisplay.scheme("hextale:hextech_gemstone", "hextale:hextech_crystal", "hextale:master_cutting_disc"),
            MASTER_CUTTING_DISC, HEXTECH_CRYSTAL);
    public static final Quest HEXTECH_PICKAXE = add(Region.FORGE, "hextech_pickaxe", Quest.Kind.ITEM, List.of(HEXTECH_CRYSTAL),
            () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.HEXTECH).get()), Quests.pickaxe(1),
            QuestDisplay.infusion("hextale:hextech_pickaxe"), ResearchTopics.HEXTECH_PICKAXE, Quest.Aura.GOLD);
    public static final Quest PICKAXE_RUNE = add(Region.FORGE, "pickaxe_rune", Quest.Kind.ITEM, List.of(HEXTECH_PICKAXE, RUNE_STELE, RUNE_SCROLL),
            () -> new ItemStack(ModItems.RUNE_SCROLL.get()), Quests.pickaxeRunes(1), QuestDisplay.pickaxeRunes(), null, Quest.Aura.NONE);
    public static final Quest REFINED_PICKAXE = add(Region.FORGE, "refined_pickaxe", Quest.Kind.ITEM, List.of(HEXTECH_PICKAXE, HEXTECH_GEMSTONE),
            () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.REFINED).get()), Quests.pickaxe(2),
            QuestDisplay.infusion("hextale:refined_hextech_pickaxe"), ResearchTopics.REFINED_PICKAXE, Quest.Aura.HEXTECH);
    public static final Quest RADIANT_PICKAXE = add(Region.FORGE, "radiant_pickaxe", Quest.Kind.ITEM, List.of(REFINED_PICKAXE),
            () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.RADIANT).get()), Quests.pickaxe(3),
            QuestDisplay.infusion("hextale:radiant_hextech_pickaxe"), ResearchTopics.RADIANT_PICKAXE, Quest.Aura.HEXTECH);

    // ---- Devices still being worked out, each hanging off the step it will grow from ---------------------------------

    static {
        // The workshop.
        wip("jar_gauge", Items.COMPARATOR, Quest.Aura.NONE, RESEARCHERS_GOGGLES);
        wip("respirator", Items.LEATHER_HELMET, Quest.Aura.NONE, RESEARCHERS_GOGGLES, CONCENTRATE);
        wip("storm_collector", Items.LIGHTNING_ROD.asList().getFirst(), Quest.Aura.STORM, UNSTABLE_HEXTECH);
        wip("rain_caller", Items.TRIDENT, Quest.Aura.STORM, UNSTABLE_HEXTECH);
        wip("xp_condenser", Items.SCULK_CATALYST, Quest.Aura.HEXTECH, ITEM_STUDY);
        wip("hex_archive", Items.CHISELED_BOOKSHELF, Quest.Aura.HEXTECH, INK);
        // The essence works, the fields and the woods.
        wip("sawmill", Items.OAK_PLANKS, Quest.Aura.NONE, ALEMBIC);
        wip("sap_tap", Items.HONEY_BOTTLE, Quest.Aura.LIFE, ESSENCE_BOTTLE);
        wip("essence_sprinkler", Items.POINTED_DRIPSTONE, Quest.Aura.LIFE, BRASS_PIPE, CONCENTRATE);
        wip("bloom_censer", Items.SPORE_BLOSSOM, Quest.Aura.LIFE, BRASS_PIPE);
        wip("quench_tank", Items.WATER_BUCKET, Quest.Aura.NONE, PUMP);
        wip("steam_boiler", Items.CAULDRON, Quest.Aura.NONE, PUMP);
        wip("fish_weir", Items.FISHING_ROD, Quest.Aura.NONE, PUMP);
        wip("rune_pump", Items.HOPPER, Quest.Aura.NONE, PUMP);
        wip("chem_burner", Items.CAMPFIRE, Quest.Aura.FLAME, SEPARATOR);
        wip("lab_bench", Items.BREWING_STAND, Quest.Aura.NONE, SEPARATOR);
        wip("experiment_pot", Items.FLOWER_POT, Quest.Aura.LIFE, SEPARATOR, CONCENTRATE);
        wip("specimen_cage", Items.IRON_BARS, Quest.Aura.CURSED, SEPARATOR);
        wip("seed_sower", Items.WHEAT_SEEDS, Quest.Aura.NONE, ESSENCE_TANK);
        wip("reaper", Items.WHEAT, Quest.Aura.NONE, ESSENCE_TANK);
        wip("feeding_trough", Items.HAY_BLOCK, Quest.Aura.LIFE, ESSENCE_TANK);
        wip("herd_warden", Items.SHEARS, Quest.Aura.LIFE, ESSENCE_TANK);
        wip("living_soil", Items.ROOTED_DIRT, Quest.Aura.LIFE, MAGIC_STONE);
        wip("field_ward", Items.CARVED_PUMPKIN, Quest.Aura.NONE, MAGIC_STONE);
        wip("tree_feller", Items.OAK_LOG, Quest.Aura.NONE, MAGIC_STONE, ESSENCE_TANK);
        wip("greenhouse", Items.FLOWERING_AZALEA, Quest.Aura.LIFE, MAGIC_STONE);
        wip("mutation_injector", Items.SPIDER_EYE, Quest.Aura.CURSED, CONCENTRATE);
        wip("healing_spring", Items.GLISTERING_MELON_SLICE, Quest.Aura.LIFE, CONCENTRATE);
        wip("spring_basin", Items.LILY_PAD, Quest.Aura.LIFE, CONCENTRATE);
        wip("shimmer", Items.EXPERIENCE_BOTTLE, Quest.Aura.CURSED, CONCENTRATE);
        wip("compost_vat", Items.COMPOSTER, Quest.Aura.NONE, DRAIN_VALVE);
        wip("the_gray", Items.GUNPOWDER, Quest.Aura.CURSED, DRAIN_VALVE);
        wip("acid_bath", Items.FERMENTED_SPIDER_EYE, Quest.Aura.CURSED, DRAIN_VALVE);
        wip("cooler", Items.BLUE_ICE, Quest.Aura.NONE, DRAIN_VALVE);
        wip("crystal_vat", Items.BUDDING_AMETHYST, Quest.Aura.CURSED, BIG_ESSENCE_JAR, HEXTECH_GEMSTONE);
        wip("regen_tank", Items.AXOLOTL_BUCKET, Quest.Aura.CURSED, BIG_ESSENCE_JAR);
        wip("grove_keeper", Items.OAK_SAPLING, Quest.Aura.LIFE, BIG_ESSENCE_JAR);
        // Rune lore.
        wip("rune_engraver", Items.BRUSH, Quest.Aura.NONE, WRITTEN_SCROLL);
        wip("lull_lantern", Items.SOUL_LANTERN, Quest.Aura.CURSED, WRITTEN_SCROLL);
        wip("veil", Items.PHANTOM_MEMBRANE, Quest.Aura.CURSED, RUNE_SCROLL);
        // The circle.
        wip("geode_resonator", Items.SMALL_AMETHYST_BUD, Quest.Aura.STORM, HEXTECH_ALTAR, ESSENCE_TANK);
        wip("nullstone", Items.CRYING_OBSIDIAN, Quest.Aura.CURSED, HEXTECH_ALTAR);
        wip("hexcore", Items.HEART_OF_THE_SEA, Quest.Aura.GOLD, HEXTECH_ALTAR, HEXTECH_GEMSTONE);
        wip("rune_workbench", Items.CRAFTER, Quest.Aura.HEXTECH, PEDESTAL, HEXTECH_CRYSTAL);
        wip("barrier_ward", Items.SHIELD, Quest.Aura.HEXTECH, PEDESTAL);
        wip("rune_lock", Items.TRIAL_KEY, Quest.Aura.CURSED, RUNE_STELE);
        wip("crystal_battery", Items.AMETHYST_CLUSTER, Quest.Aura.STORM, HEXTECH_CRYSTAL);
        wip("world_anchor", Items.RESPAWN_ANCHOR, Quest.Aura.STORM, HEXTECH_CRYSTAL, HEXTECH_GEMSTONE);
        wip("construct", Items.ARMOR_STAND, Quest.Aura.HEXTECH, HEXTECH_CRYSTAL);
        wip("star_rod", Items.END_ROD, Quest.Aura.HEXTECH, HEXTECH_LAMP);
        wip("signal_beacon", Items.BEACON, Quest.Aura.NONE, HEXTECH_LAMP);
        wip("mirage_projector", Items.TINTED_GLASS, Quest.Aura.NONE, HEXTECH_LAMP);
        wip("beam_relay", Items.SPYGLASS, Quest.Aura.NONE, HEXTECH_LAMP);
        wip("glowbloom_bed", Items.GLOW_BERRIES, Quest.Aura.LIFE, HEXTECH_LAMP);
        // The pylons.
        wip("harvest_construct", Items.GOLDEN_HOE, Quest.Aura.HEXTECH, GATHERING_PYLON);
        wip("forester_construct", Items.GOLDEN_AXE, Quest.Aura.HEXTECH, GATHERING_PYLON);
        wip("sorting_matrix", Items.OBSERVER, Quest.Aura.HEXTECH, SENDING_PYLON, PYLON_FILTER);
        wip("winged_courier", Items.FEATHER, Quest.Aura.HEXTECH, SENDING_PYLON);
        wip("hex_gate", Items.ENDER_EYE, Quest.Aura.HEXTECH, RELAY_PYLON);
        wip("folding_gate", Items.ENDER_PEARL, Quest.Aura.CURSED, RELAY_PYLON);
        wip("levitation_plate", Items.HEAVY_WEIGHTED_PRESSURE_PLATE, Quest.Aura.HEXTECH, RELAY_PYLON);
        wip("hex_turret", Items.DISPENSER, Quest.Aura.STORM, RELAY_PYLON);
        wip("essence_resonator", Items.GLASS_BOTTLE, Quest.Aura.HEXTECH, SIPHON_PYLON);
        wip("purifier", Items.CONDUIT, Quest.Aura.HEXTECH, SIPHON_PYLON);
        wip("hex_lift", Items.PISTON, Quest.Aura.NONE, PYLON_RUNE);
        wip("wind_turbine", Items.BREEZE_ROD, Quest.Aura.NONE, PYLON_RUNE);
        // The forge.
        wip("hextech_axe", Items.IRON_AXE, Quest.Aura.HEXTECH, HEXTECH_PICKAXE);
        wip("hextech_hoe", Items.IRON_HOE, Quest.Aura.LIFE, HEXTECH_PICKAXE);
        wip("hex_bore", Items.DIAMOND_PICKAXE, Quest.Aura.HEXTECH, HEXTECH_PICKAXE);
        wip("drainer", Items.SPONGE, Quest.Aura.NONE, PICKAXE_RUNE);
        wip("shadow_coffer", Items.ENDER_CHEST, Quest.Aura.CURSED, PICKAXE_RUNE);
        wip("hex_blade", Items.IRON_SWORD, Quest.Aura.STORM, REFINED_PICKAXE);
        wip("ore_washer", Items.RAW_IRON, Quest.Aura.NONE, REFINED_PICKAXE, PUMP);
        wip("hex_cutter", Items.STONECUTTER, Quest.Aura.NONE, RADIANT_PICKAXE);
        wip("flash_charge", Items.FIREWORK_STAR, Quest.Aura.STORM, RADIANT_PICKAXE);
        wip("chrono_chamber", Items.CLOCK, Quest.Aura.STORM, RADIANT_PICKAXE);
        wip("hex_flamethrower", Items.FIRE_CHARGE, Quest.Aura.FLAME, HEXSTEEL);
        wip("mining_charge", Items.TNT, Quest.Aura.FLAME, HEX_FURNACE);
        wip("magma_font", Items.LAVA_BUCKET, Quest.Aura.FLAME, HEX_FURNACE);
        wip("magic_metal", Items.NETHERITE_SCRAP, Quest.Aura.HEXTECH, HEXSTEEL, HEXTECH_GEMSTONE);
        wip("rune_anvil", Items.ANVIL, Quest.Aura.NONE, HEXSTEEL);
        // The altar is the heart of the guide: every device past the essence works is made around it.
        HEXTECH_ALTAR.landmark = true;
        HEXTECH_ALTAR.scene = Quest.Scene.ALTAR;
        PEDESTAL.scene = Quest.Scene.PEDESTAL;
        RUNE_STELE.scene = Quest.Scene.STELE;
        layout();
    }

    private Guide() {
    }

    // ---- Building -----------------------------------------------------------------------------------------------------

    private static Quest add(Region region, String id, Quest.Kind kind, List<Quest> parents, Supplier<ItemStack> icon,
                             Predicate<ServerPlayer> goal, @Nullable QuestDisplay display, @Nullable ResearchTopic topic, Quest.Aura aura) {
        Quest quest = new Quest(id, kind, parents, icon, goal, display, topic, aura, region);
        quest.index = ALL.size();
        ALL.add(quest);
        if (BY_ID.put(id, quest) != null) {
            throw new IllegalStateException("Two guide steps named " + id);
        }
        return quest;
    }

    private static Quest item(Region region, String id, Supplier<? extends ItemLike> item, Quest.Aura aura, @Nullable QuestDisplay display,
                              Quest... parents) {
        return add(region, id, Quest.Kind.ITEM, List.of(parents), () -> new ItemStack(item.get()), Quests.has(() -> item.get().asItem()),
                display, null, aura);
    }

    /** An item step whose item must be researched at the table first. */
    private static Quest researched(Region region, String id, Supplier<? extends ItemLike> item, ResearchTopic topic, Quest.Aura aura,
                                    @Nullable QuestDisplay display, Quest... parents) {
        return add(region, id, Quest.Kind.ITEM, List.of(parents), () -> new ItemStack(item.get()), Quests.has(() -> item.get().asItem()),
                display, topic, aura);
    }

    /** A device still being worked out: it sits in its first parent's region, on that parent's branch. */
    private static void wip(String id, ItemLike icon, Quest.Aura aura, Quest... parents) {
        add(parents[0].region(), "wip_" + id, Quest.Kind.WIP, List.of(parents), () -> new ItemStack(icon), player -> false, null, null, aura);
    }

    // ---- Layout -------------------------------------------------------------------------------------------------------

    /** A laid-out subtree: positions relative to its root ({@code u} along the region, {@code v} across) and its contour. */
    private record Shape(Map<Quest, double[]> positions, List<double[]> contour) {
    }

    /**
     * Lays every region out as a tree growing in the region's own direction: a step's children stand one {@link #STEP}
     * further along, side by side across it, siblings' branches packed as close as their outlines allow (a tidy tree).
     * The workshop sits in the middle; every other region starts next to the step its first step comes from and moves
     * further out along its direction until it keeps {@link #CLEAR} from everything laid out before it - its devices
     * still being worked out included, which gather around their steps right after the region's items. The landmark
     * keeps {@link #ROOM} of open sky around itself. A small steady wobble keeps the sky from looking like a grid.
     * Deterministic, so every reader sees the same sky.
     */
    private static void layout() {
        Map<Quest, double[]> placed = new LinkedHashMap<>();
        for (Region region : Region.values()) {
            List<Quest> roots = new ArrayList<>();
            for (Quest quest : ALL) {
                if (quest.region() == region && quest.kind() != Quest.Kind.WIP && layoutParent(quest) == null) {
                    roots.add(quest);
                }
            }
            if (roots.isEmpty()) {
                continue;
            }
            Shape forest = arrange(roots);
            double centre = 0;
            for (Quest root : roots) {
                centre += forest.positions().get(root)[1];
            }
            centre /= roots.size();
            double[] origin = {0, 0};
            double start = 0;
            Quest from = roots.getFirst().parent();
            if (from != null && placed.containsKey(from)) {
                origin = placed.get(from);
                start = roots.getFirst().landmark() ? ROOM : STEP;
            }
            double angle = region.angle();
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Map<Quest, double[]> world = new LinkedHashMap<>();
            for (int push = 0; push < 200; push++) {
                world.clear();
                double along = start + push * 16;
                for (var entry : forest.positions().entrySet()) {
                    double u = entry.getValue()[0] + along;
                    double v = entry.getValue()[1] - centre;
                    world.put(entry.getKey(), new double[]{origin[0] + u * cos - v * sin, origin[1] + u * sin + v * cos});
                }
                if (clear(world, placed)) {
                    break;
                }
            }
            placed.putAll(world);
            placeDevices(region, placed);
        }
        int n = ALL.size();
        double[] x = new double[n];
        double[] y = new double[n];
        for (Quest quest : ALL) {
            double[] at = placed.get(quest);
            x[quest.index] = at[0] + jitter(quest, 1) * 5;
            y[quest.index] = at[1] + jitter(quest, 2) * 5;
        }
        // Last, no two steps closer than SEPARATE (nothing closer than ROOM to the landmark).
        for (int pass = 0; pass < 80; pass++) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double want = spacing(ALL.get(i), ALL.get(j), SEPARATE);
                    double dx = x[j] - x[i];
                    double dy = y[j] - y[i];
                    if (Math.abs(dx) >= want || Math.abs(dy) >= want) {
                        continue;
                    }
                    double d = Math.sqrt(dx * dx + dy * dy);
                    if (d < want) {
                        if (d < 0.01) {
                            dx = 1;
                            dy = 0;
                            d = 1;
                        }
                        double push = (want - d) / d * 0.5;
                        // The landmark stays where its branch put it; the others make room.
                        double share = ALL.get(i).landmark() ? 0 : ALL.get(j).landmark() ? 2 : 1;
                        x[i] -= dx * push * share;
                        y[i] -= dy * push * share;
                        x[j] += dx * push * (2 - share);
                        y[j] += dy * push * (2 - share);
                    }
                }
            }
        }
        for (Quest quest : ALL) {
            quest.x = (int) Math.round(x[quest.index]);
            quest.y = (int) Math.round(y[quest.index]);
        }
    }

    /** The least distance between two steps: {@code base}, or {@link #ROOM} next to the landmark. */
    private static double spacing(Quest a, Quest b, double base) {
        return a.landmark() || b.landmark() ? Math.max(base, ROOM) : base;
    }

    /**
     * Devices still being worked out gather in a small fan around the step they hang from, on its outer side (away from
     * the step it grows from, so the fan leans the branch's way): each takes the first free spot on rings around the
     * step, trying the outward direction first and then turning left and right, never back past the sides.
     */
    private static void placeDevices(Region region, Map<Quest, double[]> placed) {
        for (Quest quest : ALL) {
            if (quest.kind() != Quest.Kind.WIP || quest.region() != region) {
                continue;
            }
            Quest parent = quest.parent();
            double[] at = placed.get(parent);
            Quest grand = parent.parent();
            double outward = grand != null && placed.containsKey(grand)
                    ? Math.atan2(at[1] - placed.get(grand)[1], at[0] - placed.get(grand)[0]) : parent.region().angle();
            double[] spot = null;
            double first = parent.landmark() ? ROOM : 50;
            for (int ring = 0; spot == null && ring < 14; ring++) {
                double radius = first + ring * 14;
                for (int turn = 0; turn < 7 && spot == null; turn++) {
                    double angle = outward + (turn % 2 == 0 ? 1 : -1) * ((turn + 1) / 2) * 0.5;
                    double[] candidate = {at[0] + Math.cos(angle) * radius, at[1] + Math.sin(angle) * radius};
                    if (free(quest, candidate, placed)) {
                        spot = candidate;
                    }
                }
            }
            placed.put(quest, spot != null ? spot : new double[]{at[0] + Math.cos(outward) * 260, at[1] + Math.sin(outward) * 260});
        }
    }

    private static boolean free(Quest quest, double[] spot, Map<Quest, double[]> placed) {
        for (var entry : placed.entrySet()) {
            double want = spacing(quest, entry.getKey(), SEPARATE);
            double dx = spot[0] - entry.getValue()[0];
            double dy = spot[1] - entry.getValue()[1];
            if (dx * dx + dy * dy < want * want) {
                return false;
            }
        }
        return true;
    }

    /** Whether every new position keeps {@link #CLEAR} from every step placed before. */
    private static boolean clear(Map<Quest, double[]> fresh, Map<Quest, double[]> placed) {
        for (var a : fresh.entrySet()) {
            for (var b : placed.entrySet()) {
                double want = spacing(a.getKey(), b.getKey(), CLEAR);
                double dx = a.getValue()[0] - b.getValue()[0];
                double dy = a.getValue()[1] - b.getValue()[1];
                if (dx * dx + dy * dy < want * want) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The step a step grows from on the page: its first parent when that is in the same region. */
    @Nullable
    private static Quest layoutParent(Quest quest) {
        Quest parent = quest.parent();
        return parent != null && parent.region() == quest.region() ? parent : null;
    }

    private static Shape shape(Quest quest) {
        List<Quest> children = new ArrayList<>();
        for (Quest child : quest.children()) {
            if (layoutParent(child) == quest && child.kind() != Quest.Kind.WIP) {
                children.add(child);
            }
        }
        if (children.isEmpty()) {
            Map<Quest, double[]> positions = new LinkedHashMap<>();
            positions.put(quest, new double[]{0, 0});
            List<double[]> contour = new ArrayList<>();
            contour.add(new double[]{0, 0});
            return new Shape(positions, contour);
        }
        Shape below = arrange(children);
        double first = below.positions().get(children.getFirst())[1];
        double last = below.positions().get(children.getLast())[1];
        double middle = (first + last) / 2;
        Map<Quest, double[]> positions = new LinkedHashMap<>();
        positions.put(quest, new double[]{0, 0});
        double along = quest.landmark() ? ROOM : STEP;
        below.positions().forEach((step, at) -> positions.put(step, new double[]{at[0] + along, at[1] - middle}));
        List<double[]> contour = new ArrayList<>();
        contour.add(new double[]{0, 0});
        for (double[] level : below.contour()) {
            contour.add(new double[]{level[0] - middle, level[1] - middle});
        }
        return new Shape(positions, contour);
    }

    /**
     * Steps side by side across the region (their branches following them along it): each next branch is pushed across
     * until its outline keeps {@link #LANE} from the outlines before it at every depth they share.
     */
    private static Shape arrange(List<Quest> steps) {
        Map<Quest, double[]> positions = new LinkedHashMap<>();
        List<double[]> contour = new ArrayList<>();
        for (Quest step : steps) {
            Shape shape = shape(step);
            double offset = 0;
            if (!contour.isEmpty()) {
                offset = -Double.MAX_VALUE;
                for (int depth = 0; depth < Math.min(contour.size(), shape.contour().size()); depth++) {
                    offset = Math.max(offset, contour.get(depth)[1] - shape.contour().get(depth)[0] + LANE);
                }
            }
            for (var entry : shape.positions().entrySet()) {
                positions.put(entry.getKey(), new double[]{entry.getValue()[0], entry.getValue()[1] + offset});
            }
            for (int depth = 0; depth < shape.contour().size(); depth++) {
                double low = shape.contour().get(depth)[0] + offset;
                double high = shape.contour().get(depth)[1] + offset;
                if (depth < contour.size()) {
                    contour.get(depth)[0] = Math.min(contour.get(depth)[0], low);
                    contour.get(depth)[1] = Math.max(contour.get(depth)[1], high);
                } else {
                    contour.add(new double[]{low, high});
                }
            }
        }
        return new Shape(positions, contour);
    }

    /** A steady pseudo-random number in -1..1 for a step and a salt. */
    private static double jitter(Quest quest, int salt) {
        long h = quest.id().hashCode() * 0x9E3779B97F4A7C15L + salt * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 32;
        return ((h & 0xFFFF) / 32767.5) - 1;
    }

    // ---- Queries ------------------------------------------------------------------------------------------------------

    /** Every step, parents before children. */
    public static List<Quest> all() {
        return Collections.unmodifiableList(ALL);
    }

    @Nullable
    public static Quest byId(String id) {
        return BY_ID.get(id);
    }

    /** The first step that asks for this research topic. Every topic has one. */
    public static Quest of(ResearchTopic topic) {
        for (Quest quest : ALL) {
            if (quest.topic() == topic) {
                return quest;
            }
        }
        throw new IllegalStateException("Research topic " + topic.id() + " is on no guide step");
    }

    /** An item quest when its goal was met; WIP steps never. */
    public static boolean done(Quest quest, RuneKnowledge knowledge) {
        return quest.kind() == Quest.Kind.ITEM && knowledge.questDone(quest);
    }

    /** Every parent is done. */
    public static boolean open(Quest quest, RuneKnowledge knowledge) {
        for (Quest parent : quest.parents()) {
            if (!done(parent, knowledge)) {
                return false;
            }
        }
        return true;
    }

    /** What the book shows: done steps and open ones. The rest stays in the fog. */
    public static boolean visible(Quest quest, RuneKnowledge knowledge) {
        return done(quest, knowledge) || open(quest, knowledge);
    }

    /** Whether the researcher may research this topic at the table: a step asking for it is open (or done). */
    public static boolean canResearch(ResearchTopic topic, RuneKnowledge knowledge) {
        for (Quest quest : ALL) {
            if (quest.topic() == topic && visible(quest, knowledge)) {
                return true;
            }
        }
        return false;
    }

    /** Bounds of the laid-out tree (for the camera). */
    public static int left() {
        return ALL.stream().mapToInt(Quest::x).min().orElse(0);
    }

    public static int top() {
        return ALL.stream().mapToInt(Quest::y).min().orElse(0);
    }

    public static int right() {
        return ALL.stream().mapToInt(Quest::x).max().orElse(0);
    }

    public static int bottom() {
        return ALL.stream().mapToInt(Quest::y).max().orElse(0);
    }
}
