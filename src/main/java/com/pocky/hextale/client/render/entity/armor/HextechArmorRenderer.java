package com.pocky.hextale.client.render.entity.armor;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.items.armor.HextechArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class HextechArmorRenderer extends GeoArmorRenderer<HextechArmorItem> {
    public HextechArmorRenderer() {
        super(new DefaultedItemGeoModel<>(new ResourceLocation(HexTaleMod.MODID, "armor/hextech_armor")));
    }
}
