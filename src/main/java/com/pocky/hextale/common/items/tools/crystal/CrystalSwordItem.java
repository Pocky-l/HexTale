package com.pocky.hextale.common.items.tools.crystal;

import com.pocky.hextale.client.render.item.CrystalClientItemExtensions;
import com.pocky.hextale.utils.ModColors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class CrystalSwordItem extends SwordItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final String ID = "crystal_sword";

    public CrystalSwordItem() {
        super(Tiers.NETHERITE, 8, -2.0F, new Item.Properties());
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
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int n, boolean inHand) {
        if (inHand) {
            if (entity instanceof ServerPlayer player) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60));
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
