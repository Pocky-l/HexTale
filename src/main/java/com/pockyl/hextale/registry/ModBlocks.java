package com.pockyl.hextale.registry;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import com.pockyl.hextale.port.MultifaceBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import com.pockyl.hextale.port.PortRegisters;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.transport.PylonKind;
import com.pockyl.hextale.block.AlembicBlock;
import com.pockyl.hextale.block.RuneCircleBlock;
import com.pockyl.hextale.block.BrassPipeBlock;
import com.pockyl.hextale.block.CreativeEssenceTankBlock;
import com.pockyl.hextale.block.DrainValveBlock;
import com.pockyl.hextale.block.EssenceTankBlock;
import com.pockyl.hextale.block.PumpBlock;
import com.pockyl.hextale.block.HexPylonBlock;
import com.pockyl.hextale.block.HexFurnaceBlock;
import com.pockyl.hextale.block.HexWorkbenchBlock;
import com.pockyl.hextale.block.HextechAltarBlock;
import com.pockyl.hextale.block.HextechLampBlock;
import com.pockyl.hextale.block.HexWispBlock;
import com.pockyl.hextale.block.MagicLeavesBlock;
import com.pockyl.hextale.block.MagicSaplingBlock;
import com.pockyl.hextale.block.HexcoreBlock;
import com.pockyl.hextale.block.PedestalBlock;
import com.pockyl.hextale.block.RuneSteleBlock;
import com.pockyl.hextale.block.SeparatorBlock;
import com.pockyl.hextale.block.ResearchTableBlock;

public final class ModBlocks {
    public static final PortRegisters.Blocks BLOCKS = PortRegisters.createBlocks(Hextale.MOD_ID);

    public static final DeferredBlock<ResearchTableBlock> RESEARCH_TABLE = BLOCKS.registerBlock("research_table",
            ResearchTableBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.BASS)
                    .strength(2.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()
                    .lightLevel(state -> state.getValue(ResearchTableBlock.LIT) ? ResearchTableBlock.LIGHT : 0)
                    .pushReaction(PushReaction.BLOCK));

