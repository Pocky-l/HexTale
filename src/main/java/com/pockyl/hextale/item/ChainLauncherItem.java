package com.pockyl.hextale.item;

import com.pockyl.hextale.block.ChainAnchorBlockEntity;
import com.pockyl.hextale.chem.Zipline;
import com.pockyl.hextale.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Chain Launcher: a hand-held pneumatic harpoon. Use shoots a zipline from where its user stands (a Chain Post
 * rises there, or the post standing there takes the new line too) to what they aim at: a floor or a ceiling gets a
 * post, a wall a claw. While it is held a hologram shows the line (client, {@code client/ChainAim}). It costs no chains;
 * every line wears it a little (owner: craft once, about six lines).
 */
public class ChainLauncherItem extends Item {
    /** Lines one launcher shoots before it breaks. */
    public static final int SHOTS = 6;
    /** Ticks between shots. */
    private static final int REST = 20;

    public ChainLauncherItem(Properties properties) {
        super(properties);
    }

    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        if (player.isPassenger()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        Zipline.Aim aim = Zipline.aim(level, player);
        if (!(level instanceof ServerLevel server)) {
            if (!aim.ok()) {
                player.playSound(SoundEvents.DISPENSER_FAIL, 0.5F, 1.2F);
            }
            return aim.ok() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        Zipline.Status status = shoot(server, player, stack, hand, aim);
        if (status != Zipline.Status.OK) {
            int length = aim.line() == null ? 0 : (int) Math.round(aim.line().length());
            player.displayClientMessage(Component.translatable(status.key(), length), true);
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.Interactions.holder(usePort(level, player, hand), player.getItemInHand(hand));
    }

    /** Fires the judged shot: the line flies, the launcher wears (not in creative) and rests a moment. */
    public static Zipline.Status shoot(ServerLevel level, Player player, ItemStack stack, InteractionHand hand, Zipline.Aim aim) {
        if (!aim.ok()) {
            return aim.status();
        }
        if (!ChainAnchorBlockEntity.fire(level, aim)) {
            return Zipline.Status.NO_FOOTING;
        }
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.CHAIN_LAUNCHER_FIRE.get(), SoundSource.PLAYERS, 0.8F,
                0.95F + level.getRandom().nextFloat() * 0.1F);
        player.getCooldowns().addCooldown(stack.getItem(), REST);
        stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
        return Zipline.Status.OK;
    }
}
