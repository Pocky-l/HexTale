package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.MagnetRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.RuneHolder;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * The Hextech Magnet: a brass horseshoe with hextech poles. Switched on (use; it glints while on) and carried anywhere
 * in the inventory, it draws loose items and experience within its reach to its holder, unless they sneak. Items
 * someone just dropped are left alone. Takes runes (Shift, Radiance, Cache, Lens).
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public class HextechMagnetItem extends Item {
    /** Ticks between two pulls. */
    public static final int PULL_TICKS = 4;
    /** How fast things fly to the holder, blocks per tick. */
    public static final double SPEED = 0.45;

    public HextechMagnetItem(Properties properties) {
        super(properties);
    }

    public static boolean isOn(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean on = !isOn(stack);
        if (on) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable(on ? "item.hextale.hextech_magnet.on" : "item.hextale.hextech_magnet.off"));
            level.playSound(null, player.blockPosition(), on ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS,
                    0.25F, on ? 1.8F : 1.5F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof Player player) || !isOn(stack) || player.isSpectator() || player.isShiftKeyDown()
                || player.tickCount % PULL_TICKS != 0) {
            return;
        }
        pull(level, player, RuneHolder.MAGNET.runes(stack));
    }

    /** Draws the loose items and experience around {@code player} in; returns how many things moved. */
    public static int pull(ServerLevel level, Player player, PickaxeRunes runes) {
        int radius = MagnetRune.radius(runes.strength(MagnetRune.SHIFT.rune()));
        boolean cache = runes.strength(MagnetRune.CACHE.rune()) > 0;
        Vec3 target = player.position().add(0, 0.6, 0);
        AABB reach = player.getBoundingBox().inflate(radius);
        int moved = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, reach, item -> item.isAlive() && !item.hasPickUpDelay())) {
            if (cache) {
                item.playerTouch(player);
            } else {
                fly(item, target);
            }
            moved++;
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, reach, Entity::isAlive)) {
            if (cache) {
                player.takeXpDelay = 0;
                orb.playerTouch(player);
            } else {
                fly(orb, target);
            }
            moved++;
        }
        if (moved > 0 && level.getRandom().nextInt(3) == 0) {
            level.sendParticles(ModParticles.glint(ModParticles.HEXTECH), player.getX(), player.getY() + 0.9, player.getZ(), 2, 0.3, 0.4, 0.3, 0.01);
        }
        return moved;
    }

    private static void fly(Entity entity, Vec3 target) {
        Vec3 way = target.subtract(entity.position());
        double length = way.length();
        if (length < 0.3) {
            return;
        }
        entity.setDeltaMovement(way.scale(Math.min(SPEED, length) / length));
        entity.needsSync = true;
    }

    /** Radiance: experience drawn by a switched-on magnet is worth more. */
    @SubscribeEvent
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        int best = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() instanceof HextechMagnetItem && isOn(stack)) {
                best = Math.max(best, RuneHolder.MAGNET.runes(stack).strength(MagnetRune.RADIANCE.rune()));
            }
        }
        if (best > 0) {
            float bonus = event.getOrb().getValue() * HolderRune.at(MagnetRune.BONUS, best);
            int extra = (int) bonus + (player.getRandom().nextFloat() < bonus - (int) bonus ? 1 : 0);
            if (extra > 0) {
                player.giveExperiencePoints(extra);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", RuneHolder.MAGNET.runes(stack).count(), MagnetRune.SLOTS)
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.hextale.hextech_magnet.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.hextech_magnet.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
