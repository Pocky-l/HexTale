package com.pocky.hextale.client.render.block;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.blocks.BaseCuttingMachineEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class BaseCuttingMachineModel extends DefaultedBlockGeoModel<BaseCuttingMachineEntity> {

    public BaseCuttingMachineModel() {
        super(new ResourceLocation(HexTaleMod.MODID, "base_cutting_machine"));
    }

    @Override
    public RenderType getRenderType(BaseCuttingMachineEntity animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(getTextureResource(animatable));
    }
}
