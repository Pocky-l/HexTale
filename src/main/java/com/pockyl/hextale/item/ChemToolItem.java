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
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        return kind != ChemGear.Kind.SWORD && super.canAttackBlock(state, level, pos, player);
    }

    /** 1.21.1: postHurtEnemy runs only after a hurtEnemy that answers true (vanilla tools do). */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    /** 1.21.1: what a vanilla tool of this kind can do (sweeping, shield disabling...); 26.x reads it off the tool component. */
    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility ability) {
        return switch (kind) {
            case PICKAXE -> net.neoforged.neoforge.common.ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(ability);
            case AXE -> net.neoforged.neoforge.common.ItemAbilities.DEFAULT_AXE_ACTIONS.contains(ability);
            case SHOVEL -> net.neoforged.neoforge.common.ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability);
            case SWORD -> net.neoforged.neoforge.common.ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(ability);
        };
    }

    /** 1.21.1: the attribute lines are hidden here (26.x: the tooltip display component). */
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return super.getDefaultAttributeModifiers(stack).withTooltip(false);
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
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
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
