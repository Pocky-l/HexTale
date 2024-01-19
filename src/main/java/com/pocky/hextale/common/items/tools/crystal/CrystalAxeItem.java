package com.pocky.hextale.common.items.tools.crystal;

import com.pocky.hextale.client.render.item.CrystalClientItemExtensions;
import com.pocky.hextale.utils.ExcavateUtils;
import com.pocky.hextale.utils.ModColors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class CrystalAxeItem extends AxeItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final String ID = "crystal_axe";

    public CrystalAxeItem() {
        super(Tiers.NETHERITE, 4, -2.8F, new Item.Properties());
    }

    @Override
    public boolean mineBlock(@NotNull ItemStack item, @NotNull Level level, @NotNull BlockState blockState,
                             @NotNull BlockPos blockPos, @NotNull LivingEntity entity) {
        if (entity instanceof ServerPlayer player && !player.isShiftKeyDown() && blockState.is(BlockTags.LOGS)) {
            ExcavateUtils.veinMineArea(level, blockPos, player, item, item.getItem(), 128);
        }
        return super.mineBlock(item, level, blockState, blockPos, entity);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int n, boolean inHand) {
        if (inHand) {
            if (entity instanceof ServerPlayer player) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60));
            }
        }
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
