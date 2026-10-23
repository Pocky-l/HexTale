package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import com.pockyl.hextale.thevoid.TheVoid;
import com.pockyl.hextale.registry.ModSounds;

/** The breath of the Void behind every breach the client sees: a loop that swells as it opens and dies as it shuts. */
public final class TheVoidSounds {
    private TheVoidSounds() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof TheVoid breach) {
                Minecraft.getInstance().getSoundManager().queueTickingSound(new Hum(breach));
            }
        });
    }

    private static final class Hum extends AbstractTickableSoundInstance {
        private final TheVoid breach;

        Hum(TheVoid breach) {
            super(ModSounds.THE_VOID_HUM.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.breach = breach;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.x = breach.getX();
            this.y = breach.getY();
            this.z = breach.getZ();
        }

        @Override
        public void tick() {
            if (breach.isRemoved()) {
                stop();
                return;
            }
            float open = breach.reveal(0);
            float size = Math.min(breach.size(), 10);
            x = breach.getX();
            y = breach.getY();
            z = breach.getZ();
            volume = Math.max(0.01F, (0.35F + size * 0.1F) * open);
            pitch = 1.1F - size * 0.045F + 0.03F * Mth.sin(breach.age(0) * 0.02F);
        }
    }
}
