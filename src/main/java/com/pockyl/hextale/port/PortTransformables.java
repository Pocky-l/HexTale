package com.pockyl.hextale.port;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;

import com.pockyl.hextale.Hextale;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Axe stripping from NeoForge 26.x's {@code transformables} block data map ({@code data/neoforge/data_maps/block/
 * transformables.json}, kept as it is): on 1.21.1 the axe's tool modification event turns each listed block into the
 * block its rule copies the properties to.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class PortTransformables {
    private static final String FILE = "/data/neoforge/data_maps/block/transformables.json";
    private static Map<Block, Block> stripped;

    private PortTransformables() {
    }

    @SubscribeEvent
    static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.AXE_STRIP) {
            return;
        }
        BlockState state = event.getState();
        Block target = stripped().get(state.getBlock());
        if (target != null) {
            event.setFinalState(copyProperties(state, target.defaultBlockState()));
        }
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        return to.hasProperty(property) ? to.setValue(property, from.getValue(property)) : to;
    }

    private static BlockState copyProperties(BlockState from, BlockState to) {
        for (Property<?> property : from.getProperties()) {
            to = copy(from, to, property);
        }
        return to;
    }

    private static Map<Block, Block> stripped() {
        if (stripped == null) {
            stripped = new HashMap<>();
            try (InputStream in = PortTransformables.class.getResourceAsStream(FILE)) {
                if (in != null) {
                    JsonObject values = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                            .getAsJsonObject("values");
                    for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
                        JsonObject value = entry.getValue().getAsJsonObject();
                        if (!"minecraft:axe".equals(value.get("transformer").getAsString())) {
                            continue;
                        }
                        String id = findSource(value);
                        if (id != null) {
                            stripped.put(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.getKey())),
                                    BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)));
                        }
                    }
                }
            } catch (Exception e) {
                Hextale.LOGGER.warn("Transformables not readable", e);
            }
        }
        return stripped;
    }

    /** The block a transform rule turns into: the first {@code source.id} in it. */
    private static String findSource(JsonElement node) {
        if (node.isJsonObject()) {
            JsonObject object = node.getAsJsonObject();
            if (object.has("source") && object.get("source").isJsonObject() && object.getAsJsonObject("source").has("id")) {
                return object.getAsJsonObject("source").get("id").getAsString();
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String found = findSource(entry.getValue());
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isJsonArray()) {
            for (JsonElement element : node.getAsJsonArray()) {
                String found = findSource(element);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
