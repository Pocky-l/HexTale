package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.essence.EssenceFluid;

/** The mod's fluids: pure essence for the tanks and pipes of other mods ({@link EssenceFluid}). */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Hextale.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Hextale.MOD_ID);

    public static final DeferredHolder<FluidType, EssenceFluid.Type> ESSENCE_TYPE = FLUID_TYPES.register("essence", EssenceFluid.Type::new);
    public static final DeferredHolder<Fluid, EssenceFluid> ESSENCE = FLUIDS.register("essence", EssenceFluid::new);

    private ModFluids() {
    }

    public static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
    }
}
