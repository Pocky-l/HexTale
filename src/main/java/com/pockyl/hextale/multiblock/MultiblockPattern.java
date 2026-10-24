package com.pockyl.hextale.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The shape of a multiblock: layers from the bottom up, each a list of rows from north to south, each row a string of
 * cells from west to east. A character names a {@link Key} (what may stand there); a space is "anything", {@code '.'} is
 * air (a hollow inside). One cell is the controller: the block that becomes the structure's heart when it is formed.
 * The shape is found in the world in any of the four turns around the vertical axis, from any of its blocks.
 */
public final class MultiblockPattern {
    /** What may stand in a cell, and the items that show it in the book. */
    public record Key(Predicate<BlockState> test, Supplier<List<ItemStack>> shown) {
        public static final Key AIR = new Key(BlockState::isAir, List::of);

        public static Key block(Supplier<? extends Block> block) {
            return new Key(state -> state.is(block.get()), () -> List.of(new ItemStack(block.get())));
        }

        /** Any block of the tag, shown by the items of the item tag of the same idea (wooden slabs, say). */
        public static Key tag(TagKey<Block> blocks, TagKey<Item> items) {
            return new Key(state -> state.is(blocks), () -> {
                List<ItemStack> shown = new ArrayList<>();
                BuiltInRegistries.ITEM.getTagOrEmpty(items).forEach(item -> shown.add(new ItemStack(item)));
                return shown;
            });
        }
    }

    /** Where a pattern was found: the world position of its cell (0, 0, 0) and its turn. */
    public record Placement(BlockPos origin, Rotation rotation) {
        public static final Codec<Placement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("origin").forGetter(Placement::origin),
                Rotation.CODEC.fieldOf("rotation").forGetter(Placement::rotation)
        ).apply(instance, Placement::new));

        /** The world position of a pattern cell. */
        public BlockPos at(int x, int y, int z) {
            return origin.offset(new BlockPos(x, y, z).rotate(rotation));
        }
    }

    /** One cell of a pattern with its key ({@code null} = anything). */
    public record Cell(int x, int y, int z, char symbol, Key key) {
    }

    private final List<Cell> cells;
    private final int width;
    private final int height;
    private final int depth;
    private final Cell controller;

    private MultiblockPattern(List<Cell> cells, int width, int height, int depth, Cell controller) {
        this.cells = List.copyOf(cells);
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.controller = controller;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Every cell that has a key (cells meaning "anything" are left out). */
    public List<Cell> cells() {
        return cells;
    }

    public Cell controller() {
        return controller;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int depth() {
        return depth;
    }

    /** Looks for the pattern around a block that belongs to it, in every turn; the first fit wins. */
    public Optional<Placement> find(Level level, BlockPos clicked) {
        BlockState state = level.getBlockState(clicked);
        for (Rotation rotation : Rotation.values()) {
            for (Cell cell : cells) {
                if (cell.key() == Key.AIR || !cell.key().test().test(state)) {
                    continue;
                }
                BlockPos origin = clicked.subtract(new BlockPos(cell.x(), cell.y(), cell.z()).rotate(rotation));
                Placement placement = new Placement(origin, rotation);
                if (matches(level, placement)) {
                    return Optional.of(placement);
                }
            }
        }
        return Optional.empty();
    }

    public boolean matches(Level level, Placement placement) {
        for (Cell cell : cells) {
            if (!cell.key().test().test(level.getBlockState(placement.at(cell.x(), cell.y(), cell.z())))) {
                return false;
            }
        }
        return true;
    }

    /** What one kind of cell takes: its key and how many times it stands in a layer (or in the whole structure). */
    public record Material(Key key, int count) {
    }

    /**
     * The blocks a layer takes ({@code -1} = the whole structure), one entry per kind in the order they first appear,
     * kinds that show the same item merged (a controller of glass among glass walls); the hollow cells are left out.
     */
    public List<Material> materials(int layer) {
        Map<Object, Integer> counts = new java.util.LinkedHashMap<>();
        Map<Object, Key> keys = new HashMap<>();
        for (Cell cell : cells) {
            if (cell.key() == Key.AIR || (layer >= 0 && cell.y() != layer)) {
                continue;
            }
            List<ItemStack> shown = cell.key().shown().get();
            Object kind = shown.isEmpty() ? cell.key() : shown.getFirst().getItem();
            counts.merge(kind, 1, Integer::sum);
            keys.putIfAbsent(kind, cell.key());
        }
        List<Material> materials = new ArrayList<>();
        counts.forEach((kind, count) -> materials.add(new Material(keys.get(kind), count)));
        return materials;
    }

    /** How many cells of a layer ({@code -1} = the whole structure) must stay empty. */
    public int hollow(int layer) {
        int hollow = 0;
        for (Cell cell : cells) {
            if (cell.key() == Key.AIR && (layer < 0 || cell.y() == layer)) {
                hollow++;
            }
        }
        return hollow;
    }

    public BlockPos controllerPos(Placement placement) {
        return placement.at(controller.x(), controller.y(), controller.z());
    }

    public static final class Builder {
        private final List<String[]> layers = new ArrayList<>();
        private final Map<Character, Key> keys = new HashMap<>();
        private char controller;

        private Builder() {
            keys.put('.', Key.AIR);
        }

        /** The next layer up: rows from north to south, cells from west to east. */
        public Builder layer(String... rows) {
            layers.add(rows);
            return this;
        }

        public Builder where(char symbol, Key key) {
            keys.put(symbol, key);
            return this;
        }

        /** The controller is the only cell with this symbol. */
        public Builder controller(char symbol) {
            controller = symbol;
            return this;
        }

        public MultiblockPattern build() {
            List<Cell> cells = new ArrayList<>();
            Cell heart = null;
            int width = 0;
            int depth = 0;
            for (int y = 0; y < layers.size(); y++) {
                String[] rows = layers.get(y);
                depth = Math.max(depth, rows.length);
                for (int z = 0; z < rows.length; z++) {
                    width = Math.max(width, rows[z].length());
                    for (int x = 0; x < rows[z].length(); x++) {
                        char symbol = rows[z].charAt(x);
                        if (symbol == ' ') {
                            continue;
                        }
                        Key key = keys.get(symbol);
                        if (key == null) {
                            throw new IllegalStateException("No key for '" + symbol + "'");
                        }
                        Cell cell = new Cell(x, y, z, symbol, key);
                        cells.add(cell);
                        if (symbol == controller) {
                            if (heart != null) {
                                throw new IllegalStateException("Two controllers '" + symbol + "'");
                            }
                            heart = cell;
                        }
                    }
                }
            }
            if (heart == null) {
                throw new IllegalStateException("No controller '" + controller + "'");
            }
            return new MultiblockPattern(cells, width, layers.size(), depth, heart);
        }
    }
}
