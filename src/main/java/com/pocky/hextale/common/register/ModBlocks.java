package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.blocks.*;
import com.pocky.hextale.common.items.CuttingMachineItem;
import com.pocky.hextale.common.items.SuspiciousRemainsGravelItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, HexTaleMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HexTaleMod.MODID);

    public static final RegistryObject<Block> SUSPICIOUS_REMAINS_GRAVEL = BLOCKS.register("suspicious_remains_gravel", () -> new SuspiciousRemainsGravel(Blocks.GRAVEL, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.SNARE).noOcclusion().strength(0.25F).requiresCorrectToolForDrops().sound(SoundType.SUSPICIOUS_GRAVEL).pushReaction(PushReaction.DESTROY), SoundEvents.BRUSH_GRAVEL, SoundEvents.BRUSH_GRAVEL_COMPLETED));
    public static final RegistryObject<BlockItem> SUSPICIOUS_REMAINS_GRAVEL_ITEM = ITEMS.register("suspicious_remains_gravel", () -> new SuspiciousRemainsGravelItem(SUSPICIOUS_REMAINS_GRAVEL.get(), new Item.Properties()));

    public static final RegistryObject<Block> BASE_CUTTING_MACHINE = BLOCKS.register("base_cutting_machine", () -> new BaseCuttingMachine(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(0.25F).sound(SoundType.STONE)));
    public static final RegistryObject<BlockItem> BASE_CUTTING_MACHINE_ITEM = ITEMS.register("base_cutting_machine", () -> new CuttingMachineItem(BASE_CUTTING_MACHINE.get(), new Item.Properties(), "base_cutting_machine"));

    public static final RegistryObject<Block> ADVANCED_CUTTING_MACHINE = BLOCKS.register("advanced_cutting_machine", () -> new AdvancedCuttingMachine(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(0.25F).sound(SoundType.STONE)));
    public static final RegistryObject<BlockItem> ADVANCED_CUTTING_MACHINE_ITEM = ITEMS.register("advanced_cutting_machine", () -> new CuttingMachineItem(ADVANCED_CUTTING_MACHINE.get(), new Item.Properties(), "advanced_cutting_machine"));

    public static final RegistryObject<Block> ELITE_CUTTING_MACHINE = BLOCKS.register("elite_cutting_machine", () -> new EliteCuttingMachine(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(0.25F).sound(SoundType.STONE)));
    public static final RegistryObject<BlockItem> ELITE_CUTTING_MACHINE_ITEM = ITEMS.register("elite_cutting_machine", () -> new CuttingMachineItem(ELITE_CUTTING_MACHINE.get(), new Item.Properties(), "elite_cutting_machine"));

    public static final RegistryObject<Block> MASTER_CUTTING_MACHINE = BLOCKS.register("master_cutting_machine", () -> new MasterCuttingMachine(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(0.25F).sound(SoundType.STONE)));
    public static final RegistryObject<BlockItem> MASTER_CUTTING_MACHINE_ITEM = ITEMS.register("master_cutting_machine", () -> new CuttingMachineItem(MASTER_CUTTING_MACHINE.get(), new Item.Properties(), "master_cutting_machine"));



    public static final RegistryObject<Block> BUDDING_HEXTECH = BLOCKS.register("budding_hextech", () -> new BuddingHextechBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).randomTicks().strength(0.2F).sound(SoundType.SCULK).requiresCorrectToolForDrops().pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> HEXTECH_CLUSTER = BLOCKS.register("hextech_cluster", () -> new HextechClusterBlock(7, 3, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).forceSolidOn().noOcclusion().randomTicks().sound(SoundType.AMETHYST_CLUSTER).strength(1.5F).lightLevel((p_152632_) -> 5).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> LARGE_HEXTECH_BUD = BLOCKS.register("large_hextech_bud", () -> new HextechClusterBlock(5, 3, BlockBehaviour.Properties.copy(HEXTECH_CLUSTER.get()).sound(SoundType.MEDIUM_AMETHYST_BUD).forceSolidOn().lightLevel((p_152629_) -> 4).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> MEDIUM_HEXTECH_BUD = BLOCKS.register("medium_hextech_bud", () -> new HextechClusterBlock(4, 3, BlockBehaviour.Properties.copy(HEXTECH_CLUSTER.get()).sound(SoundType.LARGE_AMETHYST_BUD).forceSolidOn().lightLevel((p_152617_) -> 2).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> SMALL_HEXTECH_BUD = BLOCKS.register("small_hextech_bud", () -> new HextechClusterBlock(3, 4, BlockBehaviour.Properties.copy(HEXTECH_CLUSTER.get()).sound(SoundType.SMALL_AMETHYST_BUD).forceSolidOn().lightLevel((p_187409_) -> 1).pushReaction(PushReaction.DESTROY)));

    public static final RegistryObject<BlockItem> BUDDING_HEXTECH_ITEM = ITEMS.register("budding_hextech", () -> new BlockItem(BUDDING_HEXTECH.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> HEXTECH_CLUSTER_ITEM = ITEMS.register("hextech_cluster", () -> new BlockItem(HEXTECH_CLUSTER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> LARGE_HEXTECH_BUD_ITEM = ITEMS.register("large_hextech_bud", () -> new BlockItem(LARGE_HEXTECH_BUD.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> MEDIUM_HEXTECH_BUD_ITEM = ITEMS.register("medium_hextech_bud", () -> new BlockItem(MEDIUM_HEXTECH_BUD.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> SMALL_HEXTECH_BUD_ITEM = ITEMS.register("small_hextech_bud", () -> new BlockItem(SMALL_HEXTECH_BUD.get(), new Item.Properties()));

}
