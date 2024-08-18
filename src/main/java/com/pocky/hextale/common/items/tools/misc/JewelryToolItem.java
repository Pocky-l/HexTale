package com.pocky.hextale.common.items.tools.misc;

import com.pocky.hextale.common.register.ModItems;
import com.pocky.hextale.utils.ModColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class JewelryToolItem extends Item {

    public JewelryToolItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public boolean canAttackBlock(@NotNull BlockState blockState, @NotNull Level level,
                                  @NotNull BlockPos blockPos, @NotNull Player player) {

        if (level instanceof ServerLevel serverLevel) {
            if (player.getCooldowns().isOnCooldown(this)) return false;
            List<ItemEntity> list = serverLevel.getEntitiesOfClass(ItemEntity.class, new AABB(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ(),
                    blockPos.getX() + 1, blockPos.getY() + 2, blockPos.getZ() + 1));

            list.stream().filter(e -> e.getItem().is(ModItems.HEXTECH_SHARD.get())).findFirst().ifPresent(e -> {
                e.getItem().shrink(1);

                if (Math.random() > 0.85) {
                    ItemEntity entity = new ItemEntity(level, e.getX(), e.getY(), e.getZ(), ModItems.CLEAR_HEXTECH.get().getDefaultInstance());
                    level.addFreshEntity(entity);

                    serverLevel.sendParticles(
                            ParticleTypes.GLOW,
                            e.getX(),
                            e.getY(),
                            e.getZ(),
                            1,  // Number of particles
                            0, 0, 0,  // Motion
                            0  // Scale
                    );

                    List<ServerPlayer> players = new ArrayList<>(level.getEntitiesOfClass(ServerPlayer.class, new AABB(blockPos).inflate(15.0D)));
                    ClientboundSoundPacket packet = new ClientboundSoundPacket(Holder.direct(SoundEvents.AMETHYST_BLOCK_BREAK), SoundSource.AMBIENT, blockPos.getX(), blockPos.getY(), blockPos.getZ(), 1.0F, 1.0F, 0L);
                    players.forEach(p -> p.connection.send(packet));
                } else {
                    ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, ModItems.HEXTECH_SHARD.get().getDefaultInstance());
                    serverLevel.sendParticles(
                            particle,
                            e.getX(),
                            e.getY()+0.2D,
                            e.getZ(),
                            10,  // Number of particles
                            0, 0, 0,  // Motion
                            0.15  // Scale
                    );

                    List<ServerPlayer> players = new ArrayList<>(level.getEntitiesOfClass(ServerPlayer.class, new AABB(blockPos).inflate(15.0D)));
                    ClientboundSoundPacket packet = new ClientboundSoundPacket(Holder.direct(SoundEvents.GLASS_BREAK), SoundSource.AMBIENT, blockPos.getX(), blockPos.getY(), blockPos.getZ(), 1.0F, 1.0F, 0L);
                    players.forEach(p -> p.connection.send(packet));
                }
            });
            player.getCooldowns().addCooldown(this, 10);
        }

        return false;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack item, @Nullable Level level, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Component.translatable("tooltip." + item.getDescriptionId())
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(ModColors.HEXTECH))));
        super.appendHoverText(item, level, tooltip, tooltipFlag);
    }

    @Override
    public float getDestroySpeed(ItemStack p_41425_, BlockState p_41426_) {
        return 20.f;
    }
}
