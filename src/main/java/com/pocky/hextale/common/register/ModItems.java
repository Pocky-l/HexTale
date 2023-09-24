package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.items.*;
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
    public static final RegistryObject<Item> PROCESSED_HEXTECH_CRYSTAL = ITEMS.register("processed_hextech_crystal", HextechCrystalItem.Processed::new);

    public static final RegistryObject<Item> METICULOUSLY_PROCESSED_HEXTECH_CRYSTAL = ITEMS.register("meticulously_processed_hextech_crystal", HextechCrystalItem.MeticulouslyProcessed::new);
    public static final RegistryObject<Item> EXPERTLY_PROCESSED_HEXTECH_CRYSTAL = ITEMS.register("expertly_processed_hextech_crystal", HextechCrystalItem.ExpertlyProcessed::new);
    public static final RegistryObject<Item> PURIFIED_HEXTECH_CRYSTAL = ITEMS.register("purified_hextech_crystal", HextechCrystalItem.Purified::new);
    public static final RegistryObject<Item> METICULOUSLY_PURIFIED_HEXTECH_CRYSTAL = ITEMS.register("meticulously_purified_hextech_crystal", HextechCrystalItem.MeticulouslyPurified::new);
    public static final RegistryObject<Item> EXPERTLY_PURIFIED_HEXTECH_CRYSTAL = ITEMS.register("expertly_purified_hextech_crystal", HextechCrystalItem.ExpertlyPurified::new);

    public static final RegistryObject<Item> POLISHED_HEXTECH_CRYSTAL = ITEMS.register("polished_hextech_crystal", HextechCrystalItem.Polished::new);
    public static final RegistryObject<Item> METICULOUSLY_POLISHED_HEXTECH_CRYSTAL = ITEMS.register("meticulously_polished_hextech_crystal", HextechCrystalItem.MeticulouslyPolished::new);
    public static final RegistryObject<Item> EXPERTLY_POLISHED_HEXTECH_CRYSTAL = ITEMS.register("expertly_polished_hextech_crystal", HextechCrystalItem.ExpertlyPolished::new);
    public static final RegistryObject<Item> FLAWLESS_HEXTECH_CRYSTAL = ITEMS.register("flawless_hextech_crystal", HextechCrystalItem.Flawless::new);


    public static final RegistryObject<Item> SHIMMER = ITEMS.register("shimmer", Shimmer::new);

    public static final RegistryObject<Item> HEXTECH_PICKAXE = ITEMS.register("hextech_pickaxe", () -> new HextechPickaxeItem(Tiers.NETHERITE, 4, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_AXE = ITEMS.register("hextech_axe", () -> new HextechAxeItem(Tiers.NETHERITE, 4, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_SWORD = ITEMS.register("hextech_sword", () -> new HextechSwordItem(Tiers.NETHERITE, 8, -2.0F, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_SHOVEL = ITEMS.register("hextech_shovel", () -> new HextechShovelItem(Tiers.NETHERITE, 4, -2.8F, new Item.Properties()));


    public static final RegistryObject<Item> HEXTECH_HELMET = ITEMS.register("hextech_helmet", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_CHESTPLATE = ITEMS.register("hextech_chestplate", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_LEGGINGS = ITEMS.register("hextech_leggings", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_BOOTS = ITEMS.register("hextech_boots", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.BOOTS, new Item.Properties()));

}
