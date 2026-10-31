package com.pockyl.hextale.runework;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.pockyl.hextale.port.ArmorType;

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

    public BulwarkArmorItem(net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> material, ArmorType type,
                            Properties properties) {
        super(material, type.vanilla(), properties);
        this.type = type;
    }

    /**
     * The sheet a worn piece of this level is painted with (26.x: the client's equipment layer texture), whichever
     * armour layer asks: {@code textures/entity/equipment/bulwark/<level>_<piece>.png}.
     */
    @Override
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity,
            net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.item.ArmorMaterial.Layer layer, boolean innerModel) {
        return wornTexture(type, stack);
    }

    /** The worn sheet of a piece of this kind and level. */
    public static net.minecraft.resources.ResourceLocation wornTexture(ArmorType type, ItemStack stack) {
        String piece = switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            default -> "boots";
        };
        return com.pockyl.hextale.Hextale.id("textures/entity/equipment/bulwark/" + LEVELS[RuneTier.tier(stack) - 1] + "_" + piece + ".png");
    }

    private static final String[] LEVELS = {"base", "refined", "radiant", "riftforged"};

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
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        super.appendHoverText(stack, context, tooltipLines, flag);
        if (type == ArmorType.CHESTPLATE) {
            BulwarkHeart.Charge charge = BulwarkHeart.of(stack);
            tooltip.accept((charge.cracked()
                    ? Component.translatable("item.hextale.bulwark.heart_cracked", charge.charge(), BulwarkHeart.MAX)
                    : Component.translatable("item.hextale.bulwark.heart", charge.charge(), BulwarkHeart.MAX))
                    .withStyle(charge.cracked() ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA));
        }
    }
}
