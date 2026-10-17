package com.pockyl.hextale.registry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.item.HextechGemstoneItem;
import com.pockyl.hextale.item.ResearchersDiaryItem;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.item.UnstableHextechItem;
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
    public static final DeferredItem<BlockItem> HEXTECH_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.HEXTECH_BRICKS);
    public static final DeferredItem<BlockItem> HEXTECH_BRICK_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.HEXTECH_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> HEXTECH_BRICK_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.HEXTECH_BRICK_SLAB);
    public static final DeferredItem<BlockItem> HEXTECH_BRICK_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.HEXTECH_BRICK_WALL);
    public static final DeferredItem<BlockItem> CHISELED_HEXTECH_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.CHISELED_HEXTECH_BRICKS);
    public static final DeferredItem<BlockItem> HEXTECH_ALTAR = ITEMS.registerSimpleBlockItem(ModBlocks.HEXTECH_ALTAR,
            properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> HEXTECH_LAMP = ITEMS.registerSimpleBlockItem(ModBlocks.HEXTECH_LAMP);
    public static final DeferredItem<BlockItem> PEDESTAL = ITEMS.registerSimpleBlockItem(ModBlocks.PEDESTAL);
    public static final DeferredItem<BlockItem> RUNE_STELE = ITEMS.registerSimpleBlockItem(ModBlocks.RUNE_STELE);
    public static final DeferredItem<EssenceFlaskItem> ESSENCE_FLASK = ITEMS.registerItem("essence_flask",
            properties -> new EssenceFlaskItem(properties.stacksTo(1)));
    public static final DeferredItem<Item> BRASS_INGOT = ITEMS.registerSimpleItem("brass_ingot");
    public static final DeferredItem<Item> BRASS_NUGGET = ITEMS.registerSimpleItem("brass_nugget");
    /** Lightning-born: neither fire nor further lightning harms it. */
    public static final DeferredItem<UnstableHextechItem> UNSTABLE_HEXTECH = ITEMS.registerItem("unstable_hextech",
            properties -> new UnstableHextechItem(properties.rarity(Rarity.UNCOMMON).fireResistant()));
    /** Unstable hextech refined at the altar: steady, it no longer bursts. */
    public static final DeferredItem<HextechGemstoneItem> HEXTECH_GEMSTONE = ITEMS.registerItem("hextech_gemstone",
            properties -> new HextechGemstoneItem(properties.rarity(Rarity.UNCOMMON).fireResistant()));
    public static final DeferredItem<RuneScrollItem> RUNE_SCROLL = ITEMS.registerItem("rune_scroll",
            properties -> new RuneScrollItem(properties.stacksTo(16).rarity(Rarity.UNCOMMON)));
    /** Worn on the head: shows the essences inside the essence works and where a Research Table's ink comes from. */
    public static final ResourceKey<EquipmentAsset> GOGGLES_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Hextale.id("researchers_goggles"));
    public static final DeferredItem<Item> RESEARCHERS_GOGGLES = ITEMS.registerItem("researchers_goggles",
            properties -> new Item(properties.stacksTo(1).component(DataComponents.EQUIPPABLE,
                    Equippable.builder(EquipmentSlot.HEAD).setAsset(GOGGLES_ASSET).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).build())));
    public static final DeferredItem<ResearchersDiaryItem> RESEARCHERS_DIARY = ITEMS.registerItem("researchers_diary",
            properties -> new ResearchersDiaryItem(properties.stacksTo(1)));

    /** Hextech tools: iron-tier mining, long-lived, repaired with brass. */
    public static final ToolMaterial HEXTECH = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 900, 6.5F, 2.0F, 12,
            ModTags.HEXTECH_TOOL_MATERIALS);
    public static final DeferredItem<HextechToolItem> HEXTECH_PICKAXE = ITEMS.registerItem("hextech_pickaxe",
            properties -> new HextechToolItem(properties.pickaxe(HEXTECH, 1.0F, -2.8F).rarity(Rarity.UNCOMMON)));

    private ModItems() {
    }


    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
