package com.pocky.hextale.common.items;

import com.pocky.hextale.client.render.item.HextechCrystalRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class HextechCrystalItem {
    public static class Cube extends AbstractHextechCrystalItem {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private HextechCrystalRenderer<HextechCrystalItem.Cube> renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return (this.renderer == null) ? (this.renderer = new HextechCrystalRenderer<>("cube_hextech_crystal")) : this.renderer;
                }
            });
        }
    }

    public static class Rectangle extends AbstractHextechCrystalItem {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private HextechCrystalRenderer<HextechCrystalItem.Rectangle> renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return (this.renderer == null) ? (this.renderer = new HextechCrystalRenderer<>("rectangle_hextech_crystal")) : this.renderer;
                }
            });
        }
    }
}
