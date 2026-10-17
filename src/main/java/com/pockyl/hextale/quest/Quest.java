package com.pockyl.hextale.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechPickaxe;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

/**
 * The researcher's path: the quests on the first page of the research book, in the order they are meant to be done.
 * Each quest follows its {@code parent}; a quest is done the moment its goal is met, in any order, and stays done.
 * Positions are map pixels of the book's path page. Texts: {@code quest.hextale.<id>}, {@code .goal}, {@code .desc}.
 */
public enum Quest {
    // Row 1, left to right: the table, its brass and the diary.
    RESEARCH_TABLE(null, 0, 0, () -> new ItemStack(ModItems.RESEARCH_TABLE.get()), Quests.has(ModItems.RESEARCH_TABLE),
            QuestDisplay.shaped("hextale:research_table", 1, new String[]{"BBB", "PSP", "P P"},
                    "B=hextale:brass_ingot", "P=#minecraft:planks", "S=minecraft:bookshelf")),
    BRASS(RESEARCH_TABLE, 56, 0, () -> new ItemStack(ModItems.BRASS_INGOT.get()), Quests.has(ModItems.BRASS_INGOT),
            QuestDisplay.shapeless("hextale:brass_ingot", 2, "minecraft:copper_ingot", "minecraft:copper_ingot",
                    "minecraft:copper_ingot", "minecraft:gold_ingot")),
    UNSTABLE_HEXTECH(BRASS, 112, 0, () -> new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), Quests.has(ModItems.UNSTABLE_HEXTECH),
            QuestDisplay.scheme("hextale:unstable_hextech", "minecraft:lightning_rod", "minecraft:amethyst_block")),
    RESEARCHERS_DIARY(UNSTABLE_HEXTECH, 168, 0, () -> new ItemStack(ModItems.RESEARCHERS_DIARY.get()), Quests.has(ModItems.RESEARCHERS_DIARY),
            QuestDisplay.shapeless("hextale:researchers_diary", 1, "minecraft:book", "hextale:brass_ingot", "hextale:unstable_hextech")),
    ITEM_STUDY(RESEARCHERS_DIARY, 224, 0, () -> new ItemStack(Items.PAPER), Quests.studied(1), null),
    ESSENCES(ITEM_STUDY, 280, 0, () -> new ItemStack(Items.AMETHYST_SHARD), Quests.studied(5), QuestDisplay.essences()),

    // Row 2, right to left: the essence works, from boiling to ink.
    ALEMBIC(ESSENCES, 280, 80, () -> new ItemStack(ModItems.ALEMBIC.get()), Quests.has(ModItems.ALEMBIC),
            QuestDisplay.shaped("hextale:alembic", 1, new String[]{" B ", "G G", "BGB"},
                    "B=hextale:brass_ingot", "G=minecraft:glass")),
    BRASS_PIPE(ALEMBIC, 336, 80, () -> new ItemStack(ModItems.BRASS_PIPE.get()), Quests.has(ModItems.BRASS_PIPE),
            QuestDisplay.shaped("hextale:brass_pipe", 8, new String[]{"BBB"}, "B=hextale:brass_ingot")),
    PUMP(BRASS_PIPE, 392, 80, () -> new ItemStack(ModItems.PUMP.get()), Quests.has(ModItems.PUMP),
            QuestDisplay.shaped("hextale:pump", 1, new String[]{" P ", "BXB", "BBB"},
                    "P=hextale:brass_pipe", "B=hextale:brass_ingot", "X=minecraft:piston")),
    ESSENCE_BOTTLE(ALEMBIC, 224, 80, () -> EssenceFlaskItem.filled(Quests.SAMPLE_MIX), Quests.has(ModItems.ESSENCE_FLASK),
            QuestDisplay.scheme("hextale:essence_flask", "minecraft:glass_bottle", "hextale:alembic")),
    SEPARATOR(ESSENCE_BOTTLE, 168, 80, () -> new ItemStack(ModItems.SEPARATOR.get()), Quests.has(ModItems.SEPARATOR),
            QuestDisplay.shaped("hextale:separator", 1, new String[]{"BGB", "G G", "BBB"},
                    "B=hextale:brass_ingot", "G=minecraft:glass")),
    DRAIN_VALVE(SEPARATOR, 196, 122, () -> new ItemStack(ModItems.DRAIN_VALVE.get()), Quests.has(ModItems.DRAIN_VALVE),
            QuestDisplay.shaped("hextale:drain_valve", 1, new String[]{" I ", "BPB"},
                    "I=minecraft:iron_ingot", "B=hextale:brass_ingot", "P=hextale:brass_pipe")),
    CONCENTRATE(SEPARATOR, 112, 80, () -> EssenceFlaskItem.filled(EssenceMix.pure(Essence.WATER, EssenceFlaskItem.CAPACITY)),
            Quests::concentrate, QuestDisplay.reagents()),
    ESSENCE_TANK(CONCENTRATE, 56, 80, () -> new ItemStack(ModItems.ESSENCE_TANK.get()), Quests.has(ModItems.ESSENCE_TANK),
            QuestDisplay.shaped("hextale:essence_tank", 1, new String[]{"BSB", "G G", "GGG"},
                    "B=hextale:brass_ingot", "S=#minecraft:wooden_slabs", "G=minecraft:glass")),
    RESEARCHERS_GOGGLES(ESSENCE_TANK, 84, 122, () -> new ItemStack(ModItems.RESEARCHERS_GOGGLES.get()), Quests.has(ModItems.RESEARCHERS_GOGGLES),
            QuestDisplay.shaped("hextale:researchers_goggles", 1, new String[]{"LBL", "G G"},
                    "L=minecraft:leather", "B=hextale:brass_ingot", "G=minecraft:glass_pane")),
    INK(ESSENCE_TANK, 0, 80, () -> new ItemStack(Items.INK_SAC), Quests::inkAtTable, null),

    // Row 3, left to right: runes.
    RUNE_RESEARCH(INK, 0, 160, () -> new ItemStack(ModItems.RUNE_SCROLL.get()), Quests.has(ModItems.RUNE_SCROLL), null),
    FIRST_RUNE(RUNE_RESEARCH, 56, 160, () -> new ItemStack(Items.RAW_IRON), Quests.knowledge(k -> k.knows(ResearchBook.FIRST)), null),
    THREE_STARS(FIRST_RUNE, 56, 216, () -> new ItemStack(Items.NETHER_STAR),
            Quests.knowledge(k -> k.runes().stream().anyMatch(rune -> k.stars(rune) == 3)), null),
    FIRST_SIGNS(FIRST_RUNE, 112, 160, () -> new ItemStack(Items.GLOW_INK_SAC), Quests.knowledge(k -> ResearchBook.chapterOpen(k, 2)), null),
    CRAFT(FIRST_SIGNS, 168, 160, () -> new ItemStack(Items.ANVIL), Quests.knowledge(k -> ResearchBook.chapterOpen(k, 3)), null),
    DEPTHS(CRAFT, 224, 160, () -> new ItemStack(Items.ECHO_SHARD), Quests.knowledge(k -> ResearchBook.learnedIn(k, 3) > 0), null),
    FULL_CODEX(DEPTHS, 280, 160, () -> new ItemStack(Items.ENCHANTED_BOOK), Quests.knowledge(k -> k.runeCount() >= 72), null),

    // Row 4, left to right: rune works - the magic stone of the circle, the Hextech Altar and what it makes.
    MAGIC_STONE(FIRST_RUNE, 112, 216, () -> new ItemStack(ModItems.MAGIC_STONE.get()), Quests.has(ModItems.MAGIC_STONE),
            QuestDisplay.shaped("hextale:magic_stone", 8, new String[]{"SSS", "SBS", "SSS"},
                    "S=#hextale:soaks_essence", "B=hextale:essence_flask")),
    HEXTECH_ALTAR(MAGIC_STONE, 168, 216, () -> new ItemStack(ModItems.HEXTECH_ALTAR.get()), Quests.has(ModItems.HEXTECH_ALTAR),
            QuestDisplay.shaped("hextale:hextech_altar", 1, new String[]{" H ", "IAI", "CBC"}, "H=hextale:unstable_hextech",
                    "I=hextale:brass_ingot", "A=minecraft:amethyst_block", "C=hextale:chiseled_magic_bricks", "B=hextale:magic_bricks")),
    PEDESTAL(HEXTECH_ALTAR, 224, 216, () -> new ItemStack(ModItems.PEDESTAL.get()), Quests.has(ModItems.PEDESTAL),
            QuestDisplay.shaped("hextale:pedestal", 2, new String[]{" I ", " B ", "BCB"},
                    "I=hextale:brass_ingot", "B=hextale:magic_bricks", "C=hextale:chiseled_magic_bricks")),
    RUNE_STELE(HEXTECH_ALTAR, 196, 258, () -> new ItemStack(ModItems.RUNE_STELE.get()), Quests.has(ModItems.RUNE_STELE),
            QuestDisplay.shaped("hextale:rune_stele", 1, new String[]{" A ", " C ", "BBB"},
                    "A=minecraft:amethyst_shard", "C=hextale:chiseled_magic_bricks", "B=hextale:magic_bricks")),
    HEXTECH_GEMSTONE(PEDESTAL, 252, 258, () -> new ItemStack(ModItems.HEXTECH_GEMSTONE.get()), Quests.has(ModItems.HEXTECH_GEMSTONE),
            QuestDisplay.infusion("hextale:hextech_gemstone")),
    HEXTECH_LAMP(HEXTECH_GEMSTONE, 308, 258, () -> new ItemStack(ModItems.HEXTECH_LAMP.get()), Quests.has(ModItems.HEXTECH_LAMP),
            QuestDisplay.shaped("hextale:hextech_lamp", 2, new String[]{" N ", "BGB", " S "}, "N=hextale:brass_nugget",
                    "B=hextale:brass_ingot", "G=hextale:hextech_gemstone", "S=hextale:magic_brick_slab")),
    // The hextech pickaxes, one quest per number of runes their recipes need.
    HEXTECH_PICKAXE(HEXTECH_GEMSTONE, 280, 216, () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.SMELTING).get()), Quests.pickaxe(1),
            QuestDisplay.infusion("hextale:smelting_pickaxe")),
    PICKAXE_TWO_RUNES(HEXTECH_PICKAXE, 336, 216, () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.DRILL).get()), Quests.pickaxe(2),
            QuestDisplay.infusion("hextale:drill_pickaxe")),
    PICKAXE_THREE_RUNES(PICKAXE_TWO_RUNES, 392, 216, () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.TUNNEL).get()), Quests.pickaxe(3),
            QuestDisplay.infusion("hextale:tunnel_pickaxe")),
    PICKAXE_FOUR_RUNES(PICKAXE_THREE_RUNES, 448, 216, () -> new ItemStack(ModItems.pickaxe(HextechPickaxe.CORE).get()), Quests.pickaxe(4),
            QuestDisplay.infusion("hextale:core_pickaxe"));

    @Nullable
    private final Quest parent;
    private final int x;
    private final int y;
    private final Supplier<ItemStack> icon;
    private final Predicate<ServerPlayer> goal;
    @Nullable
    private final QuestDisplay display;

    Quest(@Nullable Quest parent, int x, int y, Supplier<ItemStack> icon, Predicate<ServerPlayer> goal, @Nullable QuestDisplay display) {
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.icon = icon;
        this.goal = goal;
        this.display = display;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static Quest byId(String id) {
        return BY_ID.get(id);
    }

    @Nullable
    public Quest parent() {
        return parent;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public ItemStack icon() {
        return icon.get();
    }

    public boolean isMet(ServerPlayer player) {
        return goal.test(player);
    }

    @Nullable
    public QuestDisplay display() {
        return display;
    }

    /** Rune quests lead on to the star map. */
    public boolean leadsToStars() {
        return ordinal() >= RUNE_RESEARCH.ordinal() && ordinal() <= FULL_CODEX.ordinal();
    }

    public String translationKey() {
        return "quest.hextale." + id();
    }

    private static final Map<String, Quest> BY_ID = Arrays.stream(values()).collect(Collectors.toMap(Quest::id, quest -> quest));
}
