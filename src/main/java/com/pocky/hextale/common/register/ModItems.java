package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.items.RawHextechCrystalItem;
import com.pocky.hextale.common.items.Shimmer;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HexTaleMod.MODID);

    public static final RegistryObject<Item> RAW_HEXTECH_CRYSTAL = ITEMS.register("raw_hextech_crystal", RawHextechCrystalItem::new);

    public static final RegistryObject<Item> SHIMMER = ITEMS.register("shimmer", Shimmer::new);
}
