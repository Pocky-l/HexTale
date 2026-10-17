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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * The researcher's guide: everything the mod asks of a researcher, as a web of steps gathered by theme ({@link Region}):
 * the workshop, the essence works, the hextech circle, the pylons, the rune lore and the devices still being worked out
 * (WIP). There is no single road: a step opens when all its parents are done, and parents are only what the step really
 * needs, so several themes are open at once and the researcher picks where to go. A rune can be studied at the table only
 * once its step is open. The order is the order things can really be made in: {@code QuestGameTests.guideOrderIsLogical}
 * checks that every mod item a step's recipe needs comes from a step before it.
 */
public final class Guide {
    /** Map pixels between linked steps of one region. */
    public static final int SPACING = 50;
    /** The least distance between two steps of one region, and between steps of different regions. */
    private static final int GAP = 54;
    private static final int APART = 120;
    /** A region's room grows with the square root of its steps; regions keep this margin between them. */
    private static final int RADIUS = 34;
    private static final int MARGIN = 60;
    /** No two steps closer than this, whatever the springs want. */
    private static final int SEPARATE = 46;

    private static final List<Quest> ALL = new ArrayList<>();
    private static final Map<String, Quest> BY_ID = new HashMap<>();
    private static final Map<ComplexRune, Quest> BY_RUNE = new HashMap<>();

    // ---- The workshop: brass, raw hextech, the table and the diary --------------------------------------------------

