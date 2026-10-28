package com.pockyl.hextale.essence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import com.pockyl.hextale.port.FluidResource;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModFluids;
import com.pockyl.hextale.rune.Essence;

import org.jspecify.annotations.Nullable;

/**
 * Pure essence as a fluid ({@code hextale:essence}), so the tanks, pipes and storage networks of other mods can hold and
 * move it: one fluid whose stacks carry the essence in the component {@code hextale:essence_kind}, 1 mB of fluid = 1 mB
 * of essence. Only a single essence ever becomes fluid - mixtures stay in the mod's own vessels and are never split.
 * The fluid without a kind is not essence at all: nothing gives it and nothing takes it.
 * <p>
 * It never stands in the world: no block, no bucket, no flowing form.
 */
public final class EssenceFluid extends Fluid {
    private static volatile FluidResource @Nullable [] resources;

    /** The fluid of one essence, as a resource for the transfer API. */
    public static FluidResource resource(Essence essence) {
        FluidResource[] known = resources;
        if (known == null) {
            known = new FluidResource[Essence.COUNT];
            for (Essence each : Essence.values()) {
                known[each.ordinal()] = FluidResource.of(ModFluids.ESSENCE.get()).with(ModDataComponents.ESSENCE_KIND.get(), each);
            }
            resources = known;
        }
        return known[essence.ordinal()];
    }

    /** {@code amount} mB of the fluid of one essence. */
    public static FluidStack stack(Essence essence, int amount) {
        return resource(essence).toStack(amount);
    }

    /**
     * The essence a fluid resource is, or null when it is no essence fluid, has no kind or carries anything else (which
     * is never essence).
     */
    public static @Nullable Essence kind(FluidResource resource) {
        if (resource.isEmpty() || resource.getFluid() != ModFluids.ESSENCE.get()) {
            return null;
        }
        Essence essence = resource.get(ModDataComponents.ESSENCE_KIND.get());
        return essence != null && resource.equals(resource(essence)) ? essence : null;
    }

    /** The essence of a fluid stack (see {@link #kind(FluidResource)}). */
    public static @Nullable Essence kind(FluidStack stack) {
        return stack.isEmpty() ? null : kind(FluidResource.of(stack));
    }

    @Override
    public FluidType getFluidType() {
        return ModFluids.ESSENCE_TYPE.get();
    }

    @Override
    public Item getBucket() {
        return Items.AIR;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
        return true;
    }

    @Override
    protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState fluidState) {
        return Vec3.ZERO;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 0;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public float getHeight(FluidState fluidState, BlockGetter level, BlockPos pos) {
        return 0.0F;
    }

    @Override
    public float getOwnHeight(FluidState fluidState) {
        return 0.0F;
    }

    /** Never placed: the "block" of the fluid is air, so buckets and fluid placers of other mods refuse it. */
    @Override
    protected BlockState createLegacyBlock(FluidState fluidState) {
        return Blocks.AIR.defaultBlockState();
    }

    /** Its only state counts as a source, as tanks and recipe viewers expect of a storable fluid. */
    @Override
    public boolean isSource(FluidState fluidState) {
        return true;
    }

    @Override
    public int getAmount(FluidState fluidState) {
        return 8;
    }

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    /** The essence fluid's type: named after the essence a stack carries ("Fire Essence"). */
    public static final class Type extends FluidType {
        public Type() {
            super(Properties.create().canConvertToSource(false).canSwim(false).canDrown(false).canPushEntity(false)
                    .canExtinguish(false).canHydrate(false).supportsBoating(false).lightLevel(10).density(1100).viscosity(1500));
        }

        @Override
        public String getDescriptionId(FluidStack stack) {
            Essence essence = kind(stack);
            return essence == null ? getDescriptionId() : getDescriptionId() + "." + essence.id();
        }
    }
}
