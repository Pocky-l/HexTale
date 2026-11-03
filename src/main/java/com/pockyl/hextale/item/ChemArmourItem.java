package com.pockyl.hextale.item;

import com.pockyl.hextale.chem.ChemGear;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.TankFuel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.function.Consumer;

/**
 * A piece of chem-steel armour: no runes, never breaks, protects as well as the fuel in the tank on its wearer's back
 * lets it ({@link ChemGear}); every hit taken spends one action of fuel. Its bar is that tank's level, and its tooltip
 * lists the protection it gives with that fuel (the item's own attribute lines always show the full one, so they are
 * hidden).
 */
public class ChemArmourItem extends Item {
    private final ArmorType type;

    public ChemArmourItem(ArmorType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public ArmorType type() {
        return type;
    }

    /** The tank whose fuel this piece shows: its own for a chestplate with a built-in tank, else the worn one. */
    protected TankFuel shown(ItemStack stack) {
        return ChemTank.contents(ChemGear.clientTank.get());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * shown(stack).level());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        TankFuel fuel = shown(stack);
        return fuel.isEmpty() ? 0x5A5A5A : fuel.color();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        TankFuel tank = shown(stack);
        tooltip.accept(ChemToolItem.power(tank));
        float[] armour = ChemGear.armour(type, tank.isEmpty() ? null : tank.fuel());
        tooltip.accept(CommonComponents.EMPTY);
        tooltip.accept(Component.translatable("item.modifiers." + type.getSlot().getSerializedName()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(ChemToolItem.plus(Attributes.ARMOR, armour[0]));
        tooltip.accept(ChemToolItem.plus(Attributes.ARMOR_TOUGHNESS, armour[1]));
        if (armour[2] > 0) {
            tooltip.accept(ChemToolItem.plus(Attributes.KNOCKBACK_RESISTANCE, armour[2] * 10));
        }
    }
}
