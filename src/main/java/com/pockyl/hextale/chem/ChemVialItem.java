package com.pockyl.hextale.chem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import com.pockyl.hextale.entity.ThrownChemVial;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * A stoppered glass vial of a crucible brew (component {@code hextale:brew}): a glass bottle on a crucible that holds a
 * brew takes one dose. Splash brews are thrown (right click, or from a dispenser); a vial of combustible mix feeds a
 * Chem Burner and burns in any furnace as long as a blaze rod, leaving the glass bottle.
 */
public class ChemVialItem extends Item implements ProjectileItem {
    private static final int THROW_COOLDOWN = 8;

    public ChemVialItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Brew brew) {
        return of(brew, 1);
    }

    public static ItemStack of(Brew brew, int count) {
        ItemStack stack = new ItemStack(ModItems.CHEM_VIAL.get(), count);
        stack.set(ModDataComponents.BREW.get(), brew);
        if (brew == Brew.FUEL) {
            stack.set(DataComponents.COOKING_FUEL,
                    new CookingFuel(ContextIntProviders.COOKING_TIME_BLAZE_ROD, ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER));
        }
        return stack;
    }

    /** The brew in a vial, or null for anything that is not a filled vial. */
    public static @Nullable Brew brew(ItemStack stack) {
        return stack.is(ModItems.CHEM_VIAL.get()) ? stack.get(ModDataComponents.BREW.get()) : null;
    }

    @Override
    public Component getName(ItemStack stack) {
        Brew brew = brew(stack);
        return brew == null ? super.getName(stack)
                : Component.translatable("item.hextale.chem_vial.of", Component.translatable(brew.translationKey())).withColor(brew.color());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        Brew brew = brew(stack);
        if (brew != null) {
            tooltip.accept(Component.translatable(brew.useKey()).withStyle(ChatFormatting.GRAY));
            if (brew.thrown()) {
                tooltip.accept(Component.translatable("item.hextale.chem_vial.throw").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Brew brew = brew(stack);
        if (brew == null || !brew.thrown()) {
            return InteractionResult.PASS;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel serverLevel) {
            Projectile.spawnProjectileFromRotation(ThrownChemVial::new, serverLevel, stack, player, -20.0F, 0.6F, 1.0F);
        }
        player.getCooldowns().addCooldown(stack, THROW_COOLDOWN);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        ThrownChemVial vial = new ThrownChemVial(ModEntities.THROWN_CHEM_VIAL.get(), level);
        vial.setPos(position.x(), position.y(), position.z());
        vial.setItem(stack);
        return vial;
    }

    @Override
    public DispenseConfig createDispenseConfig() {
        return DispenseConfig.builder().uncertainty(DispenseConfig.DEFAULT.uncertainty() * 0.5F)
                .power(DispenseConfig.DEFAULT.power() * 1.25F).build();
    }
}
