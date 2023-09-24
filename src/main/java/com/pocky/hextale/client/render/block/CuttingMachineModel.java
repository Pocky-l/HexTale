package com.pocky.hextale.client.render.block;

import com.pocky.hextale.HexTaleMod;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class CuttingMachineModel<T extends GeoAnimatable> extends DefaultedBlockGeoModel<T> {

    public CuttingMachineModel(String name) {
        super(new ResourceLocation(HexTaleMod.MODID, name));
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(getTextureResource(animatable));
    }
}
