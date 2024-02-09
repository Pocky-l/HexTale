package com.pocky.hextale.common.items.tools.crystal;

import com.pocky.hextale.client.render.item.CrystalClientItemExtensions;
import com.pocky.hextale.utils.ModColors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class CrystalBowItem extends BowItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean animate = false;

    private static final RawAnimation STRINGING = RawAnimation.begin().thenPlay("animation.crystal_bow.stringing").thenLoop("animation.crystal_bow.stringing_idle");

    public static final String ID = "crystal_bow";

    public CrystalBowItem() {
        super((new Item.Properties()).durability(384).stacksTo(1));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "stringing", 5, this::animation));
    }

    protected <E extends CrystalBowItem> PlayState animation(final AnimationState<E> event) {
        if (animate) {
            animate = false;
            return event.setAndContinue(STRINGING);
        }
        return PlayState.STOP;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack p_40678_) {
        animate = true;
        return UseAnim.BOW;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new CrystalClientItemExtensions<>(ID));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack item, @Nullable Level level, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Component.translatable("tooltip." + item.getDescriptionId())
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(ModColors.HEXTECH))));
        super.appendHoverText(item, level, tooltip, tooltipFlag);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
