package com.pockyl.hextale.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * The picture on a quest's page in the research book: a crafting grid, a "this plus that" scheme, the nine essences or
 * the separator's blaze powder per essence. Ingredients are item ids or {@code #tag} ids, {@code ""} is an empty cell. Crafting grids
 * mirror the recipe JSON; {@code questRecipesMatchTheRecipes} keeps them honest. A step may show a second picture under
 * the first ({@code then}): the lapidary and the disc it cuts with. Part of a guide step's data ({@link QuestDefinition}).
 */
public record QuestDisplay(Kind kind, List<String> ingredients, String result, int count, @Nullable QuestDisplay then) {
    /** {@code {"kind": "hex_crafting", "result": "hextale:alembic"}}; grids list nine cells, {@code then} nests a picture. */
    public static final Codec<QuestDisplay> CODEC = Codec.recursive("QuestDisplay", self -> RecordCodecBuilder.create(i -> i.group(
            Kind.CODEC.fieldOf("kind").forGetter(QuestDisplay::kind),
            Codec.STRING.listOf().optionalFieldOf("ingredients", List.of()).forGetter(QuestDisplay::ingredients),
            Codec.STRING.optionalFieldOf("result", "").forGetter(QuestDisplay::result),
            Codec.INT.optionalFieldOf("count", 1).forGetter(QuestDisplay::count),
            self.optionalFieldOf("then").forGetter(display -> Optional.ofNullable(display.then()))
    ).apply(i, (kind, ingredients, result, count, then) -> new QuestDisplay(kind, ingredients, result, count, then.orElse(null)))));

    public enum Kind implements StringRepresentable {
        /** A 3 x 3 crafting grid, row by row. */
        SHAPED,
        /** Ingredients in any order. */
        SHAPELESS,
        /** Ingredients side by side, then an arrow and the result: something that happens in the world. */
        SCHEME,
        /** The nine essences' signs. */
        ESSENCES,
        /** The blaze powder that draws out each primal essence. */
        REAGENTS,
        /** A Hextech Altar ritual; {@code result} is the altar recipe's id, drawn from the recipes the server sent. */
        INFUSION,
        /** The runes a hextech pickaxe takes, with their costs and clashes. */
        PICKAXE_RUNES,
        /** The runes the hex pylons take. */
        PYLON_RUNES,
        /** The runes any other rune holder takes; {@code result} is the holder's id ({@code RuneHolder#id}, e.g. "reaper"). */
        RUNES,
        /** A multiblock layer by layer and the item that wakes it; {@code result} is the multiblock's id. */
        MULTIBLOCK,
        /** A Hex Workbench recipe; {@code result} is its id, drawn from the recipes the server sent. */
        HEX_CRAFTING,
        /** A Hex Furnace alloy; {@code result} is its id, drawn from the recipes the server sent. */
        ALLOY;

        static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public boolean isRecipe() {
        return kind == Kind.SHAPED || kind == Kind.SHAPELESS;
    }
}
