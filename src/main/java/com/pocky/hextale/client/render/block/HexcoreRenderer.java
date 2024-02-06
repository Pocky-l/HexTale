package com.pocky.hextale.client.render.block;

import com.pocky.hextale.common.blocks.HexcoreEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class HexcoreRenderer extends GeoBlockRenderer<HexcoreEntity> {

    public HexcoreRenderer() {
        super(new HexcoreModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
