package com.pockyl.hextale.port;

import net.minecraft.core.Holder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Block and item registers that also take the property forms of NeoForge 26.x (a supplier of block properties, a
 * function over item properties), so the registry classes read the same on every Minecraft version.
 */
public final class PortRegisters {
    private PortRegisters() {
    }

    public static Blocks createBlocks(String namespace) {
        return new Blocks(namespace);
    }

    public static Items createItems(String namespace) {
        return new Items(namespace);
    }

    public static final class Blocks extends DeferredRegister.Blocks {
        private Blocks(String namespace) {
            super(namespace);
        }

        public <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> func,
                                                                Supplier<BlockBehaviour.Properties> properties) {
            return this.register(name, () -> func.apply(properties.get()));
        }
    }

    public static final class Items extends DeferredRegister.Items {
        private Items(String namespace) {
            super(namespace);
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block, UnaryOperator<Item.Properties> properties) {
            return this.registerSimpleBlockItem(block, properties.apply(new Item.Properties()));
        }

        public DeferredItem<Item> registerSimpleItem(String name, UnaryOperator<Item.Properties> properties) {
            return this.registerSimpleItem(name, properties.apply(new Item.Properties()));
        }
    }
}
