package com.pocky.hextale.client.render.entity.armor;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.items.armor.HexElytraItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HexElytraRenderer extends GeoArmorRenderer<HexElytraItem> {
    public HexElytraRenderer() {
        super(new DefaultedItemGeoModel<>(new ResourceLocation(HexTaleMod.MODID, "armor/hex_elytra")));
    }

    @Override
    public RenderType getRenderType(HexElytraItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    public static class Item extends GeoItemRenderer<HexElytraItem> {
        public Item() {
            super(new DefaultedItemGeoModel<>(new ResourceLocation(HexTaleMod.MODID, "armor/hex_elytra")));
        }
        @Override
        public RenderType getRenderType(HexElytraItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }
    }
}
