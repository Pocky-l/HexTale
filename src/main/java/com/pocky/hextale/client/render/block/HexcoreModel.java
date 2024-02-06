package com.pocky.hextale.client.render.block;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.blocks.HexcoreEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class HexcoreModel extends DefaultedBlockGeoModel<HexcoreEntity> {

    public HexcoreModel() {
        super(new ResourceLocation(HexTaleMod.MODID, "hexcore"));
    }

    @Override
    public RenderType getRenderType(HexcoreEntity animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(getTextureResource(animatable));
    }
}
