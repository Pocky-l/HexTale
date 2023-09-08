package com.pocky.hextale.client.render.block;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.blocks.SuspiciousRemainsGravelEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class SuspiciousRemainsGravelModel extends DefaultedBlockGeoModel<SuspiciousRemainsGravelEntity> {

    public SuspiciousRemainsGravelModel() {
        super(new ResourceLocation(HexTaleMod.MODID, "suspicious_remains_gravel"));
    }

    @Override
    public RenderType getRenderType(SuspiciousRemainsGravelEntity animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(getTextureResource(animatable));
    }
}
