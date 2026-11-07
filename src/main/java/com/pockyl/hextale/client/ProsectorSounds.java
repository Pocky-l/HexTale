package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.ProsectorState;

/** The low loop of the fight: it swells when the Prosector wakes and goes with it. */
public final class ProsectorSounds {
    private static final float FULL = 0.6F;

    private ProsectorSounds() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof Prosector boss) {
                Minecraft.getInstance().getSoundManager().queueTickingSound(new Loop(boss));
            }
        });
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private final Prosector boss;

        Loop(Prosector boss) {
            super(ModSounds.PROSECTOR_LOOP.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
            this.boss = boss;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.x = boss.getX();
            this.y = boss.getY();
            this.z = boss.getZ();
        }

        @Override
        public void tick() {
            if (boss.isRemoved()) {
                stop();
                return;
            }
            x = boss.getX();
            y = boss.getY();
            z = boss.getZ();
            ProsectorState state = boss.state();
            float wanted = state == ProsectorState.DORMANT || state == ProsectorState.RISING || state == ProsectorState.DYING ? 0.01F : FULL;
            volume += (wanted - volume) * 0.05F;
        }
    }
}
