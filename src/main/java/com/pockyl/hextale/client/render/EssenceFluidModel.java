package com.pockyl.hextale.client.render;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.essence.EssenceFluid;
import com.pockyl.hextale.registry.ModFluids;
import com.pockyl.hextale.rune.Essence;

/**
 * How the essence fluid looks in the screens of other mods (tanks, storage terminals, recipe viewers): the jars' own
 * animated {@code block/essence_liquid} sprite, tinted by the stack's essence in the same vivid colour the jars show. On
 * 26.3 a fluid's sprites and tint come from its fluid model; screens ask the tint for a stack
 * ({@link FluidTintSource#colorAsStack}), which reads the essence from the stack's components.
 */
public final class EssenceFluidModel {
    private static final Material LIQUID = new Material(Hextale.id("block/essence_liquid"));
    /** The fluid without a kind (never made; only a recipe viewer's plain entry could show it): pale grey. */
    private static final int PLAIN = 0xD8D2E0;

    private EssenceFluidModel() {
    }

    public static void register(RegisterFluidModelsEvent event) {
        event.register(new FluidModel.Unbaked(LIQUID, LIQUID, null, Tint.INSTANCE), ModFluids.ESSENCE);
        // The Void's ichor wears its own colours: no tint.
        event.register(new FluidModel.Unbaked(new Material(Hextale.id("block/void_ichor_still")), new Material(Hextale.id("block/void_ichor_flow")),
                null, Plain.INSTANCE), ModFluids.VOID_ICHOR, ModFluids.FLOWING_VOID_ICHOR);
    }

    private enum Plain implements FluidTintSource {
        INSTANCE;

        @Override
        public int color(FluidState state) {
            return 0xFFFFFFFF;
        }

        @Override
        public int colorAsStack(FluidStack stack) {
            return 0xFFFFFFFF;
        }
    }

    private enum Tint implements FluidTintSource {
        INSTANCE;

        @Override
        public int color(FluidState state) {
            return ARGB.opaque(PLAIN);
        }

        @Override
        public int colorAsStack(FluidStack stack) {
            Essence essence = EssenceFluid.kind(stack);
            return ARGB.opaque(essence == null ? PLAIN : EssenceTankRenderer.vivid(essence.color()));
        }
    }
}
