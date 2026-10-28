package com.pockyl.hextale.registry;

import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.pockyl.hextale.port.DelegatingResourceHandler;
import com.pockyl.hextale.port.RangedResourceHandler;
import com.pockyl.hextale.port.ResourceHandler;
import com.pockyl.hextale.port.ItemResource;
import com.pockyl.hextale.port.VanillaContainerWrapper;
import com.pockyl.hextale.port.WorldlyContainerWrapper;
import com.pockyl.hextale.port.TransactionContext;

import com.pockyl.hextale.block.HexWorkbenchBlockEntity;

/**
 * The item handlers of the mod's machines, so hex pylons (and other mods' pipes) can feed and empty them like hoppers
 * do: each sided machine takes and gives items through the same faces as for a hopper. Fluid handlers let other mods'
 * pipes fill the water vessels, draw liquid experience (when a mod adds it) from the Experience Condenser and move pure
 * essence in and out of Essence Jars. (1.21.1: the mod's own 26.x-style handlers are handed over as item and fluid
 * handlers.)
 */
public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModCapabilities::onRegister);
    }

    private static void onRegister(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ALEMBIC.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SEPARATOR.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.HEX_FURNACE.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.HEX_FOUNDRY.get(), WorldlyContainerWrapper::new);
        // A structure that shares its items takes and gives them through every block, as its controller would.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MULTIBLOCK_PART.get(), (part, side) -> {
            net.minecraft.world.WorldlyContainer container = part.sharedContainer();
            return container == null ? null : new WorldlyContainerWrapper(container, side);
        });
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.RESEARCH_TABLE.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.PEDESTAL.get(), (pedestal, side) -> VanillaContainerWrapper.of(pedestal));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.DISINTEGRATOR.get(), (bowl, side) -> VanillaContainerWrapper.of(bowl));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.RIFT_GATE.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.RIFT_GATE.get(), (gate, side) -> gate.fluidMouth());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CHEM_BURNER.get(), (burner, side) -> VanillaContainerWrapper.of(burner));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.AXOLOTL_TANK.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CHEM_STILL.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.LAB_BENCH.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.PINK_BRAZIER.get(), (brazier, side) -> VanillaContainerWrapper.of(brazier));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.HEXTECH_PURIFIER.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.VOID_ANCHOR.get(), WorldlyContainerWrapper::new);
        // Crystal in, cut stone out: the cut itself stays the worker's (top: crystals, sides: discs, bottom: the result).
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.LAPIDARY.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CRYSTAL_BATTERY.get(), (battery, side) -> ResourceHandler.asItemHandler(battery.intake()));
        // Only the crystal socket: the grid stays the crafter's.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.HEX_WORKBENCH.get(), (bench, side) ->
                ResourceHandler.asItemHandler(insertOnly(RangedResourceHandler.ofSingleIndex(VanillaContainerWrapper.of(bench), HexWorkbenchBlockEntity.CRYSTAL))));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CRUCIBLE.get(), (crucible, side) ->
                side == Direction.DOWN ? null : ResourceHandler.asItemHandler(crucible.itemIntake()));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.CRUCIBLE.get(), (crucible, side) -> ResourceHandler.asFluidHandler(crucible.waterInlet()));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.ALEMBIC.get(), (alembic, side) ->
                ResourceHandler.asFluidHandler(side == null || side.getAxis().isHorizontal() ? alembic.waterInlet() : null));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.XP_CONDENSER.get(), (condenser, side) -> ResourceHandler.asFluidHandler(condenser.outlet()));
        // Pure essence as a fluid: Essence Jars (the creative one too), the Big Essence Jar through every block of it.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.ESSENCE_TANK.get(), (jar, side) -> ResourceHandler.asFluidHandler(jar.fluidPort()));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.BIG_ESSENCE_JAR.get(), (jar, side) -> ResourceHandler.asFluidHandler(jar.fluidPort()));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MULTIBLOCK_PART.get(), (part, side) ->
                ResourceHandler.asFluidHandler(part.core() instanceof com.pockyl.hextale.block.EssenceTankBlockEntity jar ? jar.fluidPort() : null));
    }

    /** The handler with taking out refused. */
    private static ResourceHandler<ItemResource> insertOnly(ResourceHandler<ItemResource> handler) {
        return new DelegatingResourceHandler<>(handler) {
            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return 0;
            }

            @Override
            public int extract(ItemResource resource, int amount, TransactionContext transaction) {
                return 0;
            }
        };
    }
}
