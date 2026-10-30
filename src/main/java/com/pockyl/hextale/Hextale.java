package com.pockyl.hextale;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.gametest.GameTests;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModCapabilities;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataMaps;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModMenus;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.registry.ModTriggers;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.PockyModsTab;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.PickaxeEffects;

@Mod(Hextale.MOD_ID)
public final class Hextale {
    public static final String MOD_ID = "hextale";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Hextale(IEventBus modBus, ModContainer container) {
        com.pockyl.hextale.registry.ModAliases.register();
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        com.pockyl.hextale.registry.ModEntities.register(modBus);
        ModDataComponents.register(modBus);
        ModBlockEntities.register(modBus);
        com.pockyl.hextale.registry.ModFluids.register(modBus);
        ModCapabilities.register(modBus);
        ModMenus.register(modBus);
        ModAttachments.register(modBus);
        com.pockyl.hextale.registry.ModEffects.register(modBus);
        ModSounds.register(modBus);
        ModParticles.register(modBus);
        com.pockyl.hextale.registry.ModFeatures.register(modBus);
        ModTriggers.register(modBus);
        com.pockyl.hextale.registry.ModRecipes.register(modBus);
        modBus.addListener(ModNetwork::register);
        modBus.addListener((net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent event) ->
                event.register(com.pockyl.hextale.block.WorldAnchorBlock.TICKETS));
        PickaxeEffects.register();
        com.pockyl.hextale.runework.ToolEffects.register();
        com.pockyl.hextale.runework.WeaponRunes.register();
        com.pockyl.hextale.farm.FunnelEvents.register();
        com.pockyl.hextale.farm.ShackleEvents.register();
        com.pockyl.hextale.runework.Slumber.register();
        com.pockyl.hextale.farm.CondenserEvents.register();
        com.pockyl.hextale.event.SkypackEvents.register();
        com.pockyl.hextale.multiblock.Multiblocks.register();
        com.pockyl.hextale.loot.RuneScrollLoot.register(modBus);
        com.pockyl.hextale.loot.ChestItemsLoot.register(modBus);
        com.pockyl.hextale.event.ResearchLocks.register(NeoForge.EVENT_BUS);
        GameTests.register(modBus);
        modBus.addListener(ModDataMaps::register);
        modBus.addListener(Hextale::addToVanillaTabs);
        PockyModsTab.register(modBus, () -> new ItemStack(ModItems.HEXCORE.get()), Hextale::fillCreativeTab);

        container.registerConfig(ModConfig.Type.SYNCED, Config.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    /**
     * The mod's part of the Pocky Mods tab, grouped the way a player meets things: research, the workshop, the altar,
     * essence works, chemtech, pylons, automation, gear, materials, blocks, nature; the long lists of variants
     * (essences, brews, research notes) and the creative-only items come last.
     */
    private static void fillCreativeTab(CreativeModeTab.Output output) {
        // Research: where every path starts.
        output.accept(ModItems.RESEARCHERS_DIARY);
        output.accept(ModItems.RESEARCH_TABLE);
        output.accept(ModItems.LAB_BENCH);
        output.accept(ModItems.RESEARCHERS_GOGGLES);
        output.accept(ModItems.BLANK_SCROLL);
        output.accept(exampleScroll());

        // Workshop: crafting and processing stations with what they run on.
        output.accept(ModItems.HEX_WORKBENCH);
        output.accept(ModItems.CRYSTAL_BATTERY);
        output.accept(ModItems.HEX_LAPIDARY);
        output.accept(ModItems.CUTTING_DISC);
        output.accept(ModItems.MASTER_CUTTING_DISC);
        output.accept(ModItems.ENGRAVING_DISC);
        output.accept(ModItems.ENGRAVED_DIAMOND);
        output.accept(ModItems.HEX_FURNACE);
        output.accept(ModItems.HEX_FOUNDRY);
        output.accept(ModItems.CRUCIBLE);

        // Altar and runework.
        output.accept(ModItems.HEXTECH_ALTAR);
        output.accept(ModItems.PEDESTAL);
        output.accept(ModItems.RUNE_CIRCLE);
        output.accept(ModItems.RUNE_STELE);
        output.accept(ModItems.RUNE_CHALK);
        output.accept(ModItems.BINDING_CHALK);
        output.accept(ModItems.HEXCORE);
        output.accept(ModItems.NULLSTONE);
        output.accept(ModItems.HEXTECH_LAMP);
        output.accept(ModItems.HEXTECH_LAMPPOST);

        // Essence works.
        output.accept(ModItems.ALEMBIC);
        output.accept(ModItems.SEPARATOR);
        output.accept(ModItems.ESSENCE_TANK);
        output.accept(ModItems.BRASS_PIPE);
        output.accept(ModItems.PUMP);
        output.accept(ModItems.DRAIN_VALVE);
        output.accept(ModItems.ESSENCE_STICKER);

        // Chemtech.
        output.accept(ModItems.CHEM_BURNER);
        output.accept(ModItems.CHEM_STILL);
        output.accept(ModItems.CHEM_BOILER);
        output.accept(ModItems.AXOLOTL_TANK);
        output.accept(ModItems.PINK_BRAZIER);

        // Pylon logistics.
        output.accept(ModItems.GATHERING_PYLON);
        output.accept(ModItems.SENDING_PYLON);
        output.accept(ModItems.RELAY_PYLON);
        output.accept(ModItems.SIPHON_PYLON);
        output.accept(ModItems.SORTING_MATRIX);
        output.accept(ModItems.PYLON_FILTER);

        // Automation: farms, mob handling, defence.
        output.accept(ModItems.WHIRL_REAPER);
        output.accept(ModItems.HEX_SAWMILL);
        output.accept(ModItems.GREEDY_FUNNEL);
        output.accept(ModItems.XP_CONDENSER);
        output.accept(ModItems.ARC_SPIRE);
        output.accept(ModItems.HEXTECH_PURIFIER);
        output.accept(ModItems.VOID_ANCHOR);
        output.accept(ModItems.MIASMA_VENT);

        // Tools and weapons: the hextech line by tier, then the hexsteel set.
        output.accept(ModItems.TUNING_FORK);
        output.accept(ModItems.RECALL_CHARM);
        output.accept(ModItems.GRAPPLE_GAUNTLET);
        output.accept(ModItems.SOUL_FLASK);
        output.accept(ModItems.SPAWNER_CAGE);
        output.accept(ModItems.DUST_CHARGE);
        output.accept(ModItems.LEVITATION_PLATE);
        output.accept(ModItems.NULL_LANTERN);
        output.accept(ModItems.IMPULSE_PLATE);
        output.accept(ModItems.RIFT_GATE);
        output.accept(ModItems.RIFT_LINK_CRYSTAL);
        output.accept(ModItems.WORLD_ANCHOR);
        output.accept(ModItems.DISINTEGRATOR);
        ModItems.PICKAXES.values().forEach(output::accept);
        output.accept(ModItems.HEXTECH_AXE);
        output.accept(ModItems.HEXTECH_SHOVEL);
        output.accept(ModItems.HEXTECH_SWORD);
        output.accept(ModItems.REFINED_AXE);
        output.accept(ModItems.REFINED_SHOVEL);
        output.accept(ModItems.REFINED_SWORD);
        output.accept(ModItems.RADIANT_AXE);
        output.accept(ModItems.RADIANT_SHOVEL);
        output.accept(ModItems.RADIANT_SWORD);
        output.accept(ModItems.RIFTFORGED_AXE);
        output.accept(ModItems.RIFTFORGED_SHOVEL);
        output.accept(ModItems.RIFTFORGED_SWORD);
        output.accept(ModItems.HEXSTEEL_PICKAXE);
        output.accept(ModItems.HEXSTEEL_AXE);
        output.accept(ModItems.HEXSTEEL_SHOVEL);
        output.accept(ModItems.HEXSTEEL_HOE);
        output.accept(ModItems.HEXSTEEL_SWORD);

        // Armour and wearables.
        output.accept(ModItems.HEXSTEEL_HELMET);
        output.accept(ModItems.HEXSTEEL_CHESTPLATE);
        output.accept(ModItems.HEXSTEEL_LEGGINGS);
        output.accept(ModItems.HEXSTEEL_BOOTS);
        output.accept(ModItems.RESEARCHER_HOOD);
        output.accept(ModItems.RESEARCHER_ROBE);
        output.accept(ModItems.RESEARCHER_TROUSERS);
        output.accept(ModItems.RESEARCHER_BOOTS);
        output.accept(ModItems.HEXTECH_SKYPACK);
        output.accept(com.pockyl.hextale.item.HextechSkypackItem.full());

        // Materials: hextech from raw to cut, then metals, then the rest.
        output.accept(ModItems.UNSTABLE_HEXTECH);
        output.accept(ModItems.HEXTECH_DUST);
        output.accept(ModItems.HEXTECH_CRYSTAL);
        output.accept(ModItems.SYNTHETIC_CRYSTAL);
        output.accept(ModItems.HEXTECH_GEMSTONE);
        output.accept(ModItems.RAW_ZINC);
        output.accept(ModItems.ZINC_NUGGET);
        output.accept(ModItems.ZINC_INGOT);
        output.accept(ModItems.BRASS_NUGGET);
        output.accept(ModItems.BRASS_INGOT);
        output.accept(ModItems.HEXSTEEL_INGOT);
        output.accept(ModItems.CHARGED_HEXSTEEL_INGOT);
        output.accept(ModItems.RADIANT_ALLOY_INGOT);
        output.accept(ModItems.GEMSTONE_DUST);
        output.accept(ModItems.FIRECLAY_BALL);
        output.accept(ModItems.FIRE_BRICK);
        output.accept(ModItems.ARCANE_CLOTH);
        output.accept(ModItems.REGENERATIVE_SLIME);
        output.accept(ModItems.VOID_CHITIN);
        output.accept(ModItems.CLOUDED_CRYSTAL);
        // A heart holds a breach of its own: the tab's one holds an eye of the Void.
        ItemStack heart = new ItemStack(ModItems.BREACH_HEART.get());
        heart.set(com.pockyl.hextale.registry.ModDataComponents.BREACH_HEART.get(),
                new com.pockyl.hextale.thevoid.BreachHeart(com.pockyl.hextale.thevoid.VoidShape.EYE, 4.0F, 1337, 0));
        output.accept(heart);

        // Ores and storage blocks.
        output.accept(ModItems.SPHALERITE_ORE);
        output.accept(ModItems.DEEPSLATE_SPHALERITE_ORE);
        output.accept(ModItems.DORMANT_HEXTECH);
        output.accept(ModItems.SMALL_HEXTECH_BUD);
        output.accept(ModItems.MEDIUM_HEXTECH_BUD);
        output.accept(ModItems.LARGE_HEXTECH_BUD);
        output.accept(ModItems.HEXTECH_CLUSTER);
        output.accept(ModItems.RAW_ZINC_BLOCK);
        output.accept(ModItems.ZINC_BLOCK);
        output.accept(ModItems.BRASS_BLOCK);
        output.accept(ModItems.HEXSTEEL_BLOCK);

        // Building blocks, one family after another.
        output.accept(ModItems.MAGIC_STONE);
        output.accept(ModItems.POLISHED_MAGIC_STONE);
        output.accept(ModItems.WHORLED_MAGIC_STONE);
        output.accept(ModItems.MAGIC_STONE_PILLAR);
        output.accept(ModItems.MAGIC_BRICKS);
        output.accept(ModItems.MAGIC_BRICK_STAIRS);
        output.accept(ModItems.MAGIC_BRICK_SLAB);
        output.accept(ModItems.MAGIC_BRICK_WALL);
        output.accept(ModItems.CHISELED_MAGIC_BRICKS);
        output.accept(ModItems.SCORCHED_CALCITE);
        output.accept(ModItems.POLISHED_SCORCHED_CALCITE);
        output.accept(ModItems.SCORCHED_CALCITE_BRICKS);
        output.accept(ModItems.SCORCHED_CALCITE_BRICK_STAIRS);
        output.accept(ModItems.SCORCHED_CALCITE_BRICK_SLAB);
        output.accept(ModItems.SCORCHED_CALCITE_BRICK_WALL);
        output.accept(ModItems.CHISELED_SCORCHED_CALCITE);
        output.accept(ModItems.FULGURITE);
        output.accept(ModItems.FULGURITE_SPIKE);
        output.accept(ModItems.FULGURITE_GLASS);
        output.accept(ModItems.FULGURITE_GLASS_PANE);
        output.accept(ModItems.FLOATING_STONE);
        output.accept(ModItems.WARD_GLASS);
        output.accept(ModItems.VEIL_GLASS);
        output.accept(ModItems.MAGIC_LOG);
        output.accept(ModItems.MAGIC_WOOD);
        output.accept(ModItems.STRIPPED_MAGIC_LOG);
        output.accept(ModItems.STRIPPED_MAGIC_WOOD);
        output.accept(ModItems.MAGIC_PLANKS);
        output.accept(ModItems.MAGIC_STAIRS);
        output.accept(ModItems.MAGIC_SLAB);
        output.accept(ModItems.MAGIC_FENCE);
        output.accept(ModItems.MAGIC_FENCE_GATE);
        output.accept(ModItems.MAGIC_DOOR);
        output.accept(ModItems.MAGIC_TRAPDOOR);
        output.accept(ModItems.MAGIC_PRESSURE_PLATE);
        output.accept(ModItems.MAGIC_BUTTON);
        output.accept(ModItems.FIRE_BRICKS);
        output.accept(ModItems.FIRE_BRICK_STAIRS);
        output.accept(ModItems.FIRE_BRICK_SLAB);
        output.accept(ModItems.FIRE_BRICK_WALL);
        output.accept(ModItems.RUSTED_PLATING);
        output.accept(ModItems.RUSTED_PLATING_STAIRS);
        output.accept(ModItems.RUSTED_PLATING_SLAB);
        output.accept(ModItems.RIVETED_RUSTED_PLATING);
        output.accept(ModItems.RUSTED_GRATE);
        output.accept(ModItems.CHEM_TUBES);
        output.accept(ModItems.BRASS_GRATE);
        output.accept(ModItems.CHEM_GLASS);
        output.accept(ModItems.CHEM_GLASS_PANE);
        output.accept(ModItems.POLISHED_NULLSTONE);
        output.accept(ModItems.NULLSTONE_BRICKS);
        output.accept(ModItems.VOID_ROCK_BRICKS);
        output.accept(ModItems.VOID_ROCK_BRICK_STAIRS);
        output.accept(ModItems.VOID_ROCK_BRICK_SLAB);
        output.accept(ModItems.VOID_ROCK_BRICK_WALL);

        // Nature: the magic grove and shimmer, then the land the Void wears thin.
        output.accept(ModItems.MAGIC_SAPLING);
        output.accept(ModItems.MAGIC_LEAVES);
        output.accept(ModItems.SHIMMERBLOOM);
        output.accept(ModItems.SHIMMER_MOSS);
        output.accept(ModItems.SHIMMER_MOSS_CARPET);
        output.accept(ModItems.VOID_SOIL);
        output.accept(ModItems.VOID_ROCK);
        output.accept(ModItems.VOID_CORAL);
        output.accept(ModItems.VOID_ROOTS);
        output.accept(ModItems.VOID_BULB);
        output.accept(ModItems.HOLLOWBLOOM);

        // Bottles of the primals and of unknown essence, then every essence as an item in catalogue order
        // (primals, pairs, deeper compounds), then every brew.
        for (Essence essence : Essence.PRIMALS) {
            output.accept(EssenceFlaskItem.filled(EssenceMix.pure(essence, EssenceFlaskItem.CAPACITY)));
        }
        output.accept(EssenceFlaskItem.filled(EssenceMix.unknown(EssenceFlaskItem.CAPACITY)));
        for (Essence essence : Essence.values()) {
            output.accept(com.pockyl.hextale.item.EssenceItem.of(essence));
        }
        for (com.pockyl.hextale.chem.Brew brew : com.pockyl.hextale.chem.Brew.values()) {
            output.accept(com.pockyl.hextale.chem.ChemVialItem.of(brew));
        }

        // Research notes of every topic.
        com.pockyl.hextale.research.ResearchTopics.all().forEach(topic -> output.accept(com.pockyl.hextale.item.ResearchNotesItem.of(topic)));

        // Creative only: the Creative Essence Jar (every essence, endless) is the one jar of essence.
        output.accept(ModItems.CREATIVE_ESSENCE_TANK);
        output.accept(ModItems.SCROLL_OF_OMNISCIENCE);
        output.accept(ModItems.SCROLL_OF_OBLIVION);
        output.accept(ModItems.VOID_SHARD);
    }

    /** A scroll of "Smelting" shows players what scrolls look like; real ones come from the Research Table. */
    private static ItemStack exampleScroll() {
        return RuneScrollItem.of(ComplexRunes.byKey("fire_earth"), 1, ModItems.RUNE_SCROLL.get());
    }

    private static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModItems.VOID_SOIL);
            event.accept(ModItems.VOID_ROCK);
            event.accept(ModItems.VOID_CORAL);
            event.accept(ModItems.VOID_ROOTS);
            event.accept(ModItems.VOID_BULB);
            event.accept(ModItems.HOLLOWBLOOM);
            event.accept(ModItems.SPHALERITE_ORE);
            event.accept(ModItems.DEEPSLATE_SPHALERITE_ORE);
            event.accept(ModItems.DORMANT_HEXTECH);
            event.accept(ModItems.SMALL_HEXTECH_BUD);
            event.accept(ModItems.MEDIUM_HEXTECH_BUD);
            event.accept(ModItems.LARGE_HEXTECH_BUD);
            event.accept(ModItems.HEXTECH_CLUSTER);
            event.accept(ModItems.FULGURITE_SPIKE);
            event.accept(ModItems.FULGURITE);
            event.accept(ModItems.RAW_ZINC_BLOCK);
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.HEX_WORKBENCH);
            event.accept(ModItems.CRUCIBLE);
            event.accept(ModItems.HEX_FURNACE);
            event.accept(ModItems.HEX_FOUNDRY);
            event.accept(ModItems.HEX_LAPIDARY);
            event.accept(ModItems.RESEARCH_TABLE);
            event.accept(ModItems.HEXTECH_ALTAR);
            event.accept(ModItems.PEDESTAL);
            event.accept(ModItems.RUNE_CIRCLE);
            event.accept(ModItems.RUNE_STELE);
            event.accept(ModItems.HEXTECH_LAMP);
            event.accept(ModItems.LEVITATION_PLATE);
            event.accept(ModItems.NULL_LANTERN);
            event.accept(ModItems.IMPULSE_PLATE);
            event.accept(ModItems.RIFT_GATE);
            event.accept(ModItems.WORLD_ANCHOR);
            event.accept(ModItems.DISINTEGRATOR);
            event.accept(ModItems.FLOATING_STONE);
            event.accept(ModItems.WARD_GLASS);
            event.accept(ModItems.VEIL_GLASS);
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.MAGIC_STONE);
            event.accept(ModItems.POLISHED_MAGIC_STONE);
            event.accept(ModItems.MAGIC_BRICKS);
            event.accept(ModItems.MAGIC_BRICK_STAIRS);
            event.accept(ModItems.MAGIC_BRICK_SLAB);
            event.accept(ModItems.MAGIC_BRICK_WALL);
            event.accept(ModItems.CHISELED_MAGIC_BRICKS);
            event.accept(ModItems.SCORCHED_CALCITE);
            event.accept(ModItems.POLISHED_SCORCHED_CALCITE);
            event.accept(ModItems.SCORCHED_CALCITE_BRICKS);
            event.accept(ModItems.SCORCHED_CALCITE_BRICK_STAIRS);
            event.accept(ModItems.SCORCHED_CALCITE_BRICK_SLAB);
            event.accept(ModItems.SCORCHED_CALCITE_BRICK_WALL);
            event.accept(ModItems.CHISELED_SCORCHED_CALCITE);
            event.accept(ModItems.FIRE_BRICKS);
            event.accept(ModItems.FIRE_BRICK_STAIRS);
            event.accept(ModItems.FIRE_BRICK_SLAB);
            event.accept(ModItems.FIRE_BRICK_WALL);
            event.accept(ModItems.VOID_ROCK_BRICKS);
            event.accept(ModItems.VOID_ROCK_BRICK_STAIRS);
            event.accept(ModItems.VOID_ROCK_BRICK_SLAB);
            event.accept(ModItems.VOID_ROCK_BRICK_WALL);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.BRASS_INGOT);
            event.accept(ModItems.BRASS_NUGGET);
            event.accept(ModItems.RAW_ZINC);
            event.accept(ModItems.ZINC_INGOT);
            event.accept(ModItems.ZINC_NUGGET);
            event.accept(ModItems.FIRECLAY_BALL);
            event.accept(ModItems.FIRE_BRICK);
            event.accept(ModItems.HEXSTEEL_INGOT);
            event.accept(ModItems.CHARGED_HEXSTEEL_INGOT);
            event.accept(ModItems.RADIANT_ALLOY_INGOT);
            event.accept(ModItems.UNSTABLE_HEXTECH);
            event.accept(ModItems.HEXTECH_DUST);
            event.accept(ModItems.HEXTECH_CRYSTAL);
            event.accept(ModItems.HEXTECH_GEMSTONE);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.RESEARCHERS_DIARY);
            ModItems.PICKAXES.values().forEach(event::accept);
            event.accept(ModItems.HEXTECH_AXE);
            event.accept(ModItems.HEXTECH_SHOVEL);
            event.accept(ModItems.REFINED_AXE);
            event.accept(ModItems.REFINED_SHOVEL);
            event.accept(ModItems.RADIANT_AXE);
            event.accept(ModItems.RADIANT_SHOVEL);
            event.accept(ModItems.RIFTFORGED_AXE);
            event.accept(ModItems.RIFTFORGED_SHOVEL);
            event.accept(ModItems.HEXSTEEL_SHOVEL);
            event.accept(ModItems.HEXSTEEL_PICKAXE);
            event.accept(ModItems.HEXSTEEL_AXE);
            event.accept(ModItems.HEXSTEEL_HOE);
            event.accept(ModItems.TUNING_FORK);
            event.accept(ModItems.RECALL_CHARM);
            event.accept(ModItems.GRAPPLE_GAUNTLET);
            event.accept(ModItems.SOUL_FLASK);
            event.accept(ModItems.SPAWNER_CAGE);
            event.accept(ModItems.DUST_CHARGE);
            event.accept(ModItems.HEXTECH_SKYPACK);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.HEXTECH_SWORD);
            event.accept(ModItems.REFINED_SWORD);
            event.accept(ModItems.RADIANT_SWORD);
            event.accept(ModItems.RIFTFORGED_SWORD);
            event.accept(ModItems.HEXSTEEL_SWORD);
            event.accept(ModItems.HEXSTEEL_AXE);
            event.accept(ModItems.HEXSTEEL_HELMET);
            event.accept(ModItems.HEXSTEEL_CHESTPLATE);
            event.accept(ModItems.HEXSTEEL_LEGGINGS);
            event.accept(ModItems.HEXSTEEL_BOOTS);
            event.accept(ModItems.RESEARCHER_HOOD);
            event.accept(ModItems.RESEARCHER_ROBE);
            event.accept(ModItems.RESEARCHER_TROUSERS);
            event.accept(ModItems.RESEARCHER_BOOTS);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
