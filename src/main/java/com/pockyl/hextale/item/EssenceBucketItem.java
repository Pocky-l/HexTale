package com.pockyl.hextale.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

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
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        tooltip.accept(Component.translatable("item.hextale.essence_bucket.tooltip").withColor(0xA0A0A0));
    }

    /**
     * The fluid view of a bucket of essence, for other mods' tanks and pipes (and the jars).
     *
     * <p>1.21.1: an {@link IFluidHandlerItem} over the stack (26.x: a resource handler over the item access).
     */
    public static final class Handler implements IFluidHandlerItem {
        private ItemStack container;

        public Handler(ItemStack container) {
            this.container = container;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            Essence essence = container.is(ModItems.ESSENCE_BUCKET.get()) ? essence(container) : null;
            return essence == null ? FluidStack.EMPTY : EssenceFluid.resource(essence).toStack(FluidType.BUCKET_VOLUME);
        }

        @Override
        public int getTankCapacity(int tank) {
            return FluidType.BUCKET_VOLUME;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return EssenceFluid.kind(stack) != null;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            Essence essence = EssenceFluid.kind(resource);
            if (container.getCount() != 1 || !container.is(Items.BUCKET) || essence == null || resource.getAmount() < FluidType.BUCKET_VOLUME) {
                return 0;
            }
            if (action.execute()) {
                container = of(essence);
            }
            return FluidType.BUCKET_VOLUME;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack inside = getFluidInTank(0);
            return inside.isEmpty() || !FluidStack.isSameFluidSameComponents(inside, resource) ? FluidStack.EMPTY : drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack inside = getFluidInTank(0);
            if (container.getCount() != 1 || inside.isEmpty() || maxDrain < FluidType.BUCKET_VOLUME) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                container = new ItemStack(Items.BUCKET);
            }
            return inside;
        }
    }
}
