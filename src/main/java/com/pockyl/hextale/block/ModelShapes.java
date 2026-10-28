package com.pockyl.hextale.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pockyl.hextale.Hextale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Outline and collision shapes that follow the block models. {@code tools/shape-gen} voxelizes the models of every
 * blockstate into a few boxes and writes them to {@code data/hextale/block_shapes.json} (per-block settings in the
 * mod's {@code shape-gen.json}); a block whose shape is its model returns {@link #of(BlockState)}. Regenerate after
 * changing a block model: {@code python tools/shape-gen/shape_gen.py mods/hextale}.
 */
public final class ModelShapes {
    private static final String FILE = "/data/" + Hextale.MOD_ID + "/block_shapes.json";
    private static final Map<Identifier, Entry> ENTRIES = load();
    private static final Map<BlockState, VoxelShape> CACHE = new ConcurrentHashMap<>();
    private static final Map<Named, VoxelShape> NAMED_CACHE = new ConcurrentHashMap<>();

    private ModelShapes() {
    }

    /** Whether the block has a generated shape. */
    public static boolean covers(Block block) {
        return ENTRIES.containsKey(BuiltInRegistries.BLOCK.getKey(block));
    }

    /** The shape of the state's models: the first matching variant, or every matching multipart part. */
    public static VoxelShape of(BlockState state) {
        return CACHE.computeIfAbsent(state, s -> compute(s, BuiltInRegistries.BLOCK.getKey(s.getBlock())));
    }

    /** A further shape of the block from its {@code shapes} settings (e.g. a collision wider than the cell). */
    public static VoxelShape of(BlockState state, String name) {
        return NAMED_CACHE.computeIfAbsent(new Named(state, name), key -> {
            Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            return compute(state, id.withPath(id.getPath() + "/" + name));
        });
    }

    private static VoxelShape compute(BlockState state, Identifier id) {
        Entry entry = ENTRIES.get(id);
        if (entry == null) {
            throw new IllegalStateException("No generated shape for " + id + ": run tools/shape-gen/shape_gen.py");
        }
        VoxelShape shape = Shapes.empty();
        for (Case part : entry.cases()) {
            if (matches(part.when(), state)) {
                if (entry.first()) {
                    return part.shape();
                }
                shape = Shapes.or(shape, part.shape());
            }
        }
        return shape.optimize();
    }

    /** Blockstate condition semantics: "a|b" lists values, "!" negates, OR / AND hold nested conditions. */
    private static boolean matches(JsonObject when, BlockState state) {
        for (Map.Entry<String, JsonElement> condition : when.entrySet()) {
            String key = condition.getKey();
            if (key.equals("OR") || key.equals("AND")) {
                boolean any = false;
                boolean all = true;
                for (JsonElement sub : condition.getValue().getAsJsonArray()) {
                    boolean match = matches(sub.getAsJsonObject(), state);
                    any |= match;
                    all &= match;
                }
                if (key.equals("OR") ? !any : !all) {
                    return false;
                }
                continue;
            }
            Property<?> property = state.getBlock().getStateDefinition().getProperty(key);
            if (property == null) {
                return false;
            }
            String text = condition.getValue().getAsString();
            boolean negate = text.startsWith("!");
            boolean listed = Arrays.asList((negate ? text.substring(1) : text).split("\\|")).contains(name(state, property));
            if (listed == negate) {
                return false;
            }
        }
        return true;
    }

    private static <T extends Comparable<T>> String name(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static Map<Identifier, Entry> load() {
        try (InputStream in = ModelShapes.class.getResourceAsStream(FILE)) {
            if (in == null) {
                throw new IllegalStateException(FILE + " is missing: run tools/shape-gen/shape_gen.py");
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<Identifier, Entry> entries = new HashMap<>();
            for (Map.Entry<String, JsonElement> block : root.entrySet()) {
                JsonObject data = block.getValue().getAsJsonObject();
                List<Case> cases = new ArrayList<>();
                for (JsonElement element : data.getAsJsonArray("cases")) {
                    JsonObject part = element.getAsJsonObject();
                    VoxelShape shape = Shapes.empty();
                    for (JsonElement box : part.getAsJsonArray("boxes")) {
                        JsonArray b = box.getAsJsonArray();
                        shape = Shapes.or(shape, Block.box(b.get(0).getAsDouble(), b.get(1).getAsDouble(),
                                b.get(2).getAsDouble(), b.get(3).getAsDouble(), b.get(4).getAsDouble(), b.get(5).getAsDouble()));
                    }
                    JsonObject when = part.has("when") ? part.getAsJsonObject("when") : new JsonObject();
                    cases.add(new Case(when, shape.optimize()));
                }
                entries.put(Identifier.parse(block.getKey()), new Entry("first".equals(data.get("mode").getAsString()), cases));
            }
            return Map.copyOf(entries);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record Named(BlockState state, String name) {
    }

    private record Entry(boolean first, List<Case> cases) {
    }

    private record Case(JsonObject when, VoxelShape shape) {
    }
}
