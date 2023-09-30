package com.pocky.hextale.common.items;

import com.pocky.hextale.client.render.item.HextechCrystalRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class HextechCrystalItem {
    public static class Processed extends AbstractHextechCrystalItem {
        public Processed() {
            super(new Properties().fireResistant().rarity(Rarity.COMMON));
        }

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
        public MeticulouslyProcessed() {
            super(new Properties().fireResistant().rarity(Rarity.UNCOMMON));
        }

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
        public ExpertlyProcessed() {
            super(new Properties().fireResistant().rarity(Rarity.RARE));
        }

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

    public static class Flawless extends AbstractHextechCrystalItem {
        public Flawless() {
            super(new Properties().fireResistant().rarity(Rarity.EPIC));
        }

        @Override
        public boolean isFoil(@NotNull ItemStack stack) {
            return true;
        }

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
