package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.blocks.CuttingMachineEntity;
import com.pocky.hextale.common.blocks.HexcoreEntity;
import com.pocky.hextale.common.blocks.SuspiciousRemainsGravelEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(ForgeRegistries.BLOCK_ENTITY_TYPES, HexTaleMod.MODID);

    public static final RegistryObject<BlockEntityType<SuspiciousRemainsGravelEntity>> SUSPICIOUS_REMAINS_GRAVEL_BLOCK = BLOCK_ENTITIES
            .register("suspicious_remains_gravel", () -> BlockEntityType.Builder
                    .of(SuspiciousRemainsGravelEntity::new, ModBlocks.SUSPICIOUS_REMAINS_GRAVEL.get()).build(null));

    public static final RegistryObject<BlockEntityType<CuttingMachineEntity.Base>> BASE_CUTTING_MACHINE = BLOCK_ENTITIES
            .register("base_cutting_machine", () -> BlockEntityType.Builder
                    .of(CuttingMachineEntity.Base::new, ModBlocks.BASE_CUTTING_MACHINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<CuttingMachineEntity.Advanced>> ADVANCED_CUTTING_MACHINE = BLOCK_ENTITIES
            .register("advanced_cutting_machine", () -> BlockEntityType.Builder
                    .of(CuttingMachineEntity.Advanced::new, ModBlocks.ADVANCED_CUTTING_MACHINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<CuttingMachineEntity.Elite>> ELITE_CUTTING_MACHINE = BLOCK_ENTITIES
            .register("elite_cutting_machine", () -> BlockEntityType.Builder
                    .of(CuttingMachineEntity.Elite::new, ModBlocks.ELITE_CUTTING_MACHINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<CuttingMachineEntity.Master>> MASTER_CUTTING_MACHINE = BLOCK_ENTITIES
            .register("master_cutting_machine", () -> BlockEntityType.Builder
                    .of(CuttingMachineEntity.Master::new, ModBlocks.MASTER_CUTTING_MACHINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<HexcoreEntity>> HEXCORE = BLOCK_ENTITIES
            .register("hexcore", () -> BlockEntityType.Builder
                    .of(HexcoreEntity::new, ModBlocks.HEXCORE.get()).build(null));

}
