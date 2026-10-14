package com.pockyl.hextale.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.item.HextechInkItem;
import com.pockyl.hextale.item.ResearchersDiaryItem;
import com.pockyl.hextale.item.RuneBrushItem;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.item.UnstableHextechItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Hextale.MOD_ID);

    public static final DeferredItem<BlockItem> RESEARCH_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.RESEARCH_TABLE);
    public static final DeferredItem<Item> BRASS_INGOT = ITEMS.registerSimpleItem("brass_ingot");
    public static final DeferredItem<Item> BRASS_NUGGET = ITEMS.registerSimpleItem("brass_nugget");
    /** Lightning-born: neither fire nor further lightning harms it. */
    public static final DeferredItem<UnstableHextechItem> UNSTABLE_HEXTECH = ITEMS.registerItem("unstable_hextech",
            properties -> new UnstableHextechItem(properties.rarity(Rarity.UNCOMMON).fireResistant()));
    public static final DeferredItem<HextechInkItem> HEXTECH_INK = ITEMS.registerItem("hextech_ink", HextechInkItem::new);
    public static final DeferredItem<RuneBrushItem> RUNE_BRUSH = ITEMS.registerItem("rune_brush", RuneBrushItem::new);
    public static final DeferredItem<RuneScrollItem> RUNE_SCROLL = ITEMS.registerItem("rune_scroll",
            properties -> new RuneScrollItem(properties.stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ResearchersDiaryItem> RESEARCHERS_DIARY = ITEMS.registerItem("researchers_diary",
            properties -> new ResearchersDiaryItem(properties.stacksTo(1)));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
