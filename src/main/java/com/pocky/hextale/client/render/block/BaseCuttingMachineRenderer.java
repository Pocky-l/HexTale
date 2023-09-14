package com.pocky.hextale.client.render.block;

import com.pocky.hextale.common.blocks.BaseCuttingMachineEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class BaseCuttingMachineRenderer extends GeoBlockRenderer<BaseCuttingMachineEntity> {
    public BaseCuttingMachineRenderer() {
        super(new BaseCuttingMachineModel());
    }
}
