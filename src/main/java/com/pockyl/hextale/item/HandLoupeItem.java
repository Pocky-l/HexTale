package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.research.Loupe;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.LoupeRune;
import com.pockyl.hextale.runework.RuneHolder;
import com.pockyl.hextale.runework.ToolEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * The Hand Loupe: a lens in a brass ring, the researcher's first way to look closer. Held to the eye (use held down), it
 * looks at what the other hand holds or, with that hand empty, at the block in front: after {@link #LOOK_TICKS} the thing
 * is studied - its essences are known, as at the Research Table. Slower than the table's loupe arm, but it goes
 * everywhere. Takes runes ({@link LoupeRune}).
 */
public class HandLoupeItem extends Item {
    /** How long a look takes without runes. */
    public static final int LOOK_TICKS = 50;
    /** How far it reaches for a block. */
    public static final double REACH = 4.5;

    public HandLoupeItem(Properties properties) {
        super(properties);
    }

    public static int strength(ItemStack loupe, LoupeRune rune) {
        return RuneHolder.LOUPE.runes(loupe).strength(rune.rune());
    }

    /** Ticks a look takes with this loupe's runes. */
    public static int lookTicks(ItemStack loupe) {
        int haste = strength(loupe, LoupeRune.HASTE);
        return Math.round(LOOK_TICKS * (1 - (haste > 0 ? HolderRune.at(LoupeRune.QUICKER, haste) : 0)));
    }

    /** What the loupe looks at: the item in the other hand, else the block in reach (as an item), else empty. */
    public static ItemStack target(Player player, InteractionHand hand) {
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!other.isEmpty()) {
            return other;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1).scale(REACH));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return ItemStack.EMPTY;
        }
        BlockPos pos = hit.getBlockPos();
        return new ItemStack(player.level().getBlockState(pos).getBlock().asItem());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack target = target(player, hand);
        if (!Loupe.studiable(target)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("item.hextale.hand_loupe.nothing"));
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        if (level.isClientSide()) {
            level.playLocalSound(player, ModSounds.LOUPE_GRAB.get(), SoundSource.PLAYERS, 0.5F, 1.1F);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPYGLASS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int used = getUseDuration(stack, entity) - remaining;
        int need = lookTicks(stack);
        if (level.isClientSide()) {
            // A lens clicks in as the look deepens: three of them, like the table's arm.
            if (used == need / 3 || used == 2 * need / 3) {
                level.playLocalSound(player, ModSounds.LOUPE_LENS.get(), SoundSource.PLAYERS, 0.4F, 0.9F + used / (float) need * 0.3F);
            }
            return;
        }
        if (used >= need) {
            player.stopUsingItem();
            if (player instanceof ServerPlayer server) {
                study(server, stack, target(player, player.getUsedItemHand()));
            }
        }
    }

    /** The look is done: the target is studied, the runes pay out. Game tests call it straight. */
    public static boolean study(ServerPlayer player, ItemStack loupe, ItemStack target) {
        if (!Loupe.studiable(target)) {
            return false;
        }
        Item item = target.getItem();
        ServerLevel level = player.level();
        Vec3 at = player.getEyePosition().add(player.getViewVector(1).scale(0.6));
        if (Research.of(player).hasStudied(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item))) {
            player.sendOverlayMessage(Component.translatable("item.hextale.hand_loupe.known", ResearchText.item(item)));
            return false;
        }
        Research.study(player, item);
        player.sendOverlayMessage(Component.translatable("research.hextale.study.complete", ResearchText.item(item)));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOUPE_STUDIED.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
        for (Essence essence : ResearchItems.of(target).essences().keySet()) {
            level.sendParticles(ModParticles.glint(essence.color()), at.x, at.y, at.z, 3, 0.1, 0.08, 0.1, 0.02);
        }
        int radiance = strength(loupe, LoupeRune.RADIANCE);
        if (radiance > 0) {
            ExperienceOrb.award(level, player.position(), HolderRune.at(LoupeRune.EXPERIENCE, radiance));
            ToolEffects.puff(level, at, LoupeRune.RADIANCE.rune(), 4, 0.2);
        }
        int insight = strength(loupe, LoupeRune.INSIGHT);
        if (insight > 0 && level.getRandom().nextFloat() < HolderRune.at(LoupeRune.SECOND_FIND, insight)) {
            Item extra = unstudied(player);
            if (extra != null) {
                Research.study(player, extra);
                player.sendSystemMessage(Component.translatable("item.hextale.hand_loupe.insight", ResearchText.item(extra))
                        .withColor(LoupeRune.INSIGHT.rune().color()));
                ToolEffects.puff(level, at, LoupeRune.INSIGHT.rune(), 6, 0.25);
            }
        }
        return true;
    }

    /** A random item of the inventory the loupe could study that is not studied yet. */
    @Nullable
    private static Item unstudied(ServerPlayer player) {
        List<Item> found = new ArrayList<>();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (Loupe.studiable(stack) && !found.contains(stack.getItem())
                    && !Research.of(player).hasStudied(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                found.add(stack.getItem());
            }
        }
        return found.isEmpty() ? null : found.get(player.getRandom().nextInt(found.size()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hextale.hand_loupe.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", RuneHolder.LOUPE.runes(stack).count(), LoupeRune.SLOTS)
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.hextale.hand_loupe.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
