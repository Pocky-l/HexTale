package com.pockyl.hextale.item;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemJets;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.chem.TankFuel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * The Chem Tank as an item: worn on the back (chest slot or the Curios back slot) it feeds chemtech gear; set down it is
 * a block that is filled and pumps into the tank one wears. Its bar is the fuel left, in the fuel's colour. Fuel vials
 * clicked onto it in the inventory pour in.
 */
public class ChemTankItem extends BlockItem implements net.minecraft.world.item.Equipable {
    public ChemTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** 1.21.1: worn in the chest slot as an {@link net.minecraft.world.item.Equipable} (26.x: the equippable component). */
    @Override
    public net.minecraft.world.entity.EquipmentSlot getEquipmentSlot() {
        return net.minecraft.world.entity.EquipmentSlot.CHEST;
    }

    @Override
    public net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> getEquipSound() {
        return net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    /** Used in the air it is put on, like armour. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, Player player,
                                                                      net.minecraft.world.InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !ChemTank.contents(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(1, Math.round(13.0F * ChemTank.contents(stack).level()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ChemTank.contents(stack).color();
    }

    /** A fuel vial carried onto the tank pours in, as many as fit. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player,
                                            SlotAccess carriedItem) {
        return clickAction == ClickAction.PRIMARY && ChemTank.pourOnto(self, other, player);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        TankFuel contents = ChemTank.contents(stack);
        if (contents.isEmpty()) {
            tooltip.accept(Component.translatable("item.hextale.chem_tank.empty").withColor(0xA0A0A0));
        } else {
            tooltip.accept(Component.translatable("item.hextale.chem_tank.contents",
                    Component.translatable(contents.fuel().brew().translationKey()), contents.vials()).withColor(contents.color()));
        }
        jetLines(stack, tooltip);
    }

    /** What a tank with nozzles adds: how fast its fuel climbs and the two keys. */
    static void jetLines(ItemStack stack, Consumer<Component> tooltip) {
        if (!ChemTank.hasJets(stack)) {
            return;
        }
        TankFuel contents = ChemTank.contents(stack);
        tooltip.accept(contents.isEmpty() ? Component.translatable("item.hextale.chem_gear.jets_off").withStyle(ChatFormatting.GRAY)
                : Component.translatable("item.hextale.chem_gear.jets",
                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(ChemJets.climbSpeed(contents.fuel()) * 20)).withStyle(ChatFormatting.BLUE));
        tooltip.accept(Component.translatable("item.hextale.chem_gear.hover", Component.keybind("key.hextale.jet_hover")).withStyle(ChatFormatting.GRAY));
    }
}
