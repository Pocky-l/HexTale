package com.pocky.hextale.client.render.entity;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.entity.GeoVoidlingEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class VoidlingEntityModel extends DefaultedEntityGeoModel<GeoVoidlingEntity> {

    public VoidlingEntityModel() {
        super(new ResourceLocation(HexTaleMod.MODID, "voidling"), true);
    }
}
