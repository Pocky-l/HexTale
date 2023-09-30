package com.pocky.hextale.client.render.entity.armor;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.items.armor.HexJetpackItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HexJetpackRenderer extends GeoArmorRenderer<HexJetpackItem> {
    public HexJetpackRenderer() {
        super(new DefaultedItemGeoModel<>(new ResourceLocation(HexTaleMod.MODID, "armor/hex_jetpack")));
    }

    @Override
    public RenderType getRenderType(HexJetpackItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    public static class Item extends GeoItemRenderer<HexJetpackItem> {
        public Item() {
            super(new DefaultedItemGeoModel<>(new ResourceLocation(HexTaleMod.MODID, "armor/hex_jetpack")));
        }

        @Override
        public RenderType getRenderType(HexJetpackItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }
    }
}