    public static final DeferredBlock<AlembicBlock> ALEMBIC = BLOCKS.registerBlock("alembic", AlembicBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.5F).sound(SoundType.COPPER).noOcclusion()
                    .lightLevel(state -> state.getValue(AlembicBlock.LIT) ? 5 : 0));

    public static final DeferredBlock<SeparatorBlock> SEPARATOR = BLOCKS.registerBlock("separator", SeparatorBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.5F).sound(SoundType.COPPER).noOcclusion());
    public static final DeferredBlock<EssenceTankBlock> ESSENCE_TANK = BLOCKS.registerBlock("essence_tank", EssenceTankBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.3F).sound(SoundType.GLASS).noOcclusion());
    /** The heart of a Big Essence Jar and the blocks of formed multiblocks: no items, made only by forming a structure. */
    public static final DeferredBlock<com.pockyl.hextale.block.BigEssenceJarBlock> BIG_ESSENCE_JAR = BLOCKS.registerBlock("big_essence_jar",
            com.pockyl.hextale.block.BigEssenceJarBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.6F)
                    .sound(SoundType.GLASS).noOcclusion().noLootTable());
    public static final DeferredBlock<com.pockyl.hextale.multiblock.MultiblockPartBlock> MULTIBLOCK_PART = BLOCKS.registerBlock("multiblock_part",
            com.pockyl.hextale.multiblock.MultiblockPartBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.6F)
                    .sound(SoundType.GLASS).noOcclusion().noLootTable());
    public static final DeferredBlock<com.pockyl.hextale.block.ShapePartBlock> SHAPE_PART = BLOCKS.registerBlock("shape_part",
            com.pockyl.hextale.block.ShapePartBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.0F)
                    .noOcclusion().noLootTable().replaceable().dynamicShape().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<BrassPipeBlock> BRASS_PIPE = BLOCKS.registerBlock("brass_pipe", BrassPipeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.0F).sound(SoundType.COPPER).noOcclusion());

    public static final DeferredBlock<CreativeEssenceTankBlock> CREATIVE_ESSENCE_TANK = BLOCKS.registerBlock("creative_essence_tank",
            CreativeEssenceTankBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(-1.0F, 3600000.0F)
                    .sound(SoundType.GLASS).noOcclusion().noLootTable());
    public static final DeferredBlock<DrainValveBlock> DRAIN_VALVE = BLOCKS.registerBlock("drain_valve", DrainValveBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.0F).sound(SoundType.COPPER).noOcclusion());
    public static final DeferredBlock<PumpBlock> PUMP = BLOCKS.registerBlock("pump", PumpBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.5F).sound(SoundType.COPPER).noOcclusion());

    // Magic stone: stone that has drunk essence (a crafted bottle or a drain valve pouring onto stone). The ritual circle
    // is built from it; the set follows vanilla tuff: stone -> polished -> bricks -> chiseled, plus brick stairs/slab/wall.
    public static final DeferredBlock<Block> MAGIC_STONE = BLOCKS.registerBlock("magic_stone", Block::new,
            () -> magicStoneProperties().sound(SoundType.TUFF));
    public static final DeferredBlock<Block> POLISHED_MAGIC_STONE = BLOCKS.registerBlock("polished_magic_stone", Block::new,
            () -> magicStoneProperties().sound(SoundType.POLISHED_TUFF));
    public static final DeferredBlock<Block> MAGIC_BRICKS = BLOCKS.registerBlock("magic_bricks", Block::new, ModBlocks::brickProperties);
    public static final DeferredBlock<StairBlock> MAGIC_BRICK_STAIRS = BLOCKS.registerBlock("magic_brick_stairs",
            properties -> new StairBlock(MAGIC_BRICKS.get().defaultBlockState(), properties), ModBlocks::brickProperties);
    public static final DeferredBlock<SlabBlock> MAGIC_BRICK_SLAB = BLOCKS.registerBlock("magic_brick_slab", SlabBlock::new,
            ModBlocks::brickProperties);
    public static final DeferredBlock<WallBlock> MAGIC_BRICK_WALL = BLOCKS.registerBlock("magic_brick_wall", WallBlock::new,
            () -> brickProperties().forceSolidOn());
    /** Carved bricks with the circle's hexagon cut into them. */
    public static final DeferredBlock<Block> CHISELED_MAGIC_BRICKS = BLOCKS.registerBlock("chiseled_magic_bricks", Block::new,
            ModBlocks::brickProperties);

    /** Nullstone banded with brass round a gemstone: it holds its chunks loaded while it stands. */
    public static final DeferredBlock<com.pockyl.hextale.block.WorldAnchorBlock> WORLD_ANCHOR = BLOCKS.registerBlock("world_anchor",
            com.pockyl.hextale.block.WorldAnchorBlock::new, () -> nullstoneProperties().lightLevel(state -> 6).noOcclusion());
    /** A Lesser Rift Gate: tied to a partner gate, it sends whatever passes into its ring out of the partner. */
    public static final DeferredBlock<com.pockyl.hextale.block.RiftGateBlock> RIFT_GATE = BLOCKS.registerBlock("rift_gate",
            com.pockyl.hextale.block.RiftGateBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).instrument(NoteBlockInstrument.BASEDRUM).strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.POLISHED_DEEPSLATE).noOcclusion()
                    .lightLevel(state -> state.getValue(com.pockyl.hextale.block.RiftGateBlock.LINKED) ? 7 : 2));
    /** A plate with a brass arrow: whatever steps on it is thrown up and along the arrow. */
    public static final DeferredBlock<com.pockyl.hextale.block.ImpulsePlateBlock> IMPULSE_PLATE = BLOCKS.registerBlock("impulse_plate",
            com.pockyl.hextale.block.ImpulsePlateBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F).sound(SoundType.POLISHED_TUFF).lightLevel(state -> 3));
    /** A lantern of nullstone: no hostile creature forms by itself within its quiet. */
    public static final DeferredBlock<com.pockyl.hextale.block.NullLanternBlock> NULL_LANTERN = BLOCKS.registerBlock("null_lantern",
            com.pockyl.hextale.block.NullLanternBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).forceSolidOn().strength(3.5F).sound(SoundType.LANTERN)
                    .lightLevel(state -> 12).noOcclusion().pushReaction(PushReaction.DESTROY));
    /** Smooth stone with a rune of lifting underneath: it can be set into thin air. */
    public static final DeferredBlock<Block> FLOATING_STONE = BLOCKS.registerBlock("floating_stone", Block::new,
            () -> magicStoneProperties().sound(SoundType.POLISHED_TUFF).lightLevel(state -> 4));
    /** Glass woven with a rune of barrier: Ward Glass stops players and lets creatures through, Veil Glass the other way. */
    public static final DeferredBlock<com.pockyl.hextale.block.SieveGlassBlock> WARD_GLASS = BLOCKS.registerBlock("ward_glass",
            properties -> new com.pockyl.hextale.block.SieveGlassBlock(true, properties), ModBlocks::sieveGlassProperties);
    public static final DeferredBlock<com.pockyl.hextale.block.SieveGlassBlock> VEIL_GLASS = BLOCKS.registerBlock("veil_glass",
            properties -> new com.pockyl.hextale.block.SieveGlassBlock(false, properties), ModBlocks::sieveGlassProperties);
    /** A stone plate with a hextech crystal in a brass ring: the air above it lifts whatever stands there. */
    public static final DeferredBlock<com.pockyl.hextale.block.LevitationPlateBlock> LEVITATION_PLATE = BLOCKS.registerBlock("levitation_plate",
            com.pockyl.hextale.block.LevitationPlateBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                    .strength(2.0F, 6.0F).sound(SoundType.POLISHED_TUFF).lightLevel(state -> 6));

    // Struck geodes: an amethyst geode lightning hit long ago. Its amethyst partly turned into dormant hextech, its calcite
    // shell scorched, and the bolt fused the ground on its way down into fulgurite.
    public static final DeferredBlock<com.pockyl.hextale.block.DormantHextechBlock> DORMANT_HEXTECH = BLOCKS.registerBlock("dormant_hextech",
            com.pockyl.hextale.block.DormantHextechBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(1.5F).sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops().lightLevel(state -> 3));
    /** Sand and earth fused into rough glass where a bolt went into the ground. */
    public static final DeferredBlock<Block> FULGURITE = BLOCKS.registerBlock("fulgurite", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(1.2F, 3.0F).sound(SoundType.TUFF));
    /** A glassy spike of fused earth standing up where a bolt went in: base, middles, a tip. */
    public static final DeferredBlock<com.pockyl.hextale.block.FulguriteSpikeBlock> FULGURITE_SPIKE = BLOCKS.registerBlock("fulgurite_spike",
            com.pockyl.hextale.block.FulguriteSpikeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(1.0F, 3.0F).sound(SoundType.POINTED_DRIPSTONE).noOcclusion()
                    .dynamicShape().offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY));
    /** Void Crystals: grow on Void Rock round a held breach (level 4 gem; sealed in nullstone before any craft). */
    public static final DeferredBlock<net.minecraft.world.level.block.AmethystClusterBlock> VOID_CRYSTAL_CLUSTER = BLOCKS.registerBlock(
            "void_crystal_cluster", properties -> new net.minecraft.world.level.block.AmethystClusterBlock(7.0F, 3.0F, properties),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).forceSolidOn().noOcclusion().strength(1.5F)
                    .requiresCorrectToolForDrops().lightLevel(state -> 5).sound(SoundType.AMETHYST_CLUSTER).pushReaction(PushReaction.DESTROY));
    // Hextech crystals: amethyst the strike woke all the way, in the four amethyst sizes (sizes and sounds as vanilla).
    public static final DeferredBlock<com.pockyl.hextale.block.HextechClusterBlock> HEXTECH_CLUSTER = BLOCKS.registerBlock("hextech_cluster",
            properties -> new com.pockyl.hextale.block.HextechClusterBlock(7.0F, 3.0F, properties),
            () -> hextechCrystalProperties(7).sound(SoundType.AMETHYST_CLUSTER));
    public static final DeferredBlock<com.pockyl.hextale.block.HextechClusterBlock> LARGE_HEXTECH_BUD = BLOCKS.registerBlock("large_hextech_bud",
            properties -> new com.pockyl.hextale.block.HextechClusterBlock(5.0F, 3.0F, properties),
            () -> hextechCrystalProperties(5).sound(SoundType.LARGE_AMETHYST_BUD));
    public static final DeferredBlock<com.pockyl.hextale.block.HextechClusterBlock> MEDIUM_HEXTECH_BUD = BLOCKS.registerBlock("medium_hextech_bud",
            properties -> new com.pockyl.hextale.block.HextechClusterBlock(4.0F, 3.0F, properties),
            () -> hextechCrystalProperties(3).sound(SoundType.MEDIUM_AMETHYST_BUD));
    public static final DeferredBlock<com.pockyl.hextale.block.HextechClusterBlock> SMALL_HEXTECH_BUD = BLOCKS.registerBlock("small_hextech_bud",
            properties -> new com.pockyl.hextale.block.HextechClusterBlock(3.0F, 4.0F, properties),
            () -> hextechCrystalProperties(2).sound(SoundType.SMALL_AMETHYST_BUD));
    /** Fulgurite melted again: smoky amber glass with the bolt's thread still inside. */
    public static final DeferredBlock<TransparentBlock> FULGURITE_GLASS = BLOCKS.registerBlock("fulgurite_glass", TransparentBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.6F).sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false));
    public static final DeferredBlock<IronBarsBlock> FULGURITE_GLASS_PANE = BLOCKS.registerBlock("fulgurite_glass_pane", IronBarsBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.3F).sound(SoundType.GLASS).noOcclusion());
    // Scorched calcite: the geode's shell after the strike; a set like the magic stone (polished, bricks, chiseled).
    public static final DeferredBlock<Block> SCORCHED_CALCITE = BLOCKS.registerBlock("scorched_calcite", Block::new,
            () -> scorchedCalciteProperties().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> POLISHED_SCORCHED_CALCITE = BLOCKS.registerBlock("polished_scorched_calcite", Block::new,
            () -> scorchedCalciteProperties().sound(SoundType.POLISHED_TUFF));
    public static final DeferredBlock<Block> SCORCHED_CALCITE_BRICKS = BLOCKS.registerBlock("scorched_calcite_bricks", Block::new,
            ModBlocks::scorchedBrickProperties);
    public static final DeferredBlock<StairBlock> SCORCHED_CALCITE_BRICK_STAIRS = BLOCKS.registerBlock("scorched_calcite_brick_stairs",
            properties -> new StairBlock(SCORCHED_CALCITE_BRICKS.get().defaultBlockState(), properties), ModBlocks::scorchedBrickProperties);
    public static final DeferredBlock<SlabBlock> SCORCHED_CALCITE_BRICK_SLAB = BLOCKS.registerBlock("scorched_calcite_brick_slab",
            SlabBlock::new, ModBlocks::scorchedBrickProperties);
    public static final DeferredBlock<WallBlock> SCORCHED_CALCITE_BRICK_WALL = BLOCKS.registerBlock("scorched_calcite_brick_wall",
            WallBlock::new, () -> scorchedBrickProperties().forceSolidOn());
    /** Polished scorched calcite with a bolt carved into it. */
    public static final DeferredBlock<Block> CHISELED_SCORCHED_CALCITE = BLOCKS.registerBlock("chiseled_scorched_calcite", Block::new,
            ModBlocks::scorchedBrickProperties);

    public static final DeferredBlock<HextechAltarBlock> HEXTECH_ALTAR = BLOCKS.registerBlock("hextech_altar", HextechAltarBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.5F, 9.0F).sound(SoundType.POLISHED_DEEPSLATE)
                    .noOcclusion().requiresCorrectToolForDrops().lightLevel(state -> 6));
    public static final DeferredBlock<PedestalBlock> PEDESTAL = BLOCKS.registerBlock("pedestal", PedestalBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(2.5F, 6.0F).sound(SoundType.POLISHED_DEEPSLATE)
                    .noOcclusion().requiresCorrectToolForDrops().lightLevel(state -> 3));
    /** The Hexcore: a rune matrix around a hextech crystal, floating over a brass cradle. */
    public static final DeferredBlock<HexcoreBlock> HEXCORE = BLOCKS.registerBlock("hexcore", HexcoreBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(3.5F, 9.0F).sound(SoundType.POLISHED_DEEPSLATE)
                    .noOcclusion().requiresCorrectToolForDrops().lightLevel(state -> 11));
    public static final DeferredBlock<RuneSteleBlock> RUNE_STELE = BLOCKS.registerBlock("rune_stele", RuneSteleBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(2.5F, 6.0F).sound(SoundType.POLISHED_DEEPSLATE)
                    .noOcclusion().requiresCorrectToolForDrops().lightLevel(state -> state.getValue(RuneSteleBlock.LIT) ? 9 : 2));
    /** The Rune Circle: a carved stone disc in the floor where the steles' runes are burned in and copied. */
    public static final DeferredBlock<RuneCircleBlock> RUNE_CIRCLE = BLOCKS.registerBlock("rune_circle", RuneCircleBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.5F, 9.0F).sound(SoundType.POLISHED_DEEPSLATE)
                    .noOcclusion().requiresCorrectToolForDrops().lightLevel(state -> 4));
    /** A mote of hextech light a pickaxe with the Eternal Flame rune leaves in the dark: no body, no drops, goes out at a touch. */
    public static final DeferredBlock<HexWispBlock> HEX_WISP = BLOCKS.registerBlock("hex_wisp", HexWispBlock::new,
            () -> BlockBehaviour.Properties.of().replaceable().noCollission().instabreak().noLootTable().randomTicks().lightLevel(state -> 13)
                    .sound(SoundType.AMETHYST_CLUSTER).pushReaction(PushReaction.DESTROY));
    /** A refined gemstone in a brass cage: stands or hangs like a lantern, full light. */
    public static final DeferredBlock<HextechLampBlock> HEXTECH_LAMP = BLOCKS.registerBlock("hextech_lamp", HextechLampBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).sound(SoundType.LANTERN)
                    .lightLevel(state -> state.getValue(HextechLampBlock.FLICKER) ? HextechLampBlock.DIM : 15).noOcclusion()
                    .pushReaction(PushReaction.DESTROY).requiresCorrectToolForDrops());

    /** A furnace woken with hextech dust: it melts metals into alloys; light while it burns. */
    public static final DeferredBlock<HexFurnaceBlock> HEX_FURNACE = BLOCKS.registerBlock("hex_furnace", HexFurnaceBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLUE).strength(3.5F, 6.0F).sound(SoundType.TUFF_BRICKS)
                    .requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> state.getValue(HexFurnaceBlock.LIT) ? 13 : 3));
    /** The iron door of a Hex Foundry; built into the kiln and lit, it is the kiln's heart. Light while the kiln burns. */
    public static final DeferredBlock<com.pockyl.hextale.block.HexFoundryBlock> HEX_FOUNDRY = BLOCKS.registerBlock("hex_foundry",
            com.pockyl.hextale.block.HexFoundryBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 6.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(state -> state.getValue(com.pockyl.hextale.block.HexFoundryBlock.LIT) ? 13 : 0));
    // Fire bricks: pale kiln bricks of fireclay that a Hex Foundry is built of, plus stairs, slab and wall.
    public static final DeferredBlock<Block> FIRE_BRICKS = BLOCKS.registerBlock("fire_bricks", Block::new, ModBlocks::fireBrickProperties);
    public static final DeferredBlock<StairBlock> FIRE_BRICK_STAIRS = BLOCKS.registerBlock("fire_brick_stairs",
            properties -> new StairBlock(FIRE_BRICKS.get().defaultBlockState(), properties), ModBlocks::fireBrickProperties);
    public static final DeferredBlock<SlabBlock> FIRE_BRICK_SLAB = BLOCKS.registerBlock("fire_brick_slab", SlabBlock::new,
            ModBlocks::fireBrickProperties);
    public static final DeferredBlock<WallBlock> FIRE_BRICK_WALL = BLOCKS.registerBlock("fire_brick_wall", WallBlock::new,
            () -> fireBrickProperties().forceSolidOn());
    /** A crafting table woken with hextech dust: almost every device is made here, on a crystal's charge. */
    public static final DeferredBlock<HexWorkbenchBlock> HEX_WORKBENCH = BLOCKS.registerBlock("hex_workbench", HexWorkbenchBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(state -> 4));
    /** A cauldron woken with hextech dust: boils water over heat and dissolves essences and items in it. */
    /** A deepslate bowl that unmakes what falls into it into unknown essence. */
    public static final DeferredBlock<com.pockyl.hextale.block.DisintegratorBlock> DISINTEGRATOR = BLOCKS.registerBlock("disintegrator",
            com.pockyl.hextale.block.DisintegratorBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.0F).sound(SoundType.POLISHED_DEEPSLATE).noOcclusion()
                    .requiresCorrectToolForDrops());
    public static final DeferredBlock<com.pockyl.hextale.block.CrucibleBlock> CRUCIBLE = BLOCKS.registerBlock("crucible",
            com.pockyl.hextale.block.CrucibleBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F).sound(SoundType.METAL).noOcclusion()
                    .requiresCorrectToolForDrops());
    /** A bench with a treadle-turned lap disc: hextech crystals are cut here along their cleavage planes. */
    public static final DeferredBlock<com.pockyl.hextale.block.LapidaryBlock> HEX_LAPIDARY = BLOCKS.registerBlock("hex_lapidary",
            com.pockyl.hextale.block.LapidaryBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion());
    /** Zinc ore: sphalerite in stone and in deepslate, a little rarer than copper. */
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> SPHALERITE_ORE = BLOCKS.registerBlock(
            "sphalerite_ore", properties -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.util.valueproviders.ConstantInt.of(0), properties),
            () -> BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_ORE));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> DEEPSLATE_SPHALERITE_ORE = BLOCKS.registerBlock(
            "deepslate_sphalerite_ore", properties -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.util.valueproviders.ConstantInt.of(0), properties),
            () -> BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DEEPSLATE_IRON_ORE));
    public static final DeferredBlock<Block> RAW_ZINC_BLOCK = BLOCKS.registerBlock("raw_zinc_block", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.RAW_IRON_BLOCK).mapColor(MapColor.COLOR_LIGHT_GRAY));
    public static final DeferredBlock<Block> ZINC_BLOCK = BLOCKS.registerBlock("zinc_block", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_LIGHT_GRAY));
    /** Nine hexsteel ingots: dark blue steel with a faint hextech shimmer. */
    public static final DeferredBlock<Block> HEXSTEEL_BLOCK = BLOCKS.registerBlock("hexsteel_block", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_BLUE));
    /** Nine brass ingots: a building block with the soft sheen of the machines. */
    public static final DeferredBlock<Block> BRASS_BLOCK = BLOCKS.registerBlock("brass_block", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(4.0F, 6.0F).sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops());

    // Hex pylons: gemstone-crowned columns of magic stone that move items without belts or pipes.
    public static final DeferredBlock<HexPylonBlock> GATHERING_PYLON = BLOCKS.registerBlock("gathering_pylon",
            properties -> new HexPylonBlock(PylonKind.GATHERING, properties), ModBlocks::pylonProperties);
    public static final DeferredBlock<HexPylonBlock> SENDING_PYLON = BLOCKS.registerBlock("sending_pylon",
            properties -> new HexPylonBlock(PylonKind.SENDING, properties), ModBlocks::pylonProperties);
    public static final DeferredBlock<HexPylonBlock> RELAY_PYLON = BLOCKS.registerBlock("relay_pylon",
            properties -> new HexPylonBlock(PylonKind.RELAY, properties),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(2.0F, 6.0F).sound(SoundType.CALCITE)
                    .noOcclusion().requiresCorrectToolForDrops().lightLevel(state -> 5));
    public static final DeferredBlock<HexPylonBlock> SIPHON_PYLON = BLOCKS.registerBlock("siphon_pylon",
            properties -> new HexPylonBlock(PylonKind.SIPHON, properties),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.5F).sound(SoundType.GLASS).noOcclusion().lightLevel(state -> 5));
    /** A lattice of lenses over a brass frame: sends what the container under it holds to the storages that keep it. */
    public static final DeferredBlock<HexPylonBlock> SORTING_MATRIX = BLOCKS.registerBlock("sorting_matrix",
            properties -> new HexPylonBlock(PylonKind.SORTING, properties),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()
                    .requiresCorrectToolForDrops().lightLevel(state -> 6));
    /** Brass mast and wind vanes on a magic stone foot: sends whirls of wind to reap the ripe plants around it. */
    public static final DeferredBlock<com.pockyl.hextale.block.WhirlReaperBlock> WHIRL_REAPER = BLOCKS.registerBlock("whirl_reaper",
            com.pockyl.hextale.block.WhirlReaperBlock::new,
            () -> magicStoneProperties().strength(2.5F, 6.0F).sound(SoundType.COPPER).noOcclusion().lightLevel(state -> 4));
    /** A dark oak stump with a hextech saw disc: fells the grown trees around it and plants them again. */
    public static final DeferredBlock<com.pockyl.hextale.block.HexSawmillBlock> HEX_SAWMILL = BLOCKS.registerBlock("hex_sawmill",
            com.pockyl.hextale.block.HexSawmillBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2.5F, 6.0F).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(state -> 4));
    /** A two-block brass-and-walnut street lamp whose gem burns off the dirt of the work around it. */
    public static final DeferredBlock<com.pockyl.hextale.block.HextechLamppostBlock> HEXTECH_LAMPPOST = BLOCKS.registerBlock("hextech_lamppost",
            com.pockyl.hextale.block.HextechLamppostBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(3.0F, 6.0F).sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> com.pockyl.hextale.block.HextechLamppostBlock.upper(state)
                            && state.getValue(com.pockyl.hextale.block.HextechLamppostBlock.LIT) ? 15 : 0));
    /** A two-block hexsteel spike wound with brass coils under a hextech crystal: strikes the hostile creatures around it with arcs. */
    public static final DeferredBlock<com.pockyl.hextale.block.ArcSpireBlock> ARC_SPIRE = BLOCKS.registerBlock("arc_spire",
            com.pockyl.hextale.block.ArcSpireBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.BLOCK));
    /** A standing stone of nullstone with a crystal in its window: six round a breach hold it. */
    public static final DeferredBlock<com.pockyl.hextale.block.VoidAnchorBlock> VOID_ANCHOR = BLOCKS.registerBlock("void_anchor",
            com.pockyl.hextale.block.VoidAnchorBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.5F, 9.0F).sound(SoundType.DEEPSLATE_BRICKS)
                    .requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> 4));
    /** A hextech crystal over a nullstone basin: draws the Thinning out of the land around into the crystal. */
    public static final DeferredBlock<com.pockyl.hextale.block.HextechPurifierBlock> HEXTECH_PURIFIER = BLOCKS.registerBlock("hextech_purifier",
            com.pockyl.hextale.block.HextechPurifierBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F).sound(SoundType.DEEPSLATE_BRICKS)
                    .requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> 5));
    /** A glass column in a brass frame: drinks the experience orbs about to appear around it and holds them as a liquid. */
    public static final DeferredBlock<com.pockyl.hextale.block.XpCondenserBlock> XP_CONDENSER = BLOCKS.registerBlock("xp_condenser",
            com.pockyl.hextale.block.XpCondenserBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(2.0F, 6.0F).sound(SoundType.GLASS)
                    .noOcclusion());
    /** A rusted chemtech stack: breathes choking fumes over the hostile creatures around it, for nothing, for ever. */
    public static final DeferredBlock<com.pockyl.hextale.block.MiasmaVentBlock> MIASMA_VENT = BLOCKS.registerBlock("miasma_vent",
            com.pockyl.hextale.block.MiasmaVentBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(3.0F, 6.0F).sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops().noOcclusion());
    /** A nullstone bowl on brass ribs under a hextech crystal: swallows the deaths around it into the container below. */
    public static final DeferredBlock<com.pockyl.hextale.block.GreedyFunnelBlock> GREEDY_FUNNEL = BLOCKS.registerBlock("greedy_funnel",
            com.pockyl.hextale.block.GreedyFunnelBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 6.0F).sound(SoundType.POLISHED_DEEPSLATE)
                    .requiresCorrectToolForDrops().noOcclusion());
    /** Polished magic stone with a whirl carved into it, the stone a reaper's field is laid out with. */
    public static final DeferredBlock<Block> WHORLED_MAGIC_STONE = BLOCKS.registerBlock("whorled_magic_stone", Block::new,
            () -> magicStoneProperties().sound(SoundType.POLISHED_TUFF));
    /** A fluted column of polished magic stone, like a quartz pillar. */
    public static final DeferredBlock<RotatedPillarBlock> MAGIC_STONE_PILLAR = BLOCKS.registerBlock("magic_stone_pillar",
            RotatedPillarBlock::new, () -> magicStoneProperties().sound(SoundType.POLISHED_TUFF));
    /** A see-through brass lattice, like a copper grate. */
    public static final DeferredBlock<TransparentBlock> BRASS_GRATE = BLOCKS.registerBlock("brass_grate", TransparentBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0F, 6.0F).sound(SoundType.COPPER_GRATE)
                    .noOcclusion().requiresCorrectToolForDrops().isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false));


    // The magic tree: a sapling that soaked up magic, plant and sap in a crucible grows into it. Its bark carries glowing
    // channels, its canopy weeps and sheds glowing leaves; the wood never burns.
    public static final BlockSetType MAGIC_SET = new BlockSetType("hextale_magic", true, true, true,
            BlockSetType.PressurePlateSensitivity.EVERYTHING, SoundType.CHERRY_WOOD,
            SoundEvents.CHERRY_WOOD_DOOR_CLOSE, SoundEvents.CHERRY_WOOD_DOOR_OPEN,
            SoundEvents.CHERRY_WOOD_TRAPDOOR_CLOSE, SoundEvents.CHERRY_WOOD_TRAPDOOR_OPEN,
            SoundEvents.CHERRY_WOOD_PRESSURE_PLATE_CLICK_OFF, SoundEvents.CHERRY_WOOD_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.CHERRY_WOOD_BUTTON_CLICK_OFF, SoundEvents.CHERRY_WOOD_BUTTON_CLICK_ON);
    public static final WoodType MAGIC_WOOD_TYPE = new WoodType("hextale_magic", MAGIC_SET, SoundType.CHERRY_WOOD,
            SoundType.CHERRY_WOOD_HANGING_SIGN, SoundEvents.CHERRY_WOOD_FENCE_GATE_CLOSE, SoundEvents.CHERRY_WOOD_FENCE_GATE_OPEN);
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_MAGIC_LOG = BLOCKS.registerBlock("stripped_magic_log",
            RotatedPillarBlock::new, ModBlocks::magicWoodProperties);
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_MAGIC_WOOD = BLOCKS.registerBlock("stripped_magic_wood",
            RotatedPillarBlock::new, ModBlocks::magicWoodProperties);
    /** An axe strips it (data map {@code neoforge:transformables}) and bares the glowing heartwood. */
    public static final DeferredBlock<RotatedPillarBlock> MAGIC_LOG = BLOCKS.registerBlock("magic_log",
            RotatedPillarBlock::new, ModBlocks::magicWoodProperties);
    public static final DeferredBlock<RotatedPillarBlock> MAGIC_WOOD = BLOCKS.registerBlock("magic_wood",
            RotatedPillarBlock::new, ModBlocks::magicWoodProperties);
    public static final DeferredBlock<Block> MAGIC_PLANKS = BLOCKS.registerBlock("magic_planks", Block::new,
            () -> magicWoodProperties().strength(2.0F, 3.0F));
    public static final DeferredBlock<StairBlock> MAGIC_STAIRS = BLOCKS.registerBlock("magic_stairs",
            properties -> new StairBlock(MAGIC_PLANKS.get().defaultBlockState(), properties), () -> magicWoodProperties().strength(2.0F, 3.0F));
    public static final DeferredBlock<SlabBlock> MAGIC_SLAB = BLOCKS.registerBlock("magic_slab", SlabBlock::new,
            () -> magicWoodProperties().strength(2.0F, 3.0F));
    public static final DeferredBlock<FenceBlock> MAGIC_FENCE = BLOCKS.registerBlock("magic_fence", FenceBlock::new,
            () -> magicWoodProperties().strength(2.0F, 3.0F));
    public static final DeferredBlock<FenceGateBlock> MAGIC_FENCE_GATE = BLOCKS.registerBlock("magic_fence_gate",
            properties -> new FenceGateBlock(MAGIC_WOOD_TYPE, properties), () -> magicWoodProperties().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<DoorBlock> MAGIC_DOOR = BLOCKS.registerBlock("magic_door",
            properties -> new DoorBlock(MAGIC_SET, properties),
            () -> magicWoodProperties().strength(3.0F).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<TrapDoorBlock> MAGIC_TRAPDOOR = BLOCKS.registerBlock("magic_trapdoor",
            properties -> new TrapDoorBlock(MAGIC_SET, properties),
            () -> magicWoodProperties().strength(3.0F).noOcclusion().isValidSpawn((state, level, pos, type) -> false));
    public static final DeferredBlock<ButtonBlock> MAGIC_BUTTON = BLOCKS.registerBlock("magic_button",
            properties -> new ButtonBlock(MAGIC_SET, 30, properties),
            () -> BlockBehaviour.Properties.of().noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<PressurePlateBlock> MAGIC_PRESSURE_PLATE = BLOCKS.registerBlock("magic_pressure_plate",
            properties -> new PressurePlateBlock(MAGIC_SET, properties),
            () -> magicWoodProperties().forceSolidOn().noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<MagicLeavesBlock> MAGIC_LEAVES = BLOCKS.registerBlock("magic_leaves", MagicLeavesBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.2F).randomTicks()
                    .sound(SoundType.CHERRY_LEAVES).noOcclusion().isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .pushReaction(PushReaction.DESTROY).isRedstoneConductor((state, level, pos) -> false));
    public static final DeferredBlock<MagicSaplingBlock> MAGIC_SAPLING = BLOCKS.registerBlock("magic_sapling", MagicSaplingBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).noCollission().randomTicks().instabreak()
                    .sound(SoundType.CHERRY_SAPLING).lightLevel(state -> 4).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<FlowerPotBlock> POTTED_MAGIC_SAPLING = BLOCKS.registerBlock("potted_magic_sapling",
            properties -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, MAGIC_SAPLING, properties),
            () -> BlockBehaviour.Properties.of().instabreak().noOcclusion().lightLevel(state -> 4).pushReaction(PushReaction.DESTROY));

    /** Chemtech's cheap heat: a riveted iron stove burning a green flame. */
    public static final DeferredBlock<com.pockyl.hextale.block.ChemBurnerBlock> CHEM_BURNER = BLOCKS.registerBlock("chem_burner",
            com.pockyl.hextale.block.ChemBurnerBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(3.0F, 6.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(state -> state.getValue(com.pockyl.hextale.block.ChemBurnerBlock.LIT) ? 10 : 0));
    /** Patches of cold green light a splash of glow mix leaves on the blocks it hits. */
    public static final DeferredBlock<MultifaceBlock> CHEM_GLOW = BLOCKS.registerBlock("chem_glow", MultifaceBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GLOW_LICHEN).replaceable().noCollission().instabreak().noLootTable()
                    .sound(SoundType.SLIME_BLOCK).lightLevel(GlowLichenBlock.emission(12)).pushReaction(PushReaction.DESTROY));
    /** Green-tinted glass in a rusty frame: glass boiled with venom in a crucible. */
    public static final DeferredBlock<TransparentBlock> CHEM_GLASS = BLOCKS.registerBlock("chem_glass", TransparentBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.6F).sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false));
    public static final DeferredBlock<IronBarsBlock> CHEM_GLASS_PANE = BLOCKS.registerBlock("chem_glass_pane", IronBarsBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.3F).sound(SoundType.GLASS).noOcclusion());
    /** Iron eaten by acid: chemtech's building metal, brown and pitted. */
    public static final DeferredBlock<Block> RUSTED_PLATING = BLOCKS.registerBlock("rusted_plating", Block::new, ModBlocks::platingProperties);
    public static final DeferredBlock<StairBlock> RUSTED_PLATING_STAIRS = BLOCKS.registerBlock("rusted_plating_stairs",
            properties -> new StairBlock(RUSTED_PLATING.get().defaultBlockState(), properties), ModBlocks::platingProperties);
    public static final DeferredBlock<SlabBlock> RUSTED_PLATING_SLAB = BLOCKS.registerBlock("rusted_plating_slab", SlabBlock::new,
            ModBlocks::platingProperties);
    public static final DeferredBlock<Block> RIVETED_RUSTED_PLATING = BLOCKS.registerBlock("riveted_rusted_plating", Block::new,
            ModBlocks::platingProperties);
    /** A see-through grating of rusted iron: chemtech's sibling of the brass grate. */
    public static final DeferredBlock<TransparentBlock> RUSTED_GRATE = BLOCKS.registerBlock("rusted_grate", TransparentBlock::new,
            () -> platingProperties().sound(SoundType.COPPER_GRATE).noOcclusion().isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false));
    /** A bundle of glass tubes in iron bands with green chemistry glowing in them. */
    public static final DeferredBlock<RotatedPillarBlock> CHEM_TUBES = BLOCKS.registerBlock("chem_tubes", RotatedPillarBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.COPPER).lightLevel(state -> 7));

    /** Shimmerbloom: the glowing purple flower of deep caves, standing on the floor or hanging from the ceiling. */
    public static final DeferredBlock<com.pockyl.hextale.block.ShimmerbloomBlock> SHIMMERBLOOM = BLOCKS.registerBlock("shimmerbloom",
            com.pockyl.hextale.block.ShimmerbloomBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission().instabreak().sound(SoundType.SPORE_BLOSSOM)
                    .lightLevel(state -> 10).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<FlowerPotBlock> POTTED_SHIMMERBLOOM = BLOCKS.registerBlock("potted_shimmerbloom",
            properties -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, SHIMMERBLOOM, properties),
            () -> BlockBehaviour.Properties.of().instabreak().noOcclusion().lightLevel(state -> 10).pushReaction(PushReaction.DESTROY));
    /** The violet moss that carpets bloom caves, faintly glowing. */
    public static final DeferredBlock<Block> SHIMMER_MOSS = BLOCKS.registerBlock("shimmer_moss", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.1F).sound(SoundType.MOSS).lightLevel(state -> 4)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<CarpetBlock> SHIMMER_MOSS_CARPET = BLOCKS.registerBlock("shimmer_moss_carpet", CarpetBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.1F).sound(SoundType.MOSS_CARPET)
                    .lightLevel(state -> 3).pushReaction(PushReaction.DESTROY));
    /** A rusted aquarium where an axolotl lives and sheds regenerative slime. */
    public static final DeferredBlock<com.pockyl.hextale.block.AxolotlTankBlock> AXOLOTL_TANK = BLOCKS.registerBlock("axolotl_tank",
            com.pockyl.hextale.block.AxolotlTankBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(2.0F, 6.0F).sound(SoundType.COPPER)
                    .noOcclusion().lightLevel(state -> 6).isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false));
    /** Where chemtech is worked out: a flask over a small burner, drops of essence from the jars around. */
    public static final DeferredBlock<com.pockyl.hextale.block.LabBenchBlock> LAB_BENCH = BLOCKS.registerBlock("lab_bench",
            com.pockyl.hextale.block.LabBenchBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2.5F, 6.0F).sound(SoundType.WOOD)
                    .noOcclusion().lightLevel(state -> state.getValue(com.pockyl.hextale.block.LabBenchBlock.LIT) ? 6 : 0)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
    /** A copper pot still that refines raw shimmer over a Chem Burner. */
    public static final DeferredBlock<com.pockyl.hextale.block.ChemStillBlock> CHEM_STILL = BLOCKS.registerBlock("chem_still",
            com.pockyl.hextale.block.ChemStillBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0F, 6.0F).sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(state -> state.getValue(com.pockyl.hextale.block.ChemStillBlock.WORKING) ? 7 : 3));
    /** A riveted pressure tank on a Chem Burner that cracks mixtures of essence into all their essences at once. */
    public static final DeferredBlock<com.pockyl.hextale.block.ChemBoilerBlock> CHEM_BOILER = BLOCKS.registerBlock("chem_boiler",
            com.pockyl.hextale.block.ChemBoilerBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(3.0F, 6.0F).sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(state -> state.getValue(com.pockyl.hextale.block.ChemBoilerBlock.WORKING) ? 5 : 0));
    /** An iron bowl of burning shimmer whose pink light makes plants grow. */
    public static final DeferredBlock<com.pockyl.hextale.block.PinkBrazierBlock> PINK_BRAZIER = BLOCKS.registerBlock("pink_brazier",
            com.pockyl.hextale.block.PinkBrazierBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(3.0F, 6.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(state -> state.getValue(com.pockyl.hextale.block.PinkBrazierBlock.LIT) ? 15 : 0));

    /** A rune drawn in Rune Chalk on the floor: quickens the altar's rituals around it, glows while one runs. */
    public static final DeferredBlock<com.pockyl.hextale.block.ChalkMarkBlock> CHALK_MARK = BLOCKS.registerBlock("chalk_mark",
            com.pockyl.hextale.block.ChalkMarkBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).replaceable().noCollission().instabreak().noLootTable()
                    .sound(SoundType.CALCITE).pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> state.getValue(com.pockyl.hextale.block.ChalkMarkBlock.LIT) ? 6 : 0));
    /** A line drawn in Binding Chalk: joins its neighbours; a closed figure of them holds what stands inside. */
    public static final DeferredBlock<com.pockyl.hextale.block.ChalkSigilBlock> CHALK_SIGIL = BLOCKS.registerBlock("chalk_sigil",
            com.pockyl.hextale.block.ChalkSigilBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission().instabreak().noLootTable()
                    .sound(SoundType.CALCITE).pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> state.getValue(com.pockyl.hextale.block.ChalkSigilBlock.ACTIVE) ? 3 : 0));
    /** Nullstone drinks stray magic: it calms a Hexcore at work and glows as it fills. Polished and bricks are decor. */
    public static final DeferredBlock<com.pockyl.hextale.block.NullstoneBlock> NULLSTONE = BLOCKS.registerBlock("nullstone",
            com.pockyl.hextale.block.NullstoneBlock::new,
            () -> nullstoneProperties().lightLevel(state -> state.getValue(com.pockyl.hextale.block.NullstoneBlock.SATURATION) * 3));
    public static final DeferredBlock<Block> POLISHED_NULLSTONE = BLOCKS.registerBlock("polished_nullstone", Block::new,
            ModBlocks::nullstoneProperties);
    public static final DeferredBlock<Block> NULLSTONE_BRICKS = BLOCKS.registerBlock("nullstone_bricks", Block::new,
            () -> nullstoneProperties().sound(SoundType.DEEPSLATE_BRICKS));
    /** A brass cage around a crystal core that keeps hextech charge and feeds the workbenches around it. */
    public static final DeferredBlock<com.pockyl.hextale.block.CrystalBatteryBlock> CRYSTAL_BATTERY = BLOCKS.registerBlock("crystal_battery",
            com.pockyl.hextale.block.CrystalBatteryBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0F, 6.0F).sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(state -> state.getValue(com.pockyl.hextale.block.CrystalBatteryBlock.LEVEL) * 3));

    private ModBlocks() {
    }

    /** Magic wood: white with golden sap, strong as oak, and it never catches fire (magic burned out of it would leave ash). */
    private static BlockBehaviour.Properties magicWoodProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASS)
                .strength(2.0F).sound(SoundType.CHERRY_WOOD);
    }

    private static BlockBehaviour.Properties magicStoneProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(2.0F, 6.0F);
    }

    // The Void's blight: ground it has touched, its growth, and bricks cut from its rock.
    public static final DeferredBlock<com.pockyl.hextale.thevoid.VoidSoilBlock> VOID_SOIL = BLOCKS.registerBlock("void_soil",
            com.pockyl.hextale.thevoid.VoidSoilBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.6F).sound(SoundType.SOUL_SOIL));
    public static final DeferredBlock<com.pockyl.hextale.thevoid.VoidSoilBlock> VOID_ROCK = BLOCKS.registerBlock("void_rock",
            com.pockyl.hextale.thevoid.VoidSoilBlock::new, ModBlocks::voidRockProperties);
    public static final DeferredBlock<Block> VOID_ROCK_BRICKS = BLOCKS.registerBlock("void_rock_bricks", Block::new,
            () -> voidRockProperties().sound(SoundType.DEEPSLATE_BRICKS));
    public static final DeferredBlock<StairBlock> VOID_ROCK_BRICK_STAIRS = BLOCKS.registerBlock("void_rock_brick_stairs",
            properties -> new StairBlock(VOID_ROCK_BRICKS.get().defaultBlockState(), properties),
            () -> voidRockProperties().sound(SoundType.DEEPSLATE_BRICKS));
    public static final DeferredBlock<SlabBlock> VOID_ROCK_BRICK_SLAB = BLOCKS.registerBlock("void_rock_brick_slab", SlabBlock::new,
            () -> voidRockProperties().sound(SoundType.DEEPSLATE_BRICKS));
    public static final DeferredBlock<WallBlock> VOID_ROCK_BRICK_WALL = BLOCKS.registerBlock("void_rock_brick_wall", WallBlock::new,
            () -> voidRockProperties().sound(SoundType.DEEPSLATE_BRICKS).forceSolidOn());
    public static final DeferredBlock<com.pockyl.hextale.thevoid.VoidFloraBlock> VOID_CORAL = BLOCKS.registerBlock("void_coral",
            properties -> new com.pockyl.hextale.thevoid.VoidFloraBlock(false, properties), () -> voidFloraProperties(0));
    public static final DeferredBlock<com.pockyl.hextale.thevoid.VoidFloraBlock> VOID_ROOTS = BLOCKS.registerBlock("void_roots",
            properties -> new com.pockyl.hextale.thevoid.VoidFloraBlock(false, properties), () -> voidFloraProperties(0));
    public static final DeferredBlock<com.pockyl.hextale.thevoid.VoidFloraBlock> VOID_BULB = BLOCKS.registerBlock("void_bulb",
            properties -> new com.pockyl.hextale.thevoid.VoidFloraBlock(true, properties), () -> voidFloraProperties(7));
    /** A dead bush woken with hextech dust: the more alive it is, the thinner the world around it. */
    public static final DeferredBlock<com.pockyl.hextale.thevoid.HollowbloomBlock> HOLLOWBLOOM = BLOCKS.registerBlock("hollowbloom",
            com.pockyl.hextale.thevoid.HollowbloomBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission().instabreak().sound(SoundType.ROOTS)
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> com.pockyl.hextale.thevoid.HollowbloomBlock.LIGHT[state.getValue(com.pockyl.hextale.thevoid.HollowbloomBlock.VIGOR)]));

    private static BlockBehaviour.Properties voidRockProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.TUFF);
    }

    private static BlockBehaviour.Properties voidFloraProperties(int light) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).replaceable().noCollission().instabreak()
                .sound(SoundType.ROOTS).offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> light)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties nullstoneProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(3.0F, 9.0F).sound(SoundType.DEEPSLATE);
    }

    private static BlockBehaviour.Properties pylonProperties() {
        return magicStoneProperties().strength(2.5F, 6.0F).sound(SoundType.POLISHED_TUFF).noOcclusion().lightLevel(state -> 5);
    }

    private static BlockBehaviour.Properties platingProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                .requiresCorrectToolForDrops().strength(4.0F, 6.0F).sound(SoundType.COPPER);
    }

    private static BlockBehaviour.Properties fireBrickProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.MUD_BRICKS);
    }

    private static BlockBehaviour.Properties sieveGlassProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.6F).sound(SoundType.GLASS).noOcclusion().dynamicShape()
                .isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false);
    }

    private static BlockBehaviour.Properties hextechCrystalProperties(int light) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).forceSolidOn().noOcclusion().strength(1.5F)
                .lightLevel(state -> light).pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties scorchedCalciteProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.0F, 4.0F);
    }

    private static BlockBehaviour.Properties scorchedBrickProperties() {
        return scorchedCalciteProperties().strength(1.5F, 6.0F).sound(SoundType.TUFF_BRICKS);
    }

    private static BlockBehaviour.Properties brickProperties() {
        return magicStoneProperties().strength(2.5F, 6.0F).sound(SoundType.TUFF_BRICKS);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
