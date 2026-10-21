package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.RuneHolder;
import com.pockyl.hextale.runework.SkypackRune;
import com.pockyl.hextale.runework.ToolEffects;

import java.util.function.Consumer;

/**
 * The Hextech Skypack: two brass tanks of air essence around a hextech crystal, worn on the back. While its tanks hold
 * air the wearer may glide like on elytra (jump, then jump again in the air), and while gliding its jets push like a
 * firework rocket that never burns out - until the air does. Sneaking cuts the jets (a free glide). The air is drawn out
 * of Essence Jars: a click on a jar with the pack in hand, or sneaking on the ground with it on near jars.
 *
 * <p>The thrust itself is applied on the wearer's client ({@code SkypackClient}), like a firework's; the server only
 * spends the air: one mB a second of thrust, taken by {@link #onGlideDamage} (vanilla calls it every second of a
 * glide). The pack can glide only while it holds air ({@code SkypackEvents} gives it the gliding attribute).
 */
public class HextechSkypackItem extends Item {
    /** mB of air the tanks hold. */
    public static final int CAPACITY = 160;
    /** mB of air a second of thrust costs. */
    public static final int AIR_PER_SECOND = 1;
    /** mB of air drawn from a jar at a time while sneaking beside it. */
    public static final int SIP_MB = 8;
    /** Ticks between two sips. */
    public static final int SIP_TICKS = 10;
    /** Below this much air the jets start to cough. */
    public static final int LOW = 16;
    private static final int AIR_COLOR = Essence.AIR.color();

    public HextechSkypackItem(Properties properties) {
        super(properties);
    }

    public static boolean is(ItemStack stack) {
        return stack.is(ModItems.HEXTECH_SKYPACK.get());
    }

    public static int air(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SKYPACK_AIR.get(), 0);
    }

    public static void setAir(ItemStack stack, int air) {
        int value = Mth.clamp(air, 0, CAPACITY);
        if (value == 0) {
            stack.remove(ModDataComponents.SKYPACK_AIR.get());
        } else {
            stack.set(ModDataComponents.SKYPACK_AIR.get(), value);
        }
    }

    /** A pack with full tanks (the creative tab). */
    public static ItemStack full() {
        ItemStack stack = new ItemStack(ModItems.HEXTECH_SKYPACK.get());
        setAir(stack, CAPACITY);
        return stack;
    }

    /** The pack worn on the chest of this entity, or empty. */
    public static ItemStack worn(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return is(chest) ? chest : ItemStack.EMPTY;
    }

    public static int strength(ItemStack stack, SkypackRune rune) {
        return RuneHolder.SKYPACK.runes(stack).strength(rune.rune());
    }

    /** Whether the jets of a gliding wearer burn: air in the tanks and not sneaking. */
    public static boolean jetsOn(LivingEntity wearer, ItemStack pack) {
        return wearer.isFallFlying() && air(pack) > 0 && !wearer.isShiftKeyDown();
    }

    /** Called by vanilla every second of a glide: the jets spend their air, and the runes that work in flight work. */
    @Override
    public void onGlideDamage(ItemStack stack, LivingEntity wearer, EquipmentSlot slot) {
        if (!(wearer.level() instanceof ServerLevel level) || wearer.isShiftKeyDown()) {
            return;
        }
        int fire = strength(stack, SkypackRune.FIREPROOFING);
        if (fire > 0) {
            ToolEffects.keep(wearer, MobEffects.FIRE_RESISTANCE, HolderRune.at(SkypackRune.FIRE_TICKS, fire), 0);
        }
        int dam = strength(stack, SkypackRune.DAM);
        if (dam > 0 && level.getRandom().nextFloat() < HolderRune.at(SkypackRune.THRIFT, dam)) {
            ToolEffects.puff(level, wearer.position().add(0, 0.6, 0), SkypackRune.DAM.rune(), 4, 0.3);
            return;
        }
        int air = air(stack) - AIR_PER_SECOND;
        setAir(stack, air);
        if (air <= 0 || air == LOW) {
            // The jets cough: the tanks are nearly or wholly empty.
            level.playSound(null, wearer.getX(), wearer.getY(), wearer.getZ(), ModSounds.SKYPACK_SPUTTER.get(), SoundSource.PLAYERS, 0.8F,
                    air <= 0 ? 0.9F : 1.1F);
        }
    }

    /** Fills the tanks from a jar with the air it holds; true when anything came. */
    public static boolean fillFrom(ServerLevel level, EssenceTankBlockEntity jar, ItemStack pack, Vec3 to, int most) {
        int taken = Math.min(Math.min(most, CAPACITY - air(pack)), jar.tank().contents().get(Essence.AIR));
        if (taken <= 0) {
            return false;
        }
        if (!jar.isCreative()) {
            jar.tank().remove(EssenceMix.pure(Essence.AIR, taken));
            jar.setChanged();
        }
        setAir(pack, air(pack) + taken);
        // Motes of air flow out of the jar's lid into the pack.
        Vec3 mouth = JarReach.mouth(jar.getBlockPos());
        Vec3 offset = mouth.subtract(to);
        for (int i = 0; i < 6; i++) {
            level.sendParticles(ModParticles.flow(AIR_COLOR), to.x, to.y, to.z, 0, offset.x, offset.y, offset.z, 1);
        }
        level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.35F, 1.3F);
        return true;
    }

    /** Sneaking on the ground with the pack on, near jars: it sips air out of the nearest one that has some. */
    public static void sip(ServerLevel level, Player player, ItemStack pack) {
        if (air(pack) >= CAPACITY) {
            return;
        }
        Vec3 back = player.position().add(0, 1.1, 0).subtract(player.getLookAngle().multiply(1, 0, 1).normalize().scale(0.3));
        for (EssenceTankBlockEntity jar : JarReach.around(level, BlockPos.containing(player.position().add(0, 1, 0)))) {
            if (fillFrom(level, jar, pack, back, SIP_MB)) {
                return;
            }
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return air(stack) < CAPACITY;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * air(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return AIR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hextale.hextech_skypack.air", air(stack), CAPACITY).withColor(AIR_COLOR));
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", RuneHolder.SKYPACK.runes(stack).count(), SkypackRune.SLOTS)
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.hextale.hextech_skypack.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.hextech_skypack.controls").withStyle(ChatFormatting.DARK_AQUA));
    }

    /** Whether it may still glide (the air is what keeps the crystal awake). */
    public static boolean canGlide(ItemStack stack) {
        return air(stack) > 0;
    }
}
