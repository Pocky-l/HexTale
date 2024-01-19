package com.pocky.hextale.client.render.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

public class CrystalClientItemExtensions<T extends Item & GeoAnimatable> implements IClientItemExtensions {

    private CrystalToolRenderer renderer;

    private final String ITEM_ID;

    public CrystalClientItemExtensions(String id) {
        this.ITEM_ID = id;
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (this.renderer == null)
            this.renderer = new CrystalToolRenderer<>(ITEM_ID);

        return this.renderer;
    }
}
