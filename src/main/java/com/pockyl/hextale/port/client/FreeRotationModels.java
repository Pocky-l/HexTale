package com.pockyl.hextale.port.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.SimpleUnbakedGeometry;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Model elements turned freely, as 26.x allows ({@code "rotation": {"x", "y", "z", "origin"}} or any single-axis
 * angle); 1.21.1's model format only turns about one axis by 22.5 degree steps. The asset port script gives such models
 * the loader {@code hextale:free_rotation} and renames their {@code elements} to {@code free_elements} (vanilla parses
 * {@code elements} itself and rejects the turns); here each element is baked axis-aligned with its turn (Euler ZYX, like
 * 26.x's CuboidRotation, rescale included) folded into the model state's transform.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FreeRotationModels {
    private static final FaceBakery FACE_BAKERY = new FaceBakery();

    private FreeRotationModels() {
    }

    @SubscribeEvent
    static void registerLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(Hextale.id("free_rotation"), Loader.INSTANCE);
    }

    /** One element and its turn about {@code origin} (block units), or none. */
    private record Part(BlockElement element, @Nullable Matrix4f rotation, Vector3f origin) {
    }

    private static final class Loader implements IGeometryLoader<Geometry> {
        static final Loader INSTANCE = new Loader();

        @Override
        public Geometry read(JsonObject json, JsonDeserializationContext context) {
            List<Part> parts = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "free_elements", new JsonArray())) {
                JsonObject copy = element.getAsJsonObject().deepCopy();
                JsonObject rotation = copy.has("rotation") ? copy.remove("rotation").getAsJsonObject() : null;
                BlockElement parsed = context.deserialize(copy, BlockElement.class);
                Matrix4f matrix = null;
                Vector3f origin = new Vector3f(0.5F, 0.5F, 0.5F);
                if (rotation != null) {
                    JsonArray o = GsonHelper.getAsJsonArray(rotation, "origin");
                    origin = new Vector3f(o.get(0).getAsFloat() / 16, o.get(1).getAsFloat() / 16, o.get(2).getAsFloat() / 16);
                    float rad = (float) (Math.PI / 180);
                    if (rotation.has("axis")) {
                        float angle = GsonHelper.getAsFloat(rotation, "angle") * rad;
                        Direction.Axis axis = Direction.Axis.byName(GsonHelper.getAsString(rotation, "axis"));
                        Vector3f unit = new Vector3f(axis == Direction.Axis.X ? 1 : 0, axis == Direction.Axis.Y ? 1 : 0, axis == Direction.Axis.Z ? 1 : 0);
                        matrix = new Matrix4f().rotation(angle, unit);
                    } else {
                        matrix = new Matrix4f().rotationZYX(GsonHelper.getAsFloat(rotation, "z", 0) * rad,
                                GsonHelper.getAsFloat(rotation, "y", 0) * rad, GsonHelper.getAsFloat(rotation, "x", 0) * rad);
                    }
                    if (GsonHelper.getAsBoolean(rotation, "rescale", false)) {
                        matrix.scale(rescale(matrix, 0), rescale(matrix, 1), rescale(matrix, 2));
                    }
                }
                parts.add(new Part(parsed, matrix, origin));
            }
            return new Geometry(parts);
        }

        private static float rescale(Matrix4f rotation, int axis) {
            Vector3f unit = rotation.transformDirection(new Vector3f(axis == 0 ? 1 : 0, axis == 1 ? 1 : 0, axis == 2 ? 1 : 0));
            return 1 / Math.max(Math.max(Math.abs(unit.x), Math.abs(unit.y)), Math.abs(unit.z));
        }
    }

    private static final class Geometry extends SimpleUnbakedGeometry<Geometry> {
        private final List<Part> parts;

        Geometry(List<Part> parts) {
            this.parts = parts;
        }

        @Override
        protected void addQuads(IGeometryBakingContext owner, IModelBuilder<?> builder, ModelBaker baker,
                                Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState) {
            Matrix4f state = modelState.getRotation().getMatrix();
            Vector3f centre = new Vector3f(0.5F, 0.5F, 0.5F);
            for (Part part : parts) {
                ModelState transform = modelState;
                if (part.rotation() != null) {
                    // p -> state(R(p - o) + o) as a turn about the block's centre (FaceBakery's pivot): M = state * [R | t],
                    // t = R(c - o) + o - c.
                    Vector3f t = part.rotation().transformPosition(new Vector3f(centre).sub(part.origin())).add(part.origin()).sub(centre);
                    Matrix4f combined = new Matrix4f(state).mul(new Matrix4f().translation(t).mul(part.rotation()));
                    transform = new Turned(new Transformation(combined));
                }
                BlockElement element = part.element();
                for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                    BlockElementFace face = entry.getValue();
                    TextureAtlasSprite sprite = spriteGetter.apply(owner.getMaterial(face.texture()));
                    BakedQuad quad = FACE_BAKERY.bakeQuad(element.from, element.to, face, sprite, entry.getKey(), transform, null, element.shade);
                    if (part.rotation() == null && face.cullForDirection() != null) {
                        builder.addCulledFace(Direction.rotate(state, face.cullForDirection()), quad);
                    } else {
                        builder.addUnculledFace(quad);
                    }
                }
            }
        }
    }

    /** A model state with an arbitrary turn (FaceBakery then keeps the winding as baked). */
    private record Turned(Transformation rotation) implements ModelState {
        @Override
        public Transformation getRotation() {
            return rotation;
        }

        @Override
        public boolean isUvLocked() {
            return false;
        }

        @Override
        public boolean mayApplyArbitraryRotation() {
            return true;
        }
    }
}
