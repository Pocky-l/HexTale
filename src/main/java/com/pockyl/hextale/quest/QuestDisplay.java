package com.pockyl.hextale.quest;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The picture on a quest's page in the research book: a crafting grid, a "this plus that" scheme, the nine essences or
 * the separator's blaze powder per essence. Ingredients are item ids or {@code #tag} ids, {@code ""} is an empty cell. Crafting grids
 * mirror the recipe JSON; {@code questRecipesMatchTheRecipes} keeps them honest. A step may show a second picture under
 * the first ({@code then}): the lapidary and the disc it cuts with.
 */
public record QuestDisplay(Kind kind, List<String> ingredients, String result, int count, @Nullable QuestDisplay then) {
    public enum Kind {
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
        /** A multiblock layer by layer and the item that wakes it; {@code result} is the multiblock's id. */
        MULTIBLOCK,
        /** A Hex Workbench recipe; {@code result} is its id, drawn from the recipes the server sent. */
        HEX_CRAFTING,
        /** A Hex Furnace alloy; {@code result} is its id, drawn from the recipes the server sent. */
        ALLOY
    }

    QuestDisplay(Kind kind, List<String> ingredients, String result, int count) {
        this(kind, ingredients, result, count, null);
    }

    /** This picture with {@code next} drawn under it. */
    QuestDisplay then(QuestDisplay next) {
        return new QuestDisplay(kind, ingredients, result, count, next);
    }

    static QuestDisplay hexCrafting(String recipe) {
        return new QuestDisplay(Kind.HEX_CRAFTING, List.of(), recipe, 1);
    }

    static QuestDisplay alloy(String recipe) {
        return new QuestDisplay(Kind.ALLOY, List.of(), recipe, 1);
    }

    /** Rows of up to three cells, spaces for empty cells, keys as in the recipe JSON. */
    static QuestDisplay shaped(String result, int count, String[] rows, String... keys) {
        String[] cells = new String[9];
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                char c = row < rows.length && column < rows[row].length() ? rows[row].charAt(column) : ' ';
                cells[row * 3 + column] = c == ' ' ? "" : key(keys, c);
            }
        }
        return new QuestDisplay(Kind.SHAPED, List.of(cells), result, count);
    }

    private static String key(String[] keys, char c) {
        for (String key : keys) {
            if (key.charAt(0) == c) {
                return key.substring(2);
            }
        }
        throw new IllegalArgumentException("No key " + c);
    }

    static QuestDisplay shapeless(String result, int count, String... ingredients) {
        return new QuestDisplay(Kind.SHAPELESS, List.of(ingredients), result, count);
    }

    static QuestDisplay scheme(String result, String... ingredients) {
        return new QuestDisplay(Kind.SCHEME, List.of(ingredients), result, 1);
    }

    static QuestDisplay essences() {
        return new QuestDisplay(Kind.ESSENCES, List.of(), "", 0);
    }

    static QuestDisplay reagents() {
        return new QuestDisplay(Kind.REAGENTS, List.of(), "", 0);
    }

    static QuestDisplay pickaxeRunes() {
        return new QuestDisplay(Kind.PICKAXE_RUNES, List.of(), "hextale:hextech_pickaxe", 1);
    }


    static QuestDisplay pylonRunes() {
        return new QuestDisplay(Kind.PYLON_RUNES, List.of(), "hextale:sending_pylon", 1);
    }

    static QuestDisplay multiblock(String id) {
        return new QuestDisplay(Kind.MULTIBLOCK, List.of(), id, 1);
    }

    static QuestDisplay infusion(String recipe) {
        return new QuestDisplay(Kind.INFUSION, List.of(), recipe, 1);
    }

    public boolean isRecipe() {
        return kind == Kind.SHAPED || kind == Kind.SHAPELESS;
    }
}
