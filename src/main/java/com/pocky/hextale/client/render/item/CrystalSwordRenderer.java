package com.pocky.hextale.client.render.item;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.items.tools.crystal.CrystalSwordItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class CrystalSwordRenderer extends GeoItemRenderer<CrystalSwordItem> {

    public CrystalSwordRenderer() {
        super(new DefaultedItemGeoModel<>(new ResourceLocation(HexTaleMod.MODID, "crystal_sword")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

//    @Override
//    public RenderType getRenderType(CrystalSwordItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
//        return AutoGlowingTexture.getRenderType(texture);
//    }
}