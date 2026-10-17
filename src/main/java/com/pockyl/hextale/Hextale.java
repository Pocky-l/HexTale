package com.pockyl.hextale;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
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
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.PockyModsTab;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.ToolRuneEffects;

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
        ModParticles.register(modBus);
        ModTriggers.register(modBus);
        modBus.addListener(ModNetwork::register);
        ToolRuneEffects.register();
        GameTests.register(modBus);
        modBus.addListener(ModDataMaps::register);
        modBus.addListener(Hextale::addToVanillaTabs);
        PockyModsTab.register(modBus, () -> new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), output -> {
            output.accept(ModItems.RESEARCH_TABLE);
            output.accept(ModItems.RESEARCHERS_GOGGLES);
            output.accept(ModItems.ALEMBIC);
            output.accept(ModItems.SEPARATOR);
            output.accept(ModItems.ESSENCE_TANK);
            // For creative building: the Creative Essence Tank (every essence, endless), full tanks and bottles of primals.
            output.accept(ModItems.CREATIVE_ESSENCE_TANK);
            for (Essence essence : Essence.PRIMALS) {
                output.accept(withEssence(ModItems.ESSENCE_TANK.get(), EssenceMix.pure(essence, EssenceTankBlockEntity.CAPACITY)));
            }
            for (Essence essence : Essence.PRIMALS) {
                output.accept(EssenceFlaskItem.filled(EssenceMix.pure(essence, EssenceFlaskItem.CAPACITY)));
            }
            output.accept(EssenceFlaskItem.filled(EssenceMix.unknown(EssenceFlaskItem.CAPACITY)));
            output.accept(ModItems.BRASS_PIPE);
            output.accept(ModItems.PUMP);
            output.accept(ModItems.DRAIN_VALVE);
            output.accept(ModItems.HEXTECH_ALTAR);
            output.accept(ModItems.PEDESTAL);
            output.accept(ModItems.RUNE_STELE);
            output.accept(ModItems.HEXTECH_BRICKS);
            output.accept(ModItems.HEXTECH_BRICK_STAIRS);
            output.accept(ModItems.HEXTECH_BRICK_SLAB);
            output.accept(ModItems.HEXTECH_BRICK_WALL);
            output.accept(ModItems.CHISELED_HEXTECH_BRICKS);
            output.accept(ModItems.HEXTECH_LAMP);
            output.accept(ModItems.HEXTECH_PICKAXE);
            output.accept(ModItems.RESEARCHERS_DIARY);
            output.accept(ModItems.UNSTABLE_HEXTECH);
            output.accept(ModItems.HEXTECH_GEMSTONE);
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
            event.accept(ModItems.HEXTECH_ALTAR);
            event.accept(ModItems.PEDESTAL);
            event.accept(ModItems.RUNE_STELE);
            event.accept(ModItems.HEXTECH_LAMP);
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.HEXTECH_BRICKS);
            event.accept(ModItems.HEXTECH_BRICK_STAIRS);
            event.accept(ModItems.HEXTECH_BRICK_SLAB);
            event.accept(ModItems.HEXTECH_BRICK_WALL);
            event.accept(ModItems.CHISELED_HEXTECH_BRICKS);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.BRASS_INGOT);
            event.accept(ModItems.BRASS_NUGGET);
            event.accept(ModItems.UNSTABLE_HEXTECH);
            event.accept(ModItems.HEXTECH_GEMSTONE);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.RESEARCHERS_DIARY);
            event.accept(ModItems.HEXTECH_PICKAXE);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static ItemStack withEssence(Item item, EssenceMix contents) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.ESSENCE.get(), contents);
        return stack;
    }
}
