package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
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

}
