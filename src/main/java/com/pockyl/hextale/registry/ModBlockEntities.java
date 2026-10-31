package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.RuneCircleBlockEntity;
import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.block.DrainValveBlockEntity;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.PumpBlockEntity;
import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.block.HexFurnaceBlockEntity;
import com.pockyl.hextale.block.HextechAltarBlockEntity;
import com.pockyl.hextale.block.HexcoreBlockEntity;
import com.pockyl.hextale.block.PedestalBlockEntity;
import com.pockyl.hextale.block.RuneSteleBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Hextale.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE =
            BLOCK_ENTITIES.register("research_table",
                    () -> com.pockyl.hextale.port.PortTypes.blockEntity(ResearchTableBlockEntity::new, ModBlocks.RESEARCH_TABLE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.CrucibleBlockEntity>> CRUCIBLE =
            BLOCK_ENTITIES.register("crucible", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.CrucibleBlockEntity::new,
                    ModBlocks.CRUCIBLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.ChemBurnerBlockEntity>> CHEM_BURNER =
            BLOCK_ENTITIES.register("chem_burner", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.ChemBurnerBlockEntity::new,
                    ModBlocks.CHEM_BURNER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.AxolotlTankBlockEntity>> AXOLOTL_TANK =
            BLOCK_ENTITIES.register("axolotl_tank", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.AxolotlTankBlockEntity::new,
                    ModBlocks.AXOLOTL_TANK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.LabBenchBlockEntity>> LAB_BENCH =
            BLOCK_ENTITIES.register("lab_bench", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.LabBenchBlockEntity::new,
                    ModBlocks.LAB_BENCH.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.ChemStillBlockEntity>> CHEM_STILL =
            BLOCK_ENTITIES.register("chem_still", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.ChemStillBlockEntity::new,
                    ModBlocks.CHEM_STILL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.ChemBoilerBlockEntity>> CHEM_BOILER =
            BLOCK_ENTITIES.register("chem_boiler", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.ChemBoilerBlockEntity::new,
                    ModBlocks.CHEM_BOILER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.CrystalBatteryBlockEntity>> CRYSTAL_BATTERY =
            BLOCK_ENTITIES.register("crystal_battery", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.CrystalBatteryBlockEntity::new,
                    ModBlocks.CRYSTAL_BATTERY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.PinkBrazierBlockEntity>> PINK_BRAZIER =
            BLOCK_ENTITIES.register("pink_brazier", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.PinkBrazierBlockEntity::new,
                    ModBlocks.PINK_BRAZIER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.HexWorkbenchBlockEntity>> HEX_WORKBENCH =
            BLOCK_ENTITIES.register("hex_workbench", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.HexWorkbenchBlockEntity::new,
                    ModBlocks.HEX_WORKBENCH.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.LapidaryBlockEntity>> LAPIDARY =
            BLOCK_ENTITIES.register("hex_lapidary", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.LapidaryBlockEntity::new,
                    ModBlocks.HEX_LAPIDARY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.HexFoundryBlockEntity>> HEX_FOUNDRY =
            BLOCK_ENTITIES.register("hex_foundry", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.HexFoundryBlockEntity::new,
                    ModBlocks.HEX_FOUNDRY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HexFurnaceBlockEntity>> HEX_FURNACE =
            BLOCK_ENTITIES.register("hex_furnace", () -> com.pockyl.hextale.port.PortTypes.blockEntity(HexFurnaceBlockEntity::new, ModBlocks.HEX_FURNACE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.WhirlReaperBlockEntity>> WHIRL_REAPER =
            BLOCK_ENTITIES.register("whirl_reaper", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.WhirlReaperBlockEntity::new,
                    ModBlocks.WHIRL_REAPER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.HexSawmillBlockEntity>> HEX_SAWMILL =
            BLOCK_ENTITIES.register("hex_sawmill", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.HexSawmillBlockEntity::new,
                    ModBlocks.HEX_SAWMILL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.HexWispBlock.Glow>> HEX_WISP =
            BLOCK_ENTITIES.register("hex_wisp", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.HexWispBlock.Glow::new,
                    ModBlocks.HEX_WISP.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.DisintegratorBlock.Bowl>> DISINTEGRATOR =
            BLOCK_ENTITIES.register("disintegrator", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.DisintegratorBlock.Bowl::new,
                    ModBlocks.DISINTEGRATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.WorldAnchorBlock.Anchor>> WORLD_ANCHOR =
            BLOCK_ENTITIES.register("world_anchor", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.WorldAnchorBlock.Anchor::new,
                    ModBlocks.WORLD_ANCHOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.RiftGateBlock.Gate>> RIFT_GATE =
            BLOCK_ENTITIES.register("rift_gate", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.RiftGateBlock.Gate::new,
                    ModBlocks.RIFT_GATE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.ImpulsePlateBlock.Plate>> IMPULSE_PLATE =
            BLOCK_ENTITIES.register("impulse_plate", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.ImpulsePlateBlock.Plate::new,
                    ModBlocks.IMPULSE_PLATE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.NullLanternBlockEntity>> NULL_LANTERN =
            BLOCK_ENTITIES.register("null_lantern", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.NullLanternBlockEntity::new,
                    ModBlocks.NULL_LANTERN.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.LevitationPlateBlockEntity>> LEVITATION_PLATE =
            BLOCK_ENTITIES.register("levitation_plate", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.LevitationPlateBlockEntity::new,
                    ModBlocks.LEVITATION_PLATE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.GreedyFunnelBlockEntity>> GREEDY_FUNNEL =
            BLOCK_ENTITIES.register("greedy_funnel", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.GreedyFunnelBlockEntity::new,
                    ModBlocks.GREEDY_FUNNEL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.VoidAnchorBlockEntity>> VOID_ANCHOR =
            BLOCK_ENTITIES.register("void_anchor", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.VoidAnchorBlockEntity::new,
                    ModBlocks.VOID_ANCHOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.HextechPurifierBlockEntity>> HEXTECH_PURIFIER =
            BLOCK_ENTITIES.register("hextech_purifier", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.HextechPurifierBlockEntity::new,
                    ModBlocks.HEXTECH_PURIFIER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.HextechLamppostBlockEntity>> HEXTECH_LAMPPOST =
            BLOCK_ENTITIES.register("hextech_lamppost", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.HextechLamppostBlockEntity::new,
                    ModBlocks.HEXTECH_LAMPPOST.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.ArcSpireBlockEntity>> ARC_SPIRE =
            BLOCK_ENTITIES.register("arc_spire", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.ArcSpireBlockEntity::new,
                    ModBlocks.ARC_SPIRE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.XpCondenserBlockEntity>> XP_CONDENSER =
            BLOCK_ENTITIES.register("xp_condenser", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.XpCondenserBlockEntity::new,
                    ModBlocks.XP_CONDENSER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.MiasmaVentBlockEntity>> MIASMA_VENT =
            BLOCK_ENTITIES.register("miasma_vent", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.MiasmaVentBlockEntity::new,
                    ModBlocks.MIASMA_VENT.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.HexPylonBlockEntity>> HEX_PYLON =
            BLOCK_ENTITIES.register("hex_pylon", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.HexPylonBlockEntity::new,
                    ModBlocks.GATHERING_PYLON.get(), ModBlocks.SENDING_PYLON.get(), ModBlocks.RELAY_PYLON.get(),
                    ModBlocks.SIPHON_PYLON.get(), ModBlocks.SORTING_MATRIX.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlembicBlockEntity>> ALEMBIC =
            BLOCK_ENTITIES.register("alembic", () -> com.pockyl.hextale.port.PortTypes.blockEntity(AlembicBlockEntity::new, ModBlocks.ALEMBIC.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SeparatorBlockEntity>> SEPARATOR =
            BLOCK_ENTITIES.register("separator", () -> com.pockyl.hextale.port.PortTypes.blockEntity(SeparatorBlockEntity::new, ModBlocks.SEPARATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssenceTankBlockEntity>> ESSENCE_TANK =
            BLOCK_ENTITIES.register("essence_tank", () -> com.pockyl.hextale.port.PortTypes.blockEntity(EssenceTankBlockEntity::new, ModBlocks.ESSENCE_TANK.get(),
                    ModBlocks.CREATIVE_ESSENCE_TANK.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.block.BigEssenceJarBlockEntity>> BIG_ESSENCE_JAR =
            BLOCK_ENTITIES.register("big_essence_jar", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.block.BigEssenceJarBlockEntity::new,
                    ModBlocks.BIG_ESSENCE_JAR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pockyl.hextale.multiblock.MultiblockPartBlockEntity>> MULTIBLOCK_PART =
            BLOCK_ENTITIES.register("multiblock_part", () -> com.pockyl.hextale.port.PortTypes.blockEntity(com.pockyl.hextale.multiblock.MultiblockPartBlockEntity::new,
                    ModBlocks.MULTIBLOCK_PART.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrainValveBlockEntity>> DRAIN_VALVE =
            BLOCK_ENTITIES.register("drain_valve", () -> com.pockyl.hextale.port.PortTypes.blockEntity(DrainValveBlockEntity::new, ModBlocks.DRAIN_VALVE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PumpBlockEntity>> PUMP =
            BLOCK_ENTITIES.register("pump", () -> com.pockyl.hextale.port.PortTypes.blockEntity(PumpBlockEntity::new, ModBlocks.PUMP.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HextechAltarBlockEntity>> HEXTECH_ALTAR =
            BLOCK_ENTITIES.register("hextech_altar", () -> com.pockyl.hextale.port.PortTypes.blockEntity(HextechAltarBlockEntity::new, ModBlocks.HEXTECH_ALTAR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PedestalBlockEntity>> PEDESTAL =
            BLOCK_ENTITIES.register("pedestal", () -> com.pockyl.hextale.port.PortTypes.blockEntity(PedestalBlockEntity::new, ModBlocks.PEDESTAL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RuneCircleBlockEntity>> RUNE_CIRCLE =
            BLOCK_ENTITIES.register("rune_circle", () -> com.pockyl.hextale.port.PortTypes.blockEntity(RuneCircleBlockEntity::new, ModBlocks.RUNE_CIRCLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HexcoreBlockEntity>> HEXCORE =
            BLOCK_ENTITIES.register("hexcore", () -> com.pockyl.hextale.port.PortTypes.blockEntity(HexcoreBlockEntity::new, ModBlocks.HEXCORE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RuneSteleBlockEntity>> RUNE_STELE =
            BLOCK_ENTITIES.register("rune_stele", () -> com.pockyl.hextale.port.PortTypes.blockEntity(RuneSteleBlockEntity::new, ModBlocks.RUNE_STELE.get()));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
