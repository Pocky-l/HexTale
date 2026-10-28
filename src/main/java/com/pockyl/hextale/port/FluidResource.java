package com.pockyl.hextale.port;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import org.jspecify.annotations.Nullable;

/**
 * A fluid with its components and no amount (26.x transfer API), over a 1.21.1 {@link FluidStack} of one unit.
 *
 * <p>port-of: net.neoforged.neoforge.transfer.fluid.FluidResource
 */
public final class FluidResource {
    public static final FluidResource EMPTY = new FluidResource(FluidStack.EMPTY);
    private final FluidStack stack;

    private FluidResource(FluidStack stack) {
        this.stack = stack;
    }

    public static FluidResource of(FluidStack stack) {
        return stack.isEmpty() ? EMPTY : new FluidResource(stack.copyWithAmount(1));
    }

    public static FluidResource of(Fluid fluid) {
        return fluid == Fluids.EMPTY ? EMPTY : new FluidResource(new FluidStack(fluid, 1));
    }

    public static FluidResource of(Fluid fluid, DataComponentPatch patch) {
        return fluid == Fluids.EMPTY ? EMPTY : new FluidResource(new FluidStack(Holder.direct(fluid), 1, patch));
    }

    /** A copy with one component set. */
    public <T> FluidResource with(DataComponentType<T> type, T value) {
        FluidStack copy = stack.copy();
        copy.set(type, value);
        return of(copy);
    }

    public boolean isComponentsPatchEmpty() {
        return stack.getComponentsPatch().isEmpty();
    }

    /** The fluid (26.x: its type holder; the 1.21.1 fluid answers the same tag questions). */
    public Fluid getType() {
        return stack.getFluid();
    }

    public FluidStack toStack(int amount) {
        return stack.isEmpty() ? FluidStack.EMPTY : stack.copyWithAmount(amount);
    }

    public Fluid getFluid() {
        return stack.getFluid();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public boolean is(Fluid fluid) {
        return stack.is(fluid);
    }

    public boolean is(TagKey<Fluid> tag) {
        return stack.is(tag);
    }

    public boolean matches(FluidStack other) {
        return FluidStack.isSameFluidSameComponents(stack, other);
    }

    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        return stack.get(type);
    }

    public DataComponentPatch getComponentsPatch() {
        return stack.getComponentsPatch();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FluidResource resource && FluidStack.isSameFluidSameComponents(stack, resource.stack);
    }

    @Override
    public int hashCode() {
        return FluidStack.hashFluidAndComponents(stack);
    }

    @Override
    public String toString() {
        return stack.isEmpty() ? "FluidResource[empty]" : "FluidResource[" + stack.getFluid() + stack.getComponentsPatch() + "]";
    }
}
