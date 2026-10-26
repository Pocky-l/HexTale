package com.pockyl.hextale.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/**
 * The item handlers of the mod's machines, so hex pylons (and other mods' pipes) can feed and empty them like hoppers
 * do: each sided machine takes and gives items through the same faces as for a hopper.
 */
public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModCapabilities::onRegister);
    }

    private static void onRegister(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.ALEMBIC.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.SEPARATOR.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.HEX_FURNACE.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.HEX_FOUNDRY.get(), WorldlyContainerWrapper::new);
        // A structure that shares its items takes and gives them through every block, as its controller would.
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.MULTIBLOCK_PART.get(), (part, side) -> {
            net.minecraft.world.WorldlyContainer container = part.sharedContainer();
            return container == null ? null : new WorldlyContainerWrapper(container, side);
        });
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.RESEARCH_TABLE.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.PEDESTAL.get(), (pedestal, side) -> VanillaContainerWrapper.of(pedestal));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CHEM_BURNER.get(), (burner, side) -> VanillaContainerWrapper.of(burner));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.AXOLOTL_TANK.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CHEM_STILL.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.LAB_BENCH.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.PINK_BRAZIER.get(), (brazier, side) -> VanillaContainerWrapper.of(brazier));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.HEXTECH_PURIFIER.get(), WorldlyContainerWrapper::new);
    }
}
