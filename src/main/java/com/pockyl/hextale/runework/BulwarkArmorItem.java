package com.pockyl.hextale.runework;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.function.Consumer;

import com.pockyl.hextale.registry.ModSounds;

/**
 * A piece of Bulwark armour: brass plates over blued steel, made in four levels like the tools (first, refined, radiant,
 * riftforged). Each piece takes the runes of the clothes piece worn in its slot (helmet = hood, ...), as many as its
 * level holds ({@link RuneTier}). The chestplate carries the crystal heart ({@link BulwarkHeart}): raw hextech clicked
 * onto it in the inventory charges it.
 */
public class BulwarkArmorItem extends ResearcherClothesItem {
    private final ArmorType type;

    public BulwarkArmorItem(ArmorType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public ArmorType type() {
        return type;
    }

    public static boolean isHeart(ItemStack stack) {
        return stack.getItem() instanceof BulwarkArmorItem armor && armor.type == ArmorType.CHESTPLATE;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player,
                                            SlotAccess carriedItem) {
        if (type != ArmorType.CHESTPLATE || !BulwarkHeart.feeds(other) || !slot.allowModification(player)) {
            return false;
        }
        BulwarkHeart.Charge charge = BulwarkHeart.of(self);
        if (charge.charge() >= BulwarkHeart.MAX) {
            return false;
        }
        BulwarkHeart.set(self, charge.plus(BulwarkHeart.PER_CRYSTAL));
        other.shrink(1);
        player.playSound(ModSounds.RITUAL_ABSORB.get(), 0.4F, 1.4F);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        if (type == ArmorType.CHESTPLATE) {
            BulwarkHeart.Charge charge = BulwarkHeart.of(stack);
            tooltip.accept((charge.cracked()
                    ? Component.translatable("item.hextale.bulwark.heart_cracked", charge.charge(), BulwarkHeart.MAX)
                    : Component.translatable("item.hextale.bulwark.heart", charge.charge(), BulwarkHeart.MAX))
                    .withStyle(charge.cracked() ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA));
        }
    }
}
