package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.blocks.*;
import com.pocky.hextale.common.items.CuttingMachineItem;
import com.pocky.hextale.common.items.SuspiciousRemainsGravelItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
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


}
