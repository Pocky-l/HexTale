package com.pockyl.hextale.port;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.common.conditions.ConditionalOps;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import org.slf4j.Logger;

/** Data file loading of Minecraft 26.x that 1.21.1 lacks. */
public final class PortReload {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PortReload() {
    }

    /** {@code SimpleJsonResourceReloadListener.scanDirectory} of 26.x: every JSON file under the lister, decoded with conditions. */
    public static <T> Map<ResourceLocation, T> scanDirectory(ResourceManager manager, FileToIdConverter lister, DynamicOps<JsonElement> ops,
                                                            Codec<T> codec, Map<ResourceLocation, T> result) {
        var conditionalCodec = ConditionalOps.createConditionalCodec(codec);
        for (Map.Entry<ResourceLocation, Resource> entry : lister.listMatchingResources(manager).entrySet()) {
            ResourceLocation location = entry.getKey();
            ResourceLocation id = lister.fileToId(location);
            try (Reader reader = entry.getValue().openAsReader()) {
                conditionalCodec.parse(ops, JsonParser.parseReader(reader)).ifSuccess(parsed -> {
                    if (parsed.isPresent() && result.putIfAbsent(id, parsed.get()) != null) {
                        throw new IllegalStateException("Duplicate data file ignored with ID " + id);
                    }
                }).ifError(error -> LOGGER.error("Couldn't parse data file '{}' from '{}': {}", id, location, error));
            } catch (JsonParseException | IllegalArgumentException | IOException e) {
                LOGGER.error("Couldn't parse data file '{}' from '{}'", id, location, e);
            }
        }
        return result;
    }
}
