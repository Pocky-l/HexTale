package com.pockyl.hextale.item;

import com.pockyl.hextale.chem.ChemFuel;
import com.pockyl.hextale.chem.ChemGear;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.TankFuel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * A chemtech tool: no runes, never breaks, as strong as the fuel in the tank on its holder's back ({@link ChemGear}).
 * Its bar is that tank's level in the fuel's colour; every block it breaks and every hit spends one action of fuel. The
 * item's own attribute lines are hidden: they always show the full strength, so the tooltip lists what the tool gives
 * with the fuel worn right now. The sword never breaks blocks.
 */
public class ChemToolItem extends Item {
    private final ChemGear.Kind kind;

    public ChemToolItem(ChemGear.Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public ChemGear.Kind kind() {
        return kind;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0) {
            ChemGear.spend(owner, 1);
        }
        return true;
    }

    @Override
    public boolean canDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
        return kind != ChemGear.Kind.SWORD && super.canDestroyBlock(stack, state, level, pos, user);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity mob, LivingEntity attacker) {
        ChemGear.spend(attacker, 1);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * ChemTank.contents(ChemGear.clientTank.get()).level());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        TankFuel fuel = ChemTank.contents(ChemGear.clientTank.get());
        return fuel.isEmpty() ? 0x5A5A5A : fuel.color();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        TankFuel tank = ChemTank.contents(ChemGear.clientTank.get());
        tooltip.accept(power(tank));
        ChemFuel fuel = tank.isEmpty() ? null : tank.fuel();
        tooltip.accept(CommonComponents.EMPTY);
        tooltip.accept(Component.translatable("item.modifiers.mainhand").withStyle(ChatFormatting.GRAY));
        tooltip.accept(base(Attributes.ATTACK_DAMAGE, ChemGear.damage(kind, fuel)));
        tooltip.accept(base(Attributes.ATTACK_SPEED, 4 + kind.attackSpeed()));
        if (kind != ChemGear.Kind.SWORD) {
            tooltip.accept(CommonComponents.space().append(Component.translatable("item.hextale.chem_gear.dig_speed",
                    ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(ChemGear.digSpeed(fuel)))).withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    /** A line like the vanilla "7 Attack Damage" one: the whole value, base included. */
    static Component base(Holder<Attribute> attribute, double value) {
        return CommonComponents.space().append(Component.translatable("attribute.modifier.equals.0",
                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value), Component.translatable(attribute.value().getDescriptionId())))
                .withStyle(ChatFormatting.DARK_GREEN);
    }

    /** A line like the vanilla "+3 Armor" one. */
    static Component plus(Holder<Attribute> attribute, double value) {
        return Component.translatable("attribute.modifier.plus.0", ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value),
                Component.translatable(attribute.value().getDescriptionId())).withStyle(ChatFormatting.BLUE);
    }

    /** One tooltip line: what the tank gives right now. */
    public static Component power(TankFuel fuel) {
        if (fuel.isEmpty()) {
            return Component.translatable("item.hextale.chem_gear.empty").withColor(0xA0A0A0);
        }
        ChemFuel kind = fuel.fuel();
        return Component.translatable("item.hextale.chem_gear.power", Component.translatable(kind.brew().translationKey()))
                .withColor(kind.brew().color());
    }
}
