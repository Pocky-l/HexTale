package com.pockyl.hextale;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

import com.pockyl.hextale.gametest.GameTests;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModDataMaps;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModMenus;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.registry.ModTriggers;
import com.pockyl.hextale.registry.PockyModsTab;
import com.pockyl.hextale.rune.ComplexRunes;

@Mod(Hextale.MOD_ID)
public final class Hextale {
    public static final String MOD_ID = "hextale";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Hextale(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModDataComponents.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModAttachments.register(modBus);
        ModSounds.register(modBus);
        ModTriggers.register(modBus);
        modBus.addListener(ModNetwork::register);
        GameTests.register(modBus);
        modBus.addListener(ModDataMaps::register);
        modBus.addListener(Hextale::addToVanillaTabs);
        PockyModsTab.register(modBus, () -> new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), output -> {
            output.accept(ModItems.RESEARCH_TABLE);
            output.accept(ModItems.ALEMBIC);
            output.accept(ModItems.SEPARATOR);
            output.accept(ModItems.ESSENCE_TANK);
            output.accept(ModItems.BRASS_PIPE);
            output.accept(ModItems.PUMP);
            output.accept(ModItems.DRAIN_VALVE);
            output.accept(ModItems.RESEARCHERS_DIARY);
            output.accept(ModItems.UNSTABLE_HEXTECH);
            output.accept(ModItems.RESEARCHERS_LENS);
            output.accept(ModItems.FORK_QUILL);
            output.accept(ModItems.MORTAR);
            output.accept(ModItems.BRASS_SCRAPER);
            output.accept(ModItems.PARCHMENT_STRETCHER);
            output.accept(ModItems.BRASS_INGOT);
            output.accept(ModItems.BRASS_NUGGET);
            output.accept(exampleScroll());
        });

        container.registerConfig(ModConfig.Type.SYNCED, Config.SPEC);
    }

    /** A scroll of "Smelting" shows players what scrolls look like; real ones come from the Research Table. */
    private static ItemStack exampleScroll() {
        return RuneScrollItem.of(ComplexRunes.byKey("fire_earth"), 1, ModItems.RUNE_SCROLL.get());
    }

    private static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.RESEARCH_TABLE);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.BRASS_INGOT);
            event.accept(ModItems.BRASS_NUGGET);
            event.accept(ModItems.UNSTABLE_HEXTECH);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.RESEARCHERS_DIARY);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
