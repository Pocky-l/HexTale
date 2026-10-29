package com.pockyl.hextale.essence;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.network.RuneEffectPayload;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * A bottle of essence: a vanilla glass bottle filled with up to {@value #CAPACITY} mB of one liquid (it does not
 * stack, like a potion); emptied, it turns back into a glass bottle. A bottle of a single element is a concentrate -
 * the only ink the Research Table takes. "Flask" in the code means either: a glass bottle or a bottle of essence.
 */
public class EssenceFlaskItem extends Item {
    /** Millibuckets, like a vanilla bottle. */
    public static final int CAPACITY = 16;

    public EssenceFlaskItem(Properties properties) {
        super(properties);
    }

    public static EssenceMix contents(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ESSENCE.get(), EssenceMix.EMPTY);
    }

    /** A glass bottle or a bottle of essence. */
    public static boolean isFlask(ItemStack stack) {
        return stack.is(ModItems.ESSENCE_FLASK.get()) || isEmptyFlask(stack);
    }

    /** An empty vanilla glass bottle. */
    public static boolean isEmptyFlask(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE);
    }

    /** A bottle holding this essence (a glass bottle for no essence). */
    public static ItemStack filled(EssenceMix essence) {
        if (essence.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        ItemStack stack = new ItemStack(ModItems.ESSENCE_FLASK.get());
        stack.set(ModDataComponents.ESSENCE.get(), essence);
        return stack;
    }

    /**
     * A sip of essence: every primal it holds gives its own short gift (fire - no burns, water - breath, earth - a full
     * belly, air - a soft fall, light - sight in the dark, shadow - unseen, life - healing, metal - quick hands, spark -
     * light feet), longer the more of it there is. The unknown essence turns the stomach.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            drink(contents(stack), entity);
            if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
                // Light essence washes shimmer out of the veins.
                com.pockyl.hextale.chem.ShimmerVeins.wash(player, contents(stack));
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    /** Millibuckets of a primal a gift needs. */
    public static final int GIFT_MIN = 2;

    /** Gives {@code drinker} what a sip of {@code essence} gives; returns how many gifts it gave. */
    public static int drink(EssenceMix essence, LivingEntity drinker) {
        double[] primal = new double[Essence.PRIMALS.size()];
        essence.asMap().forEach((kind, amount) -> {
            double[] share = kind.composition();
            for (int i = 0; i < primal.length; i++) {
                primal[i] += amount * share[i];
            }
        });
        int gifts = 0;
        List<Integer> given = new ArrayList<>();
        for (int i = 0; i < primal.length; i++) {
            if (primal[i] < GIFT_MIN) {
                continue;
            }
            given.add(i);
            int ticks = (int) Math.round((15 + primal[i] * 3) * 20);
            Holder<MobEffect> effect = GIFTS.get(i);
            if (effect == MobEffects.SATURATION) {
                drinker.addEffect(new MobEffectInstance(effect, 1 + (int) (primal[i] / 4)));
            } else {
                drinker.addEffect(new MobEffectInstance(effect, ticks));
            }
            gifts++;
        }
        if (essence.unknown() > 0) {
            drinker.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200 + essence.unknown() * 10));
            drinker.addEffect(new MobEffectInstance(MobEffects.POISON, 40 + essence.unknown() * 5));
        }
        if (drinker.level() instanceof ServerLevel server) {
            server.sendParticles(com.pockyl.hextale.registry.ModParticles.wisp(essence.color()), drinker.getX(), drinker.getY() + 1.2, drinker.getZ(),
                    10, 0.3, 0.4, 0.3, 0.02);
            given.sort(Comparator.comparingDouble(i -> -primal[i]));
            for (int k = 0; k < Math.min(SHOWN_GIFTS, given.size()); k++) {
                // The strongest gift is heard; the others only seen.
                showGift(server, drinker, Essence.PRIMALS.get(given.get(k)), k == 0);
            }
            if (essence.unknown() > 0) {
                // A bad brew: murky fumes sink round the drinker.
                server.sendParticles(com.pockyl.hextale.registry.ModParticles.vapour(EssenceMix.UNKNOWN_COLOR), drinker.getX(), drinker.getY() + 1.4,
                        drinker.getZ(), 8, 0.3, 0.2, 0.3, 0.0);
                RuneEffectPayload.show(server, RuneEffectPayload.Family.GUST,
                        drinker.position().add(0, 0.6, 0), List.of(), EssenceMix.UNKNOWN_COLOR, 0.8F, false);
            }
        }
        return gifts;
    }

    /** At most this many gifts of one sip are shown, the strongest first. */
    private static final int SHOWN_GIFTS = 3;

    /**
     * Each primal's gift has its own look on the drinker, from the rune effects: fire, water, light, life and metal rise
     * round the body in their colour, air and shadow whirl round it, earth crumbles under the feet, spark arcs into the
     * ground.
     */
    private static void showGift(ServerLevel level, LivingEntity drinker, Essence primal, boolean sound) {
        Vec3 body = drinker.position().add(0, drinker.getBbHeight() * 0.45, 0);
        BlockPos ground = drinker.blockPosition().below();
        RuneEffectPayload.Family family = switch (primal) {
            case AIR, SHADOW -> RuneEffectPayload.Family.GUST;
            case EARTH -> RuneEffectPayload.Family.CRUMBLE;
            case SPARK -> RuneEffectPayload.Family.ARCS;
            default -> RuneEffectPayload.Family.RISE;
        };
        List<BlockPos> blocks = family == RuneEffectPayload.Family.CRUMBLE
                || family == RuneEffectPayload.Family.ARCS ? List.of(ground) : List.of();
        RuneEffectPayload.show(level, family, body, blocks, primal.color(), 0.9F, sound);
    }

    /** The gift of each primal, in {@link Essence#PRIMALS} order. */
    private static final java.util.List<Holder<MobEffect>> GIFTS = java.util.List.of(MobEffects.FIRE_RESISTANCE, MobEffects.WATER_BREATHING,
            MobEffects.SATURATION, MobEffects.SLOW_FALLING, MobEffects.NIGHT_VISION, MobEffects.INVISIBILITY, MobEffects.REGENERATION,
            MobEffects.HASTE, MobEffects.JUMP_BOOST);

    @Override
    public Component getName(ItemStack stack) {
        EssenceMix essence = contents(stack);
        Essence pure = essence.pureEssence();
        if (pure != null) {
            return Component.translatable("item.hextale.essence_flask.concentrate", ResearchText.essence(pure));
        }
        if (essence.isUnknown() && essence.unknown() == essence.total()) {
            return Component.translatable("item.hextale.essence_flask.unknown");
        }
        return essence.isEmpty() ? super.getName(stack) : Component.translatable("item.hextale.essence_flask.mixture");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        EssenceMix essence = contents(stack);
        if (essence.isEmpty()) {
            return;
        }
        tooltip.accept(essence.describe());
        tooltip.accept(Component.translatable("item.hextale.essence_flask.drops", essence.total(), CAPACITY).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.essence_flask.drink").withStyle(ChatFormatting.DARK_AQUA));
        if (essence.isUnknown()) {
            tooltip.accept(Component.translatable("item.hextale.essence_flask.unknown.hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (essence.pureEssence() == null) {
            tooltip.accept(Component.translatable("item.hextale.essence_flask.mixture.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
