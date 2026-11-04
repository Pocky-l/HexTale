package com.pockyl.hextale.item;

import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.TankFuel;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.pockyl.hextale.port.ArmorType;

/**
 * The chem-steel chestplate with a built-in tank: armour and tank in one piece, so the Curios back slot (or nothing)
 * is left free. It holds its own fuel like a Chem Tank (vials clicked onto it pour in, a placed tank pumps into it) and
 * its bar is its own level; the tank shows on the wearer's back.
 */
public class ChemTankChestplateItem extends ChemArmourItem {
    public ChemTankChestplateItem(net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> material, Properties properties) {
        super(material, ArmorType.CHESTPLATE, properties);
    }

    @Override
    protected TankFuel shown(ItemStack stack) {
        return ChemTank.contents(stack);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player,
                                            SlotAccess carriedItem) {
        return clickAction == ClickAction.PRIMARY && ChemTank.pourOnto(self, other, player);
    }
}
