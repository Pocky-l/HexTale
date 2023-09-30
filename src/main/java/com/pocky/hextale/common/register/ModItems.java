package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.items.*;
import com.pocky.hextale.common.items.armor.HexElytraItem;
import com.pocky.hextale.common.items.armor.HexJetpackItem;
import com.pocky.hextale.common.items.armor.HextechArmorItem;
import com.pocky.hextale.common.items.armor.ModArmorMaterials;
import net.minecraft.world.item.*;
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
    public static final RegistryObject<Item> FLAWLESS_HEXTECH_CRYSTAL = ITEMS.register("flawless_hextech_crystal", HextechCrystalItem.Flawless::new);

    public static final RegistryObject<Item> HEX_GLASS = ITEMS.register("hex_glass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MASTER_HEX_GLASS = ITEMS.register("master_hex_glass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> EXPERTLY_HEX_GLASS = ITEMS.register("expertly_hex_glass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLOUDY_LENS = ITEMS.register("cloudy_lens", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BASE_LENS = ITEMS.register("base_lens", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MASTER_LENS = ITEMS.register("master_lens", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> EXPERTLY_LENS = ITEMS.register("expertly_lens", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COPPER_DISK = ITEMS.register("copper_disk", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> IRON_DISK = ITEMS.register("iron_disk", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GOLD_DISK = ITEMS.register("gold_disk", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DIAMOND_DISK = ITEMS.register("diamond_disk", () -> new Item(new Item.Properties()));


    public static final RegistryObject<Item> SHIMMER = ITEMS.register("shimmer", Shimmer::new);

    public static final RegistryObject<Item> HEXTECH_PICKAXE = ITEMS.register("hextech_pickaxe", () -> new HextechPickaxeItem(Tiers.NETHERITE, 4, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_AXE = ITEMS.register("hextech_axe", () -> new HextechAxeItem(Tiers.NETHERITE, 4, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_SWORD = ITEMS.register("hextech_sword", () -> new HextechSwordItem(Tiers.NETHERITE, 8, -2.0F, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_SHOVEL = ITEMS.register("hextech_shovel", () -> new HextechShovelItem(Tiers.NETHERITE, 4, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> HEX_HAMMER = ITEMS.register("hex_hammer", () -> new HexHammerItem(Tiers.NETHERITE, 4, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> HEX_PICKAXE = ITEMS.register("hex_pickaxe", () -> new HexPickaxeItem(Tiers.NETHERITE, 4, -2.8F, new Item.Properties()));


    public static final RegistryObject<Item> HEXTECH_HELMET = ITEMS.register("hextech_helmet", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_CHESTPLATE = ITEMS.register("hextech_chestplate", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_LEGGINGS = ITEMS.register("hextech_leggings", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_BOOTS = ITEMS.register("hextech_boots", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> HEX_JETPACK = ITEMS.register("hex_jetpack", () -> new HexJetpackItem(ModArmorMaterials.HEX_JETPACK, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> HEX_ELYTRA = ITEMS.register("hex_elytra", () -> new HexElytraItem(ModArmorMaterials.HEX_ELYTRA, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

}
