package com.pockyl.hextale.client.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.essence.EssenceFluid;
import com.pockyl.hextale.registry.ModFluids;
import com.pockyl.hextale.rune.Essence;

/**
 * How the essence fluid looks in the screens of other mods (tanks, storage terminals, recipe viewers): the jars' own
 * animated {@code block/essence_liquid} sprite, tinted by the stack's essence in the same vivid colour the jars show. On
 * 26.3 a fluid's sprites and tint come from its fluid model; screens ask the tint for a stack, which reads the essence
 * from the stack's components. (1.21.1: the fluid type's client extensions give the sprites and the stack's tint.)
 */
public final class EssenceFluidModel {
    private static final ResourceLocation LIQUID = Hextale.id("block/essence_liquid");
    /** The fluid without a kind (never made; only a recipe viewer's plain entry could show it): pale grey. */
    private static final int PLAIN = 0xD8D2E0;

    private EssenceFluidModel() {
    }

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return LIQUID;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return LIQUID;
            }

            @Override
            public int getTintColor() {
                return FastColor.ARGB32.opaque(PLAIN);
            }

            @Override
            public int getTintColor(FluidStack stack) {
                Essence essence = EssenceFluid.kind(stack);
                return FastColor.ARGB32.opaque(essence == null ? PLAIN : EssenceTankRenderer.vivid(essence.color()));
            }
        }, ModFluids.ESSENCE_TYPE.get());
        // The Void's ichor wears its own colours, no tint (1.21.1: its sprites are given by IchorView's extensions).
    }
}
