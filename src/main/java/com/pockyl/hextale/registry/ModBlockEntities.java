package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.block.DrainValveBlockEntity;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.PumpBlockEntity;
import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.block.HexFurnaceBlockEntity;
import com.pockyl.hextale.block.HextechAltarBlockEntity;
import com.pockyl.hextale.block.PedestalBlockEntity;
import com.pockyl.hextale.block.RuneSteleBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Hextale.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE =
            BLOCK_ENTITIES.register("research_table",
                    () -> new BlockEntityType<>(ResearchTableBlockEntity::new, ModBlocks.RESEARCH_TABLE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.CrucibleBlockEntity>> CRUCIBLE =
            BLOCK_ENTITIES.register("crucible", () -> new BlockEntityType<>(com.pockyl.hextale.block.CrucibleBlockEntity::new,
                    ModBlocks.CRUCIBLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.HexWorkbenchBlockEntity>> HEX_WORKBENCH =
            BLOCK_ENTITIES.register("hex_workbench", () -> new BlockEntityType<>(com.pockyl.hextale.block.HexWorkbenchBlockEntity::new,
                    ModBlocks.HEX_WORKBENCH.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.LapidaryBlockEntity>> LAPIDARY =
            BLOCK_ENTITIES.register("hex_lapidary", () -> new BlockEntityType<>(com.pockyl.hextale.block.LapidaryBlockEntity::new,
                    ModBlocks.HEX_LAPIDARY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HexFurnaceBlockEntity>> HEX_FURNACE =
            BLOCK_ENTITIES.register("hex_furnace", () -> new BlockEntityType<>(HexFurnaceBlockEntity::new, ModBlocks.HEX_FURNACE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.HexPylonBlockEntity>> HEX_PYLON =
            BLOCK_ENTITIES.register("hex_pylon", () -> new BlockEntityType<>(com.pockyl.hextale.block.HexPylonBlockEntity::new,
                    ModBlocks.GATHERING_PYLON.get(), ModBlocks.SENDING_PYLON.get(), ModBlocks.RELAY_PYLON.get(),
                    ModBlocks.SIPHON_PYLON.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlembicBlockEntity>> ALEMBIC =
            BLOCK_ENTITIES.register("alembic", () -> new BlockEntityType<>(AlembicBlockEntity::new, ModBlocks.ALEMBIC.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SeparatorBlockEntity>> SEPARATOR =
            BLOCK_ENTITIES.register("separator", () -> new BlockEntityType<>(SeparatorBlockEntity::new, ModBlocks.SEPARATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssenceTankBlockEntity>> ESSENCE_TANK =
            BLOCK_ENTITIES.register("essence_tank", () -> new BlockEntityType<>(EssenceTankBlockEntity::new, ModBlocks.ESSENCE_TANK.get(),
                    ModBlocks.CREATIVE_ESSENCE_TANK.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.BigEssenceJarBlockEntity>> BIG_ESSENCE_JAR =
            BLOCK_ENTITIES.register("big_essence_jar", () -> new BlockEntityType<>(com.pockyl.hextale.block.BigEssenceJarBlockEntity::new,
                    ModBlocks.BIG_ESSENCE_JAR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.multiblock.MultiblockPartBlockEntity>> MULTIBLOCK_PART =
            BLOCK_ENTITIES.register("multiblock_part", () -> new BlockEntityType<>(com.pockyl.hextale.multiblock.MultiblockPartBlockEntity::new,
                    ModBlocks.MULTIBLOCK_PART.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrainValveBlockEntity>> DRAIN_VALVE =
            BLOCK_ENTITIES.register("drain_valve", () -> new BlockEntityType<>(DrainValveBlockEntity::new, ModBlocks.DRAIN_VALVE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PumpBlockEntity>> PUMP =
            BLOCK_ENTITIES.register("pump", () -> new BlockEntityType<>(PumpBlockEntity::new, ModBlocks.PUMP.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HextechAltarBlockEntity>> HEXTECH_ALTAR =
            BLOCK_ENTITIES.register("hextech_altar", () -> new BlockEntityType<>(HextechAltarBlockEntity::new, ModBlocks.HEXTECH_ALTAR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PedestalBlockEntity>> PEDESTAL =
            BLOCK_ENTITIES.register("pedestal", () -> new BlockEntityType<>(PedestalBlockEntity::new, ModBlocks.PEDESTAL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RuneSteleBlockEntity>> RUNE_STELE =
            BLOCK_ENTITIES.register("rune_stele", () -> new BlockEntityType<>(RuneSteleBlockEntity::new, ModBlocks.RUNE_STELE.get()));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