    public static final Quest BRASS = item(Region.WORKSHOP, "brass", ModItems.BRASS_INGOT, Quest.Aura.NONE,
            QuestDisplay.shapeless("hextale:brass_ingot", 2, "minecraft:copper_ingot", "minecraft:copper_ingot",
                    "minecraft:copper_ingot", "minecraft:gold_ingot"));
    public static final Quest UNSTABLE_HEXTECH = item(Region.WORKSHOP, "unstable_hextech", ModItems.UNSTABLE_HEXTECH, Quest.Aura.STORM,
            QuestDisplay.scheme("hextale:unstable_hextech", "minecraft:lightning_rod", "minecraft:amethyst_block"));
    public static final Quest RESEARCH_TABLE = item(Region.WORKSHOP, "research_table", ModItems.RESEARCH_TABLE, Quest.Aura.GOLD,
            QuestDisplay.shaped("hextale:research_table", 1, new String[]{"BBB", "PSP", "P P"},
                    "B=hextale:brass_ingot", "P=#minecraft:planks", "S=minecraft:bookshelf"), BRASS);
    public static final Quest RESEARCHERS_DIARY = item(Region.WORKSHOP, "researchers_diary", ModItems.RESEARCHERS_DIARY, Quest.Aura.NONE,
            QuestDisplay.shapeless("hextale:researchers_diary", 1, "minecraft:book", "hextale:brass_ingot", "hextale:unstable_hextech"),
            UNSTABLE_HEXTECH, BRASS);
    public static final Quest ITEM_STUDY = add(Region.WORKSHOP, "item_study", Quest.Kind.ITEM, List.of(RESEARCH_TABLE, RESEARCHERS_DIARY),
            () -> new ItemStack(Items.PAPER), Quests.studied(1), QuestDisplay.essences(), null, Quest.Aura.NONE);
    public static final Quest RESEARCHERS_GOGGLES = item(Region.WORKSHOP, "researchers_goggles", ModItems.RESEARCHERS_GOGGLES, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:researchers_goggles", 1, new String[]{"LBL", "G G"},
                    "L=minecraft:leather", "B=hextale:brass_ingot", "G=minecraft:glass_pane"), BRASS);
    public static final Quest WIP_JAR_GAUGE = wip(Region.WORKSHOP, "jar_gauge", Items.COMPARATOR, Quest.Aura.NONE, RESEARCHERS_GOGGLES);

    // ---- The essence works: boiling, bottling, separating, storing ---------------------------------------------------

    public static final Quest ALEMBIC = item(Region.ESSENCES, "alembic", ModItems.ALEMBIC, Quest.Aura.GOLD,
            QuestDisplay.shaped("hextale:alembic", 1, new String[]{" B ", "G G", "BGB"}, "B=hextale:brass_ingot", "G=minecraft:glass"),
            BRASS);
    public static final Quest ESSENCE_BOTTLE = add(Region.ESSENCES, "essence_bottle", Quest.Kind.ITEM, List.of(ALEMBIC),
            () -> EssenceFlaskItem.filled(Quests.SAMPLE_MIX), Quests.has(ModItems.ESSENCE_FLASK),
            QuestDisplay.scheme("hextale:essence_flask", "minecraft:glass_bottle", "hextale:alembic"), null, Quest.Aura.NONE);
    public static final Quest BRASS_PIPE = item(Region.ESSENCES, "brass_pipe", ModItems.BRASS_PIPE, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:brass_pipe", 8, new String[]{"BBB"}, "B=hextale:brass_ingot"), ALEMBIC);
    public static final Quest PUMP = item(Region.ESSENCES, "pump", ModItems.PUMP, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:pump", 1, new String[]{" P ", "BXB", "BBB"},
                    "P=hextale:brass_pipe", "B=hextale:brass_ingot", "X=minecraft:piston"), BRASS_PIPE);
    public static final Quest SEPARATOR = item(Region.ESSENCES, "separator", ModItems.SEPARATOR, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:separator", 1, new String[]{"BGB", "G G", "BBB"}, "B=hextale:brass_ingot", "G=minecraft:glass"),
            ESSENCE_BOTTLE);
    public static final Quest ESSENCE_TANK = item(Region.ESSENCES, "essence_tank", ModItems.ESSENCE_TANK, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:essence_tank", 1, new String[]{"SSS", "G G", "GGG"},
                    "S=#minecraft:wooden_slabs", "G=minecraft:glass"), ESSENCE_BOTTLE);
    public static final Quest MAGIC_STONE = item(Region.ESSENCES, "magic_stone", ModItems.MAGIC_STONE, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:magic_stone", 8, new String[]{"SSS", "SBS", "SSS"},
                    "S=#hextale:soaks_essence", "B=hextale:essence_flask"), ESSENCE_BOTTLE);
    // The first multiblock: built of glass and slabs, woken with a hextech crystal (Multiblocks completes it).
    public static final Quest BIG_ESSENCE_JAR = add(Region.ESSENCES, "big_essence_jar", Quest.Kind.ITEM, List.of(ESSENCE_TANK, UNSTABLE_HEXTECH),
            () -> new ItemStack(ModItems.ESSENCE_TANK.get()), player -> false, QuestDisplay.multiblock("big_essence_jar"), null, Quest.Aura.GOLD);
    public static final Quest DRAIN_VALVE = item(Region.ESSENCES, "drain_valve", ModItems.DRAIN_VALVE, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:drain_valve", 1, new String[]{" I ", "BPB"},
                    "I=minecraft:iron_ingot", "B=hextale:brass_ingot", "P=hextale:brass_pipe"), SEPARATOR, BRASS_PIPE);
    public static final Quest CONCENTRATE = add(Region.ESSENCES, "concentrate", Quest.Kind.ITEM, List.of(SEPARATOR),
            () -> EssenceFlaskItem.filled(EssenceMix.pure(Essence.WATER, EssenceFlaskItem.CAPACITY)), Quests::concentrate,
            QuestDisplay.reagents(), null, Quest.Aura.NONE);

    // The table's ink comes from the jars: the workshop and the essence works meet here, and the runes begin.
    public static final Quest INK = add(Region.WORKSHOP, "ink", Quest.Kind.ITEM, List.of(ITEM_STUDY, ESSENCE_TANK),
            () -> new ItemStack(Items.INK_SAC), Quests::inkAtTable, null, null, Quest.Aura.NONE);

    // ---- Fields and woods: farms and wood works still being worked out ----------------------------------------------

    public static final Quest WIP_SAP_TAP = wip(Region.FIELDS, "sap_tap", Items.HONEY_BOTTLE, Quest.Aura.LIFE, ESSENCE_BOTTLE);
    public static final Quest WIP_SAWMILL = wip(Region.FIELDS, "sawmill", Items.OAK_PLANKS, Quest.Aura.NONE, ALEMBIC);
    public static final Quest WIP_ESSENCE_SPRINKLER = wip(Region.FIELDS, "essence_sprinkler", Items.POINTED_DRIPSTONE, Quest.Aura.LIFE,
            BRASS_PIPE, CONCENTRATE);
    public static final Quest WIP_LIVING_SOIL = wip(Region.FIELDS, "living_soil", Items.ROOTED_DIRT, Quest.Aura.LIFE, MAGIC_STONE);
    public static final Quest WIP_FIELD_WARD = wip(Region.FIELDS, "field_ward", Items.CARVED_PUMPKIN, Quest.Aura.NONE, MAGIC_STONE);
    public static final Quest WIP_TREE_FELLER = wip(Region.FIELDS, "tree_feller", Items.OAK_LOG, Quest.Aura.NONE, MAGIC_STONE, ESSENCE_TANK);
    public static final Quest WIP_SEED_SOWER = wip(Region.FIELDS, "seed_sower", Items.WHEAT_SEEDS, Quest.Aura.NONE, ESSENCE_TANK);
    public static final Quest WIP_REAPER = wip(Region.FIELDS, "reaper", Items.WHEAT, Quest.Aura.NONE, ESSENCE_TANK);
    public static final Quest WIP_GROVE_KEEPER = wip(Region.FIELDS, "grove_keeper", Items.OAK_SAPLING, Quest.Aura.LIFE, ESSENCE_TANK);
    public static final Quest WIP_FEEDING_TROUGH = wip(Region.FIELDS, "feeding_trough", Items.HAY_BLOCK, Quest.Aura.LIFE, ESSENCE_TANK);
    public static final Quest WIP_FISH_WEIR = wip(Region.FIELDS, "fish_weir", Items.FISHING_ROD, Quest.Aura.NONE, PUMP);
    public static final Quest WIP_COMPOST_VAT = wip(Region.FIELDS, "compost_vat", Items.COMPOSTER, Quest.Aura.NONE, DRAIN_VALVE);

    // ---- Chemtech: what the waste of the separator could become -------------------------------------------------------

    public static final Quest WIP_THE_GRAY = wip(Region.CHEMTECH, "the_gray", Items.GUNPOWDER, Quest.Aura.CURSED, DRAIN_VALVE);
    public static final Quest WIP_CHEM_BURNER = wip(Region.CHEMTECH, "chem_burner", Items.CAMPFIRE, Quest.Aura.FLAME, SEPARATOR);
    public static final Quest WIP_LAB_BENCH = wip(Region.CHEMTECH, "lab_bench", Items.BREWING_STAND, Quest.Aura.NONE, SEPARATOR);
    public static final Quest WIP_EXPERIMENT_POT = wip(Region.CHEMTECH, "experiment_pot", Items.FLOWER_POT, Quest.Aura.LIFE, SEPARATOR, CONCENTRATE);
    public static final Quest WIP_SPECIMEN_CAGE = wip(Region.CHEMTECH, "specimen_cage", Items.IRON_BARS, Quest.Aura.CURSED, SEPARATOR);

    // ---- Smelting: the first rune, the key to the rune lore and the tools of the circle -----------------------------

    public static final Quest SMELTING = rune(Region.FORGE, "fire_earth", Quest.Aura.GOLD, INK);

    // ---- The hextech circle: the altar, its pedestals and steles, refined hextech and its tools -----------------------

    public static final Quest HEXTECH_ALTAR = item(Region.CIRCLE, "hextech_altar", ModItems.HEXTECH_ALTAR, Quest.Aura.GOLD,
            QuestDisplay.shaped("hextale:hextech_altar", 1, new String[]{" H ", "IAI", "CBC"}, "H=hextale:unstable_hextech",
                    "I=hextale:brass_ingot", "A=minecraft:amethyst_block", "C=hextale:chiseled_magic_bricks", "B=hextale:magic_bricks"),
            MAGIC_STONE, UNSTABLE_HEXTECH);
    public static final Quest PEDESTAL = item(Region.CIRCLE, "pedestal", ModItems.PEDESTAL, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:pedestal", 2, new String[]{" I ", " B ", "BCB"},
                    "I=hextale:brass_ingot", "B=hextale:magic_bricks", "C=hextale:chiseled_magic_bricks"), HEXTECH_ALTAR);
    public static final Quest RUNE_STELE = item(Region.CIRCLE, "rune_stele", ModItems.RUNE_STELE, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:rune_stele", 1, new String[]{" A ", " C ", "BBB"},
                    "A=minecraft:amethyst_shard", "C=hextale:chiseled_magic_bricks", "B=hextale:magic_bricks"), HEXTECH_ALTAR);
    // A ritual draws its essences from the jars around the altar.
    public static final Quest HEXTECH_GEMSTONE = item(Region.CIRCLE, "hextech_gemstone", ModItems.HEXTECH_GEMSTONE, Quest.Aura.HEXTECH,
            QuestDisplay.infusion("hextale:hextech_gemstone"), PEDESTAL, ESSENCE_TANK);
    public static final Quest HEXTECH_LAMP = item(Region.CIRCLE, "hextech_lamp", ModItems.HEXTECH_LAMP, Quest.Aura.HEXTECH,
            QuestDisplay.shaped("hextale:hextech_lamp", 2, new String[]{" N ", "BGB", " S "}, "N=hextale:brass_nugget",
                    "B=hextale:brass_ingot", "G=hextale:hextech_gemstone", "S=hextale:magic_brick_slab"), HEXTECH_GEMSTONE);
    public static final Quest HEX_SMELTER = item(Region.CIRCLE, "hex_smelter", ModItems.HEX_SMELTER, Quest.Aura.GOLD,
            QuestDisplay.infusion("hextale:hex_smelter"), HEXTECH_GEMSTONE, RUNE_STELE, SMELTING);
    public static final Quest SMELTER_RUNE = add(Region.CIRCLE, "smelter_rune", Quest.Kind.ITEM, List.of(HEX_SMELTER),
            () -> new ItemStack(ModItems.RUNE_STELE.get()), Quests.smelterRunes(1), QuestDisplay.smelterRunes(), null, Quest.Aura.FLAME);
    public static final Quest HEXTECH_PICKAXE = add(Region.CIRCLE, "hextech_pickaxe", Quest.Kind.ITEM, List.of(HEXTECH_GEMSTONE, RUNE_STELE, SMELTING),
            () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.HEXTECH).get()), Quests.pickaxe(1),
            QuestDisplay.infusion("hextale:hextech_pickaxe"), null, Quest.Aura.GOLD);
    public static final Quest PICKAXE_RUNE = add(Region.CIRCLE, "pickaxe_rune", Quest.Kind.ITEM, List.of(HEXTECH_PICKAXE),
            () -> new ItemStack(ModItems.RUNE_SCROLL.get()), Quests.pickaxeRunes(1), QuestDisplay.pickaxeRunes(), null, Quest.Aura.NONE);
    public static final Quest REFINED_PICKAXE = add(Region.CIRCLE, "refined_pickaxe", Quest.Kind.ITEM, List.of(HEXTECH_PICKAXE),
            () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.REFINED).get()), Quests.pickaxe(2),
            QuestDisplay.infusion("hextale:refined_hextech_pickaxe"), null, Quest.Aura.HEXTECH);
    public static final Quest RADIANT_PICKAXE = add(Region.CIRCLE, "radiant_pickaxe", Quest.Kind.ITEM, List.of(REFINED_PICKAXE),
            () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.RADIANT).get()), Quests.pickaxe(3),
            QuestDisplay.infusion("hextale:radiant_hextech_pickaxe"), null, Quest.Aura.HEXTECH);
    public static final Quest WIP_GEODE_RESONATOR = wip(Region.CIRCLE, "geode_resonator", Items.SMALL_AMETHYST_BUD, Quest.Aura.STORM,
            HEXTECH_ALTAR, ESSENCE_TANK);
    public static final Quest WIP_RUNE_WORKBENCH = wip(Region.CIRCLE, "rune_workbench", Items.CRAFTER, Quest.Aura.HEXTECH, PEDESTAL, HEXTECH_GEMSTONE);
    public static final Quest WIP_HEXTECH_AXE = wip(Region.CIRCLE, "hextech_axe", Items.IRON_AXE, Quest.Aura.HEXTECH, HEXTECH_PICKAXE);
    public static final Quest WIP_HEXTECH_HOE = wip(Region.CIRCLE, "hextech_hoe", Items.IRON_HOE, Quest.Aura.LIFE, HEXTECH_PICKAXE);

    // ---- Hex pylons: items carried by hextech, gathered off the ground, thrown to links, passed on by relays ----------

    public static final Quest GATHERING_PYLON = item(Region.PYLONS, "gathering_pylon", ModItems.GATHERING_PYLON, Quest.Aura.HEXTECH,
            QuestDisplay.infusion("hextale:gathering_pylon"), HEXTECH_GEMSTONE);
    public static final Quest SENDING_PYLON = item(Region.PYLONS, "sending_pylon", ModItems.SENDING_PYLON, Quest.Aura.GOLD,
            QuestDisplay.infusion("hextale:sending_pylon"), HEXTECH_GEMSTONE);
    public static final Quest RELAY_PYLON = item(Region.PYLONS, "relay_pylon", ModItems.RELAY_PYLON, Quest.Aura.HEXTECH,
            QuestDisplay.infusion("hextale:relay_pylon"), SENDING_PYLON);
    public static final Quest SIPHON_PYLON = item(Region.PYLONS, "siphon_pylon", ModItems.SIPHON_PYLON, Quest.Aura.HEXTECH,
            QuestDisplay.infusion("hextale:siphon_pylon"), HEXTECH_GEMSTONE, ESSENCE_TANK, BRASS_PIPE);
    public static final Quest PYLON_FILTER = item(Region.PYLONS, "pylon_filter", ModItems.PYLON_FILTER, Quest.Aura.NONE,
            QuestDisplay.shaped("hextale:pylon_filter", 1, new String[]{"N N", " S ", "N N"}, "N=hextale:brass_nugget", "S=minecraft:string"),
            GATHERING_PYLON);
    public static final Quest ESSENCE_STICKER = item(Region.PYLONS, "essence_sticker", ModItems.ESSENCE_STICKER, Quest.Aura.NONE,
            QuestDisplay.shapeless("hextale:essence_sticker", 6, "minecraft:paper", "minecraft:paper", "minecraft:paper", "minecraft:slime_ball"),
            SIPHON_PYLON);
    public static final Quest PYLON_RUNE = add(Region.PYLONS, "pylon_rune", Quest.Kind.ITEM, List.of(GATHERING_PYLON, RUNE_STELE),
            () -> new ItemStack(ModItems.RUNE_STELE.get()), Quests.pylonRunes(1), QuestDisplay.pylonRunes(), null, Quest.Aura.NONE);

    // ---- Rune lore. Chapter I runes open straight from Smelting; each starts a theme of its own ---------------------

    // The forge: heat and metal.
    public static final Quest TEMPERING = rune(Region.FORGE, "earth_fire", Quest.Aura.FLAME, SMELTING);
    public static final Quest FORGE = rune(Region.FORGE, "metal_fire", Quest.Aura.FLAME, TEMPERING);
    public static final Quest REINFORCEMENT = rune(Region.FORGE, "earth_metal", Quest.Aura.NONE, TEMPERING);
    public static final Quest REFORGING = rune(Region.FORGE, "fire_metal", Quest.Aura.NONE, FORGE);
    public static final Quest MOTOR = rune(Region.FORGE, "metal_spark", Quest.Aura.STORM, FORGE);
    public static final Quest BLADES = rune(Region.FORGE, "metal_air", Quest.Aura.NONE, REFORGING);
    public static final Quest MAGNET = rune(Region.FORGE, "spark_metal", Quest.Aura.STORM, MOTOR);
    public static final Quest LOCK = rune(Region.FORGE, "metal_shadow", Quest.Aura.CURSED, REINFORCEMENT);
    public static final Quest WIP_RUNE_LOCK = wip(Region.FORGE, "rune_lock", Items.TRIAL_KEY, Quest.Aura.CURSED, LOCK);

    // Fire: bellows, flames and blasts.
    public static final Quest BELLOWS = rune(Region.FIRE, "air_fire", Quest.Aura.NONE, TEMPERING);
    public static final Quest FLAMETHROWER = rune(Region.FIRE, "fire_air", Quest.Aura.FLAME, BELLOWS);
    public static final Quest EXPLOSION = rune(Region.FIRE, "fire_spark", Quest.Aura.FLAME, BELLOWS);
    public static final Quest SCORCH = rune(Region.FIRE, "fire_life", Quest.Aura.FLAME, FLAMETHROWER);
    public static final Quest DETONATOR = rune(Region.FIRE, "spark_fire", Quest.Aura.STORM, EXPLOSION);
    public static final Quest WIP_FLAMETHROWER = wip(Region.FIRE, "hex_flamethrower", Items.FIRE_CHARGE, Quest.Aura.FLAME, FLAMETHROWER);
    public static final Quest WIP_MINING_CHARGE = wip(Region.FIRE, "mining_charge", Items.TNT, Quest.Aura.FLAME, EXPLOSION, DETONATOR);

    // Waters: quenching, steam, pumps and what water eats away.
    public static final Quest QUENCHING = rune(Region.WATERS, "water_fire", Quest.Aura.NONE, TEMPERING);
    public static final Quest STEAM = rune(Region.WATERS, "fire_water", Quest.Aura.NONE, QUENCHING);
    public static final Quest PUMP_RUNE = rune(Region.WATERS, "air_water", Quest.Aura.NONE, QUENCHING);
    public static final Quest HEAT_EATER = rune(Region.WATERS, "shadow_fire", Quest.Aura.CURSED, QUENCHING);
    public static final Quest FOG = rune(Region.WATERS, "water_air", Quest.Aura.NONE, STEAM);
    public static final Quest PIPES = rune(Region.WATERS, "metal_water", Quest.Aura.NONE, PUMP_RUNE);
    public static final Quest CORROSION = rune(Region.WATERS, "water_metal", Quest.Aura.NONE, PUMP_RUNE);
    public static final Quest WHIRLPOOL = rune(Region.WATERS, "water_shadow", Quest.Aura.CURSED, FOG);
    public static final Quest HUSH = rune(Region.WATERS, "air_shadow", Quest.Aura.CURSED, FOG);
    public static final Quest INVISIBILITY = rune(Region.WATERS, "shadow_air", Quest.Aura.CURSED, HUSH);
    public static final Quest BLIGHT = rune(Region.WATERS, "shadow_metal", Quest.Aura.CURSED, CORROSION);
    public static final Quest WIP_QUENCH_TANK = wip(Region.WATERS, "quench_tank", Items.WATER_BUCKET, Quest.Aura.NONE, QUENCHING);
    public static final Quest WIP_STEAM_BOILER = wip(Region.WATERS, "steam_boiler", Items.CAULDRON, Quest.Aura.NONE, STEAM);
    public static final Quest WIP_COOLER = wip(Region.WATERS, "cooler", Items.BLUE_ICE, Quest.Aura.NONE, HEAT_EATER);
    public static final Quest WIP_VEIL = wip(Region.WATERS, "veil", Items.PHANTOM_MEMBRANE, Quest.Aura.CURSED, INVISIBILITY);
    public static final Quest WIP_RUNE_PUMP = wip(Region.WATERS, "rune_pump", Items.HOPPER, Quest.Aura.NONE, PUMP_RUNE, PIPES);
    public static final Quest WIP_ACID_BATH = wip(Region.WATERS, "acid_bath", Items.FERMENTED_SPIDER_EYE, Quest.Aura.CURSED, CORROSION, BLIGHT);
    public static final Quest WIP_MUTATION_INJECTOR = wip(Region.WATERS, "mutation_injector", Items.SPIDER_EYE, Quest.Aura.CURSED, BLIGHT);
    public static final Quest WIP_CRYSTAL_VAT = wip(Region.WATERS, "crystal_vat", Items.BUDDING_AMETHYST, Quest.Aura.CURSED, CORROSION,
            HEXTECH_GEMSTONE);

    // The deep earth: what the pickaxe digs.
    public static final Quest EROSION = rune(Region.EARTH, "water_earth", Quest.Aura.NONE, SMELTING);
    public static final Quest INSIGHT = rune(Region.EARTH, "light_earth", Quest.Aura.NONE, SMELTING);
    public static final Quest DRILL = rune(Region.EARTH, "metal_earth", Quest.Aura.NONE, EROSION);
    public static final Quest DAM = rune(Region.EARTH, "earth_water", Quest.Aura.NONE, EROSION);
    public static final Quest GEMSTONE = rune(Region.EARTH, "earth_light", Quest.Aura.NONE, INSIGHT);
    public static final Quest CACHE = rune(Region.EARTH, "earth_shadow", Quest.Aura.CURSED, GEMSTONE);
    public static final Quest PASSAGE = rune(Region.EARTH, "shadow_earth", Quest.Aura.CURSED, DRILL);
    public static final Quest QUAKE = rune(Region.EARTH, "spark_earth", Quest.Aura.STORM, DRILL);
    public static final Quest SPONGE = rune(Region.EARTH, "shadow_water", Quest.Aura.CURSED, DAM);
    public static final Quest WIP_DRAINER = wip(Region.EARTH, "drainer", Items.SPONGE, Quest.Aura.NONE, SPONGE);
    public static final Quest WIP_XP_CONDENSER = wip(Region.EARTH, "xp_condenser", Items.SCULK_CATALYST, Quest.Aura.HEXTECH, INSIGHT);
    public static final Quest WIP_ORE_WASHER = wip(Region.EARTH, "ore_washer", Items.RAW_IRON, Quest.Aura.NONE, EROSION, PUMP);
    public static final Quest WIP_HEX_BORE = wip(Region.EARTH, "hex_bore", Items.DIAMOND_PICKAXE, Quest.Aura.HEXTECH, DRILL, MOTOR);
    public static final Quest WIP_SHADOW_COFFER = wip(Region.EARTH, "shadow_coffer", Items.ENDER_CHEST, Quest.Aura.CURSED, CACHE);

    // Light: lenses, beams and mirrors.
    public static final Quest LENS = rune(Region.LIGHT, "water_light", Quest.Aura.HEXTECH, SMELTING);
    public static final Quest BEAM = rune(Region.LIGHT, "light_fire", Quest.Aura.NONE, LENS);
    public static final Quest MIRROR = rune(Region.LIGHT, "metal_light", Quest.Aura.NONE, LENS);
    public static final Quest PURIFICATION = rune(Region.LIGHT, "light_water", Quest.Aura.NONE, LENS);
    public static final Quest ETERNAL_FLAME = rune(Region.LIGHT, "fire_light", Quest.Aura.FLAME, BEAM);
    public static final Quest SUNLIGHT_CELL = rune(Region.LIGHT, "light_spark", Quest.Aura.STORM, BEAM);
    public static final Quest BANISHING = rune(Region.LIGHT, "light_shadow", Quest.Aura.CURSED, BEAM);
    public static final Quest SIGNAL = rune(Region.LIGHT, "light_air", Quest.Aura.NONE, MIRROR);
    public static final Quest ENGRAVING = rune(Region.LIGHT, "light_metal", Quest.Aura.NONE, MIRROR);
    public static final Quest MIRAGE = rune(Region.LIGHT, "air_light", Quest.Aura.NONE, SIGNAL);
    public static final Quest FLASH = rune(Region.LIGHT, "spark_light", Quest.Aura.STORM, SUNLIGHT_CELL);
    public static final Quest ECLIPSE = rune(Region.LIGHT, "shadow_light", Quest.Aura.CURSED, BANISHING);
    public static final Quest PURIFYING_FIRE = rune(Region.LIGHT, "fire_shadow", Quest.Aura.CURSED, ETERNAL_FLAME);
    public static final Quest WIP_STAR_ROD = wip(Region.LIGHT, "star_rod", Items.END_ROD, Quest.Aura.HEXTECH, ETERNAL_FLAME, HEXTECH_LAMP);
    public static final Quest WIP_MAGMA_FONT = wip(Region.LIGHT, "magma_font", Items.LAVA_BUCKET, Quest.Aura.FLAME, ETERNAL_FLAME);
    public static final Quest WIP_BEAM_RELAY = wip(Region.LIGHT, "beam_relay", Items.SPYGLASS, Quest.Aura.NONE, MIRROR, BEAM);
    public static final Quest WIP_HEX_CUTTER = wip(Region.LIGHT, "hex_cutter", Items.STONECUTTER, Quest.Aura.NONE, BEAM, BLADES);
    public static final Quest WIP_SIGNAL_BEACON = wip(Region.LIGHT, "signal_beacon", Items.BEACON, Quest.Aura.NONE, SIGNAL);
    public static final Quest WIP_MIRAGE_PROJECTOR = wip(Region.LIGHT, "mirage_projector", Items.TINTED_GLASS, Quest.Aura.NONE, MIRAGE);
    public static final Quest WIP_RUNE_ENGRAVER = wip(Region.LIGHT, "rune_engraver", Items.BRUSH, Quest.Aura.NONE, ENGRAVING);
    public static final Quest WIP_FLASH_CHARGE = wip(Region.LIGHT, "flash_charge", Items.FIREWORK_STAR, Quest.Aura.STORM, FLASH);
    public static final Quest WIP_PURIFIER = wip(Region.LIGHT, "purifier", Items.CONDUIT, Quest.Aura.HEXTECH, PURIFICATION, PURIFYING_FIRE);
    public static final Quest WIP_HEX_ARCHIVE = wip(Region.LIGHT, "hex_archive", Items.CHISELED_BOOKSHELF, Quest.Aura.HEXTECH, ENGRAVING, CACHE);

    // The storm: grounding lightning, conducting it, lifting things.
    public static final Quest GROUNDING = rune(Region.STORM, "earth_spark", Quest.Aura.STORM, SMELTING);
    public static final Quest CONDUCTOR = rune(Region.STORM, "water_spark", Quest.Aura.STORM, GROUNDING);
    public static final Quest GRAVITY = rune(Region.STORM, "earth_air", Quest.Aura.NONE, GROUNDING);
    public static final Quest WINDMILL = rune(Region.STORM, "air_spark", Quest.Aura.STORM, CONDUCTOR);
    public static final Quest SHOCK = rune(Region.STORM, "spark_water", Quest.Aura.STORM, CONDUCTOR);
    public static final Quest INSULATOR = rune(Region.STORM, "shadow_spark", Quest.Aura.CURSED, CONDUCTOR);
    public static final Quest CHAIN_LIGHTNING = rune(Region.STORM, "spark_air", Quest.Aura.STORM, SHOCK);
    public static final Quest LIFT = rune(Region.STORM, "air_metal", Quest.Aura.NONE, GRAVITY);
    public static final Quest SHIFT = rune(Region.STORM, "air_earth", Quest.Aura.NONE, GRAVITY);
    public static final Quest BLINK = rune(Region.STORM, "spark_shadow", Quest.Aura.CURSED, SHIFT);
    public static final Quest WIP_STORM_COLLECTOR = wip(Region.STORM, "storm_collector", Items.LIGHTNING_ROD.asList().getFirst(), Quest.Aura.STORM,
            GROUNDING);
    public static final Quest WIP_WORLD_ANCHOR = wip(Region.STORM, "world_anchor", Items.RESPAWN_ANCHOR, Quest.Aura.STORM, GROUNDING, GRAVITY);
    public static final Quest WIP_CRYSTAL_BATTERY = wip(Region.STORM, "crystal_battery", Items.AMETHYST_CLUSTER, Quest.Aura.STORM, CONDUCTOR,
            SUNLIGHT_CELL);
    public static final Quest WIP_WIND_TURBINE = wip(Region.STORM, "wind_turbine", Items.BREEZE_ROD, Quest.Aura.NONE, WINDMILL);
    public static final Quest WIP_LEVITATION_PLATE = wip(Region.STORM, "levitation_plate", Items.HEAVY_WEIGHTED_PRESSURE_PLATE, Quest.Aura.HEXTECH,
            LIFT, GRAVITY);
    public static final Quest WIP_HEX_LIFT = wip(Region.STORM, "hex_lift", Items.PISTON, Quest.Aura.NONE, LIFT);
    public static final Quest WIP_HEX_GATE = wip(Region.STORM, "hex_gate", Items.ENDER_EYE, Quest.Aura.HEXTECH, SHIFT, BLINK);
    public static final Quest WIP_FOLDING_GATE = wip(Region.STORM, "folding_gate", Items.ENDER_PEARL, Quest.Aura.CURSED, BLINK);
    public static final Quest WIP_HEX_TURRET = wip(Region.STORM, "hex_turret", Items.DISPENSER, Quest.Aura.STORM, CHAIN_LIGHTNING, BEAM);
    public static final Quest WIP_HEX_BLADE = wip(Region.STORM, "hex_blade", Items.IRON_SWORD, Quest.Aura.STORM, CHAIN_LIGHTNING, BLADES);
    public static final Quest WIP_NULLSTONE = wip(Region.STORM, "nullstone", Items.CRYING_OBSIDIAN, Quest.Aura.CURSED, INSULATOR, ECLIPSE);
    public static final Quest WIP_RAIN_CALLER = wip(Region.STORM, "rain_caller", Items.TRIDENT, Quest.Aura.STORM, CONDUCTOR, FOG);
    public static final Quest WIP_ESSENCE_RESONATOR = wip(Region.STORM, "essence_resonator", Items.GLASS_BOTTLE, Quest.Aura.HEXTECH, CONDUCTOR,
            PIPES);
    public static final Quest WIP_BARRIER_WARD = wip(Region.FORGE, "barrier_ward", Items.SHIELD, Quest.Aura.HEXTECH, REINFORCEMENT, HUSH);

    // Life: growth, healing, constructs - and the Hexcore at the very end.
    public static final Quest FERTILITY = rune(Region.LIFE, "earth_life", Quest.Aura.LIFE, EROSION);
    public static final Quest GROWTH = rune(Region.LIFE, "life_earth", Quest.Aura.LIFE, FERTILITY);
    public static final Quest SPRING = rune(Region.LIFE, "life_water", Quest.Aura.LIFE, FERTILITY);
    public static final Quest POLLEN = rune(Region.LIFE, "life_air", Quest.Aura.LIFE, GROWTH);
    public static final Quest PHOTOSYNTHESIS = rune(Region.LIFE, "light_life", Quest.Aura.LIFE, GROWTH);
    public static final Quest BREATH = rune(Region.LIFE, "air_life", Quest.Aura.LIFE, GROWTH);
    public static final Quest RADIANCE = rune(Region.LIFE, "life_light", Quest.Aura.LIFE, PHOTOSYNTHESIS);
    public static final Quest HEALING = rune(Region.LIFE, "water_life", Quest.Aura.LIFE, SPRING);
    public static final Quest FIREPROOFING = rune(Region.LIFE, "life_fire", Quest.Aura.NONE, HEALING);
    public static final Quest MENDING = rune(Region.LIFE, "life_metal", Quest.Aura.NONE, HEALING);
    public static final Quest REVIVAL = rune(Region.LIFE, "life_spark", Quest.Aura.STORM, HEALING);
    public static final Quest CONSTRUCT = rune(Region.LIFE, "metal_life", Quest.Aura.NONE, MENDING);
    public static final Quest HASTE = rune(Region.LIFE, "spark_life", Quest.Aura.STORM, REVIVAL);
    public static final Quest SLUMBER = rune(Region.LIFE, "life_shadow", Quest.Aura.CURSED, BREATH);
    public static final Quest WITHERING = rune(Region.LIFE, "shadow_life", Quest.Aura.CURSED, SLUMBER);
    public static final Quest WIP_HERD_WARDEN = wip(Region.LIFE, "herd_warden", Items.SHEARS, Quest.Aura.LIFE, FERTILITY);
    public static final Quest WIP_SPRING_BASIN = wip(Region.LIFE, "spring_basin", Items.LILY_PAD, Quest.Aura.LIFE, SPRING);
    public static final Quest WIP_BLOOM_CENSER = wip(Region.LIFE, "bloom_censer", Items.SPORE_BLOSSOM, Quest.Aura.LIFE, POLLEN);
    public static final Quest WIP_GREENHOUSE = wip(Region.LIFE, "greenhouse", Items.FLOWERING_AZALEA, Quest.Aura.LIFE, GROWTH, PHOTOSYNTHESIS);
    public static final Quest WIP_GLOWBLOOM_BED = wip(Region.LIFE, "glowbloom_bed", Items.GLOW_BERRIES, Quest.Aura.LIFE, RADIANCE);
    public static final Quest WIP_HEALING_SPRING = wip(Region.LIFE, "healing_spring", Items.GLISTERING_MELON_SLICE, Quest.Aura.LIFE, HEALING);
    public static final Quest WIP_REGEN_TANK = wip(Region.LIFE, "regen_tank", Items.AXOLOTL_BUCKET, Quest.Aura.CURSED, HEALING, PIPES);
    public static final Quest WIP_RESPIRATOR = wip(Region.LIFE, "respirator", Items.LEATHER_HELMET, Quest.Aura.NONE, BREATH, PURIFICATION);
    public static final Quest WIP_FIREPROOF_COAT = wip(Region.LIFE, "fireproof_coat", Items.MAGMA_CREAM, Quest.Aura.FLAME, FIREPROOFING, SCORCH);
    public static final Quest WIP_RUNE_ANVIL = wip(Region.LIFE, "rune_anvil", Items.ANVIL, Quest.Aura.NONE, MENDING, REFORGING);
    public static final Quest WIP_CONSTRUCT = wip(Region.LIFE, "construct", Items.ARMOR_STAND, Quest.Aura.HEXTECH, CONSTRUCT, MOTOR);
    public static final Quest WIP_HARVEST_CONSTRUCT = wip(Region.LIFE, "harvest_construct", Items.GOLDEN_HOE, Quest.Aura.HEXTECH, CONSTRUCT, BLADES);
    public static final Quest WIP_FORESTER_CONSTRUCT = wip(Region.LIFE, "forester_construct", Items.GOLDEN_AXE, Quest.Aura.HEXTECH, CONSTRUCT,
            GROWTH);
    public static final Quest WIP_WINGED_COURIER = wip(Region.LIFE, "winged_courier", Items.FEATHER, Quest.Aura.HEXTECH, CONSTRUCT, LIFT);
    public static final Quest WIP_LULL_LANTERN = wip(Region.LIFE, "lull_lantern", Items.SOUL_LANTERN, Quest.Aura.CURSED, SLUMBER);
    public static final Quest WIP_CHRONO_CHAMBER = wip(Region.LIFE, "chrono_chamber", Items.CLOCK, Quest.Aura.STORM, HASTE);
    public static final Quest WIP_SHIMMER = wip(Region.LIFE, "shimmer", Items.EXPERIENCE_BOTTLE, Quest.Aura.CURSED, HASTE, WITHERING);
    public static final Quest WIP_HEXCORE = wip(Region.LIFE, "hexcore", Items.HEART_OF_THE_SEA, Quest.Aura.GOLD, REVIVAL, CONSTRUCT, RADIANCE);

    // A device the pylons and the light will share.
    public static final Quest WIP_SORTING_MATRIX = wip(Region.PYLONS, "sorting_matrix", Items.OBSERVER, Quest.Aura.HEXTECH, SENDING_PYLON, MIRROR);

    static {
        layout();
    }

    private Guide() {
    }

    // ---- Building -----------------------------------------------------------------------------------------------------

    private static Quest add(Region region, String id, Quest.Kind kind, List<Quest> parents, Supplier<ItemStack> icon,
                             Predicate<ServerPlayer> goal, @Nullable QuestDisplay display, @Nullable ComplexRune rune, Quest.Aura aura) {
        Quest quest = new Quest(id, kind, parents, icon, goal, display, rune, aura, region);
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

    private static Quest item(Region region, String id, Supplier<? extends ItemLike> item, Quest.Aura aura, @Nullable QuestDisplay display,
                              Quest... parents) {
        return add(region, id, Quest.Kind.ITEM, List.of(parents), () -> new ItemStack(item.get()), Quests.has(() -> item.get().asItem()),
                display, null, aura);
    }

    private static Quest rune(Region region, String key, Quest.Aura aura, Quest... parents) {
        ComplexRune rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
        return add(region, key, Quest.Kind.RUNE, List.of(parents), () -> new ItemStack(ModItems.RUNE_SCROLL.get()), player -> false, null,
                rune, aura);
    }

    private static Quest wip(Region region, String id, ItemLike icon, Quest.Aura aura, Quest... parents) {
        return add(region, "wip_" + id, Quest.Kind.WIP, List.of(parents), () -> new ItemStack(icon), player -> false, null, null, aura);
    }

    // ---- Layout -------------------------------------------------------------------------------------------------------

    /**
     * Lays the steps out as loose clusters, one per region, like constellations rather than a grid. First the regions
     * find their places: each takes room for its steps, they push apart and regions linked by steps draw together.
     * Then the steps: a region's first steps sit around its anchor, every other step grows out of its main parent, away
     * from the anchor, its siblings fanned out; and the web relaxes - steps push each other apart (far apart across
     * regions), links pull like springs and every step leans gently towards its anchor. Deterministic, so every reader
     * sees the same sky.
     */
    private static void layout() {
        Map<Region, double[]> anchors = placeRegions();
        int n = ALL.size();
        double[] x = new double[n];
        double[] y = new double[n];
        Map<Region, List<Quest>> entries = new EnumMap<>(Region.class);
        for (Quest quest : ALL) {
            if (!grows(quest)) {
                entries.computeIfAbsent(quest.region(), region -> new ArrayList<>()).add(quest);
            }
        }
        for (Quest quest : ALL) {
            Region region = quest.region();
            double[] anchor = anchors.get(region);
            double angle;
            double cx;
            double cy;
            double distance;
            if (grows(quest)) {
                Quest parent = quest.parent();
                List<Quest> siblings = parent.children().stream().filter(child -> child.parent() == parent && grows(child)).toList();
                cx = x[parent.index];
                cy = y[parent.index];
                double ox = cx - anchor[0];
                double oy = cy - anchor[1];
                int k = siblings.indexOf(quest);
                int m = siblings.size();
                if (ox * ox + oy * oy < 1) {
                    angle = jitter(parent, 0) * Math.PI + k * Math.PI * 2 / m;
                } else {
                    angle = Math.atan2(oy, ox) + (k - (m - 1) / 2.0) * 0.8;
                }
                distance = SPACING;
            } else {
                List<Quest> first = entries.get(region);
                int k = first.indexOf(quest);
                cx = anchor[0];
                cy = anchor[1];
                angle = region.ordinal() * 1.3 + k * Math.PI * 2 / first.size();
                distance = first.size() == 1 ? 0 : SPACING * 0.7;
            }
            x[quest.index] = cx + Math.cos(angle) * distance + jitter(quest, 1) * 8;
            y[quest.index] = cy + Math.sin(angle) * distance + jitter(quest, 2) * 8;
        }
        int steps = 400;
        double[] fx = new double[n];
        double[] fy = new double[n];
        for (int step = 0; step < steps; step++) {
            Arrays.fill(fx, 0);
            Arrays.fill(fy, 0);
            for (int i = 0; i < n; i++) {
                Quest a = ALL.get(i);
                for (int j = i + 1; j < n; j++) {
                    Quest b = ALL.get(j);
                    double want = a.region() == b.region() ? GAP : APART;
                    double dx = x[j] - x[i];
                    double dy = y[j] - y[i];
                    if (Math.abs(dx) >= want || Math.abs(dy) >= want) {
                        continue;
                    }
                    double d = Math.sqrt(dx * dx + dy * dy);
                    if (d < want) {
                        if (d < 0.01) {
                            dx = jitter(b, step) + 0.5;
                            dy = jitter(a, step) - 0.5;
                            d = Math.sqrt(dx * dx + dy * dy);
                        }
                        double push = (want - d) / d * 0.5;
                        fx[i] -= dx * push;
                        fy[i] -= dy * push;
                        fx[j] += dx * push;
                        fy[j] += dy * push;
                    }
                }
                for (Quest parent : a.parents()) {
                    int j = parent.index;
                    double dx = x[j] - x[i];
                    double dy = y[j] - y[i];
                    double d = Math.max(0.01, Math.sqrt(dx * dx + dy * dy));
                    double pull;
                    if (parent.region() != a.region()) {
                        // Across regions only a faint pull, so a region's entries face the regions they come from.
                        pull = d > APART ? (d - APART) / d * 0.004 : 0;
                    } else {
                        pull = (d - SPACING) / d * (parent == a.parent() ? 0.12 : 0.04);
                    }
                    fx[i] += dx * pull;
                    fy[i] += dy * pull;
                    fx[j] -= dx * pull;
                    fy[j] -= dy * pull;
                }
                double[] anchor = anchors.get(a.region());
                fx[i] += (anchor[0] - x[i]) * 0.004;
                fy[i] += (anchor[1] - y[i]) * 0.004;
            }
            double limit = 10 * (1 - 0.85 * step / steps);
            for (int i = 0; i < n; i++) {
                double d = Math.sqrt(fx[i] * fx[i] + fy[i] * fy[i]);
                double scale = d > limit ? limit / d : 1;
                x[i] += fx[i] * scale;
                y[i] += fy[i] * scale;
            }
        }
        // Last, no two steps closer than SEPARATE, whatever the springs wanted.
        for (int pass = 0; pass < 60; pass++) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double dx = x[j] - x[i];
                    double dy = y[j] - y[i];
                    if (Math.abs(dx) >= SEPARATE || Math.abs(dy) >= SEPARATE) {
                        continue;
                    }
                    double d = Math.sqrt(dx * dx + dy * dy);
                    if (d < SEPARATE) {
                        if (d < 0.01) {
                            dx = 1;
                            dy = 0;
                            d = 1;
                        }
                        double push = (SEPARATE - d) / d * 0.5;
                        x[i] -= dx * push;
                        y[i] -= dy * push;
                        x[j] += dx * push;
                        y[j] += dy * push;
                    }
                }
            }
        }
        for (Quest quest : ALL) {
            quest.x = (int) Math.round(x[quest.index]);
            quest.y = (int) Math.round(y[quest.index]);
        }
    }

    /**
     * Where the regions gather: starting from their anchors in {@link Region}, each region takes a round room growing
     * with the square root of its steps; overlapping rooms push apart, regions linked by steps draw together until their
     * rooms touch. The workshop stays put.
     */
    private static Map<Region, double[]> placeRegions() {
        Region[] regions = Region.values();
        int count = regions.length;
        double[] ax = new double[count];
        double[] ay = new double[count];
        double[] radius = new double[count];
        int[][] links = new int[count][count];
        for (Region region : regions) {
            ax[region.ordinal()] = region.x();
            ay[region.ordinal()] = region.y();
        }
        for (Quest quest : ALL) {
            radius[quest.region().ordinal()] += 1;
            for (Quest parent : quest.parents()) {
                if (parent.region() != quest.region()) {
                    links[parent.region().ordinal()][quest.region().ordinal()]++;
                    links[quest.region().ordinal()][parent.region().ordinal()]++;
                }
            }
        }
        for (int i = 0; i < count; i++) {
            radius[i] = RADIUS * Math.sqrt(radius[i]);
        }
        for (int step = 0; step < 400; step++) {
            double[] fx = new double[count];
            double[] fy = new double[count];
            for (int i = 0; i < count; i++) {
                for (int j = i + 1; j < count; j++) {
                    double dx = ax[j] - ax[i];
                    double dy = ay[j] - ay[i];
                    double d = Math.max(0.01, Math.sqrt(dx * dx + dy * dy));
                    double want = radius[i] + radius[j] + MARGIN;
                    double push;
                    if (d < want) {
                        push = (want - d) / d * 0.5;
                    } else if (links[i][j] > 0) {
                        push = -(d - want) / d * 0.05 * Math.min(3, links[i][j]);
                    } else {
                        continue;
                    }
                    fx[i] -= dx * push;
                    fy[i] -= dy * push;
                    fx[j] += dx * push;
                    fy[j] += dy * push;
                }
            }
            for (int i = 0; i < count; i++) {
                if (regions[i] != Region.WORKSHOP) {
                    ax[i] += Math.clamp(fx[i], -12, 12);
                    ay[i] += Math.clamp(fy[i], -12, 12);
                }
            }
        }
        Map<Region, double[]> anchors = new EnumMap<>(Region.class);
        for (Region region : regions) {
            anchors.put(region, new double[]{Math.round(ax[region.ordinal()]), Math.round(ay[region.ordinal()])});
        }
        return anchors;
    }

    /** Whether a step grows out of its main parent in the layout (it shares the parent's region). */
    private static boolean grows(Quest quest) {
        return quest.parent() != null && quest.parent().region() == quest.region();
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

    /** Bounds of the laid-out web (for the camera). */
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
