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

    /** The Void's own blood, pooled in the hollows of its plates. */
    public static final DeferredHolder<FluidType, FluidType> VOID_ICHOR_TYPE = FLUID_TYPES.register("void_ichor",
            com.pockyl.hextale.thevoid.realm.VoidIchor::type);
    public static final DeferredHolder<Fluid, net.neoforged.neoforge.fluids.BaseFlowingFluid.Source> VOID_ICHOR = FLUIDS.register("void_ichor",
            () -> new net.neoforged.neoforge.fluids.BaseFlowingFluid.Source(com.pockyl.hextale.thevoid.realm.VoidIchor.properties()));
    public static final DeferredHolder<Fluid, net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing> FLOWING_VOID_ICHOR = FLUIDS.register(
            "flowing_void_ichor", () -> new net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing(com.pockyl.hextale.thevoid.realm.VoidIchor.properties()));

    private ModFluids() {
    }

    public static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
    }
}
