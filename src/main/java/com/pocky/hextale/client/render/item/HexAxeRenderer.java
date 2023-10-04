package com.pocky.hextale.client.render.item;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.items.HexAxeItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HexAxeRenderer extends GeoItemRenderer<HexAxeItem> {
    public HexAxeRenderer() {
        super(new DefaultedItemGeoModel<>(new ResourceLocation(HexTaleMod.MODID, "hex_axe")));
    }

    @Override
    public RenderType getRenderType(HexAxeItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
