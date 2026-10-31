package com.pockyl.hextale.port.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Special item models on 1.21.1. The 26.x item definitions ({@code assets/hextale/items/*.json}) put a code-drawn
 * model ({@code minecraft:special}) alone or next to plain models ({@code minecraft:composite}); such items get a block
 * entity without level renderer that draws the plain models, then the special one. Their 1.21.1 item models are
 * {@code builtin/entity} with the display of the definition's base (made by the asset port script). A composite may
 * hold several specials (drawn in order), a plain part that is the item's own model file (its geometry is kept in
 * {@code item/port/<id>}) and a {@code minecraft:range_dispatch} on {@code minecraft:use_duration} (the charge stages
 * of an item held in use).
 */
@EventBusSubscriber(modid = Hextale.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SpecialItemModels {
    private static final Map<ResourceLocation, MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> TYPES = new HashMap<>();
    private static final Map<Item, Entry> ENTRIES = new HashMap<>();
    private static final Map<Item, List<SpecialModelRenderer<?>>> BAKED = new HashMap<>();
    private static @Nullable BlockEntityWithoutLevelRenderer renderer;
    /** Items whose own client extensions hand out {@link #customRenderer()} (one registration per item). */
    private static final java.util.Set<Item> CLAIMED = new java.util.HashSet<>();

    private SpecialItemModels() {
    }

    /** What 26.x's {@code RegisterSpecialModelRendererEvent#register} did. */
    public static void register(ResourceLocation id, MapCodec<? extends SpecialModelRenderer.Unbaked<?>> codec) {
        TYPES.put(id, codec);
    }

    /** A plain part: one model, or models picked by how long the item has been in use (thresholds rising). */
    private record Plain(ModelResourceLocation fallback, float[] thresholds, List<ModelResourceLocation> stages) {
        static Plain of(ModelResourceLocation model) {
            return new Plain(model, new float[0], List.of());
        }

        ModelResourceLocation pick(float used) {
            ModelResourceLocation picked = fallback;
            for (int i = 0; i < thresholds.length; i++) {
                if (used >= thresholds[i]) {
                    picked = stages.get(i);
                }
            }
            return picked;
        }

        java.util.stream.Stream<ModelResourceLocation> all() {
            return java.util.stream.Stream.concat(java.util.stream.Stream.of(fallback), stages.stream());
        }
    }

    private record Entry(List<Plain> plain, List<JsonObject> specials) {
    }

    private static Map<Item, Entry> entries() {
        if (ENTRIES.isEmpty()) {
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id.getNamespace().equals(Hextale.MOD_ID)) {
                    Entry entry = read(id);
                    if (entry != null) {
                        ENTRIES.put(item, entry);
                    }
                }
            }
        }
        return ENTRIES;
    }

    private static @Nullable Entry read(ResourceLocation id) {
        JsonObject model;
        try (InputStream in = SpecialItemModels.class.getResourceAsStream("/assets/" + id.getNamespace() + "/items/" + id.getPath() + ".json")) {
            if (in == null) {
                return null;
            }
            model = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("model");
            // The mod's "animated" wrapper only matters to the 26.x GUI item cache.
            while (model.has("type") && "hextale:animated".equals(model.get("type").getAsString())) {
                model = model.getAsJsonObject("model");
            }
        } catch (Exception e) {
            Hextale.LOGGER.warn("Item definition of {} not readable", id, e);
            return null;
        }
        List<JsonObject> parts = new ArrayList<>();
        if ("minecraft:composite".equals(model.get("type").getAsString())) {
            for (JsonElement part : model.getAsJsonArray("models")) {
                parts.add(part.getAsJsonObject());
            }
        } else {
            parts.add(model);
        }
        List<Plain> plain = new ArrayList<>();
        List<JsonObject> specials = new ArrayList<>();
        for (JsonObject part : parts) {
            String type = part.get("type").getAsString();
            if (type.equals("minecraft:special")) {
                specials.add(part.getAsJsonObject("model"));
            } else if (type.equals("minecraft:model")) {
                plain.add(Plain.of(location(id, part)));
            } else if (type.equals("minecraft:range_dispatch") && "minecraft:use_duration".equals(part.get("property").getAsString())) {
                float scale = part.has("scale") ? part.get("scale").getAsFloat() : 1;
                JsonArray entries = part.getAsJsonArray("entries");
                float[] thresholds = new float[entries.size()];
                List<ModelResourceLocation> stages = new ArrayList<>();
                for (int i = 0; i < thresholds.length; i++) {
                    JsonObject entry = entries.get(i).getAsJsonObject();
                    thresholds[i] = entry.get("threshold").getAsFloat() / scale;
                    stages.add(location(id, entry.getAsJsonObject("model")));
                }
                plain.add(new Plain(location(id, part.getAsJsonObject("fallback")), thresholds, stages));
            }
        }
        return specials.isEmpty() ? null : new Entry(plain, specials);
    }

    /** A plain model part; the item's own model file is the builtin one on 1.21.1, its geometry sits in item/port. */
    private static ModelResourceLocation location(ResourceLocation item, JsonObject part) {
        ResourceLocation model = ResourceLocation.parse(part.get("model").getAsString());
        if (model.equals(item.withPrefix("item/"))) {
            model = item.withPrefix("item/port/");
        }
        return ModelResourceLocation.standalone(model);
    }

    @SubscribeEvent
    static void registerModels(ModelEvent.RegisterAdditional event) {
        entries().values().forEach(entry -> entry.plain().forEach(part -> part.all().forEach(event::register)));
    }

    @SubscribeEvent
    static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> BAKED.clear());
    }

    /** For an item that registers its own client extensions: they return this renderer, this class stays out. */
    public static BlockEntityWithoutLevelRenderer customRenderer(Item item) {
        CLAIMED.add(item);
        return renderer();
    }

    /** Claims the item for its own extensions; call while registering them (this class registers last). */
    public static void claim(Item item) {
        CLAIMED.add(item);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    static void registerExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions extensions = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer();
            }
        };
        for (Item item : entries().keySet()) {
            if (!CLAIMED.contains(item)) {
                event.registerItem(extensions, item);
            }
        }
    }

    private static BlockEntityWithoutLevelRenderer renderer() {
        if (renderer == null) {
            Minecraft minecraft = Minecraft.getInstance();
            renderer = new BlockEntityWithoutLevelRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels()) {
                @Override
                public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers,
                                         int light, int overlay) {
                    draw(stack, poseStack, buffers, light, overlay);
                }
            };
        }
        return renderer;
    }

    @SuppressWarnings("unchecked")
    private static void draw(ItemStack stack, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        Entry entry = entries().get(stack.getItem());
        if (entry == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        float used = used(stack);
        for (Plain part : entry.plain()) {
            BakedModel model = minecraft.getModelManager().getModel(part.pick(used));
            minecraft.getItemRenderer().renderModelLists(model, stack, light, overlay, poseStack,
                    ItemRenderer.getFoilBufferDirect(buffers, Sheets.translucentCullBlockSheet(), true, stack.hasFoil()));
        }
        for (SpecialModelRenderer<?> baked : BAKED.computeIfAbsent(stack.getItem(), item -> bake(entry))) {
            SpecialModelRenderer<Object> special = (SpecialModelRenderer<Object>) baked;
            special.submit(special.extractArgument(stack), poseStack, new SubmitNodeCollector(buffers), light, overlay, stack.hasFoil(), 0);
        }
    }

    /** Ticks the local player has held this very stack in use (26.x: the holder's use duration), else 0. */
    private static float used(ItemStack stack) {
        net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isUsingItem() && player.getUseItem() == stack ? player.getTicksUsingItem() : 0;
    }

    private static List<SpecialModelRenderer<?>> bake(Entry entry) {
        List<SpecialModelRenderer<?>> baked = new ArrayList<>();
        for (JsonObject special : entry.specials()) {
            SpecialModelRenderer<?> renderer = bake(special);
            if (renderer != null) {
                baked.add(renderer);
            }
        }
        return baked;
    }

    private static @Nullable SpecialModelRenderer<?> bake(JsonObject special) {
        MapCodec<? extends SpecialModelRenderer.Unbaked<?>> codec = TYPES.get(ResourceLocation.parse(special.get("type").getAsString()));
        if (codec == null) {
            return null;
        }
        return codec.codec().parse(JsonOps.INSTANCE, special).result()
                .map(unbaked -> unbaked.bake(new SpecialModelRenderer.BakingContext(Minecraft.getInstance().getEntityModels())))
                .orElse(null);
    }
}
