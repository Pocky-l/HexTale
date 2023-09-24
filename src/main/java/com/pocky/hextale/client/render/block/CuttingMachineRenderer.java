package com.pocky.hextale.client.render.block;

import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class CuttingMachineRenderer<T extends BlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    public CuttingMachineRenderer(CuttingMachineModel<T> model) {
        super(model);
    }
}
