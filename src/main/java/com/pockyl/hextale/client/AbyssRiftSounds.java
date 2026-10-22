package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import com.pockyl.hextale.abyss.AbyssRift;
import com.pockyl.hextale.registry.ModSounds;

/** The breath of the abyss behind every rift the client sees: a loop that swells as it opens and dies as it shuts. */
public final class AbyssRiftSounds {
    private AbyssRiftSounds() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof AbyssRift rift) {
                Minecraft.getInstance().getSoundManager().queueTickingSound(new Hum(rift));
            }
        });
    }

    private static final class Hum extends AbstractTickableSoundInstance {
        private final AbyssRift rift;

        Hum(AbyssRift rift) {
            super(ModSounds.ABYSS_RIFT_HUM.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.rift = rift;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.x = rift.getX();
            this.y = rift.getY();
            this.z = rift.getZ();
        }

        @Override
        public void tick() {
            if (rift.isRemoved()) {
                stop();
                return;
            }
            float open = Mth.clamp((rift.age(0) - AbyssRift.CRACK_TICKS) / (AbyssRift.OPEN_TICKS - AbyssRift.CRACK_TICKS), 0, 1);
            float size = Math.min(rift.size(), 10);
            x = rift.getX();
            y = rift.getY();
            z = rift.getZ();
            volume = Math.max(0.01F, (0.35F + size * 0.1F) * open * (1 - rift.closeProgress(0)));
            pitch = 1.1F - size * 0.045F + 0.03F * Mth.sin(rift.age(0) * 0.02F);
        }
    }
}
