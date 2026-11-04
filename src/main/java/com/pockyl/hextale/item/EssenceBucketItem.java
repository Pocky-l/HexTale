package com.pockyl.hextale.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import com.pockyl.hextale.essence.EssenceFluid;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * A bucket of one pure essence (GitHub issue #8: buckets suit storage mods better than tanks): 1000 mB of the essence
 * fluid, never poured out into the world. An empty bucket fills from an Essence Jar holding a bucketful (a Big Essence
 * Jar) or from another mod's tank; this bucket pours back into them. Item storage of other mods keeps it as an item.
 */
public class EssenceBucketItem extends Item {
    public EssenceBucketItem(Properties properties) {
        super(properties);
    }

    /** A bucket of this essence. */
    public static ItemStack of(Essence essence) {
        ItemStack stack = new ItemStack(ModItems.ESSENCE_BUCKET.get());
        stack.set(ModDataComponents.ESSENCE_KIND.get(), essence);
        return stack;
    }

    public static @Nullable Essence essence(ItemStack stack) {
        return stack.get(ModDataComponents.ESSENCE_KIND.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        Essence essence = essence(stack);
        return essence == null ? super.getName(stack)
                : Component.translatable("item.hextale.essence_bucket.of", Component.translatable(essence.translationKey())).withColor(essence.color());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hextale.essence_bucket.tooltip").withColor(0xA0A0A0));
    }

    /** The fluid view of a bucket of essence, for other mods' tanks and pipes (and the jars). */
    public static final class Handler extends ItemAccessResourceHandler<FluidResource> {
        public Handler(ItemAccess access) {
            super(access, 1);
        }

        @Override
        protected FluidResource getResourceFrom(ItemResource resource, int index) {
            Essence essence = resource.get(ModDataComponents.ESSENCE_KIND.get());
            return essence == null || !resource.is(ModItems.ESSENCE_BUCKET.get()) ? FluidResource.EMPTY : EssenceFluid.resource(essence);
        }

        @Override
        protected int getAmountFrom(ItemResource resource, int index) {
            return getResourceFrom(resource, index).isEmpty() ? 0 : FluidType.BUCKET_VOLUME;
        }

        @Override
        protected ItemResource update(ItemResource resource, int index, FluidResource newResource, int newAmount) {
            if (newAmount == 0) {
                return ItemResource.of(Items.BUCKET);
            }
            Essence essence = EssenceFluid.kind(newResource);
            return newAmount != FluidType.BUCKET_VOLUME || essence == null ? ItemResource.EMPTY : ItemResource.of(of(essence));
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return EssenceFluid.kind(resource) != null;
        }

        @Override
        protected int getCapacity(int index, FluidResource resource) {
            return FluidType.BUCKET_VOLUME;
        }
    }
}
