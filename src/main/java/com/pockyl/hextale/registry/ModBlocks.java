package com.pockyl.hextale.registry;

import net.minecraft.world.level.block.Block;
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
import com.pockyl.hextale.block.AlembicBlock;
import com.pockyl.hextale.block.BrassPipeBlock;
import com.pockyl.hextale.block.CreativeEssenceTankBlock;
import com.pockyl.hextale.block.DrainValveBlock;
import com.pockyl.hextale.block.EssenceTankBlock;
import com.pockyl.hextale.block.PumpBlock;
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
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.GLASS).noOcclusion());
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

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties magicStoneProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLUE).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(2.0F, 6.0F);
    }

    private static BlockBehaviour.Properties brickProperties() {
        return magicStoneProperties().strength(2.5F, 6.0F).sound(SoundType.TUFF_BRICKS);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
