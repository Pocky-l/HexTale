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
    public static final DeferredItem<BlockItem> HEXTECH_ALTAR = ITEMS.registerSimpleBlockItem(ModBlocks.HEXTECH_ALTAR,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> HEXTECH_LAMP = ITEMS.registerSimpleBlockItem(ModBlocks.HEXTECH_LAMP);
    public static final DeferredItem<BlockItem> HEX_FURNACE = ITEMS.registerSimpleBlockItem(ModBlocks.HEX_FURNACE,
            properties -> properties.rarity(Rarity.UNCOMMON));
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
    public static final DeferredItem<BlockItem> WHORLED_MAGIC_STONE = ITEMS.registerSimpleBlockItem(ModBlocks.WHORLED_MAGIC_STONE);
    /** Creative only: everything known at once - every rune with three stars, every item studied, every quest done. */
    public static final DeferredItem<KnowledgeScrollItem> SCROLL_OF_OMNISCIENCE = ITEMS.registerItem("scroll_of_omniscience",
            properties -> new KnowledgeScrollItem(true, properties.stacksTo(1).rarity(Rarity.EPIC)));
    /** Creative only: everything forgotten - runes, studied items, quests, parchments. */
    public static final DeferredItem<KnowledgeScrollItem> SCROLL_OF_OBLIVION = ITEMS.registerItem("scroll_of_oblivion",
            properties -> new KnowledgeScrollItem(false, properties.stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> PEDESTAL = ITEMS.registerSimpleBlockItem(ModBlocks.PEDESTAL);
    public static final DeferredItem<BlockItem> RUNE_STELE = ITEMS.registerSimpleBlockItem(ModBlocks.RUNE_STELE);
    /** A crafting recipe that uses a bottle of essence (soaking magic stone) gives the glass bottle back. */
    public static final DeferredItem<EssenceFlaskItem> ESSENCE_FLASK = ITEMS.registerItem("essence_flask",
            properties -> new EssenceFlaskItem(properties.stacksTo(1).craftRemainder(Items.GLASS_BOTTLE)));
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
    /** A finished research written out: whoever reads it may make the topic's items. */
    public static final DeferredItem<com.pockyl.hextale.item.ResearchNotesItem> RESEARCH_NOTES = ITEMS.registerItem("research_notes",
            properties -> new com.pockyl.hextale.item.ResearchNotesItem(properties.stacksTo(16).rarity(Rarity.UNCOMMON)));
    /** Worn on the head: shows the essences inside the essence works and where a Research Table's ink comes from. */
    public static final ResourceKey<EquipmentAsset> GOGGLES_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Hextale.id("researchers_goggles"));
    public static final DeferredItem<Item> RESEARCHERS_GOGGLES = ITEMS.registerItem("researchers_goggles",
            properties -> new Item(properties.stacksTo(1).component(DataComponents.EQUIPPABLE,
                    Equippable.builder(EquipmentSlot.HEAD).setAsset(GOGGLES_ASSET).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).build())));
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
