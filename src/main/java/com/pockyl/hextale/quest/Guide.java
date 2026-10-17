package com.pockyl.hextale.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechPickaxe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * The researcher's guide: everything the mod asks of a researcher, as one branching path from the first brass ingot to
 * the Hexcore. Item quests, the runes to study and the devices still being worked out (WIP) share the path; a step opens
 * when all its parents are done, and a rune can be studied at the table only once its step is open. The order is the
 * order things can really be made in: {@code GuideGameTests.guideOrderIsLogical} checks that every mod item a step's
 * recipe needs comes from a step before it.
 */
public final class Guide {
    /** Map pixels between columns (depth) and rows (leaves) of the laid-out path. */
    public static final int COLUMN = 56;
    public static final int ROW = 34;

    private static final List<Quest> ALL = new ArrayList<>();
    private static final Map<String, Quest> BY_ID = new HashMap<>();
    private static final Map<ComplexRune, Quest> BY_RUNE = new HashMap<>();

    // ---- The spine: brass, the table, the diary, the essence works --------------------------------------------------

    public static final Quest BRASS = item("brass", ModItems.BRASS_INGOT, Quest.Aura.NONE,
            QuestDisplay.shapeless("hextale:brass_ingot", 2, "minecraft:copper_ingot", "minecraft:copper_ingot",
                    "minecraft:copper_ingot", "minecraft:gold_ingot"));
    public static final Quest RESEARCH_TABLE = item("research_table", ModItems.RESEARCH_TABLE, Quest.Aura.GOLD,
            QuestDisplay.shaped("hextale:research_table", 1, new String[]{"BBB", "PSP", "P P"},
                    "B=hextale:brass_ingot", "P=#minecraft:planks", "S=minecraft:bookshelf"), BRASS);
    public static final Quest UNSTABLE_HEXTECH = item("unstable_hextech", ModItems.UNSTABLE_HEXTECH, Quest.Aura.STORM,
            QuestDisplay.scheme("hextale:unstable_hextech", "minecraft:lightning_rod", "minecraft:amethyst_block"), RESEARCH_TABLE);
    public static final Quest RESEARCHERS_DIARY = item("researchers_diary", ModItems.RESEARCHERS_DIARY, Quest.Aura.NONE,
            QuestDisplay.shapeless("hextale:researchers_diary", 1, "minecraft:book", "hextale:brass_ingot", "hextale:unstable_hextech"),
            UNSTABLE_HEXTECH);
    public static final Quest ITEM_STUDY = add("item_study", Quest.Kind.ITEM, List.of(RESEARCHERS_DIARY), () -> new ItemStack(Items.PAPER),
            Quests.studied(1), QuestDisplay.essences(), null, Quest.Aura.NONE);
    public static final Quest ALEMBIC = item("alembic", ModItems.ALEMBIC, Quest.Aura.GOLD,
            QuestDisplay.shaped("hextale:alembic", 1, new String[]{" B ", "G G", "BGB"}, "B=hextale:brass_ingot", "G=minecraft:glass"),
            ITEM_STUDY);
    public static final Quest ESSENCE_BOTTLE = add("essence_bottle", Quest.Kind.ITEM, List.of(ALEMBIC),
            () -> EssenceFlaskItem.filled(Quests.SAMPLE_MIX), Quests.has(ModItems.ESSENCE_FLASK),
            QuestDisplay.scheme("hextale:essence_flask", "minecraft:glass_bottle", "hextale:alembic"), null, Quest.Aura.NONE);
    public static final Quest BRASS_PIPE = item("brass_pipe", ModItems.BRASS_PIPE, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:brass_pipe", 8, new String[]{"BBB"}, "B=hextale:brass_ingot"), ALEMBIC);
    public static final Quest PUMP = item("pump", ModItems.PUMP, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:pump", 1, new String[]{" P ", "BXB", "BBB"},
                    "P=hextale:brass_pipe", "B=hextale:brass_ingot", "X=minecraft:piston"), BRASS_PIPE);
    public static final Quest SEPARATOR = item("separator", ModItems.SEPARATOR, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:separator", 1, new String[]{"BGB", "G G", "BBB"}, "B=hextale:brass_ingot", "G=minecraft:glass"),
            ESSENCE_BOTTLE);
    public static final Quest DRAIN_VALVE = item("drain_valve", ModItems.DRAIN_VALVE, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:drain_valve", 1, new String[]{" I ", "BPB"},
                    "I=minecraft:iron_ingot", "B=hextale:brass_ingot", "P=hextale:brass_pipe"), SEPARATOR, BRASS_PIPE);
    public static final Quest CONCENTRATE = add("concentrate", Quest.Kind.ITEM, List.of(SEPARATOR),
            () -> EssenceFlaskItem.filled(EssenceMix.pure(Essence.WATER, EssenceFlaskItem.CAPACITY)), Quests::concentrate,
            QuestDisplay.reagents(), null, Quest.Aura.NONE);
    public static final Quest MAGIC_STONE = item("magic_stone", ModItems.MAGIC_STONE, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:magic_stone", 8, new String[]{"SSS", "SBS", "SSS"},
                    "S=#hextale:soaks_essence", "B=hextale:essence_flask"), ESSENCE_BOTTLE);
    public static final Quest ESSENCE_TANK = item("essence_tank", ModItems.ESSENCE_TANK, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:essence_tank", 1, new String[]{"BSB", "G G", "GGG"},
                    "B=hextale:brass_ingot", "S=#minecraft:wooden_slabs", "G=minecraft:glass"), CONCENTRATE);
    public static final Quest RESEARCHERS_GOGGLES = item("researchers_goggles", ModItems.RESEARCHERS_GOGGLES, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:researchers_goggles", 1, new String[]{"LBL", "G G"},
                    "L=minecraft:leather", "B=hextale:brass_ingot", "G=minecraft:glass_pane"), ESSENCE_TANK);
    public static final Quest INK = add("ink", Quest.Kind.ITEM, List.of(ESSENCE_TANK), () -> new ItemStack(Items.INK_SAC),
            Quests::inkAtTable, null, null, Quest.Aura.NONE);

    // ---- Early devices still being worked out: farms, wood, logistics, the first chemtech -----------------------------

    public static final Quest WIP_SAP_TAP = wip("sap_tap", Items.HONEY_BOTTLE, Quest.Aura.LIFE, ESSENCE_BOTTLE);
    public static final Quest WIP_ESSENCE_SPRINKLER = wip("essence_sprinkler", Items.POINTED_DRIPSTONE, Quest.Aura.LIFE, BRASS_PIPE, CONCENTRATE);
    public static final Quest WIP_LIVING_SOIL = wip("living_soil", Items.ROOTED_DIRT, Quest.Aura.LIFE, MAGIC_STONE);
    public static final Quest WIP_FIELD_WARD = wip("field_ward", Items.CARVED_PUMPKIN, Quest.Aura.NONE, MAGIC_STONE);
    public static final Quest WIP_TREE_FELLER = wip("tree_feller", Items.OAK_LOG, Quest.Aura.NONE, MAGIC_STONE, ESSENCE_TANK);
    public static final Quest WIP_SEED_SOWER = wip("seed_sower", Items.WHEAT_SEEDS, Quest.Aura.NONE, ESSENCE_TANK);
    public static final Quest WIP_REAPER = wip("reaper", Items.WHEAT, Quest.Aura.NONE, ESSENCE_TANK);
    public static final Quest WIP_GROVE_KEEPER = wip("grove_keeper", Items.OAK_SAPLING, Quest.Aura.LIFE, ESSENCE_TANK);
    public static final Quest WIP_FEEDING_TROUGH = wip("feeding_trough", Items.HAY_BLOCK, Quest.Aura.LIFE, ESSENCE_TANK);
    public static final Quest WIP_GEODE_RESONATOR = wip("geode_resonator", Items.SMALL_AMETHYST_BUD, Quest.Aura.STORM, ESSENCE_TANK, UNSTABLE_HEXTECH);
    public static final Quest WIP_JAR_GAUGE = wip("jar_gauge", Items.COMPARATOR, Quest.Aura.NONE, RESEARCHERS_GOGGLES);
    public static final Quest WIP_SAWMILL = wip("sawmill", Items.OAK_PLANKS, Quest.Aura.NONE, ALEMBIC);
    public static final Quest WIP_FISH_WEIR = wip("fish_weir", Items.FISHING_ROD, Quest.Aura.NONE, PUMP);
    public static final Quest WIP_COMPOST_VAT = wip("compost_vat", Items.COMPOSTER, Quest.Aura.NONE, DRAIN_VALVE);
    public static final Quest WIP_THE_GRAY = wip("the_gray", Items.GUNPOWDER, Quest.Aura.CURSED, DRAIN_VALVE);
    public static final Quest WIP_CHEM_BURNER = wip("chem_burner", Items.CAMPFIRE, Quest.Aura.FLAME, SEPARATOR);
    public static final Quest WIP_LAB_BENCH = wip("lab_bench", Items.BREWING_STAND, Quest.Aura.NONE, SEPARATOR);
    public static final Quest WIP_EXPERIMENT_POT = wip("experiment_pot", Items.FLOWER_POT, Quest.Aura.LIFE, SEPARATOR, CONCENTRATE);
    public static final Quest WIP_SPECIMEN_CAGE = wip("specimen_cage", Items.IRON_BARS, Quest.Aura.CURSED, SEPARATOR);

    // ---- Smelting and the rune works it opens -------------------------------------------------------------------------

    public static final Quest SMELTING = rune("fire_earth", Quest.Aura.GOLD, INK);
    public static final Quest HEXTECH_ALTAR = item("hextech_altar", ModItems.HEXTECH_ALTAR, Quest.Aura.GOLD,
            QuestDisplay.shaped("hextale:hextech_altar", 1, new String[]{" H ", "IAI", "CBC"}, "H=hextale:unstable_hextech",
                    "I=hextale:brass_ingot", "A=minecraft:amethyst_block", "C=hextale:chiseled_magic_bricks", "B=hextale:magic_bricks"),
            SMELTING, MAGIC_STONE);
    public static final Quest RUNE_STELE = item("rune_stele", ModItems.RUNE_STELE, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:rune_stele", 1, new String[]{" A ", " C ", "BBB"},
                    "A=minecraft:amethyst_shard", "C=hextale:chiseled_magic_bricks", "B=hextale:magic_bricks"), HEXTECH_ALTAR);
    public static final Quest PEDESTAL = item("pedestal", ModItems.PEDESTAL, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:pedestal", 2, new String[]{" I ", " B ", "BCB"},
                    "I=hextale:brass_ingot", "B=hextale:magic_bricks", "C=hextale:chiseled_magic_bricks"), HEXTECH_ALTAR);
    public static final Quest HEXTECH_GEMSTONE = item("hextech_gemstone", ModItems.HEXTECH_GEMSTONE, Quest.Aura.HEXTECH,
            QuestDisplay.infusion("hextale:hextech_gemstone"), PEDESTAL);
    public static final Quest HEXTECH_LAMP = item("hextech_lamp", ModItems.HEXTECH_LAMP, Quest.Aura.HEXTECH,
            QuestDisplay.shaped("hextale:hextech_lamp", 2, new String[]{" N ", "BGB", " S "}, "N=hextale:brass_nugget",
                    "B=hextale:brass_ingot", "G=hextale:hextech_gemstone", "S=hextale:magic_brick_slab"), HEXTECH_GEMSTONE);
    public static final Quest HEX_SMELTER = item("hex_smelter", ModItems.HEX_SMELTER, Quest.Aura.GOLD,
            QuestDisplay.infusion("hextale:hex_smelter"), HEXTECH_GEMSTONE, RUNE_STELE);
    public static final Quest SMELTER_RUNE = add("smelter_rune", Quest.Kind.ITEM, List.of(HEX_SMELTER),
            () -> new ItemStack(ModItems.RUNE_STELE.get()), Quests.smelterRunes(1), QuestDisplay.smelterRunes(), null, Quest.Aura.FLAME);
    // Hex pylons: items carried by hextech, gathered off the ground, thrown to links, passed on by relays.
    public static final Quest GATHERING_PYLON = item("gathering_pylon", ModItems.GATHERING_PYLON, Quest.Aura.HEXTECH,
            QuestDisplay.infusion("hextale:gathering_pylon"), HEXTECH_GEMSTONE);
    public static final Quest SENDING_PYLON = item("sending_pylon", ModItems.SENDING_PYLON, Quest.Aura.GOLD,
            QuestDisplay.infusion("hextale:sending_pylon"), GATHERING_PYLON);
    public static final Quest RELAY_PYLON = item("relay_pylon", ModItems.RELAY_PYLON, Quest.Aura.HEXTECH,
            QuestDisplay.infusion("hextale:relay_pylon"), SENDING_PYLON);
    public static final Quest PYLON_RUNE = add("pylon_rune", Quest.Kind.ITEM, List.of(SENDING_PYLON),
            () -> new ItemStack(ModItems.RUNE_STELE.get()), Quests.pylonRunes(1), QuestDisplay.pylonRunes(), null, Quest.Aura.NONE);
    public static final Quest HEXTECH_PICKAXE = add("hextech_pickaxe", Quest.Kind.ITEM, List.of(HEXTECH_GEMSTONE, RUNE_STELE),
            () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.HEXTECH).get()), Quests.pickaxe(1),
            QuestDisplay.infusion("hextale:hextech_pickaxe"), null, Quest.Aura.GOLD);
    public static final Quest PICKAXE_RUNE = add("pickaxe_rune", Quest.Kind.ITEM, List.of(HEXTECH_PICKAXE),
            () -> new ItemStack(ModItems.RUNE_SCROLL.get()), Quests.pickaxeRunes(1), QuestDisplay.pickaxeRunes(), null, Quest.Aura.NONE);
    public static final Quest REFINED_PICKAXE = add("refined_pickaxe", Quest.Kind.ITEM, List.of(PICKAXE_RUNE),
            () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.REFINED).get()), Quests.pickaxe(2),
            QuestDisplay.infusion("hextale:refined_hextech_pickaxe"), null, Quest.Aura.HEXTECH);
    public static final Quest RADIANT_PICKAXE = add("radiant_pickaxe", Quest.Kind.ITEM, List.of(REFINED_PICKAXE),
            () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.RADIANT).get()), Quests.pickaxe(3),
            QuestDisplay.infusion("hextale:radiant_hextech_pickaxe"), null, Quest.Aura.HEXTECH);
    public static final Quest WIP_RUNE_WORKBENCH = wip("rune_workbench", Items.CRAFTER, Quest.Aura.HEXTECH, PEDESTAL, HEXTECH_GEMSTONE);
    public static final Quest WIP_HEXTECH_AXE = wip("hextech_axe", Items.IRON_AXE, Quest.Aura.HEXTECH, HEXTECH_PICKAXE);
    public static final Quest WIP_HEXTECH_HOE = wip("hextech_hoe", Items.IRON_HOE, Quest.Aura.LIFE, HEXTECH_PICKAXE);

    // ---- Rune branches. Chapter I runes sit next to Smelting, chapter III runes deep in the branches -----------------

    // Metalwork: heat and metal.
    public static final Quest TEMPERING = rune("earth_fire", Quest.Aura.FLAME, SMELTING);
    public static final Quest FORGE = rune("metal_fire", Quest.Aura.FLAME, TEMPERING);
    public static final Quest BELLOWS = rune("air_fire", Quest.Aura.NONE, TEMPERING);
    public static final Quest QUENCHING = rune("water_fire", Quest.Aura.NONE, TEMPERING);
    public static final Quest REFORGING = rune("fire_metal", Quest.Aura.NONE, FORGE);
    public static final Quest MOTOR = rune("metal_spark", Quest.Aura.STORM, FORGE);
    public static final Quest MAGNET = rune("spark_metal", Quest.Aura.STORM, MOTOR);
    public static final Quest REINFORCEMENT = rune("earth_metal", Quest.Aura.NONE, REFORGING);
    public static final Quest BLADES = rune("metal_air", Quest.Aura.NONE, REFORGING);
    public static final Quest LOCK = rune("metal_shadow", Quest.Aura.CURSED, REINFORCEMENT);
    public static final Quest WIP_QUENCH_TANK = wip("quench_tank", Items.WATER_BUCKET, Quest.Aura.NONE, QUENCHING);
    public static final Quest WIP_RUNE_LOCK = wip("rune_lock", Items.TRIAL_KEY, Quest.Aura.CURSED, LOCK);

    // Earth: what the pickaxe digs.
    public static final Quest EROSION = rune("water_earth", Quest.Aura.NONE, SMELTING);
    public static final Quest INSIGHT = rune("light_earth", Quest.Aura.NONE, SMELTING);
    public static final Quest DRILL = rune("metal_earth", Quest.Aura.NONE, EROSION);
    public static final Quest DAM = rune("earth_water", Quest.Aura.NONE, EROSION);
    public static final Quest GEMSTONE = rune("earth_light", Quest.Aura.NONE, INSIGHT);
    public static final Quest CACHE = rune("earth_shadow", Quest.Aura.CURSED, GEMSTONE);
    public static final Quest PASSAGE = rune("shadow_earth", Quest.Aura.CURSED, DRILL);
    public static final Quest QUAKE = rune("spark_earth", Quest.Aura.STORM, DRILL);
    public static final Quest SPONGE = rune("shadow_water", Quest.Aura.CURSED, DAM);
    public static final Quest WIP_DRAINER = wip("drainer", Items.SPONGE, Quest.Aura.NONE, SPONGE);

    // Light: lenses, beams and mirrors.
    public static final Quest LENS = rune("water_light", Quest.Aura.HEXTECH, INSIGHT);
    public static final Quest BEAM = rune("light_fire", Quest.Aura.NONE, LENS);
    public static final Quest MIRROR = rune("metal_light", Quest.Aura.NONE, LENS);
    public static final Quest ETERNAL_FLAME = rune("fire_light", Quest.Aura.FLAME, BEAM);
    public static final Quest SIGNAL = rune("light_air", Quest.Aura.NONE, MIRROR);
    public static final Quest ENGRAVING = rune("light_metal", Quest.Aura.NONE, MIRROR);
    public static final Quest SUNLIGHT_CELL = rune("light_spark", Quest.Aura.STORM, ETERNAL_FLAME);
    public static final Quest MIRAGE = rune("air_light", Quest.Aura.NONE, SIGNAL);
    public static final Quest FLASH = rune("spark_light", Quest.Aura.STORM, SUNLIGHT_CELL);
    public static final Quest BANISHING = rune("light_shadow", Quest.Aura.CURSED, ETERNAL_FLAME);
    public static final Quest ECLIPSE = rune("shadow_light", Quest.Aura.CURSED, BANISHING);
    public static final Quest PURIFYING_FIRE = rune("fire_shadow", Quest.Aura.CURSED, BANISHING);
    public static final Quest WIP_STAR_ROD = wip("star_rod", Items.END_ROD, Quest.Aura.HEXTECH, ETERNAL_FLAME, HEXTECH_LAMP);
    public static final Quest WIP_BEAM_RELAY = wip("beam_relay", Items.SPYGLASS, Quest.Aura.NONE, MIRROR, BEAM);
    public static final Quest WIP_HEX_CUTTER = wip("hex_cutter", Items.STONECUTTER, Quest.Aura.NONE, BEAM, BLADES);
    public static final Quest WIP_SIGNAL_BEACON = wip("signal_beacon", Items.BEACON, Quest.Aura.NONE, SIGNAL);
    public static final Quest WIP_MIRAGE_PROJECTOR = wip("mirage_projector", Items.TINTED_GLASS, Quest.Aura.NONE, MIRAGE);
    public static final Quest WIP_RUNE_ENGRAVER = wip("rune_engraver", Items.BRUSH, Quest.Aura.NONE, ENGRAVING);
    public static final Quest WIP_FLASH_CHARGE = wip("flash_charge", Items.FIREWORK_STAR, Quest.Aura.STORM, FLASH);

    // Storm: grounding lightning, conducting it, lifting things.
    public static final Quest GROUNDING = rune("earth_spark", Quest.Aura.STORM, SMELTING);
    public static final Quest CONDUCTOR = rune("water_spark", Quest.Aura.STORM, GROUNDING);
    public static final Quest GRAVITY = rune("earth_air", Quest.Aura.NONE, GROUNDING);
    public static final Quest WINDMILL = rune("air_spark", Quest.Aura.STORM, CONDUCTOR);
    public static final Quest SHOCK = rune("spark_water", Quest.Aura.STORM, CONDUCTOR);
    public static final Quest CHAIN_LIGHTNING = rune("spark_air", Quest.Aura.STORM, SHOCK);
    public static final Quest INSULATOR = rune("shadow_spark", Quest.Aura.CURSED, CONDUCTOR);
    public static final Quest LIFT = rune("air_metal", Quest.Aura.NONE, GRAVITY);
    public static final Quest SHIFT = rune("air_earth", Quest.Aura.NONE, GRAVITY);
    public static final Quest BLINK = rune("spark_shadow", Quest.Aura.CURSED, SHIFT);
    public static final Quest WIP_STORM_COLLECTOR = wip("storm_collector", Items.LIGHTNING_ROD.asList().getFirst(), Quest.Aura.STORM, GROUNDING);
    public static final Quest WIP_CRYSTAL_BATTERY = wip("crystal_battery", Items.AMETHYST_CLUSTER, Quest.Aura.STORM, CONDUCTOR, SUNLIGHT_CELL);
    public static final Quest WIP_WIND_TURBINE = wip("wind_turbine", Items.BREEZE_ROD, Quest.Aura.NONE, WINDMILL);
    public static final Quest WIP_LEVITATION_PLATE = wip("levitation_plate", Items.HEAVY_WEIGHTED_PRESSURE_PLATE, Quest.Aura.HEXTECH, GRAVITY, LIFT);
    public static final Quest WIP_HEX_LIFT = wip("hex_lift", Items.PISTON, Quest.Aura.NONE, LIFT);
    public static final Quest WIP_HEX_GATE = wip("hex_gate", Items.ENDER_EYE, Quest.Aura.HEXTECH, SHIFT, BLINK);
    public static final Quest WIP_HEX_TURRET = wip("hex_turret", Items.DISPENSER, Quest.Aura.STORM, CHAIN_LIGHTNING, BEAM);
    public static final Quest WIP_HEX_BLADE = wip("hex_blade", Items.IRON_SWORD, Quest.Aura.STORM, BLADES, CHAIN_LIGHTNING);
    public static final Quest WIP_NULLSTONE = wip("nullstone", Items.CRYING_OBSIDIAN, Quest.Aura.CURSED, INSULATOR, ECLIPSE);

    // Fire and air: bellows, steam, pipes.
    public static final Quest FLAMETHROWER = rune("fire_air", Quest.Aura.FLAME, BELLOWS);
    public static final Quest PUMP_RUNE = rune("air_water", Quest.Aura.NONE, BELLOWS);
    public static final Quest STEAM = rune("fire_water", Quest.Aura.NONE, QUENCHING);
    public static final Quest HEAT_EATER = rune("shadow_fire", Quest.Aura.CURSED, QUENCHING);
    public static final Quest EXPLOSION = rune("fire_spark", Quest.Aura.FLAME, FLAMETHROWER);
    public static final Quest DETONATOR = rune("spark_fire", Quest.Aura.STORM, EXPLOSION);
    public static final Quest FOG = rune("water_air", Quest.Aura.NONE, STEAM);
    public static final Quest WHIRLPOOL = rune("water_shadow", Quest.Aura.CURSED, FOG);
    public static final Quest HUSH = rune("air_shadow", Quest.Aura.CURSED, FOG);
    public static final Quest INVISIBILITY = rune("shadow_air", Quest.Aura.CURSED, HUSH);
    public static final Quest PIPES = rune("metal_water", Quest.Aura.NONE, PUMP_RUNE);
    public static final Quest CORROSION = rune("water_metal", Quest.Aura.NONE, PIPES);
    public static final Quest BLIGHT = rune("shadow_metal", Quest.Aura.CURSED, CORROSION);
    public static final Quest WIP_BARRIER_WARD = wip("barrier_ward", Items.SHIELD, Quest.Aura.HEXTECH, REINFORCEMENT, HUSH);
    public static final Quest WIP_FLAMETHROWER = wip("hex_flamethrower", Items.FIRE_CHARGE, Quest.Aura.FLAME, FLAMETHROWER);
    public static final Quest WIP_MINING_CHARGE = wip("mining_charge", Items.TNT, Quest.Aura.FLAME, EXPLOSION, DETONATOR);
    public static final Quest WIP_STEAM_BOILER = wip("steam_boiler", Items.CAULDRON, Quest.Aura.NONE, STEAM);
    public static final Quest WIP_COOLER = wip("cooler", Items.BLUE_ICE, Quest.Aura.NONE, HEAT_EATER);
    public static final Quest WIP_VEIL = wip("veil", Items.PHANTOM_MEMBRANE, Quest.Aura.CURSED, INVISIBILITY);
    public static final Quest WIP_RUNE_PUMP = wip("rune_pump", Items.HOPPER, Quest.Aura.NONE, PUMP_RUNE, PIPES);
    public static final Quest WIP_ACID_BATH = wip("acid_bath", Items.FERMENTED_SPIDER_EYE, Quest.Aura.CURSED, CORROSION, BLIGHT);

    // Life: growth, healing, constructs - and the Hexcore at the very end.
    public static final Quest FERTILITY = rune("earth_life", Quest.Aura.LIFE, EROSION);
    public static final Quest GROWTH = rune("life_earth", Quest.Aura.LIFE, FERTILITY);
    public static final Quest SPRING = rune("life_water", Quest.Aura.LIFE, FERTILITY);
    public static final Quest POLLEN = rune("life_air", Quest.Aura.LIFE, GROWTH);
    public static final Quest PHOTOSYNTHESIS = rune("light_life", Quest.Aura.LIFE, GROWTH);
    public static final Quest RADIANCE = rune("life_light", Quest.Aura.LIFE, PHOTOSYNTHESIS);
    public static final Quest BREATH = rune("air_life", Quest.Aura.LIFE, POLLEN);
    public static final Quest HEALING = rune("water_life", Quest.Aura.LIFE, SPRING);
    public static final Quest PURIFICATION = rune("light_water", Quest.Aura.NONE, SPRING);
    public static final Quest FIREPROOFING = rune("life_fire", Quest.Aura.NONE, HEALING);
    public static final Quest SCORCH = rune("fire_life", Quest.Aura.FLAME, FLAMETHROWER);
    public static final Quest MENDING = rune("life_metal", Quest.Aura.NONE, HEALING);
    public static final Quest CONSTRUCT = rune("metal_life", Quest.Aura.NONE, MENDING);
    public static final Quest REVIVAL = rune("life_spark", Quest.Aura.STORM, HEALING);
    public static final Quest HASTE = rune("spark_life", Quest.Aura.STORM, REVIVAL);
    public static final Quest SLUMBER = rune("life_shadow", Quest.Aura.CURSED, BREATH);
    public static final Quest WITHERING = rune("shadow_life", Quest.Aura.CURSED, SLUMBER);
    public static final Quest WIP_RUNE_ANVIL = wip("rune_anvil", Items.ANVIL, Quest.Aura.NONE, REFORGING, MENDING);
    public static final Quest WIP_GREENHOUSE = wip("greenhouse", Items.FLOWERING_AZALEA, Quest.Aura.LIFE, GROWTH, PHOTOSYNTHESIS);
    public static final Quest WIP_HEALING_SPRING = wip("healing_spring", Items.GLISTERING_MELON_SLICE, Quest.Aura.LIFE, HEALING);
    public static final Quest WIP_RESPIRATOR = wip("respirator", Items.LEATHER_HELMET, Quest.Aura.NONE, BREATH, PURIFICATION);
    public static final Quest WIP_PURIFIER = wip("purifier", Items.CONDUIT, Quest.Aura.HEXTECH, PURIFICATION, PURIFYING_FIRE);
    public static final Quest WIP_FIREPROOF_COAT = wip("fireproof_coat", Items.MAGMA_CREAM, Quest.Aura.FLAME, FIREPROOFING, SCORCH);
    public static final Quest WIP_CONSTRUCT = wip("construct", Items.ARMOR_STAND, Quest.Aura.HEXTECH, CONSTRUCT, MOTOR);
    public static final Quest WIP_SHIMMER = wip("shimmer", Items.EXPERIENCE_BOTTLE, Quest.Aura.CURSED, HASTE, WITHERING);
    public static final Quest WIP_HEXCORE = wip("hexcore", Items.HEART_OF_THE_SEA, Quest.Aura.GOLD, REVIVAL, CONSTRUCT, RADIANCE);

    // ---- Devices the runes will drive: sorting and storage, farms that tend themselves, constructs, shimmer -----------

    public static final Quest WIP_SORTING_MATRIX = wip("sorting_matrix", Items.OBSERVER, Quest.Aura.HEXTECH, MIRROR, HEXTECH_GEMSTONE);
    public static final Quest WIP_XP_CONDENSER = wip("xp_condenser", Items.SCULK_CATALYST, Quest.Aura.HEXTECH, INSIGHT);
    public static final Quest WIP_ORE_WASHER = wip("ore_washer", Items.RAW_IRON, Quest.Aura.NONE, EROSION, PUMP);
    public static final Quest WIP_HERD_WARDEN = wip("herd_warden", Items.SHEARS, Quest.Aura.LIFE, FERTILITY);
    public static final Quest WIP_SPRING_BASIN = wip("spring_basin", Items.LILY_PAD, Quest.Aura.LIFE, SPRING);
    public static final Quest WIP_BLOOM_CENSER = wip("bloom_censer", Items.SPORE_BLOSSOM, Quest.Aura.LIFE, POLLEN);
    public static final Quest WIP_MAGMA_FONT = wip("magma_font", Items.LAVA_BUCKET, Quest.Aura.FLAME, ETERNAL_FLAME);
    public static final Quest WIP_RAIN_CALLER = wip("rain_caller", Items.TRIDENT, Quest.Aura.STORM, FOG, CONDUCTOR);
    public static final Quest WIP_ESSENCE_RESONATOR = wip("essence_resonator", Items.GLASS_BOTTLE, Quest.Aura.HEXTECH, CONDUCTOR, PIPES);
    public static final Quest WIP_WORLD_ANCHOR = wip("world_anchor", Items.RESPAWN_ANCHOR, Quest.Aura.STORM, GROUNDING, GRAVITY);
    public static final Quest WIP_HEX_BORE = wip("hex_bore", Items.DIAMOND_PICKAXE, Quest.Aura.HEXTECH, DRILL, MOTOR);
    public static final Quest WIP_SHADOW_COFFER = wip("shadow_coffer", Items.ENDER_CHEST, Quest.Aura.CURSED, CACHE);
    public static final Quest WIP_HEX_ARCHIVE = wip("hex_archive", Items.CHISELED_BOOKSHELF, Quest.Aura.HEXTECH, CACHE, ENGRAVING);
    public static final Quest WIP_FOLDING_GATE = wip("folding_gate", Items.ENDER_PEARL, Quest.Aura.CURSED, BLINK);
    public static final Quest WIP_HARVEST_CONSTRUCT = wip("harvest_construct", Items.GOLDEN_HOE, Quest.Aura.HEXTECH, CONSTRUCT, BLADES);
    public static final Quest WIP_FORESTER_CONSTRUCT = wip("forester_construct", Items.GOLDEN_AXE, Quest.Aura.HEXTECH, CONSTRUCT, GROWTH);
    public static final Quest WIP_WINGED_COURIER = wip("winged_courier", Items.FEATHER, Quest.Aura.HEXTECH, CONSTRUCT, LIFT);
    public static final Quest WIP_CRYSTAL_VAT = wip("crystal_vat", Items.BUDDING_AMETHYST, Quest.Aura.CURSED, CORROSION, HEXTECH_GEMSTONE);
    public static final Quest WIP_GLOWBLOOM_BED = wip("glowbloom_bed", Items.GLOW_BERRIES, Quest.Aura.LIFE, RADIANCE);
    public static final Quest WIP_REGEN_TANK = wip("regen_tank", Items.AXOLOTL_BUCKET, Quest.Aura.CURSED, HEALING, PIPES);
    public static final Quest WIP_MUTATION_INJECTOR = wip("mutation_injector", Items.SPIDER_EYE, Quest.Aura.CURSED, BLIGHT);
    public static final Quest WIP_LULL_LANTERN = wip("lull_lantern", Items.SOUL_LANTERN, Quest.Aura.CURSED, SLUMBER);
    public static final Quest WIP_CHRONO_CHAMBER = wip("chrono_chamber", Items.CLOCK, Quest.Aura.STORM, HASTE);

    static {
        layout();
    }

    private Guide() {
    }

    // ---- Building -----------------------------------------------------------------------------------------------------

    private static Quest add(String id, Quest.Kind kind, List<Quest> parents, Supplier<ItemStack> icon, Predicate<ServerPlayer> goal,
                             @Nullable QuestDisplay display, @Nullable ComplexRune rune, Quest.Aura aura) {
        Quest quest = new Quest(id, kind, parents, icon, goal, display, rune, aura);
        quest.index = ALL.size();
        ALL.add(quest);
        if (BY_ID.put(id, quest) != null) {
            throw new IllegalStateException("Two guide steps named " + id);
        }
        if (rune != null && BY_RUNE.put(rune, quest) != null) {
            throw new IllegalStateException("Rune " + rune.key() + " is on the guide twice");
        }
        return quest;
    }

    private static Quest item(String id, Supplier<? extends ItemLike> item, Quest.Aura aura, @Nullable QuestDisplay display, Quest... parents) {
        return add(id, Quest.Kind.ITEM, List.of(parents), () -> new ItemStack(item.get()), Quests.has(() -> item.get().asItem()), display,
                null, aura);
    }

    private static Quest rune(String key, Quest.Aura aura, Quest... parents) {
        ComplexRune rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
        return add(key, Quest.Kind.RUNE, List.of(parents), () -> new ItemStack(ModItems.RUNE_SCROLL.get()), player -> false, null, rune, aura);
    }

    private static Quest wip(String id, ItemLike icon, Quest.Aura aura, Quest... parents) {
        return add("wip_" + id, Quest.Kind.WIP, List.of(parents), () -> new ItemStack(icon), player -> false, null, null, aura);
    }

    /**
     * A compact tree on the first parents, left to right: a step's column is one past its deepest parent. Each step's
     * main branch (an item quest if it has one, else its biggest) carries on along its row; the others hang above and below
     * it, as close as the branches already placed allow column by column (the contour method of Reingold and Tilford).
     */
    private static void layout() {
        int[] depth = new int[ALL.size()];
        for (Quest quest : ALL) {
            int d = 0;
            for (Quest parent : quest.parents()) {
                d = Math.max(d, depth[parent.index] + 1);
            }
            depth[quest.index] = d;
            quest.x = d * COLUMN;
        }
        int[] offset = new int[ALL.size()];
        int[] size = new int[ALL.size()];
        for (int i = ALL.size() - 1; i >= 0; i--) {
            Quest quest = ALL.get(i);
            size[i] = 1 + hanging(quest).stream().mapToInt(child -> size[child.index]).sum();
        }
        int top = 0;
        for (Quest quest : ALL) {
            if (quest.parents().isEmpty()) {
                Map<Integer, int[]> contour = place(quest, depth, offset, size);
                quest.y = top - contour.values().stream().mapToInt(range -> range[0]).min().orElse(0);
                settle(quest, offset);
                top = quest.y + contour.values().stream().mapToInt(range -> range[1]).max().orElse(0) + ROW * 2;
            }
        }
    }

    private static List<Quest> hanging(Quest quest) {
        return quest.children().stream().filter(child -> child.parent() == quest).toList();
    }

    /**
     * Places a step's branches relative to the step (their offsets in rows of pixels) and returns the contour of the
     * whole subtree: for each column, the lowest and highest y relative to the step.
     */
    private static Map<Integer, int[]> place(Quest quest, int[] depth, int[] offset, int[] size) {
        List<Quest> children = new ArrayList<>(hanging(quest));
        Map<Integer, int[]> contour = new HashMap<>();
        contour.put(depth[quest.index], new int[]{0, 0});
        if (children.isEmpty()) {
            return contour;
        }
        // The main line: item quests go on along the row (the path of things to make), runes branch off. The main
        // branch in the middle, the others alternating above and below it, smaller ones farther out.
        children.sort((a, b) -> a.kind() == Quest.Kind.ITEM && b.kind() != Quest.Kind.ITEM ? -1
                : b.kind() == Quest.Kind.ITEM && a.kind() != Quest.Kind.ITEM ? 1 : size[b.index] - size[a.index]);
        java.util.ArrayDeque<Quest> order = new java.util.ArrayDeque<>();
        order.add(children.getFirst());
        for (int i = 1; i < children.size(); i++) {
            if (i % 2 == 1) {
                order.addFirst(children.get(i));
            } else {
                order.addLast(children.get(i));
            }
        }
        Map<Integer, int[]> merged = new HashMap<>();
        Quest main = children.getFirst();
        for (Quest child : order) {
            Map<Integer, int[]> sub = place(child, depth, offset, size);
            int at = 0;
            if (!merged.isEmpty()) {
                at = Integer.MIN_VALUE;
                for (Map.Entry<Integer, int[]> entry : sub.entrySet()) {
                    int[] above = merged.get(entry.getKey());
                    if (above != null) {
                        at = Math.max(at, above[1] + ROW - entry.getValue()[0]);
                    }
                }
                if (at == Integer.MIN_VALUE) {
                    at = merged.values().stream().mapToInt(range -> range[1]).max().orElse(0) + ROW;
                }
            }
            offset[child.index] = at;
            for (Map.Entry<Integer, int[]> entry : sub.entrySet()) {
                int lo = entry.getValue()[0] + at;
                int hi = entry.getValue()[1] + at;
                merged.merge(entry.getKey(), new int[]{lo, hi}, (x, y) -> new int[]{Math.min(x[0], y[0]), Math.max(x[1], y[1])});
            }
        }
        // The step sits on its main branch's row.
        int shift = offset[main.index];
        for (Quest child : children) {
            offset[child.index] -= shift;
        }
        for (Map.Entry<Integer, int[]> entry : merged.entrySet()) {
            int[] range = entry.getValue();
            contour.merge(entry.getKey(), new int[]{range[0] - shift, range[1] - shift},
                    (x, y) -> new int[]{Math.min(x[0], y[0]), Math.max(x[1], y[1])});
        }
        return contour;
    }

    private static void settle(Quest quest, int[] offset) {
        for (Quest child : hanging(quest)) {
            child.y = quest.y + offset[child.index];
            settle(child, offset);
        }
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

    /** The step that teaches a rune. Every rune has one. */
    public static Quest of(ComplexRune rune) {
        return Objects.requireNonNull(BY_RUNE.get(rune), rune.key());
    }

    /** An item quest when its goal was met, a rune step when the rune is known; WIP steps never. */
    public static boolean done(Quest quest, RuneKnowledge knowledge) {
        return switch (quest.kind()) {
            case ITEM -> knowledge.questDone(quest);
            case RUNE -> knowledge.knows(Objects.requireNonNull(quest.rune()));
            case WIP -> false;
        };
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

    /** Whether the researcher may study this rune at the table: its step is open (or already learned). */
    public static boolean canStudy(ComplexRune rune, RuneKnowledge knowledge) {
        Quest quest = of(rune);
        return done(quest, knowledge) || open(quest, knowledge);
    }

    /** Bounds of the laid-out path (for the camera). */
    public static int top() {
        return ALL.stream().mapToInt(Quest::y).min().orElse(0);
    }

    public static int height() {
        return ALL.stream().mapToInt(Quest::y).max().orElse(0);
    }

    public static int width() {
        return ALL.stream().mapToInt(Quest::x).max().orElse(0);
    }
}
