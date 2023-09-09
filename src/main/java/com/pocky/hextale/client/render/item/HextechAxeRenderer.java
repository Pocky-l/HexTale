package com.pocky.hextale.client.render.item;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.items.HextechAxeItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HextechAxeRenderer extends GeoItemRenderer<HextechAxeItem> {
    public HextechAxeRenderer() {
        super(new DefaultedItemGeoModel<>(new ResourceLocation(HexTaleMod.MODID, "hextech_axe")));
    }

    @Override
    public RenderType getRenderType(HextechAxeItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
