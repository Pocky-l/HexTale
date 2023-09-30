package com.pocky.hextale.client.render.item;

import com.pocky.hextale.HexTaleMod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HextechCrystalRenderer<T extends Item & GeoAnimatable> extends GeoItemRenderer<T> {
    public HextechCrystalRenderer(String id) {
        super(new DefaultedItemGeoModel<>(new ResourceLocation(HexTaleMod.MODID, id)));
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        //RenderType.eyes(texture);
        //RenderType.dragonExplosionAlpha(texture);
        //RenderType.itemEntityTranslucentCull(texture);
        //RenderType.crumbling(texture);
            RenderType.beaconBeam(texture, true);
        //RenderType.entityTranslucent(texture);
        RenderType.armorCutoutNoCull(texture);
        //RenderType.energySwirl();
        RenderType.entityNoOutline(texture);
        return RenderType.text(texture);
    }
}
