package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.items.HextechCrystalItem;
import com.pocky.hextale.common.items.RawHextechCrystalItem;
import com.pocky.hextale.common.items.Shimmer;
import com.pocky.hextale.common.items.armor.HexElytraItem;
import com.pocky.hextale.common.items.armor.HexJetpackItem;
import com.pocky.hextale.common.items.armor.HextechArmorItem;
import com.pocky.hextale.common.items.armor.ModArmorMaterials;
import com.pocky.hextale.common.items.tools.crystal.*;
import com.pocky.hextale.common.items.tools.misc.JewelryToolItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HexTaleMod.MODID);

    public static final List<RegistryObject<Item>> EGGS = new LinkedList<>();

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

    public static final RegistryObject<Item> HEXTECH_SHARD = ITEMS.register("hextech_shard", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLEAR_HEXTECH = ITEMS.register("clear_hextech", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> JEWELRY_TOOL = ITEMS.register("jewelry_tool", JewelryToolItem::new);

    public static final RegistryObject<Item> SHIMMER = ITEMS.register("shimmer", Shimmer::new);

    public static final RegistryObject<Item> CRYSTAL_PICKAXE = ITEMS.register(CrystalPickaxeItem.ID, CrystalPickaxeItem::new);
    public static final RegistryObject<Item> CRYSTAL_AXE = ITEMS.register(CrystalAxeItem.ID, CrystalAxeItem::new);
    public static final RegistryObject<Item> CRYSTAL_SWORD = ITEMS.register(CrystalSwordItem.ID, CrystalSwordItem::new);
    public static final RegistryObject<Item> CRYSTAL_SHOVEL = ITEMS.register(CrystalShovelItem.ID, CrystalShovelItem::new);
    public static final RegistryObject<Item> CRYSTAL_HAMMER = ITEMS.register(CrystalHammerItem.ID, CrystalHammerItem::new);
    public static final RegistryObject<Item> CRYSTAL_BOW = ITEMS.register(CrystalBowItem.ID, CrystalBowItem::new);

    public static final RegistryObject<Item> HEXTECH_HELMET = ITEMS.register("hextech_helmet", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_CHESTPLATE = ITEMS.register("hextech_chestplate", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_LEGGINGS = ITEMS.register("hextech_leggings", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> HEXTECH_BOOTS = ITEMS.register("hextech_boots", () -> new HextechArmorItem(ModArmorMaterials.HEXTECH, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> HEX_JETPACK = ITEMS.register("hex_jetpack", () -> new HexJetpackItem(ModArmorMaterials.HEX_JETPACK, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> HEX_ELYTRA = ITEMS.register("hex_elytra", () -> new HexElytraItem(ModArmorMaterials.HEX_ELYTRA, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

}
