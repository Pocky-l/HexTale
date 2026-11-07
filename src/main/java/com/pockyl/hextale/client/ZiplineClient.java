package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Avatar;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import com.pockyl.hextale.client.render.ZiplinePose;
import com.pockyl.hextale.entity.ZiplineTrolley;
import com.pockyl.hextale.registry.ModSounds;

/**
 * A ride on a Chain Launcher's line as others see and hear it: the rider hangs straight with both arms up on the
 * trolley's grip (not sitting), and the trolley rattles along the chain, louder and higher the faster it runs.
 */
public final class ZiplineClient {
    private ZiplineClient() {
    }

    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof ZiplineTrolley trolley) {
                Minecraft.getInstance().getSoundManager().queueTickingSound(new Rattle(trolley));
            }
        });
        modBus.addListener((RegisterRenderStateModifiersEvent event) -> event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & net.minecraft.client.entity.ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                if (avatar.getVehicle() instanceof ZiplineTrolley) {
                    state.isPassenger = false;
                    HumanoidModel.ArmPose hang = ZiplinePose.HANG.getValue();
                    state.rightArmPose = hang;
                    state.leftArmPose = hang;
                }
            }
        }));
    }

    private static final class Rattle extends AbstractTickableSoundInstance {
        private final ZiplineTrolley trolley;

        Rattle(ZiplineTrolley trolley) {
            super(ModSounds.ZIPLINE_RIDE.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.trolley = trolley;
            looping = true;
            delay = 0;
            volume = 0.01F;
            x = trolley.getX();
            y = trolley.getY();
            z = trolley.getZ();
        }

        @Override
        public void tick() {
            if (trolley.isRemoved()) {
                stop();
                return;
            }
            float speed = Math.abs(trolley.syncedSpeed()) / 0.6F;
            float target = speed < 0.03F ? 0 : 0.08F + 0.3F * Math.min(1, speed);
            volume += (target - volume) * 0.25F;
            pitch = 0.75F + 0.5F * Math.min(1, speed);
            x = trolley.getX();
            y = trolley.getY();
            z = trolley.getZ();
        }
    }
}
