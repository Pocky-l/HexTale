package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.block.ResearchTableBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Hextale.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE =
            BLOCK_ENTITIES.register("research_table",
                    () -> new BlockEntityType<>(ResearchTableBlockEntity::new, ModBlocks.RESEARCH_TABLE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlembicBlockEntity>> ALEMBIC =
            BLOCK_ENTITIES.register("alembic", () -> new BlockEntityType<>(AlembicBlockEntity::new, ModBlocks.ALEMBIC.get()));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
