package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.items.HextechPickaxeItem;
import com.pocky.hextale.common.items.RawHextechCrystalItem;
import com.pocky.hextale.common.items.Shimmer;
import com.pocky.hextale.common.items.armor.HextechArmorItem;
import com.pocky.hextale.common.items.armor.ModArmorMaterials;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import software.bernie.example.item.WolfArmorItem;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HexTaleMod.MODID);

    public static final RegistryObject<Item> RAW_HEXTECH_CRYSTAL = ITEMS.register("raw_hextech_crystal", RawHextechCrystalItem::new);

    public static final RegistryObject<Item> SHIMMER = ITEMS.register("shimmer", Shimmer::new);

    public static final RegistryObject<Item> HEXTECH_PICKAXE = ITEMS.register("hextech_pickaxe", () -> new HextechPickaxeItem(Tiers.NETHERITE, 4, -2.8F, new Item.Properties()));

    public static final RegistryObject<Item> HEXTECH_HELMET = ITEMS.register("hextech_helmet", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_CHESTPLATE = ITEMS.register("hextech_chestplate", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_LEGGINGS = ITEMS.register("hextech_leggings", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_BOOTS = ITEMS.register("hextech_boots", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.BOOTS, new Item.Properties()));

}
