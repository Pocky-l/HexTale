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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.entity.ThrownChemVial;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * A stoppered glass vial of a crucible brew (component {@code hextale:brew}): a glass bottle on a crucible that holds a
 * brew takes one dose. Splash brews are thrown (right click, or from a dispenser); a vial of combustible mix feeds a
 * Chem Burner and burns in any furnace as long as a blaze rod, leaving the glass bottle. Refined shimmer is drunk: a
 * rush of strength, speed, toughness and healing with no price to pay, and then a while before the next vial works.
 */
public class ChemVialItem extends Item implements ProjectileItem {
    private static final int THROW_COOLDOWN = 8;
    /** Seconds before the next vial of refined shimmer can be drunk. */
    public static final float SHIMMER_COOLDOWN = 120;
    /** What a vial of refined shimmer gives: all buffs, nothing taken. */
    public static final int SHIMMER_TICKS = 1200;

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
            // 26.1.2: a fuel vial burns through getBurnTime below (26.3: cooking_fuel component).
        } else if (brew == Brew.REFINED_SHIMMER) {
            stack.set(DataComponents.CONSUMABLE, Consumables.defaultDrink().sound(SoundEvents.HONEY_DRINK)
                    .onConsume(new ApplyStatusEffectsConsumeEffect(shimmerEffects())).build());
            stack.set(DataComponents.USE_REMAINDER, new UseRemainder(new ItemStackTemplate(Items.GLASS_BOTTLE)));
            stack.set(DataComponents.USE_COOLDOWN, new UseCooldown(SHIMMER_COOLDOWN, Optional.of(Hextale.id("shimmer"))));
        }
        return stack;
    }

    /** The rush of a vial of refined shimmer. */
    public static List<MobEffectInstance> shimmerEffects() {
        return List.of(new MobEffectInstance(MobEffects.STRENGTH, SHIMMER_TICKS, 1),
                new MobEffectInstance(MobEffects.SPEED, SHIMMER_TICKS, 0),
                new MobEffectInstance(MobEffects.RESISTANCE, SHIMMER_TICKS, 0),
                new MobEffectInstance(MobEffects.REGENERATION, SHIMMER_TICKS / 3, 1),
                new MobEffectInstance(MobEffects.HASTE, SHIMMER_TICKS, 0));
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
            } else if (brew == Brew.REFINED_SHIMMER) {
                tooltip.accept(Component.translatable("item.hextale.chem_vial.drink").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Brew brew = brew(stack);
        if (brew == null || !brew.thrown()) {
            return super.use(level, player, hand);
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

    /** A fuel vial burns as long as a blaze rod. */
    @Override
    public int getBurnTime(ItemStack stack, net.minecraft.world.item.crafting.@Nullable RecipeType<?> recipeType,
            net.minecraft.world.level.block.entity.FuelValues fuelValues) {
        return stack.get(ModDataComponents.BREW.get()) == Brew.FUEL
                ? Items.BLAZE_ROD.getDefaultInstance().getBurnTime(recipeType, fuelValues)
                : super.getBurnTime(stack, recipeType, fuelValues);
    }
}
