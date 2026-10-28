package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.RuneHolder;
import com.pockyl.hextale.runework.SoulRune;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * The Soul Flask: right click a creature with an empty flask and it folds into the glass, whole (everything it is and
 * carries is kept); right click a block with a full flask and it steps out there. No players, no bosses. Takes runes
 * (Healing, Growth, Slumber) that act when the creature comes out.
 */
@EventBusSubscriber(modid = com.pockyl.hextale.Hextale.MOD_ID)
public class SoulFlaskItem extends Item {
    public SoulFlaskItem(Properties properties) {
        super(properties);
    }

    /** Whether this creature may go into a flask. */
    public static boolean takes(LivingEntity entity) {
        return entity instanceof Mob && entity.isAlive() && !(entity instanceof Player) && !entity.getType().is(Tags.EntityTypes.BOSSES)
                && !entity.isPassenger() && !entity.isVehicle();
    }

    /** Whether {@code player} may fold this creature: never someone else's pet, nor one on someone else's lead. */
    public static boolean takes(Player player, LivingEntity entity) {
        if (!takes(entity)) {
            return false;
        }
        if (entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null
                && !pet.getOwnerUUID().equals(player.getUUID())) {
            return false;
        }
        return !(entity instanceof Leashable leashed && leashed.getLeashHolder() instanceof Player holder && holder != player);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return fold(stack, player, target, hand);
    }

    /**
     * Creatures with their own right click (a horse is mounted, a villager trades) would never reach
     * {@link #interactLivingEntity}: an empty flask in hand takes the click first.
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof SoulFlaskItem && !stack.has(ModDataComponents.SOUL.get())
                && event.getTarget() instanceof LivingEntity target && takes(event.getEntity(), target)) {
            event.setCancellationResult(fold(stack, event.getEntity(), target, event.getHand()));
            event.setCanceled(true);
        }
    }

    /** Folds {@code target} into the empty flask {@code stack}. */
    public static InteractionResult fold(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (stack.has(ModDataComponents.SOUL.get()) || !takes(player, target)) {
            return InteractionResult.PASS;
        }
        if (target.level() instanceof ServerLevel level) {
            ItemStack full = stack.copyWithCount(1);
            full.set(ModDataComponents.SOUL.get(), capture(level, target));
            Vec3 at = target.position();
            runes(level, at.add(0, target.getBbHeight() / 2, 0), target.getBbWidth(), target.getBbHeight());
            level.playSound(null, at.x, at.y, at.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
            target.discard();
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.setItemInHand(hand, full);
            } else if (!player.getInventory().add(full)) {
                player.drop(full, false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Everything the creature is, as a tag (its type under "id"; no UUID, so a copy never clashes). */
    public static CompoundTag capture(ServerLevel level, Entity entity) {
        CompoundTag tag = entity.saveWithoutId(new CompoundTag());
        tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        tag.remove("UUID");
        return tag;
    }

    /** The creature a tag holds, standing at {@code at}; null when it cannot be made. */
    @Nullable
    public static Entity release(ServerLevel level, CompoundTag tag, Vec3 at) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(tag.getString("id"))).orElse(null);
        if (type == null) {
            return null;
        }
        Entity entity = EntityType.loadEntityRecursive(tag.copy(), level, loaded -> loaded);
        if (entity == null) {
            return null;
        }
        entity.moveTo(at.x, at.y, at.z, entity.getYRot(), entity.getXRot());
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        CompoundTag soul = stack.get(ModDataComponents.SOUL.get());
        if (soul == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel() instanceof ServerLevel level) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            Entity entity = release(level, soul, Vec3.atBottomCenterOf(pos));
            if (entity == null) {
                return InteractionResult.FAIL;
            }
            if (entity instanceof LivingEntity living) {
                bless(stack, living);
            }
            level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), entity.getX(), entity.getY() + 0.1, entity.getZ(), 0, 1.0, 1, 0, 1);
            runes(level, entity.position().add(0, entity.getBbHeight() / 2, 0), entity.getBbWidth(), entity.getBbHeight());
            level.playSound(null, pos, ModSounds.PYLON_ARRIVE.get(), SoundSource.PLAYERS, 0.8F, 1.1F);
            stack.remove(ModDataComponents.SOUL.get());
        }
        return InteractionResult.SUCCESS;
    }

    /** Rune glyphs swirling around a creature going into the glass or coming out. */
    private static void runes(ServerLevel level, Vec3 centre, float width, float height) {
        level.sendParticles(ModParticles.runeShard(ModParticles.HEXTECH), centre.x, centre.y, centre.z, 14, width / 2 + 0.2, height / 2, width / 2 + 0.2, 0.04);
        level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), centre.x, centre.y, centre.z, 6, width / 2, height / 2, width / 2, 0.02);
    }

    /** What the flask's runes do to the creature stepping out. */
    private static void bless(ItemStack flask, LivingEntity creature) {
        var runes = RuneHolder.SOUL.runes(flask);
        if (runes.strength(SoulRune.HEALING.rune()) > 0) {
            creature.setHealth(creature.getMaxHealth());
        }
        if (runes.strength(SoulRune.GROWTH.rune()) > 0 && creature instanceof AgeableMob ageable && ageable.isBaby()) {
            ageable.setAge(0);
        }
        int slumber = runes.strength(SoulRune.SLUMBER.rune());
        if (slumber > 0 && creature instanceof Enemy) {
            int ticks = SoulRune.SLUMBER_TICKS[Math.min(slumber, 4) - 1];
            creature.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 4));
            creature.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
            if (creature instanceof Mob mob) {
                mob.setTarget(null);
            }
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag soul = stack.get(ModDataComponents.SOUL.get());
        if (soul != null) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(soul.getString("id"))).orElse(null);
            if (type != null) {
                return Component.translatable("item.hextale.soul_flask.full", type.getDescription());
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", RuneHolder.SOUL.runes(stack).count(), SoulRune.SLOTS)
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.hextale.soul_flask.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.soul_flask.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
