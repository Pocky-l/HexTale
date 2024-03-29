package com.pocky.hextale.client.render.item;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.items.RawHextechCrystalItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RawHextechCrystalRenderer extends GeoItemRenderer<RawHextechCrystalItem> {
    public RawHextechCrystalRenderer() {
        super(new DefaultedItemGeoModel<>(new ResourceLocation(HexTaleMod.MODID, "raw_hextech_crystal")));
    }

    /*
    @Override
    public RenderType getRenderType(RawHextechCrystalItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }*/
}
