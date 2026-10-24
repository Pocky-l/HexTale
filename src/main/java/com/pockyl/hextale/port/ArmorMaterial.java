package com.pockyl.hextale.port;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The armour material of Minecraft 26.x (same arguments) on 1.21.1: registers the vanilla material (worn textures
 * {@code textures/models/armor/<asset>_layer_1/2.png}) and keeps the durability factor for the item properties.
 *
 * <p>port-of: net.minecraft.world.item.equipment.ArmorMaterial
 */
public final class ArmorMaterial {
    public static final DeferredRegister<net.minecraft.world.item.ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, com.pockyl.hextale.Hextale.MOD_ID);

    private final int durability;
    private final Holder<net.minecraft.world.item.ArmorMaterial> holder;

    public ArmorMaterial(int durability, Map<ArmorType, Integer> defense, int enchantmentValue, Holder<SoundEvent> equipSound,
                         float toughness, float knockbackResistance, TagKey<Item> repairItems, ResourceLocation asset) {
        this.durability = durability;
        Map<ArmorItem.Type, Integer> byType = new EnumMap<>(ArmorItem.Type.class);
        defense.forEach((type, value) -> byType.put(type.vanilla(), value));
        this.holder = MATERIALS.register(asset.getPath(), () -> new net.minecraft.world.item.ArmorMaterial(byType, enchantmentValue,
                equipSound, () -> Ingredient.of(repairItems), List.of(new net.minecraft.world.item.ArmorMaterial.Layer(asset)),
                toughness, knockbackResistance));
    }

    public Holder<net.minecraft.world.item.ArmorMaterial> holder() {
        return holder;
    }

    /** The properties of a piece of this armour (its durability). */
    public Item.Properties properties(Item.Properties properties, ArmorType type) {
        return properties.durability(type.vanilla().getDurability(durability));
    }
}
