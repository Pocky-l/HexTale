package com.pockyl.hextale.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
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
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.transport.PylonKind;
import com.pockyl.hextale.block.AlembicBlock;
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
import com.pockyl.hextale.block.PedestalBlock;
import com.pockyl.hextale.block.RuneSteleBlock;
import com.pockyl.hextale.block.SeparatorBlock;
import com.pockyl.hextale.block.ResearchTableBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Hextale.MOD_ID);

    public static final DeferredBlock<ResearchTableBlock> RESEARCH_TABLE = BLOCKS.registerBlock("research_table",
            ResearchTableBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.BASS)
                    .strength(2.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()
                    .lightLevel(state -> state.getValue(ResearchTableBlock.LIT) ? ResearchTableBlock.LIGHT : 0)
                    .pushReaction(PushReaction.IMMOVEABLE));

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

    public static final DeferredBlock<HextechAltarBlock> HEXTECH_ALTAR = BLOCKS.registerBlock("hextech_altar", HextechAltarBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.5F, 9.0F).sound(SoundType.POLISHED_DEEPSLATE)
                    .noOcclusion().requiresCorrectToolForDrops().lightLevel(state -> 6));
    public static final DeferredBlock<PedestalBlock> PEDESTAL = BLOCKS.registerBlock("pedestal", PedestalBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(2.5F, 6.0F).sound(SoundType.POLISHED_DEEPSLATE)
                    .noOcclusion().requiresCorrectToolForDrops().lightLevel(state -> 3));
    public static final DeferredBlock<RuneSteleBlock> RUNE_STELE = BLOCKS.registerBlock("rune_stele", RuneSteleBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(2.5F, 6.0F).sound(SoundType.POLISHED_DEEPSLATE)
                    .noOcclusion().requiresCorrectToolForDrops().lightLevel(state -> state.getValue(RuneSteleBlock.LIT) ? 9 : 2));
    /** A refined gemstone in a brass cage: stands or hangs like a lantern, full light. */
    public static final DeferredBlock<HextechLampBlock> HEXTECH_LAMP = BLOCKS.registerBlock("hextech_lamp", HextechLampBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).sound(SoundType.LANTERN)
                    .lightLevel(state -> 15).noOcclusion().pushReaction(PushReaction.POPPED).requiresCorrectToolForDrops());

    /** A furnace woken with hextech dust: it melts metals into alloys; light while it burns. */
    public static final DeferredBlock<HexFurnaceBlock> HEX_FURNACE = BLOCKS.registerBlock("hex_furnace", HexFurnaceBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLUE).strength(3.5F, 6.0F).sound(SoundType.TUFF_BRICKS)
                    .requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> state.getValue(HexFurnaceBlock.LIT) ? 13 : 3));
    /** A crafting table woken with hextech dust: almost every device is made here, on a crystal's charge. */
    public static final DeferredBlock<HexWorkbenchBlock> HEX_WORKBENCH = BLOCKS.registerBlock("hex_workbench", HexWorkbenchBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(state -> 4));
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
    /** A fluted column of polished magic stone, like a quartz pillar. */
    public static final DeferredBlock<RotatedPillarBlock> MAGIC_STONE_PILLAR = BLOCKS.registerBlock("magic_stone_pillar",
            RotatedPillarBlock::new, () -> magicStoneProperties().sound(SoundType.POLISHED_TUFF));
    /** A see-through brass lattice, like a copper grate. */
    public static final DeferredBlock<TransparentBlock> BRASS_GRATE = BLOCKS.registerBlock("brass_grate", TransparentBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0F, 6.0F).sound(SoundType.COPPER_GRATE)
                    .noOcclusion().requiresCorrectToolForDrops().isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties magicStoneProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLUE).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(2.0F, 6.0F);
    }

    private static BlockBehaviour.Properties pylonProperties() {
        return magicStoneProperties().strength(2.5F, 6.0F).sound(SoundType.POLISHED_TUFF).noOcclusion().lightLevel(state -> 5);
    }

    private static BlockBehaviour.Properties brickProperties() {
        return magicStoneProperties().strength(2.5F, 6.0F).sound(SoundType.TUFF_BRICKS);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
