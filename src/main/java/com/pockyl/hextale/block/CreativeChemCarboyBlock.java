package com.pockyl.hextale.block;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.CarboyContents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The Creative Chem Carboy: a vial on it picks the brew, and it stays full of that brew for creative building and testing. */
public class CreativeChemCarboyBlock extends ChemCarboyBlock {
    public CreativeChemCarboyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void pour(ItemStack stack, Brew brew, ChemCarboyBlockEntity carboy, Player player) {
        carboy.setContents(new CarboyContents(brew, CarboyContents.CAPACITY));
        carboy.getLevel().playSound(null, carboy.getBlockPos().above(), SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.4F, 0.9F);
    }
}
