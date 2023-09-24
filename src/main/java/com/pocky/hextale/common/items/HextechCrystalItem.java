package com.pocky.hextale.common.items;

import com.pocky.hextale.client.render.item.HextechCrystalRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class HextechCrystalItem {
    public static class Processed extends AbstractHextechCrystalItem {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private HextechCrystalRenderer<HextechCrystalItem.Processed> renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return (this.renderer == null) ? (this.renderer = new HextechCrystalRenderer<>("processed_hextech_crystal")) : this.renderer;
                }
            });
        }
    }

    public static class MeticulouslyProcessed extends AbstractHextechCrystalItem {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private HextechCrystalRenderer<HextechCrystalItem.MeticulouslyProcessed> renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return (this.renderer == null) ? (this.renderer = new HextechCrystalRenderer<>("meticulously_processed_hextech_crystal")) : this.renderer;
                }
            });
        }
    }

    public static class ExpertlyProcessed extends AbstractHextechCrystalItem {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private HextechCrystalRenderer<HextechCrystalItem.ExpertlyProcessed> renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return (this.renderer == null) ? (this.renderer = new HextechCrystalRenderer<>("expertly_processed_hextech_crystal")) : this.renderer;
                }
            });
        }
    }

    public static class Purified extends AbstractHextechCrystalItem {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private HextechCrystalRenderer<HextechCrystalItem.Purified> renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return (this.renderer == null) ? (this.renderer = new HextechCrystalRenderer<>("purified_hextech_crystal")) : this.renderer;
                }
            });
        }
    }

    public static class MeticulouslyPurified extends AbstractHextechCrystalItem {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private HextechCrystalRenderer<HextechCrystalItem.MeticulouslyPurified> renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return (this.renderer == null) ? (this.renderer = new HextechCrystalRenderer<>("meticulously_purified_hextech_crystal")) : this.renderer;
                }
            });
        }
    }

    public static class ExpertlyPurified extends AbstractHextechCrystalItem {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private HextechCrystalRenderer<HextechCrystalItem.ExpertlyPurified> renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return (this.renderer == null) ? (this.renderer = new HextechCrystalRenderer<>("expertly_purified_hextech_crystal")) : this.renderer;
                }
            });
        }
    }

    public static class Polished extends AbstractHextechCrystalItem {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private HextechCrystalRenderer<HextechCrystalItem.Polished> renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return (this.renderer == null) ? (this.renderer = new HextechCrystalRenderer<>("polished_hextech_crystal")) : this.renderer;
                }
            });
        }
    }

    public static class MeticulouslyPolished extends AbstractHextechCrystalItem {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private HextechCrystalRenderer<HextechCrystalItem.MeticulouslyPolished> renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return (this.renderer == null) ? (this.renderer = new HextechCrystalRenderer<>("meticulously_polished_hextech_crystal")) : this.renderer;
                }
            });
        }
    }

    public static class ExpertlyPolished extends AbstractHextechCrystalItem {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private HextechCrystalRenderer<HextechCrystalItem.ExpertlyPolished> renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return (this.renderer == null) ? (this.renderer = new HextechCrystalRenderer<>("expertly_polished_hextech_crystal")) : this.renderer;
                }
            });
        }
    }

    public static class Flawless extends AbstractHextechCrystalItem {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private HextechCrystalRenderer<HextechCrystalItem.Flawless> renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return (this.renderer == null) ? (this.renderer = new HextechCrystalRenderer<>("flawless_hextech_crystal")) : this.renderer;
                }
            });
        }
    }
}
