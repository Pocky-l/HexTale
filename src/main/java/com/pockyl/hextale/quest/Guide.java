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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    /** Devices still being worked out stand this far out from their step's rim, then further ring by ring. */
    private static final int DEVICE_RING = 36;
    private static final int DEVICE_RING_STEP = 8;
    /** No two steps closer than this, centre to centre. */
    private static final int CROWD = 34;
    /** How far a link bows out of the straight line at most, and as a share of its length. */
    private static final double BOW = 22;
    private static final double BOW_SHARE = 0.18;
    /** Points a link's curve is checked at. */
    public static final int LINK_SAMPLES = 16;

    private static final List<Quest> ALL = new ArrayList<>();
    private static final Map<String, Quest> BY_ID = new HashMap<>();
    /** The steps placed on the map by hand. */
    private static final Set<Quest> PLACED = new HashSet<>();
    /** Where the layout file (moved in the book's dev editor) puts steps, over the places written below. */
    public static final String LAYOUT_FILE = "hextale/guide_layout.json";
    private static final Map<String, int[]> MOVED = new java.util.TreeMap<>();

    // ---- The workshop: raw hextech and its dust, the woken workbench and furnace, brass, the table and the diary -------

    public static final Quest UNSTABLE_HEXTECH = item(Region.WORKSHOP, "unstable_hextech", ModItems.UNSTABLE_HEXTECH, Quest.Aura.STORM,
            QuestDisplay.scheme("hextale:unstable_hextech", "minecraft:lightning_rod", "minecraft:amethyst_block"));
    // A thrown orb shatters into dust; dust wakes the crafting table and the furnace.
    public static final Quest HEXTECH_DUST = item(Region.WORKSHOP, "hextech_dust", ModItems.HEXTECH_DUST, Quest.Aura.NONE,
            QuestDisplay.scheme("hextale:hextech_dust", "hextale:unstable_hextech"), UNSTABLE_HEXTECH);
    public static final Quest HEX_WORKBENCH = item(Region.WORKSHOP, "hex_workbench", ModItems.HEX_WORKBENCH, Quest.Aura.NONE,
            QuestDisplay.scheme("hextale:hex_workbench", "hextale:hextech_dust", "minecraft:crafting_table"), HEXTECH_DUST);
    public static final Quest HEX_FURNACE = item(Region.FORGE, "hex_furnace", ModItems.HEX_FURNACE, Quest.Aura.NONE,
            QuestDisplay.scheme("hextale:hex_furnace", "hextale:hextech_dust", "minecraft:furnace"), HEXTECH_DUST);
    // Chemtech begins with dust on a cauldron: the crucible boils water and dissolves essences and items in it.
    public static final Quest CRUCIBLE = item(Region.CHEMTECH, "crucible", ModItems.CRUCIBLE, Quest.Aura.NONE,
            QuestDisplay.scheme("hextale:crucible", "hextale:hextech_dust", "minecraft:cauldron"), HEXTECH_DUST);
    // Zinc is mined: sphalerite lies in stone and deepslate like copper, a little rarer.
    public static final Quest SPHALERITE = add(Region.WORKSHOP, "sphalerite", Quest.Kind.ITEM, List.of(),
            () -> new ItemStack(ModItems.SPHALERITE_ORE.get()), Quests.has(ModItems.RAW_ZINC), null, null, Quest.Aura.NONE);
    public static final Quest ZINC = item(Region.WORKSHOP, "zinc", ModItems.ZINC_INGOT, Quest.Aura.NONE,
            QuestDisplay.scheme("hextale:zinc_ingot", "hextale:raw_zinc", "minecraft:furnace"), SPHALERITE);
    // Brass is the first alloy of the Hex Furnace, free to melt.
    public static final Quest BRASS = item(Region.WORKSHOP, "brass", ModItems.BRASS_INGOT, Quest.Aura.NONE,
            QuestDisplay.alloy("hextale:brass_ingot"), HEX_FURNACE, ZINC);
    public static final Quest RESEARCH_TABLE = item(Region.WORKSHOP, "research_table", ModItems.RESEARCH_TABLE, Quest.Aura.NONE,
            QuestDisplay.hexCrafting("hextale:research_table"), HEX_WORKBENCH, BRASS);
    public static final Quest RESEARCHERS_DIARY = item(Region.WORKSHOP, "researchers_diary", ModItems.RESEARCHERS_DIARY, Quest.Aura.NONE,
            QuestDisplay.shapeless("hextale:researchers_diary", 1, "minecraft:book", "minecraft:amethyst_shard", "minecraft:feather"));
    public static final Quest ITEM_STUDY = add(Region.WORKSHOP, "item_study", Quest.Kind.ITEM, List.of(RESEARCH_TABLE, RESEARCHERS_DIARY),
            () -> new ItemStack(Items.PAPER), Quests.studied(1), QuestDisplay.essences(), null, Quest.Aura.NONE);
    public static final Quest RESEARCHERS_GOGGLES = item(Region.WORKSHOP, "researchers_goggles", ModItems.RESEARCHERS_GOGGLES, Quest.Aura.NONE,
            QuestDisplay.hexCrafting("hextale:researchers_goggles"), HEX_WORKBENCH, BRASS);

    // ---- The essence works: boiling, bottling, separating, storing ---------------------------------------------------

    public static final Quest ALEMBIC = item(Region.ESSENCES, "alembic", ModItems.ALEMBIC, Quest.Aura.NONE,
            QuestDisplay.hexCrafting("hextale:alembic"), HEX_WORKBENCH, BRASS);
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
            ResearchTopics.BIG_ESSENCE_JAR, Quest.Aura.NONE);

    // ---- Rune lore: scrolls found in the world, copied at the altar --------------------------------------------------

    public static final Quest RUNE_SCROLL = add(Region.RUNES, "rune_scroll", Quest.Kind.ITEM, List.of(RESEARCHERS_DIARY),
            () -> new ItemStack(ModItems.RUNE_SCROLL.get()), Quests.knowledge(knowledge -> knowledge.runeCount() > 0), null, null,
            Quest.Aura.GOLD);
    public static final Quest BLANK_SCROLL = item(Region.RUNES, "blank_scroll", ModItems.BLANK_SCROLL, Quest.Aura.NONE,
            QuestDisplay.shapeless("hextale:blank_scroll", 1, "minecraft:paper", "minecraft:paper", "minecraft:paper"), RUNE_SCROLL);

    // ---- The hextech circle: the altar, its pedestals and steles, refined hextech ------------------------------------

    public static final Quest HEXTECH_ALTAR = researched(Region.CIRCLE, "hextech_altar", ModItems.HEXTECH_ALTAR, ResearchTopics.CIRCLE,
            Quest.Aura.NONE, QuestDisplay.hexCrafting("hextale:hextech_altar"), INK, MAGIC_STONE, UNSTABLE_HEXTECH);
    public static final Quest PEDESTAL = researched(Region.CIRCLE, "pedestal", ModItems.PEDESTAL, ResearchTopics.CIRCLE, Quest.Aura.NONE,
            QuestDisplay.hexCrafting("hextale:pedestal"), HEXTECH_ALTAR);
    public static final Quest RUNE_STELE = researched(Region.CIRCLE, "rune_stele", ModItems.RUNE_STELE, ResearchTopics.CIRCLE, Quest.Aura.NONE,
            QuestDisplay.hexCrafting("hextale:rune_stele"), HEXTECH_ALTAR);
    // Crystal cutting: the lapidary from the workbench, its disc from the altar (a ritual draws essence from jars around it).
    public static final Quest HEX_LAPIDARY = researched(Region.CIRCLE, "hex_lapidary", ModItems.HEX_LAPIDARY, ResearchTopics.LAPIDARY,
            Quest.Aura.NONE, QuestDisplay.hexCrafting("hextale:hex_lapidary").then(QuestDisplay.infusion("hextale:cutting_disc")),
            PEDESTAL, ESSENCE_TANK);
    public static final Quest HEXTECH_CRYSTAL = item(Region.CIRCLE, "hextech_crystal", ModItems.HEXTECH_CRYSTAL, Quest.Aura.GOLD,
            QuestDisplay.scheme("hextale:hextech_crystal", "hextale:unstable_hextech", "hextale:cutting_disc"), HEX_LAPIDARY);
    public static final Quest HEXTECH_LAMP = researched(Region.CIRCLE, "hextech_lamp", ModItems.HEXTECH_LAMP, ResearchTopics.HEXTECH_LAMP,
            Quest.Aura.NONE, QuestDisplay.hexCrafting("hextale:hextech_lamp"), HEXTECH_CRYSTAL);

    // Copying a rune: a blank scroll on the altar, the rune on a stele (HextechAltarBlockEntity completes it).
    public static final Quest WRITTEN_SCROLL = add(Region.RUNES, "written_scroll", Quest.Kind.ITEM, List.of(BLANK_SCROLL, RUNE_STELE),
            () -> new ItemStack(ModItems.RUNE_SCROLL.get()), player -> false,
            QuestDisplay.scheme("hextale:rune_scroll", "hextale:blank_scroll", "hextale:rune_stele"), null, Quest.Aura.NONE);

    // ---- Hex pylons: items carried by hextech, gathered off the ground, thrown to links, passed on by relays ----------

    public static final Quest GATHERING_PYLON = researched(Region.PYLONS, "gathering_pylon", ModItems.GATHERING_PYLON,
            ResearchTopics.GATHERING_PYLON, Quest.Aura.NONE, QuestDisplay.infusion("hextale:gathering_pylon"), HEXTECH_CRYSTAL);
    public static final Quest SENDING_PYLON = researched(Region.PYLONS, "sending_pylon", ModItems.SENDING_PYLON, ResearchTopics.SENDING_PYLON,
            Quest.Aura.NONE, QuestDisplay.infusion("hextale:sending_pylon"), GATHERING_PYLON);
    public static final Quest RELAY_PYLON = researched(Region.PYLONS, "relay_pylon", ModItems.RELAY_PYLON, ResearchTopics.RELAY_PYLON,
            Quest.Aura.NONE, QuestDisplay.infusion("hextale:relay_pylon"), SENDING_PYLON);
    // The sigils that steer the pylons are written at the altar.
    public static final Quest PYLON_FILTER = item(Region.PYLONS, "pylon_filter", ModItems.PYLON_FILTER, Quest.Aura.NONE,
            QuestDisplay.infusion("hextale:pylon_filter"), GATHERING_PYLON);
    public static final Quest SIPHON_PYLON = researched(Region.PYLONS, "siphon_pylon", ModItems.SIPHON_PYLON, ResearchTopics.SIPHON_PYLON,
            Quest.Aura.NONE, QuestDisplay.infusion("hextale:siphon_pylon"), SENDING_PYLON, ESSENCE_TANK, BRASS_PIPE);
    public static final Quest ESSENCE_STICKER = item(Region.PYLONS, "essence_sticker", ModItems.ESSENCE_STICKER, Quest.Aura.NONE,
            QuestDisplay.infusion("hextale:essence_sticker"), SIPHON_PYLON);
    public static final Quest PYLON_RUNE = add(Region.PYLONS, "pylon_rune", Quest.Kind.ITEM, List.of(GATHERING_PYLON, RUNE_STELE, RUNE_SCROLL),
            () -> new ItemStack(ModItems.RUNE_STELE.get()), Quests.pylonRunes(1), QuestDisplay.pylonRunes(), null, Quest.Aura.NONE);

    // ---- The forge: alloys, the master disc and stabilised gemstones, hextech pickaxes ----------------------------------

    public static final Quest HEXSTEEL = researched(Region.FORGE, "hexsteel", ModItems.HEXSTEEL_INGOT, ResearchTopics.HEXSTEEL,
            Quest.Aura.NONE, QuestDisplay.alloy("hextale:hexsteel_ingot"), HEX_FURNACE, HEXTECH_DUST, INK);
    public static final Quest MASTER_CUTTING_DISC = researched(Region.FORGE, "master_cutting_disc", ModItems.MASTER_CUTTING_DISC,
            ResearchTopics.MASTER_DISC, Quest.Aura.NONE, QuestDisplay.infusion("hextale:master_cutting_disc"), HEXTECH_CRYSTAL, HEXSTEEL);
    public static final Quest HEXTECH_GEMSTONE = item(Region.FORGE, "hextech_gemstone", ModItems.HEXTECH_GEMSTONE, Quest.Aura.GOLD,
            QuestDisplay.scheme("hextale:hextech_gemstone", "hextale:hextech_crystal", "hextale:master_cutting_disc"),
            MASTER_CUTTING_DISC, HEXTECH_CRYSTAL);
    public static final Quest HEXTECH_PICKAXE = add(Region.FORGE, "hextech_pickaxe", Quest.Kind.ITEM, List.of(HEXTECH_CRYSTAL),
            () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.HEXTECH).get()), Quests.pickaxe(1),
            QuestDisplay.infusion("hextale:hextech_pickaxe"), ResearchTopics.HEXTECH_PICKAXE, Quest.Aura.NONE);
    public static final Quest PICKAXE_RUNE = add(Region.FORGE, "pickaxe_rune", Quest.Kind.ITEM, List.of(HEXTECH_PICKAXE, RUNE_STELE, RUNE_SCROLL),
            () -> new ItemStack(ModItems.RUNE_SCROLL.get()), Quests.pickaxeRunes(1), QuestDisplay.pickaxeRunes(), null, Quest.Aura.NONE);
    public static final Quest REFINED_PICKAXE = add(Region.FORGE, "refined_pickaxe", Quest.Kind.ITEM, List.of(HEXTECH_PICKAXE, HEXTECH_GEMSTONE),
            () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.REFINED).get()), Quests.pickaxe(2),
            QuestDisplay.infusion("hextale:refined_hextech_pickaxe"), ResearchTopics.REFINED_PICKAXE, Quest.Aura.NONE);
    public static final Quest RADIANT_PICKAXE = add(Region.FORGE, "radiant_pickaxe", Quest.Kind.ITEM, List.of(REFINED_PICKAXE),
            () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.RADIANT).get()), Quests.pickaxe(3),
            QuestDisplay.infusion("hextale:radiant_hextech_pickaxe"), ResearchTopics.RADIANT_PICKAXE, Quest.Aura.HEXTECH);

    // ---- Devices still being worked out, each hanging off the step it will grow from ---------------------------------

    static {
        // The workshop.
        wip("jar_gauge", Items.COMPARATOR, RESEARCHERS_GOGGLES);
        wip("respirator", Items.LEATHER_HELMET, RESEARCHERS_GOGGLES, CONCENTRATE);
        wip("storm_collector", Items.LIGHTNING_ROD.asList().getFirst(), UNSTABLE_HEXTECH);
        wip("rain_caller", Items.TRIDENT, UNSTABLE_HEXTECH);
        wip("xp_condenser", Items.SCULK_CATALYST, ITEM_STUDY);
        wip("hex_archive", Items.CHISELED_BOOKSHELF, INK);
        wip("auto_workbench", Items.DROPPER, HEX_WORKBENCH);
        wip("dust_charge", Items.GLOWSTONE_DUST, HEXTECH_DUST);
        wip("field_notes", Items.WRITABLE_BOOK, RESEARCHERS_DIARY);
        wip("star_chart", Items.MAP, RESEARCH_TABLE);
        wip("clockwork", Items.LEVER, BRASS);
        // Chemtech: what the crucible will grow into. Green and lush near the crucible, smog and rust further out.
        Quest brews = wip("crucible_brews", Items.POTION, CRUCIBLE);
        Quest vials = wip("chem_vial", Items.DRAGON_BREATH, brews);
        wip("gas_grenade", Items.LINGERING_POTION, vials);
        wip("mutation_injector", Items.SPIDER_EYE, vials, CONCENTRATE);
        Quest salts = wip("catalyst_salts", Items.SUGAR, brews);
        wip("synthetic_crystal", Items.AMETHYST_SHARD, salts, HEXTECH_CRYSTAL);
        wip("chem_still", Items.DECORATED_POT, brews);
        Quest burner = wip("chem_burner", Items.CAMPFIRE, CRUCIBLE, SEPARATOR);
        Quest boiler = wip("chem_boiler", Items.BLAST_FURNACE, burner);
        wip("chem_press", Items.STICKY_PISTON, boiler);
        wip("chem_drill", Items.NETHERITE_PICKAXE, boiler);
        wip("chem_tank_suit", Items.IRON_CHESTPLATE, boiler);
        wip("chem_lumen", Items.LANTERN, burner);
        Quest bloom = wip("cave_bloom", Items.GLOW_LICHEN, CRUCIBLE);
        Quest shimmer = wip("shimmer", Items.EXPERIENCE_BOTTLE, bloom, CONCENTRATE);
        Quest refined = wip("refined_shimmer", Items.OMINOUS_BOTTLE, shimmer);
        wip("apex_shimmer", Items.TOTEM_OF_UNDYING, refined);
        wip("shimmer_engine", Items.SCULK_SHRIEKER, refined);
        wip("shimmer_arm", Items.CHAINMAIL_CHESTPLATE, refined);
        wip("pink_brazier", Items.SOUL_CAMPFIRE, shimmer);
        Quest bench = wip("lab_bench", Items.BREWING_STAND, CRUCIBLE, SEPARATOR);
        wip("experiment_pot", Items.FLOWER_POT, bench, CONCENTRATE);
        wip("growth_serum", Items.BONE_MEAL, bench);
        Quest cage = wip("specimen_cage", Items.IRON_BARS, bench);
        Quest tissue = wip("mutant_reagents", Items.ROTTEN_FLESH, cage);
        wip("mutated_beasts", Items.GOAT_HORN, tissue);
        Quest hood = wip("fume_hood", Items.IRON_TRAPDOOR, CRUCIBLE);
        Quest gray = wip("the_gray", Items.GUNPOWDER, hood, DRAIN_VALVE);
        wip("acid_bath", Items.FERMENTED_SPIDER_EYE, gray, DRAIN_VALVE);
        wip("smog_scrubber", Items.WET_SPONGE, gray);
        wip("rust_blight", Items.IRON_NUGGET, gray);
        // The essence works, the fields and the woods.
        wip("sawmill", Items.OAK_PLANKS, ALEMBIC);
        wip("sap_tap", Items.HONEY_BOTTLE, ESSENCE_BOTTLE);
        wip("essence_sprinkler", Items.POINTED_DRIPSTONE, BRASS_PIPE, CONCENTRATE);
        wip("bloom_censer", Items.SPORE_BLOSSOM, BRASS_PIPE);
        wip("quench_tank", Items.WATER_BUCKET, PUMP);
        wip("steam_boiler", Items.CAULDRON, PUMP);
        wip("fish_weir", Items.FISHING_ROD, PUMP);
        wip("rune_pump", Items.HOPPER, PUMP);
        wip("seed_sower", Items.WHEAT_SEEDS, ESSENCE_TANK);
        wip("reaper", Items.WHEAT, ESSENCE_TANK);
        wip("feeding_trough", Items.HAY_BLOCK, ESSENCE_TANK);
        wip("herd_warden", Items.SHEARS, ESSENCE_TANK);
        wip("living_soil", Items.ROOTED_DIRT, MAGIC_STONE);
        wip("field_ward", Items.CARVED_PUMPKIN, MAGIC_STONE);
        wip("tree_feller", Items.OAK_LOG, MAGIC_STONE, ESSENCE_TANK);
        wip("greenhouse", Items.FLOWERING_AZALEA, MAGIC_STONE);
        wip("healing_spring", Items.GLISTERING_MELON_SLICE, CONCENTRATE);
        wip("spring_basin", Items.LILY_PAD, CONCENTRATE);
        wip("compost_vat", Items.COMPOSTER, DRAIN_VALVE);
        wip("cooler", Items.BLUE_ICE, DRAIN_VALVE);
        wip("crystal_vat", Items.BUDDING_AMETHYST, BIG_ESSENCE_JAR, HEXTECH_GEMSTONE);
        wip("regen_tank", Items.AXOLOTL_BUCKET, BIG_ESSENCE_JAR);
        wip("grove_keeper", Items.OAK_SAPLING, BIG_ESSENCE_JAR);
        // Rune lore.
        wip("rune_engraver", Items.BRUSH, WRITTEN_SCROLL);
        wip("lull_lantern", Items.SOUL_LANTERN, WRITTEN_SCROLL);
        wip("veil", Items.PHANTOM_MEMBRANE, RUNE_SCROLL);
        // The circle.
        wip("geode_resonator", Items.SMALL_AMETHYST_BUD, HEXTECH_ALTAR, ESSENCE_TANK);
        wip("nullstone", Items.CRYING_OBSIDIAN, HEXTECH_ALTAR);
        wip("hexcore", Items.HEART_OF_THE_SEA, HEXTECH_ALTAR, HEXTECH_GEMSTONE);
        wip("recall_device", Items.RECOVERY_COMPASS, HEXTECH_ALTAR);
        wip("gem_sockets", Items.EMERALD, HEX_LAPIDARY);
        wip("hex_bow", Items.CROSSBOW, HEXTECH_CRYSTAL);
        wip("mining_gauntlet", Items.MACE, HEXTECH_GEMSTONE);
        wip("rune_workbench", Items.CRAFTER, PEDESTAL, HEXTECH_CRYSTAL);
        wip("barrier_ward", Items.SHIELD, PEDESTAL);
        wip("rune_lock", Items.TRIAL_KEY, RUNE_STELE);
        wip("crystal_battery", Items.AMETHYST_CLUSTER, HEXTECH_CRYSTAL);
        wip("world_anchor", Items.RESPAWN_ANCHOR, HEXTECH_CRYSTAL, HEXTECH_GEMSTONE);
        wip("construct", Items.ARMOR_STAND, HEXTECH_CRYSTAL);
        wip("star_rod", Items.END_ROD, HEXTECH_LAMP);
        wip("signal_beacon", Items.BEACON, HEXTECH_LAMP);
        wip("mirage_projector", Items.TINTED_GLASS, HEXTECH_LAMP);
        wip("beam_relay", Items.SPYGLASS, HEXTECH_LAMP);
        wip("glowbloom_bed", Items.GLOW_BERRIES, HEXTECH_LAMP);
        // The pylons.
        wip("harvest_construct", Items.GOLDEN_HOE, GATHERING_PYLON);
        wip("forester_construct", Items.GOLDEN_AXE, GATHERING_PYLON);
        wip("sorting_matrix", Items.OBSERVER, SENDING_PYLON, PYLON_FILTER);
        wip("winged_courier", Items.FEATHER, SENDING_PYLON);
        wip("hex_gate", Items.ENDER_EYE, RELAY_PYLON);
        wip("folding_gate", Items.ENDER_PEARL, RELAY_PYLON);
        wip("levitation_plate", Items.HEAVY_WEIGHTED_PRESSURE_PLATE, RELAY_PYLON);
        wip("hex_turret", Items.DISPENSER, RELAY_PYLON);
        wip("essence_resonator", Items.GLASS_BOTTLE, SIPHON_PYLON);
        wip("purifier", Items.CONDUIT, SIPHON_PYLON);
        wip("hex_lift", Items.PISTON, PYLON_RUNE);
        wip("wind_turbine", Items.BREEZE_ROD, PYLON_RUNE);
        // The forge.
        wip("hextech_axe", Items.IRON_AXE, HEXTECH_PICKAXE);
        wip("hextech_hoe", Items.IRON_HOE, HEXTECH_PICKAXE);
        wip("hex_bore", Items.DIAMOND_PICKAXE, HEXTECH_PICKAXE);
        wip("drainer", Items.SPONGE, PICKAXE_RUNE);
        wip("shadow_coffer", Items.ENDER_CHEST, PICKAXE_RUNE);
        wip("hex_blade", Items.IRON_SWORD, REFINED_PICKAXE);
        wip("ore_washer", Items.RAW_IRON, REFINED_PICKAXE, PUMP);
        wip("hex_cutter", Items.STONECUTTER, RADIANT_PICKAXE);
        wip("flash_charge", Items.FIREWORK_STAR, RADIANT_PICKAXE);
        wip("chrono_chamber", Items.CLOCK, RADIANT_PICKAXE);
        wip("hex_flamethrower", Items.FIRE_CHARGE, HEXSTEEL);
        wip("mining_charge", Items.TNT, HEX_FURNACE);
        wip("magma_font", Items.LAVA_BUCKET, HEX_FURNACE);
        wip("hex_bronze", Items.COPPER_INGOT, HEX_FURNACE);
        wip("magic_metal", Items.NETHERITE_SCRAP, HEXSTEEL, HEXTECH_GEMSTONE);
        wip("rune_anvil", Items.ANVIL, HEXSTEEL);
        // Workstations: the steps that grow from them stand around them. The altar is the heart of the guide.
        for (Quest station : List.of(HEX_WORKBENCH, HEX_FURNACE, CRUCIBLE, RESEARCH_TABLE, ALEMBIC, HEXTECH_ALTAR, HEX_LAPIDARY)) {
            station.station = true;
        }
        HEXTECH_ALTAR.landmark = true;
        // The map, placed by hand so it reads like a story: what a workstation makes stands around it.
        // The start: raw hextech and its dust in the west, the workbench in the middle of the workshop, the furnace south-west
        // with the metals around it, the crucible north-west with room for chemtech to grow.
        at(UNSTABLE_HEXTECH, -300, 0);
        at(HEXTECH_DUST, -220, 0);
        at(HEX_WORKBENCH, -100, -20);
        at(HEX_FURNACE, -220, 130);
        at(CRUCIBLE, -400, -110);
        at(SPHALERITE, -250, 250);
        at(ZINC, -160, 260);
        at(BRASS, -120, 150);
        at(HEXSTEEL, -320, 180);
        at(RESEARCHERS_GOGGLES, -170, -110);
        // North: the research table with the diary; the rune lore climbs north-west from the diary, the study runs east to
        // the ink.
        at(RESEARCH_TABLE, 0, -130);
        at(RESEARCHERS_DIARY, -30, -240);
        at(ITEM_STUDY, 110, -160);
        at(INK, 200, -130);
        at(RUNE_SCROLL, -120, -270);
        at(BLANK_SCROLL, -200, -310);
        at(WRITTEN_SCROLL, -280, -290);
        // South: the essence works fanning out of the alembic.
        at(ALEMBIC, 10, 90);
        at(ESSENCE_BOTTLE, 100, 140);
        at(BRASS_PIPE, -60, 190);
        at(PUMP, -40, 280);
        at(SEPARATOR, 200, 110);
        at(ESSENCE_TANK, 160, 230);
        at(MAGIC_STONE, 60, 250);
        at(CONCENTRATE, 290, 60);
        at(DRAIN_VALVE, 300, 170);
        at(BIG_ESSENCE_JAR, 240, 320);
        // East: the altar alone in its patch of sky, its pedestal and stele, then the lapidary and the crystal it cuts; the pylons
        // grow north-east of the crystal, the pickaxes and the gemstone south-east.
        at(HEXTECH_ALTAR, 350, -130);
        at(PEDESTAL, 470, -90);
        at(RUNE_STELE, 350, -260);
        at(HEX_LAPIDARY, 570, -60);
        at(HEXTECH_CRYSTAL, 670, -60);
        at(HEXTECH_LAMP, 700, -170);
        at(GATHERING_PYLON, 770, -80);
        at(SENDING_PYLON, 860, -130);
        at(RELAY_PYLON, 950, -170);
        at(PYLON_FILTER, 800, -180);
        at(SIPHON_PYLON, 880, -30);
        at(ESSENCE_STICKER, 970, 0);
        at(PYLON_RUNE, 800, 20);
        at(MASTER_CUTTING_DISC, 600, 50);
        at(HEXTECH_GEMSTONE, 570, 140);
        at(HEXTECH_PICKAXE, 710, 80);
        at(PICKAXE_RUNE, 660, 170);
        at(REFINED_PICKAXE, 800, 120);
        at(RADIANT_PICKAXE, 880, 170);
        // West: chemtech, the crucible's devices to come. Its branches are placed by hand, their leaves gather around them.
        at(brews, -520, -180);
        at(vials, -610, -260);
        at(salts, -640, -160);
        at(burner, -540, -50);
        at(boiler, -650, -30);
        at(bloom, -500, 50);
        at(shimmer, -580, 130);
        at(refined, -690, 150);
        at(bench, -410, 100);
        at(cage, -460, 200);
        at(tissue, -560, 250);
        at(hood, -440, -240);
        at(gray, -520, -330);
        HEXTECH_ALTAR.scene = Quest.Scene.ALTAR;
        PEDESTAL.scene = Quest.Scene.PEDESTAL;
        RUNE_STELE.scene = Quest.Scene.STELE;
        readLayoutFile();
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

    /**
     * A device still being worked out: it sits in its first parent's region, on that parent's branch. Devices may grow from
     * other devices still being worked out, so the guide can show where a theme is heading.
     */
    private static Quest wip(String id, ItemLike icon, Quest... parents) {
        return add(parents[0].region(), "wip_" + id, Quest.Kind.WIP, List.of(parents), () -> new ItemStack(icon), player -> false, null, null,
                Quest.Aura.NONE);
    }

    // ---- Layout -------------------------------------------------------------------------------------------------------

    /**
     * Puts a step on the map. Every item step is placed by hand, so the map reads the way it is meant to; devices still
     * being worked out are placed by hand where they start a branch of their own, the rest find a place by themselves.
     */
    /** Applies the layout file on the classpath: steps moved in the book's dev editor, by id. */
    private static void readLayoutFile() {
        try (var in = Guide.class.getResourceAsStream("/" + LAYOUT_FILE)) {
            if (in == null) {
                return;
            }
            var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (var entry : json.entrySet()) {
                Quest quest = BY_ID.get(entry.getKey());
                if (quest != null) {
                    var at = entry.getValue().getAsJsonArray();
                    moveTo(quest, at.get(0).getAsInt(), at.get(1).getAsInt());
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            com.pockyl.hextale.Hextale.LOGGER.warn("Could not read the guide layout file", e);
        }
    }

    private static void moveTo(Quest quest, int x, int y) {
        at(quest, x, y);
        MOVED.put(quest.id(), new int[]{x, y});
    }

    /** Moves a step while it is dragged in the dev editor: only the step itself, the rest is laid out on {@link #move}. */
    public static void drag(Quest quest, int x, int y) {
        quest.x = x;
        quest.y = y;
    }

    /** Places a step by hand (the book's dev editor) and lays the map out again around it. */
    public static void move(Quest quest, int x, int y) {
        moveTo(quest, x, y);
        layout();
    }

    /** The steps moved in the dev editor and their places, by id: what the layout file holds. */
    public static Map<String, int[]> moved() {
        return Collections.unmodifiableMap(MOVED);
    }

    /** How many pairs of links cross now (a hand-placed map can make them; the game tests then fail). */
    public static int crossings() {
        List<Quest> linked = ALL.stream().filter(quest -> quest.parent() != null).toList();
        Map<Quest, double[][]> curves = new HashMap<>();
        for (Quest quest : linked) {
            curves.put(quest, curve(quest.parent(), quest, quest.bow));
        }
        int count = 0;
        for (int i = 0; i < linked.size(); i++) {
            for (int j = i + 1; j < linked.size(); j++) {
                Quest a = linked.get(i);
                Quest b = linked.get(j);
                Quest shared = a.parent() == b.parent() ? a.parent() : a.parent() == b ? b : b.parent() == a ? a : null;
                if (curvesCross(curves.get(a), curves.get(b), shared)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void at(Quest quest, int x, int y) {
        quest.x = x;
        quest.y = y;
        PLACED.add(quest);
    }

    /** The room a step's own medallion needs around its centre. */
    private static double body(Quest quest) {
        return quest.landmark() ? 46 : quest.station() ? 32 : quest.kind() == Quest.Kind.WIP ? 17 : quest.important() ? 23 : 20;
    }

    /**
     * Places the devices still being worked out around the steps they hang from (the item steps are placed by hand): each
     * takes the first free spot on rings around its step, trying the direction away from where the step grows from first
     * and then turning left and right. A spot is free when it keeps clear of every step, its link crosses no other link
     * and runs over no step, and no link runs over it. Then every link bows into a gentle arc, to the side and as far as
     * it can without touching another link or a step. Deterministic, so every reader sees the same sky.
     */
    private static void layout() {
        List<Quest[]> links = new ArrayList<>();
        for (Quest quest : ALL) {
            if (quest.kind() == Quest.Kind.ITEM) {
                if (!PLACED.contains(quest)) {
                    throw new IllegalStateException("Guide step " + quest.id() + " has no place on the map");
                }
                if (quest.parent() != null) {
                    links.add(new Quest[]{quest.parent(), quest});
                }
            }
        }
        List<Quest> placed = new ArrayList<>();
        for (Quest quest : ALL) {
            if (quest.kind() == Quest.Kind.ITEM) {
                placed.add(quest);
            }
        }
        for (Quest quest : ALL) {
            if (quest.kind() != Quest.Kind.WIP) {
                continue;
            }
            Quest parent = quest.parent();
            if (PLACED.contains(quest)) {
                placed.add(quest);
                links.add(new Quest[]{parent, quest});
                continue;
            }
            Quest grand = parent.parent();
            double out = grand != null ? Math.atan2(parent.y - grand.y, parent.x - grand.x) : -Math.PI / 2;
            boolean found = false;
            for (int ring = 0; ring < 12 && !found; ring++) {
                double radius = body(parent) + DEVICE_RING + ring * DEVICE_RING_STEP;
                for (int turn = 0; turn < 25 && !found; turn++) {
                    double angle = out + (turn % 2 == 0 ? 1 : -1) * ((turn + 1) / 2) * Math.toRadians(15);
                    quest.x = (int) Math.round(parent.x + Math.cos(angle) * radius);
                    quest.y = (int) Math.round(parent.y + Math.sin(angle) * radius);
                    found = free(quest, parent, placed, links);
                }
            }
            if (!found) {
                // Never expected: QuestGameTests.guideIsLaidOut fails on the crowding.
                quest.x = parent.x;
                quest.y = parent.y + 300;
            }
            placed.add(quest);
            links.add(new Quest[]{parent, quest});
        }
        bowLinks(placed, links);
    }

    /**
     * Bows every link, in order, as far as it can: first to the side its pair prefers (so neighbouring links bow
     * differently), then to the other side, then less and less. A bow is kept only when the curve keeps clear of every
     * step and crosses no other link as it is now; a straight link is always clear, so the map never gets a crossing.
     */
    private static void bowLinks(List<Quest> placed, List<Quest[]> links) {
        Map<Quest, double[][]> curves = new HashMap<>();
        for (Quest[] link : links) {
            link[1].bow = 0;
            curves.put(link[1], curve(link[0], link[1], 0));
        }
        for (Quest[] link : links) {
            Quest from = link[0];
            Quest to = link[1];
            double most = Math.min(BOW, Math.hypot(to.x - from.x, to.y - from.y) * BOW_SHARE);
            double side = (from.index + to.index) % 2 == 0 ? 1 : -1;
            for (double bow : new double[]{side * most, -side * most, side * most * 0.6, -side * most * 0.6, side * most * 0.3,
                    -side * most * 0.3}) {
                double[][] curve = curve(from, to, bow);
                if (clearCurve(from, to, curve, placed, links, curves)) {
                    to.bow = (float) bow;
                    curves.put(to, curve);
                    break;
                }
            }
        }
    }

    private static boolean clearCurve(Quest from, Quest to, double[][] curve, List<Quest> placed, List<Quest[]> links,
                                      Map<Quest, double[][]> curves) {
        for (Quest step : placed) {
            if (step != from && step != to && distanceToCurve(step.x, step.y, curve) < Math.max(14, body(step) - 6)) {
                return false;
            }
        }
        for (Quest[] link : links) {
            if (link[1] == to) {
                continue;
            }
            double[][] other = curves.get(link[1]);
            // Links meeting at a step are compared only outside that step's medallion.
            Quest shared = link[0] == from || link[0] == to ? link[0] : link[1] == from ? link[1] : null;
            if (curvesCross(curve, other, shared)) {
                return false;
            }
        }
        return true;
    }

    /** A link's curve sampled from its first parent's centre to its step's centre. */
    private static double[][] curve(Quest from, Quest to, double bow) {
        double[][] points = new double[LINK_SAMPLES + 1][];
        for (int i = 0; i <= LINK_SAMPLES; i++) {
            points[i] = linkPoint(from, to, bow, i / (double) LINK_SAMPLES);
        }
        return points;
    }

    /**
     * A point of the link from {@code from} to {@code to} at {@code t} (0..1): a quadratic curve whose middle bows
     * {@code bow} map pixels to the side (positive = to the right of the way from {@code from}).
     */
    public static double[] linkPoint(Quest from, Quest to, double bow, double t) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double length = Math.max(1, Math.hypot(dx, dy));
        double cx = (from.x + to.x) / 2.0 - dy / length * bow;
        double cy = (from.y + to.y) / 2.0 + dx / length * bow;
        double u = 1 - t;
        return new double[]{u * u * from.x + 2 * u * t * cx + t * t * to.x, u * u * from.y + 2 * u * t * cy + t * t * to.y};
    }

    private static boolean curvesCross(double[][] a, double[][] b, @Nullable Quest shared) {
        double room = shared == null ? 0 : body(shared) + 4;
        for (int i = 0; i + 1 < a.length; i++) {
            if (near(a[i], shared, room) || near(a[i + 1], shared, room)) {
                continue;
            }
            for (int j = 0; j + 1 < b.length; j++) {
                if (near(b[j], shared, room) || near(b[j + 1], shared, room)) {
                    continue;
                }
                if (segmentsCross(a[i], a[i + 1], b[j], b[j + 1])) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean near(double[] point, @Nullable Quest step, double room) {
        return step != null && Math.hypot(point[0] - step.x, point[1] - step.y) <= room;
    }

    /** Whether two segments cross (touching ends do not count). */
    public static boolean segmentsCross(double[] a0, double[] a1, double[] b0, double[] b1) {
        return turn(b0, b1, a0) * turn(b0, b1, a1) < 0 && turn(a0, a1, b0) * turn(a0, a1, b1) < 0;
    }

    private static int turn(double[] from, double[] to, double[] point) {
        return (int) Math.signum((to[0] - from[0]) * (point[1] - from[1]) - (to[1] - from[1]) * (point[0] - from[0]));
    }

    /** The distance from a point to a sampled curve. */
    public static double distanceToCurve(double x, double y, double[][] curve) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i + 1 < curve.length; i++) {
            double dx = curve[i + 1][0] - curve[i][0];
            double dy = curve[i + 1][1] - curve[i][1];
            double t = Math.clamp(((x - curve[i][0]) * dx + (y - curve[i][1]) * dy) / Math.max(1e-9, dx * dx + dy * dy), 0, 1);
            best = Math.min(best, Math.hypot(curve[i][0] + dx * t - x, curve[i][1] + dy * t - y));
        }
        return best;
    }

    private static boolean free(Quest quest, Quest parent, List<Quest> placed, List<Quest[]> links) {
        for (Quest other : placed) {
            if (Math.hypot(quest.x - other.x, quest.y - other.y) < Math.max(CROWD, body(quest) + body(other))) {
                return false;
            }
            if (other != parent && distanceToSegment(other, parent, quest) < body(other) - 2) {
                return false;
            }
        }
        for (Quest[] link : links) {
            if (link[0] != parent && link[1] != parent && crosses(parent, quest, link[0], link[1])) {
                return false;
            }
            if (distanceToSegment(quest, link[0], link[1]) < body(quest)) {
                return false;
            }
        }
        return true;
    }

    private static boolean crosses(Quest a0, Quest a1, Quest b0, Quest b1) {
        return side(b0, b1, a0) * side(b0, b1, a1) < 0 && side(a0, a1, b0) * side(a0, a1, b1) < 0;
    }

    private static int side(Quest from, Quest to, Quest point) {
        return Long.signum((long) (to.x - from.x) * (point.y - from.y) - (long) (to.y - from.y) * (point.x - from.x));
    }

    private static double distanceToSegment(Quest point, Quest from, Quest to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double t = Math.clamp(((point.x - from.x) * dx + (point.y - from.y) * dy) / Math.max(1e-9, dx * dx + dy * dy), 0, 1);
        return Math.hypot(from.x + dx * t - point.x, from.y + dy * t - point.y);
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

    /** Every parent is done (a device still being worked out that another one grows from only has to be seen). */
    public static boolean open(Quest quest, RuneKnowledge knowledge) {
        for (Quest parent : quest.parents()) {
            if (parent.kind() == Quest.Kind.WIP ? !visible(parent, knowledge) : !done(parent, knowledge)) {
                return false;
            }
        }
        return true;
    }

    /** What the book shows: done steps and open ones. The rest stays in the fog. */
    public static boolean visible(Quest quest, RuneKnowledge knowledge) {
        return done(quest, knowledge) || open(quest, knowledge);
    }

    /**
     * One step beyond what the reader can do now: an item step that is not open yet, but every step it waits for can be
     * seen. The book shows it greyed out, as "not reachable yet"; everything further stays in the fog.
     */
    public static boolean ahead(Quest quest, RuneKnowledge knowledge) {
        if (quest.kind() != Quest.Kind.ITEM || visible(quest, knowledge)) {
            return false;
        }
        for (Quest parent : quest.parents()) {
            if (!visible(parent, knowledge)) {
                return false;
            }
        }
        return true;
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
