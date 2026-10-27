package com.pockyl.hextale.registry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.item.HextechGemstoneItem;
import com.pockyl.hextale.item.KnowledgeScrollItem;
import com.pockyl.hextale.item.ResearchersDiaryItem;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.item.UnstableHextechItem;
import com.pockyl.hextale.runework.HextechPickaxe;
import com.pockyl.hextale.runework.HextechPickaxeItem;
import com.pockyl.hextale.runework.HextechToolItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Hextale.MOD_ID);

    public static final DeferredItem<BlockItem> RESEARCH_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.RESEARCH_TABLE);
    public static final DeferredItem<BlockItem> ALEMBIC = ITEMS.registerSimpleBlockItem(ModBlocks.ALEMBIC);
    public static final DeferredItem<BlockItem> SEPARATOR = ITEMS.registerSimpleBlockItem(ModBlocks.SEPARATOR);
    public static final DeferredItem<BlockItem> ESSENCE_TANK = ITEMS.registerSimpleBlockItem(ModBlocks.ESSENCE_TANK);
    public static final DeferredItem<BlockItem> BRASS_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.BRASS_PIPE);
    public static final DeferredItem<BlockItem> CREATIVE_ESSENCE_TANK = ITEMS.registerSimpleBlockItem(ModBlocks.CREATIVE_ESSENCE_TANK);
    public static final DeferredItem<BlockItem> DRAIN_VALVE = ITEMS.registerSimpleBlockItem(ModBlocks.DRAIN_VALVE);
    public static final DeferredItem<BlockItem> PUMP = ITEMS.registerSimpleBlockItem(ModBlocks.PUMP);
    public static final DeferredItem<BlockItem> MAGIC_STONE = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_STONE);
    public static final DeferredItem<BlockItem> POLISHED_MAGIC_STONE = ITEMS.registerSimpleBlockItem(ModBlocks.POLISHED_MAGIC_STONE);
    public static final DeferredItem<BlockItem> MAGIC_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_BRICKS);
    public static final DeferredItem<BlockItem> MAGIC_BRICK_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> MAGIC_BRICK_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_BRICK_SLAB);
    public static final DeferredItem<BlockItem> MAGIC_BRICK_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_BRICK_WALL);
    public static final DeferredItem<BlockItem> CHISELED_MAGIC_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.CHISELED_MAGIC_BRICKS);
    public static final DeferredItem<com.pockyl.hextale.item.NotedBlockItem> DORMANT_HEXTECH = ITEMS.registerItem("dormant_hextech",
            properties -> new com.pockyl.hextale.item.NotedBlockItem(ModBlocks.DORMANT_HEXTECH.get(), properties.useBlockDescriptionPrefix()));
    public static final DeferredItem<com.pockyl.hextale.item.NotedBlockItem> FULGURITE = ITEMS.registerItem("fulgurite",
            properties -> new com.pockyl.hextale.item.NotedBlockItem(ModBlocks.FULGURITE.get(), properties.useBlockDescriptionPrefix()));
    public static final DeferredItem<BlockItem> FULGURITE_SPIKE = ITEMS.registerSimpleBlockItem(ModBlocks.FULGURITE_SPIKE);
    public static final DeferredItem<com.pockyl.hextale.item.NotedBlockItem> HEXTECH_CLUSTER = ITEMS.registerItem("hextech_cluster",
            properties -> new com.pockyl.hextale.item.NotedBlockItem(ModBlocks.HEXTECH_CLUSTER.get(), properties.useBlockDescriptionPrefix()));
    public static final DeferredItem<BlockItem> LARGE_HEXTECH_BUD = ITEMS.registerSimpleBlockItem(ModBlocks.LARGE_HEXTECH_BUD);
    public static final DeferredItem<BlockItem> MEDIUM_HEXTECH_BUD = ITEMS.registerSimpleBlockItem(ModBlocks.MEDIUM_HEXTECH_BUD);
    public static final DeferredItem<BlockItem> SMALL_HEXTECH_BUD = ITEMS.registerSimpleBlockItem(ModBlocks.SMALL_HEXTECH_BUD);
    public static final DeferredItem<BlockItem> FULGURITE_GLASS = ITEMS.registerSimpleBlockItem(ModBlocks.FULGURITE_GLASS);
    public static final DeferredItem<BlockItem> FULGURITE_GLASS_PANE = ITEMS.registerSimpleBlockItem(ModBlocks.FULGURITE_GLASS_PANE);
    public static final DeferredItem<BlockItem> SCORCHED_CALCITE = ITEMS.registerSimpleBlockItem(ModBlocks.SCORCHED_CALCITE);
    public static final DeferredItem<BlockItem> POLISHED_SCORCHED_CALCITE = ITEMS.registerSimpleBlockItem(ModBlocks.POLISHED_SCORCHED_CALCITE);
    public static final DeferredItem<BlockItem> SCORCHED_CALCITE_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.SCORCHED_CALCITE_BRICKS);
    public static final DeferredItem<BlockItem> SCORCHED_CALCITE_BRICK_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.SCORCHED_CALCITE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> SCORCHED_CALCITE_BRICK_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.SCORCHED_CALCITE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> SCORCHED_CALCITE_BRICK_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.SCORCHED_CALCITE_BRICK_WALL);
    public static final DeferredItem<BlockItem> CHISELED_SCORCHED_CALCITE = ITEMS.registerSimpleBlockItem(ModBlocks.CHISELED_SCORCHED_CALCITE);
    public static final DeferredItem<BlockItem> HEXTECH_ALTAR = ITEMS.registerSimpleBlockItem(ModBlocks.HEXTECH_ALTAR,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> HEXTECH_LAMP = ITEMS.registerSimpleBlockItem(ModBlocks.HEXTECH_LAMP);
    public static final DeferredItem<BlockItem> HEX_FURNACE = ITEMS.registerSimpleBlockItem(ModBlocks.HEX_FURNACE,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> HEX_FOUNDRY = ITEMS.registerSimpleBlockItem(ModBlocks.HEX_FOUNDRY,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> FIRECLAY_BALL = ITEMS.registerSimpleItem("fireclay_ball");
    public static final DeferredItem<Item> FIRE_BRICK = ITEMS.registerSimpleItem("fire_brick");
    public static final DeferredItem<BlockItem> FIRE_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_BRICKS);
    public static final DeferredItem<BlockItem> FIRE_BRICK_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> FIRE_BRICK_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> FIRE_BRICK_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_BRICK_WALL);
    public static final DeferredItem<BlockItem> HEX_WORKBENCH = ITEMS.registerSimpleBlockItem(ModBlocks.HEX_WORKBENCH);
    public static final DeferredItem<BlockItem> HEX_LAPIDARY = ITEMS.registerSimpleBlockItem(ModBlocks.HEX_LAPIDARY);
    public static final DeferredItem<BlockItem> CRUCIBLE = ITEMS.registerSimpleBlockItem(ModBlocks.CRUCIBLE);
    public static final DeferredItem<BlockItem> CHEM_BURNER = ITEMS.registerSimpleBlockItem(ModBlocks.CHEM_BURNER,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> CHEM_GLASS = ITEMS.registerSimpleBlockItem(ModBlocks.CHEM_GLASS);
    public static final DeferredItem<BlockItem> CHEM_GLASS_PANE = ITEMS.registerSimpleBlockItem(ModBlocks.CHEM_GLASS_PANE);
    public static final DeferredItem<BlockItem> RUSTED_PLATING = ITEMS.registerSimpleBlockItem(ModBlocks.RUSTED_PLATING);
    public static final DeferredItem<BlockItem> RUSTED_PLATING_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.RUSTED_PLATING_STAIRS);
    public static final DeferredItem<BlockItem> RUSTED_PLATING_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.RUSTED_PLATING_SLAB);
    public static final DeferredItem<BlockItem> RIVETED_RUSTED_PLATING = ITEMS.registerSimpleBlockItem(ModBlocks.RIVETED_RUSTED_PLATING);
    public static final DeferredItem<BlockItem> SHIMMERBLOOM = ITEMS.registerSimpleBlockItem(ModBlocks.SHIMMERBLOOM);
    public static final DeferredItem<BlockItem> SHIMMER_MOSS = ITEMS.registerSimpleBlockItem(ModBlocks.SHIMMER_MOSS);
    public static final DeferredItem<BlockItem> SHIMMER_MOSS_CARPET = ITEMS.registerSimpleBlockItem(ModBlocks.SHIMMER_MOSS_CARPET);
    public static final DeferredItem<BlockItem> AXOLOTL_TANK = ITEMS.registerSimpleBlockItem(ModBlocks.AXOLOTL_TANK,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> LAB_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.LAB_BENCH,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> CHEM_STILL = ITEMS.registerSimpleBlockItem(ModBlocks.CHEM_STILL,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> PINK_BRAZIER = ITEMS.registerSimpleBlockItem(ModBlocks.PINK_BRAZIER,
            properties -> properties.rarity(Rarity.UNCOMMON));
    /** What an axolotl in a tank sheds as it heals: the living half of shimmer. */
    public static final DeferredItem<Item> REGENERATIVE_SLIME = ITEMS.registerSimpleItem("regenerative_slime");
    /** A hextech crystal grown in refined shimmer: cheaper and weaker than a cut one. */
    public static final DeferredItem<Item> SYNTHETIC_CRYSTAL = ITEMS.registerSimpleItem("synthetic_crystal");
    /** A stoppered glass vial of a crucible brew (component {@code hextale:brew}). */
    public static final DeferredItem<com.pockyl.hextale.chem.ChemVialItem> CHEM_VIAL = ITEMS.registerItem("chem_vial",
            properties -> new com.pockyl.hextale.chem.ChemVialItem(properties.stacksTo(16)
                    .craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE)));
    /** An iron disc edged with flint: cuts raw hextech into crystals on the lapidary; wears with every cut and crack. */
    public static final DeferredItem<Item> CUTTING_DISC = ITEMS.registerItem("cutting_disc", properties -> new Item(properties.durability(64)));
    /** A hexsteel disc edged with diamond: finishes crystals into stabilised gemstones, and cuts raw ones twice as fast. */
    public static final DeferredItem<Item> MASTER_CUTTING_DISC = ITEMS.registerItem("master_cutting_disc",
            properties -> new Item(properties.durability(256).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BlockItem> SPHALERITE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.SPHALERITE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_SPHALERITE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_SPHALERITE_ORE);
    public static final DeferredItem<BlockItem> RAW_ZINC_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.RAW_ZINC_BLOCK);
    public static final DeferredItem<BlockItem> ZINC_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.ZINC_BLOCK);
    public static final DeferredItem<BlockItem> HEXSTEEL_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.HEXSTEEL_BLOCK);
    public static final DeferredItem<Item> RAW_ZINC = ITEMS.registerSimpleItem("raw_zinc");
    public static final DeferredItem<Item> ZINC_INGOT = ITEMS.registerSimpleItem("zinc_ingot");
    public static final DeferredItem<Item> ZINC_NUGGET = ITEMS.registerSimpleItem("zinc_nugget");
    /** Iron melted with coal and hextech dust in the Hex Furnace: the metal of the better hextech tools. */
    public static final DeferredItem<Item> HEXSTEEL_INGOT = ITEMS.registerSimpleItem("hexsteel_ingot");
    public static final DeferredItem<BlockItem> BRASS_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.BRASS_BLOCK);
    public static final DeferredItem<BlockItem> GATHERING_PYLON = ITEMS.registerSimpleBlockItem(ModBlocks.GATHERING_PYLON,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> SENDING_PYLON = ITEMS.registerSimpleBlockItem(ModBlocks.SENDING_PYLON,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> RELAY_PYLON = ITEMS.registerSimpleBlockItem(ModBlocks.RELAY_PYLON,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> SIPHON_PYLON = ITEMS.registerSimpleBlockItem(ModBlocks.SIPHON_PYLON,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> SORTING_MATRIX = ITEMS.registerSimpleBlockItem(ModBlocks.SORTING_MATRIX,
            properties -> properties.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> MAGIC_STONE_PILLAR = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_STONE_PILLAR);
    // The magic tree's wood set; the sprout comes out of a crucible.
    public static final DeferredItem<BlockItem> MAGIC_SAPLING = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_SAPLING,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> MAGIC_LEAVES = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_LEAVES);
    public static final DeferredItem<BlockItem> MAGIC_LOG = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_LOG);
    public static final DeferredItem<BlockItem> MAGIC_WOOD = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_WOOD);
    public static final DeferredItem<BlockItem> STRIPPED_MAGIC_LOG = ITEMS.registerSimpleBlockItem(ModBlocks.STRIPPED_MAGIC_LOG);
    public static final DeferredItem<BlockItem> STRIPPED_MAGIC_WOOD = ITEMS.registerSimpleBlockItem(ModBlocks.STRIPPED_MAGIC_WOOD);
    public static final DeferredItem<BlockItem> MAGIC_PLANKS = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_PLANKS);
    public static final DeferredItem<BlockItem> MAGIC_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_STAIRS);
    public static final DeferredItem<BlockItem> MAGIC_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_SLAB);
    public static final DeferredItem<BlockItem> MAGIC_FENCE = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_FENCE);
    public static final DeferredItem<BlockItem> MAGIC_FENCE_GATE = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_FENCE_GATE);
    public static final DeferredItem<net.minecraft.world.item.DoubleHighBlockItem> MAGIC_DOOR = ITEMS.registerItem("magic_door",
            properties -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.MAGIC_DOOR.get(), properties.useBlockDescriptionPrefix()));
    public static final DeferredItem<BlockItem> MAGIC_TRAPDOOR = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_TRAPDOOR);
    public static final DeferredItem<BlockItem> MAGIC_BUTTON = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_BUTTON);
    public static final DeferredItem<BlockItem> MAGIC_PRESSURE_PLATE = ITEMS.registerSimpleBlockItem(ModBlocks.MAGIC_PRESSURE_PLATE);
    /** Rules what a pylon moves: white or black list, numbers to keep or to fill up to. */
    public static final DeferredItem<com.pockyl.hextale.item.PylonFilterItem> PYLON_FILTER = ITEMS.registerItem("pylon_filter",
            properties -> new com.pockyl.hextale.item.PylonFilterItem(properties.stacksTo(16)));
    /** A label with one essence: a jar wearing it takes only that essence, a siphon or relay carries only it. */
    public static final DeferredItem<com.pockyl.hextale.item.EssenceStickerItem> ESSENCE_STICKER = ITEMS.registerItem("essence_sticker",
            com.pockyl.hextale.item.EssenceStickerItem::new);
    /** One essence as an item: what recipes ask for and what items are made of, shown in JEI and the creative tab. */
    public static final DeferredItem<com.pockyl.hextale.item.EssenceItem> ESSENCE = ITEMS.registerItem("essence",
            com.pockyl.hextale.item.EssenceItem::new);
    public static final DeferredItem<BlockItem> BRASS_GRATE = ITEMS.registerSimpleBlockItem(ModBlocks.BRASS_GRATE);
    public static final DeferredItem<BlockItem> WHIRL_REAPER = ITEMS.registerSimpleBlockItem(ModBlocks.WHIRL_REAPER,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> HEX_SAWMILL = ITEMS.registerSimpleBlockItem(ModBlocks.HEX_SAWMILL,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> GREEDY_FUNNEL = ITEMS.registerSimpleBlockItem(ModBlocks.GREEDY_FUNNEL,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> ARC_SPIRE = ITEMS.registerSimpleBlockItem(ModBlocks.ARC_SPIRE,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> RUNE_SHACKLES = ITEMS.registerSimpleBlockItem(ModBlocks.RUNE_SHACKLES);
    public static final DeferredItem<BlockItem> HEXTECH_PURIFIER = ITEMS.registerSimpleBlockItem(ModBlocks.HEXTECH_PURIFIER,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> VOID_ANCHOR = ITEMS.registerSimpleBlockItem(ModBlocks.VOID_ANCHOR,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> XP_CONDENSER = ITEMS.registerSimpleBlockItem(ModBlocks.XP_CONDENSER,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> MIASMA_VENT = ITEMS.registerSimpleBlockItem(ModBlocks.MIASMA_VENT);
    public static final DeferredItem<BlockItem> FIELD_WARD = ITEMS.registerSimpleBlockItem(ModBlocks.FIELD_WARD);
    public static final DeferredItem<BlockItem> WHORLED_MAGIC_STONE = ITEMS.registerSimpleBlockItem(ModBlocks.WHORLED_MAGIC_STONE);
    /** Creative only: everything known at once - every rune with three stars, every item studied, every quest done. */
    public static final DeferredItem<KnowledgeScrollItem> SCROLL_OF_OMNISCIENCE = ITEMS.registerItem("scroll_of_omniscience",
            properties -> new KnowledgeScrollItem(true, properties.stacksTo(1).rarity(Rarity.EPIC)));
    /** Creative only for now: a sliver of the Void that breaks it open where it is pointed. */
    public static final DeferredItem<com.pockyl.hextale.item.VoidShardItem> VOID_SHARD = ITEMS.registerItem("void_shard",
            properties -> new com.pockyl.hextale.item.VoidShardItem(properties.stacksTo(1).rarity(Rarity.EPIC)));
    /** Creative only: everything forgotten - runes, studied items, quests, parchments. */
    public static final DeferredItem<KnowledgeScrollItem> SCROLL_OF_OBLIVION = ITEMS.registerItem("scroll_of_oblivion",
            properties -> new KnowledgeScrollItem(false, properties.stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> PEDESTAL = ITEMS.registerSimpleBlockItem(ModBlocks.PEDESTAL);
    public static final DeferredItem<BlockItem> RUNE_STELE = ITEMS.registerSimpleBlockItem(ModBlocks.RUNE_STELE);
    public static final DeferredItem<BlockItem> HEXCORE = ITEMS.registerSimpleBlockItem(ModBlocks.HEXCORE,
            properties -> properties.rarity(Rarity.EPIC));
    /** A crafting recipe that uses a bottle of essence (soaking magic stone) gives the glass bottle back. */
    public static final DeferredItem<EssenceFlaskItem> ESSENCE_FLASK = ITEMS.registerItem("essence_flask",
            properties -> new EssenceFlaskItem(properties.stacksTo(1).craftRemainder(Items.GLASS_BOTTLE)
                    .component(net.minecraft.core.component.DataComponents.CONSUMABLE, net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)
                    .usingConvertsTo(Items.GLASS_BOTTLE)));
    public static final DeferredItem<Item> BRASS_INGOT = ITEMS.registerSimpleItem("brass_ingot");
    public static final DeferredItem<Item> BRASS_NUGGET = ITEMS.registerSimpleItem("brass_nugget");
    /** Lightning-born: neither fire nor further lightning harms it. */
    public static final DeferredItem<UnstableHextechItem> UNSTABLE_HEXTECH = ITEMS.registerItem("unstable_hextech",
            properties -> new UnstableHextechItem(properties.rarity(Rarity.UNCOMMON).fireResistant()));
    /** What a thrown or bursting orb leaves: glittering hextech dust (wakes a crafting table, a furnace, a cauldron). */
    public static final DeferredItem<HextechGemstoneItem> HEXTECH_DUST = ITEMS.registerItem("hextech_dust",
            properties -> new HextechGemstoneItem(properties.fireResistant()));
    /** Unstable hextech cut on the lapidary: the second grade, the heart of most devices. */
    public static final DeferredItem<HextechGemstoneItem> HEXTECH_CRYSTAL = ITEMS.registerItem("hextech_crystal",
            properties -> new HextechGemstoneItem(properties.rarity(Rarity.UNCOMMON).fireResistant()));
    /** A cut crystal finished on the lapidary into a stabilised gemstone: the third grade, steady for good. */
    public static final DeferredItem<HextechGemstoneItem> HEXTECH_GEMSTONE = ITEMS.registerItem("hextech_gemstone",
            properties -> new HextechGemstoneItem(properties.rarity(Rarity.RARE).fireResistant()));
    public static final DeferredItem<RuneScrollItem> RUNE_SCROLL = ITEMS.registerItem("rune_scroll",
            properties -> new RuneScrollItem(properties.stacksTo(16).rarity(Rarity.UNCOMMON)));
    /** Rolled paper waiting for a rune: written at the altar with a rune chosen on a stele. */
    public static final DeferredItem<Item> BLANK_SCROLL = ITEMS.registerSimpleItem("blank_scroll");
    /**
     * Never in a player's hands: the picture of a diary step that has no item of its own (a thought, something that
     * happened). The picture is chosen by the stack's custom model data string; hidden from the creative tab and JEI.
     */
    public static final DeferredItem<Item> GUIDE_MARK = ITEMS.registerSimpleItem("guide_mark");
    /** A finished research written out: whoever reads it may make the topic's items. */
    public static final DeferredItem<com.pockyl.hextale.item.ResearchNotesItem> RESEARCH_NOTES = ITEMS.registerItem("research_notes",
            properties -> new com.pockyl.hextale.item.ResearchNotesItem(properties.stacksTo(16).rarity(Rarity.UNCOMMON)));
    /** Worn on the head: shows the essences inside the essence works and where a Research Table's ink comes from. */
    public static final ResourceKey<EquipmentAsset> GOGGLES_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Hextale.id("researchers_goggles"));
    public static final DeferredItem<Item> RESEARCHERS_GOGGLES = ITEMS.registerItem("researchers_goggles",
            properties -> new Item(properties.stacksTo(1).component(DataComponents.EQUIPPABLE,
                    Equippable.builder(EquipmentSlot.HEAD).setAsset(GOGGLES_ASSET).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).build())));
    /** Worn on the back: with air essence in its tanks it lets me glide, and its jets push me like a rocket. */
    public static final DeferredItem<BlockItem> LEVITATION_PLATE = ITEMS.registerSimpleBlockItem(ModBlocks.LEVITATION_PLATE);
    public static final DeferredItem<BlockItem> NULL_LANTERN = ITEMS.registerSimpleBlockItem(ModBlocks.NULL_LANTERN);
    public static final DeferredItem<BlockItem> IMPULSE_PLATE = ITEMS.registerSimpleBlockItem(ModBlocks.IMPULSE_PLATE);
    public static final DeferredItem<BlockItem> HEX_LIFT = ITEMS.registerSimpleBlockItem(ModBlocks.HEX_LIFT);
    public static final DeferredItem<BlockItem> RIFT_GATE = ITEMS.registerSimpleBlockItem(ModBlocks.RIFT_GATE);
    /** A hextech crystal that remembers a Lesser Rift Gate, to tie it with another. */
    public static final DeferredItem<com.pockyl.hextale.item.RiftLinkCrystalItem> RIFT_LINK_CRYSTAL = ITEMS.registerItem("rift_link_crystal",
            properties -> new com.pockyl.hextale.item.RiftLinkCrystalItem(properties.stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BlockItem> DISINTEGRATOR = ITEMS.registerSimpleBlockItem(ModBlocks.DISINTEGRATOR);
    public static final DeferredItem<BlockItem> SILENCE_WARD = ITEMS.registerSimpleBlockItem(ModBlocks.SILENCE_WARD);
    public static final DeferredItem<BlockItem> WORLD_ANCHOR = ITEMS.registerSimpleBlockItem(ModBlocks.WORLD_ANCHOR,
            properties -> properties.rarity(Rarity.RARE));
    public static final DeferredItem<com.pockyl.hextale.item.FloatingStoneItem> FLOATING_STONE = ITEMS.registerItem("floating_stone",
            properties -> new com.pockyl.hextale.item.FloatingStoneItem(ModBlocks.FLOATING_STONE.get(), properties.useBlockDescriptionPrefix()));
    public static final DeferredItem<BlockItem> WARD_GLASS = ITEMS.registerSimpleBlockItem(ModBlocks.WARD_GLASS);
    public static final DeferredItem<BlockItem> VEIL_GLASS = ITEMS.registerSimpleBlockItem(ModBlocks.VEIL_GLASS);
    /** A hextech cage that folds a monster spawner into itself and unfolds it once elsewhere. */
    public static final DeferredItem<com.pockyl.hextale.item.SpawnerCageItem> SPAWNER_CAGE = ITEMS.registerItem("spawner_cage",
            properties -> new com.pockyl.hextale.item.SpawnerCageItem(properties.stacksTo(1).rarity(Rarity.RARE)));
    /** A crystal flask that folds one creature into its glass and lets it out elsewhere. */
    public static final DeferredItem<com.pockyl.hextale.item.SoulFlaskItem> SOUL_FLASK = ITEMS.registerItem("soul_flask",
            properties -> new com.pockyl.hextale.item.SoulFlaskItem(properties.stacksTo(16)));
    /** A brass horseshoe with hextech poles: switched on, it draws loose items and experience to its holder. */
    public static final DeferredItem<com.pockyl.hextale.item.HextechMagnetItem> HEXTECH_MAGNET = ITEMS.registerItem("hextech_magnet",
            properties -> new com.pockyl.hextale.item.HextechMagnetItem(properties.stacksTo(1).rarity(Rarity.UNCOMMON)));
    /** A leather glove with a hextech barrel: it throws a tendril at a block and draws its wearer there. */
    public static final DeferredItem<com.pockyl.hextale.item.GrappleGauntletItem> GRAPPLE_GAUNTLET = ITEMS.registerItem("grapple_gauntlet",
            properties -> new com.pockyl.hextale.item.GrappleGauntletItem(properties.stacksTo(1).rarity(Rarity.UNCOMMON)
                    .component(net.minecraft.core.component.DataComponents.USE_EFFECTS,
                            new net.minecraft.world.item.component.UseEffects(true, false, 1.0F))));
    /** A pocket watch with a hextech face: it puts its holder back where they stood a few seconds ago. */
    public static final DeferredItem<com.pockyl.hextale.item.RecallCharmItem> RECALL_CHARM = ITEMS.registerItem("recall_charm",
            properties -> new com.pockyl.hextale.item.RecallCharmItem(properties.stacksTo(1).rarity(Rarity.UNCOMMON)));
    /** Hextech dust packed tight: thrown, it sticks and cracks the rock around it. */
    public static final DeferredItem<com.pockyl.hextale.item.DustChargeItem> DUST_CHARGE = ITEMS.registerItem("dust_charge",
            properties -> new com.pockyl.hextale.item.DustChargeItem(properties.stacksTo(16)));
    /** Held up and listened to, it hums nearer what it listens for: hextech, or the block it was struck against. */
    public static final DeferredItem<com.pockyl.hextale.item.TuningForkItem> TUNING_FORK = ITEMS.registerItem("tuning_fork",
            properties -> new com.pockyl.hextale.item.TuningForkItem(properties.stacksTo(1)));
    public static final DeferredItem<com.pockyl.hextale.item.HextechSkypackItem> HEXTECH_SKYPACK = ITEMS.registerItem("hextech_skypack",
            properties -> new com.pockyl.hextale.item.HextechSkypackItem(properties.stacksTo(1).rarity(Rarity.RARE).fireResistant()
                    .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                            .setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA).setDamageOnHurt(false).build())));
    public static final DeferredItem<ResearchersDiaryItem> RESEARCHERS_DIARY = ITEMS.registerItem("researchers_diary",
            properties -> new ResearchersDiaryItem(properties.stacksTo(1)));

    /** Hextech pickaxe materials by tier: iron, diamond, netherite strength; repaired with brass. */
    private static final ToolMaterial[] PICKAXE_MATERIALS = {
            new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 750, 7.0F, 2.0F, 14, ModTags.HEXTECH_TOOL_MATERIALS),
            new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 9.0F, 3.0F, 12, ModTags.HEXTECH_TOOL_MATERIALS),
            new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2800, 10.5F, 4.0F, 16, ModTags.HEXTECH_TOOL_MATERIALS)};
    /** The three hextech pickaxes; runes are burned into them at the altar. */
    public static final Map<HextechPickaxe, DeferredItem<HextechPickaxeItem>> PICKAXES = new EnumMap<>(HextechPickaxe.class);

    static {
        for (HextechPickaxe kind : HextechPickaxe.values()) {
            PICKAXES.put(kind, ITEMS.registerItem(kind.id(), properties -> {
                Item.Properties p = properties.pickaxe(PICKAXE_MATERIALS[kind.tier() - 1], 1.0F, -2.8F)
                        .rarity(kind == HextechPickaxe.RADIANT ? Rarity.EPIC : kind == HextechPickaxe.REFINED ? Rarity.RARE : Rarity.UNCOMMON);
                return new HextechPickaxeItem(kind, kind == HextechPickaxe.RADIANT ? p.fireResistant() : p);
            }));
        }
    }

    public static DeferredItem<HextechPickaxeItem> pickaxe(HextechPickaxe kind) {
        return PICKAXES.get(kind);
    }

    /** The hextech axe, shovel and sword: iron tools charged at the altar, as strong as the plain hextech pickaxe. */
    public static final DeferredItem<HextechToolItem> HEXTECH_AXE = ITEMS.registerItem("hextech_axe",
            properties -> new HextechToolItem(properties.axe(PICKAXE_MATERIALS[0], 6.0F, -3.1F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<HextechToolItem> HEXTECH_SHOVEL = ITEMS.registerItem("hextech_shovel",
            properties -> new HextechToolItem(properties.shovel(PICKAXE_MATERIALS[0], 1.5F, -3.0F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<HextechToolItem> HEXTECH_SWORD = ITEMS.registerItem("hextech_sword",
            properties -> new HextechToolItem(properties.sword(PICKAXE_MATERIALS[0], 3.0F, -2.4F).rarity(Rarity.UNCOMMON)));

    /** Hexsteel tools: as strong as diamond ones, repaired with hexsteel. */
    private static final ToolMaterial HEXSTEEL_TOOLS = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8.0F, 3.0F, 10,
            ModTags.REPAIRS_HEXSTEEL);
    public static final DeferredItem<Item> HEXSTEEL_SWORD = ITEMS.registerItem("hexsteel_sword",
            properties -> new Item(properties.sword(HEXSTEEL_TOOLS, 3.0F, -2.4F)));
    public static final DeferredItem<Item> HEXSTEEL_PICKAXE = ITEMS.registerItem("hexsteel_pickaxe",
            properties -> new Item(properties.pickaxe(HEXSTEEL_TOOLS, 1.0F, -2.8F)));
    public static final DeferredItem<Item> HEXSTEEL_AXE = ITEMS.registerItem("hexsteel_axe",
            properties -> new Item(properties.axe(HEXSTEEL_TOOLS, 5.0F, -3.0F)));
    public static final DeferredItem<Item> HEXSTEEL_SHOVEL = ITEMS.registerItem("hexsteel_shovel",
            properties -> new Item(properties.shovel(HEXSTEEL_TOOLS, 1.5F, -3.0F)));
    public static final DeferredItem<Item> HEXSTEEL_HOE = ITEMS.registerItem("hexsteel_hoe",
            properties -> new Item(properties.hoe(HEXSTEEL_TOOLS, -3.0F, 0.0F)));

    /** Hexsteel armour: diamond protection, its own worn look. */
    public static final ResourceKey<EquipmentAsset> HEXSTEEL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Hextale.id("hexsteel"));
    private static final ArmorMaterial HEXSTEEL_ARMOR = new ArmorMaterial(33,
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 3, ArmorType.BODY, 11),
            10, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0F, 0.0F, ModTags.REPAIRS_HEXSTEEL, HEXSTEEL_ASSET);
    public static final DeferredItem<Item> HEXSTEEL_HELMET = ITEMS.registerItem("hexsteel_helmet",
            properties -> new Item(properties.humanoidArmor(HEXSTEEL_ARMOR, ArmorType.HELMET)));
    public static final DeferredItem<Item> HEXSTEEL_CHESTPLATE = ITEMS.registerItem("hexsteel_chestplate",
            properties -> new Item(properties.humanoidArmor(HEXSTEEL_ARMOR, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> HEXSTEEL_LEGGINGS = ITEMS.registerItem("hexsteel_leggings",
            properties -> new Item(properties.humanoidArmor(HEXSTEEL_ARMOR, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> HEXSTEEL_BOOTS = ITEMS.registerItem("hexsteel_boots",
            properties -> new Item(properties.humanoidArmor(HEXSTEEL_ARMOR, ArmorType.BOOTS)));

    /** Wool soaked in the crucible: heavy and warm, and the essence never washes out of it. */
    public static final DeferredItem<Item> ARCANE_CLOTH = ITEMS.registerSimpleItem("arcane_cloth");
    /** Researcher's clothes of arcane cloth: 3D pieces (client: ResearcherRobeClient), the robe keeps sparks and blasts off. */
    public static final ResourceKey<EquipmentAsset> RESEARCHER_ROBE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Hextale.id("researcher_robe"));
    private static final ArmorMaterial ARCANE_CLOTH_ARMOR = new ArmorMaterial(12,
            Map.of(ArmorType.BOOTS, 1, ArmorType.LEGGINGS, 3, ArmorType.CHESTPLATE, 4, ArmorType.HELMET, 1, ArmorType.BODY, 4),
            18, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, ModTags.REPAIRS_ARCANE_CLOTH, RESEARCHER_ROBE_ASSET);
    public static final DeferredItem<Item> RESEARCHER_HOOD = ITEMS.registerItem("researcher_hood",
            properties -> new com.pockyl.hextale.runework.ResearcherClothesItem(properties.humanoidArmor(ARCANE_CLOTH_ARMOR, ArmorType.HELMET)));
    public static final DeferredItem<Item> RESEARCHER_ROBE = ITEMS.registerItem("researcher_robe",
            properties -> new com.pockyl.hextale.runework.ResearcherClothesItem(properties.humanoidArmor(ARCANE_CLOTH_ARMOR, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> RESEARCHER_TROUSERS = ITEMS.registerItem("researcher_trousers",
            properties -> new com.pockyl.hextale.runework.ResearcherClothesItem(properties.humanoidArmor(ARCANE_CLOTH_ARMOR, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> RESEARCHER_BOOTS = ITEMS.registerItem("researcher_boots",
            properties -> new com.pockyl.hextale.runework.ResearcherClothesItem(properties.humanoidArmor(ARCANE_CLOTH_ARMOR, ArmorType.BOOTS)));

    /** Magic stone ground with dust: draws runes on the floor. */
    public static final DeferredItem<com.pockyl.hextale.item.RuneChalkItem> RUNE_CHALK = ITEMS.registerItem("rune_chalk",
            properties -> new com.pockyl.hextale.item.RuneChalkItem(properties.durability(32)));
    /** Stabilizing Chalk steeped in shadow: draws binding lines on the floor. */
    public static final DeferredItem<com.pockyl.hextale.item.BindingChalkItem> BINDING_CHALK = ITEMS.registerItem("binding_chalk",
            properties -> new com.pockyl.hextale.item.BindingChalkItem(properties.durability(96).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BlockItem> NULLSTONE = ITEMS.registerSimpleBlockItem(ModBlocks.NULLSTONE);
    public static final DeferredItem<BlockItem> VOID_SOIL = ITEMS.registerSimpleBlockItem(ModBlocks.VOID_SOIL);
    public static final DeferredItem<BlockItem> VOID_ROCK = ITEMS.registerSimpleBlockItem(ModBlocks.VOID_ROCK);
    public static final DeferredItem<BlockItem> VOID_ROCK_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.VOID_ROCK_BRICKS);
    public static final DeferredItem<BlockItem> VOID_ROCK_BRICK_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.VOID_ROCK_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> VOID_ROCK_BRICK_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.VOID_ROCK_BRICK_SLAB);
    public static final DeferredItem<BlockItem> VOID_ROCK_BRICK_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.VOID_ROCK_BRICK_WALL);
    public static final DeferredItem<BlockItem> VOID_CORAL = ITEMS.registerSimpleBlockItem(ModBlocks.VOID_CORAL);
    public static final DeferredItem<BlockItem> VOID_ROOTS = ITEMS.registerSimpleBlockItem(ModBlocks.VOID_ROOTS);
    public static final DeferredItem<BlockItem> VOID_BULB = ITEMS.registerSimpleBlockItem(ModBlocks.VOID_BULB);
    public static final DeferredItem<com.pockyl.hextale.item.NotedBlockItem> HOLLOWBLOOM = ITEMS.registerItem("hollowbloom",
            properties -> new com.pockyl.hextale.item.NotedBlockItem(ModBlocks.HOLLOWBLOOM.get(), properties.useBlockDescriptionPrefix()));
    /** Dropped by the creatures of the Void: the material of what will be made against it. */
    public static final DeferredItem<com.pockyl.hextale.item.NotedItem> VOID_CHITIN = ITEMS.registerItem("void_chitin",
            com.pockyl.hextale.item.NotedItem::new);
    /** A hextech crystal clouded through with the Void it drew out of the land (or held a breach with). */
    public static final DeferredItem<com.pockyl.hextale.item.CloudedCrystalItem> CLOUDED_CRYSTAL = ITEMS.registerItem("clouded_crystal",
            properties -> new com.pockyl.hextale.item.CloudedCrystalItem(properties.rarity(Rarity.UNCOMMON)));
    /** A held breach folded into a beating heart: carried somewhere else and unfolded there. */
    public static final DeferredItem<com.pockyl.hextale.item.BreachHeartItem> BREACH_HEART = ITEMS.registerItem("breach_heart",
            properties -> new com.pockyl.hextale.item.BreachHeartItem(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<BlockItem> POLISHED_NULLSTONE = ITEMS.registerSimpleBlockItem(ModBlocks.POLISHED_NULLSTONE);
    public static final DeferredItem<BlockItem> NULLSTONE_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.NULLSTONE_BRICKS);
    public static final DeferredItem<BlockItem> CRYSTAL_BATTERY = ITEMS.registerSimpleBlockItem(ModBlocks.CRYSTAL_BATTERY,
            properties -> properties.rarity(Rarity.UNCOMMON));

    private ModItems() {
    }


    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /** Any of the hex pylons. */
    public static boolean isPylon(net.minecraft.world.item.ItemStack stack) {
        return stack.is(GATHERING_PYLON.get()) || stack.is(SENDING_PYLON.get()) || stack.is(RELAY_PYLON.get()) || stack.is(SIPHON_PYLON.get())
                || stack.is(SORTING_MATRIX.get());
    }
}
