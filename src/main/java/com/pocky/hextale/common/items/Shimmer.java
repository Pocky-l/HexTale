package com.pocky.hextale.common.items;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Shimmer extends Item {

    private static final int DRINK_DURATION = 32;

    private static final List<List<MobEffectInstance>> EFFECTS = new ArrayList<>(List.of(
            Potions.MUNDANE.getEffects(),
            Potions.FIRE_RESISTANCE.getEffects(),
            Potions.HEALING.getEffects(),
            Potions.INVISIBILITY.getEffects(),
            Potions.LUCK.getEffects(),
            Potions.NIGHT_VISION.getEffects(),
            Potions.LEAPING.getEffects(),
            Potions.POISON.getEffects(),
            Potions.REGENERATION.getEffects(),
            Potions.SLOW_FALLING.getEffects(),
            Potions.SWIFTNESS.getEffects()
    ));


    public Shimmer() {
        super(new Properties()
                .fireResistant()
                .rarity(Rarity.COMMON)
                .stacksTo(4)
        );
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        Player player = entity instanceof Player ? (Player)entity : null;
        if (player instanceof ServerPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)player, stack);
        }

        if (!level.isClientSide) {
            Random random = new Random();

            for(MobEffectInstance mobeffectinstance : EFFECTS.get(random.nextInt(EFFECTS.size()))) {
                if (mobeffectinstance.getEffect().isInstantenous()) {
                    mobeffectinstance.getEffect().applyInstantenousEffect(player, player, entity, mobeffectinstance.getAmplifier(), 1.0D);
                } else {
                    entity.addEffect(new MobEffectInstance(mobeffectinstance));
                }
            }
        }

        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        if (player == null || !player.getAbilities().instabuild) {
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }

            if (player != null) {
                player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
            }
        }

        entity.gameEvent(GameEvent.DRINK);
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack itemstack = context.getItemInHand();
        BlockState blockstate = level.getBlockState(blockpos);
        if (context.getClickedFace() != Direction.DOWN && blockstate.is(BlockTags.CONVERTABLE_TO_MUD) && PotionUtils.getPotion(itemstack) == Potions.WATER) {
            level.playSound((Player)null, blockpos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.setItemInHand(context.getHand(), ItemUtils.createFilledResult(itemstack, player, new ItemStack(Items.GLASS_BOTTLE)));
            player.awardStat(Stats.ITEM_USED.get(itemstack.getItem()));
            if (!level.isClientSide) {
                ServerLevel serverlevel = (ServerLevel)level;

                for(int i = 0; i < 5; ++i) {
                    serverlevel.sendParticles(ParticleTypes.SPLASH, (double)blockpos.getX() + level.random.nextDouble(), (double)(blockpos.getY() + 1), (double)blockpos.getZ() + level.random.nextDouble(), 1, 0.0D, 0.0D, 0.0D, 1.0D);
                }
            }

            level.playSound((Player)null, blockpos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent((Entity)null, GameEvent.FLUID_PLACE, blockpos);
            level.setBlockAndUpdate(blockpos, Blocks.MUD.defaultBlockState());
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public int getUseDuration(ItemStack p_43001_) {
        return DRINK_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack p_42997_) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level p_42993_, Player p_42994_, InteractionHand p_42995_) {
        return ItemUtils.startUsingInstantly(p_42993_, p_42994_, p_42995_);
    }

    /*
    @Override
    public void appendHoverText(ItemStack p_42988_, @Nullable Level p_42989_, List<Component> p_42990_, TooltipFlag p_42991_) {
        PotionUtils.addPotionTooltip(p_42988_, p_42990_, 1.0F);
    }*/
}
