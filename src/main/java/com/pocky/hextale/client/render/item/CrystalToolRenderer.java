package com.pocky.hextale.client.render.item;

import com.pocky.hextale.HexTaleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class CrystalToolRenderer<T extends Item & GeoAnimatable> extends GeoItemRenderer<T> {

    public CrystalToolRenderer(final String id) {
        super(new DefaultedItemGeoModel<>(new ResourceLocation(HexTaleMod.MODID, id)));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }


}