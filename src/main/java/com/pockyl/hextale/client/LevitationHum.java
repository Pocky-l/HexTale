package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import com.pockyl.hextale.block.LevitationPlateBlockEntity;
import com.pockyl.hextale.registry.ModSounds;

/**
 * The hum of a Levitation Plate's column while it carries someone: a loop at the plate that fades in, and fades out
 * and stops once the column is empty or the plate is gone.
 */
public final class LevitationHum extends AbstractTickableSoundInstance {
    private static final float FULL = 0.5F;
    private final LevitationPlateBlockEntity plate;

    private LevitationHum(LevitationPlateBlockEntity plate) {
        super(ModSounds.LEVITATION_HUM.get(), SoundSource.BLOCKS, RandomSource.create());
        this.plate = plate;
        looping = true;
        delay = 0;
        volume = 0.01F;
        x = plate.getBlockPos().getX() + 0.5;
        y = plate.getBlockPos().getY() + 1.5;
        z = plate.getBlockPos().getZ() + 0.5;
    }

    public static void register() {
        LevitationPlateBlockEntity.hum = plate -> Minecraft.getInstance().getSoundManager().play(new LevitationHum(plate));
    }

    @Override
    public void tick() {
        boolean carrying = !plate.isRemoved() && plate.riders() > 0;
        volume = carrying ? Math.min(FULL, volume + 0.05F) : volume - 0.04F;
        pitch = 0.95F + 0.1F * plate.lift;
        if (volume <= 0) {
            stop();
        }
    }
}
