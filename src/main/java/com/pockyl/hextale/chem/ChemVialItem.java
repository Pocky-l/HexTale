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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.entity.ThrownChemVial;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * A stoppered glass vial of a crucible brew (component {@code hextale:brew}): a glass bottle on a crucible that holds a
 * brew takes one dose. Splash brews are thrown (right click, or from a dispenser); a vial of combustible mix feeds a
 * Chem Burner and burns in any furnace as long as a blaze rod, leaving the glass bottle. Refined shimmer is drunk: a
 * rush of strength, speed, toughness and healing with no price to pay, and then a while before the next vial works.
 * On 1.21.1 the drink, the fuel and the shimmer cooldown are this item's own code (26.x does them with item components).
 */
public class ChemVialItem extends Item implements ProjectileItem {
    private static final int THROW_COOLDOWN = 8;
    /** Seconds before the next vial of refined shimmer can be drunk. */
    public static final float SHIMMER_COOLDOWN = 120;
    /** What a vial of refined shimmer gives: all buffs, nothing taken. */
    public static final int SHIMMER_TICKS = 1200;
    /** Burns as long as a blaze rod. */
    private static final int FUEL_TICKS = 2400;
    /** Game time until which the player's next vial of refined shimmer does not work (persistent player data). */
    private static final String SHIMMER_UNTIL = "hextale_shimmer_until";

    public ChemVialItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Brew brew) {
        return of(brew, 1);
    }

    public static ItemStack of(Brew brew, int count) {
        ItemStack stack = new ItemStack(ModItems.CHEM_VIAL.get(), count);
        stack.set(ModDataComponents.BREW.get(), brew);
        return stack;
    }

    /** The rush of a vial of refined shimmer. */
    public static List<MobEffectInstance> shimmerEffects() {
        return List.of(new MobEffectInstance(MobEffects.DAMAGE_BOOST, SHIMMER_TICKS, 1),
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SHIMMER_TICKS, 0),
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SHIMMER_TICKS, 0),
                new MobEffectInstance(MobEffects.REGENERATION, SHIMMER_TICKS / 3, 1),
                new MobEffectInstance(MobEffects.DIG_SPEED, SHIMMER_TICKS, 0));
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
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
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

    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Brew brew = brew(stack);
        if (brew == Brew.REFINED_SHIMMER) {
            if (player.getPersistentData().getLong(SHIMMER_UNTIL) > level.getGameTime()) {
                return InteractionResult.FAIL;
            }
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        if (brew == null || !brew.thrown()) {
            return super.use(level, player, hand).getResult();
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel serverLevel) {
            ThrownChemVial vial = new ThrownChemVial(serverLevel, player, stack.copyWithCount(1));
            vial.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.6F, 1.0F);
            serverLevel.addFreshEntity(vial);
        }
        player.getCooldowns().addCooldown(stack.getItem(), THROW_COOLDOWN);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.Interactions.holder(usePort(level, player, hand), player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return brew(stack) == Brew.REFINED_SHIMMER ? UseAnim.DRINK : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return brew(stack) == Brew.REFINED_SHIMMER ? 32 : 0;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.HONEY_DRINK;
    }

    /** A vial of refined shimmer drunk: its rush, the wait before the next one, the empty glass bottle. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (brew(stack) != Brew.REFINED_SHIMMER) {
            return stack;
        }
        if (!level.isClientSide()) {
            shimmerEffects().forEach(effect -> entity.addEffect(new MobEffectInstance(effect)));
            entity.getPersistentData().putLong(SHIMMER_UNTIL, level.getGameTime() + (long) (SHIMMER_COOLDOWN * 20));
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (entity instanceof Player player && !player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
            player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        }
        return stack;
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return brew(stack) == Brew.FUEL ? FUEL_TICKS : 0;
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
