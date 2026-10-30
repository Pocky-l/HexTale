package com.pockyl.hextale.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.HextechAltarBlockEntity;
import com.pockyl.hextale.block.PedestalBlockEntity;
import com.pockyl.hextale.block.RuneSteleBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.RuneHolder;
import com.pockyl.hextale.runework.PickaxeRunes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * The ritual circle around a Hextech Altar and what it would do now. Pure planning: nothing in the world changes here.
 */
public final class Ritual {
    /** Pedestals stand within this many blocks of the altar (any number of them), steles a little farther. */
    public static final int PEDESTAL_RADIUS = 6;
    public static final int STELE_RADIUS = 7;
    public static final int TANK_RADIUS = 8;
    public static final int WARMUP_TICKS = 56;
    public static final int TICKS_PER_ITEM = 28;
    public static final int FINALE_TICKS = 34;
    /** The altar draws {@value #DRAW_MB} mB of essence every {@value #DRAW_TICKS} ticks, one essence after another. */
    public static final int DRAW_MB = 20;
    public static final int DRAW_TICKS = 7;
    /** A ritual short of something waits this long, then collapses. */
    public static final int STALL_LIMIT = 600;

    private Ritual() {
    }

    /**
     * The Rune Circle this rune stele stands round (the nearest with an item on it, else the nearest), or null: steles
     * lend their runes only to centres that {@link HextechAltarBlockEntity#inscribes() inscribe}.
     */
    @Nullable
    public static HextechAltarBlockEntity altarOf(Level level, BlockPos stele) {
        HextechAltarBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int cx = (stele.getX() - STELE_RADIUS) >> 4; cx <= (stele.getX() + STELE_RADIUS) >> 4; cx++) {
            for (int cz = (stele.getZ() - STELE_RADIUS) >> 4; cz <= (stele.getZ() + STELE_RADIUS) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (var entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (!(entity instanceof HextechAltarBlockEntity altar) || entity.isRemoved() || !altar.inscribes()) {
                        continue;
                    }
                    BlockPos pos = entity.getBlockPos();
                    int dy = stele.getY() - pos.getY();
                    if (Math.abs(stele.getX() - pos.getX()) > STELE_RADIUS || Math.abs(stele.getZ() - pos.getZ()) > STELE_RADIUS
                            || dy < -1 || dy > 2) {
                        continue;
                    }
                    // An altar holding something counts first: that is the one the stele is being set up for.
                    double distance = pos.distSqr(stele) + (altar.center().isEmpty() ? 10_000 : 0);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = altar;
                    }
                }
            }
        }
        return best;
    }

    /**
     * The runes that do something for this centre item at the altar: the runes it takes like enchantments (a pickaxe,
     * a pylon) and the runes the recipes it fits ask for on the steles. Null for an empty centre: then any
     * rune goes.
     */
    @Nullable
    public static Set<ComplexRune> runesFor(ItemStack center, boolean clientSide) {
        List<RuneUse> uses = usesFor(center, clientSide);
        if (uses == null) {
            return null;
        }
        Set<ComplexRune> runes = new java.util.LinkedHashSet<>();
        uses.forEach(use -> runes.add(use.rune()));
        return runes;
    }

    /**
     * What a rune does for the centre item: burned into it ({@code holder}, the item takes it like an enchantment)
     * and/or asked for by {@code recipes}, the rituals that turn it into {@code makes} (same order).
     */
    public record RuneUse(ComplexRune rune, @Nullable RuneHolder holder, List<ItemStack> makes, List<InfusionRecipe> recipes,
                          boolean writes) {
        /** The essence the altar draws for the rune at these stars: burned in, written on a scroll, or asked for by a ritual. */
        public EssenceMix cost(int stars) {
            return holder != null ? holder.cost(rune, stars) : writes ? writingCost(rune, stars) : InfusionRecipe.runeEssence(rune);
        }
    }

    /** Whether this centre item is a blank scroll waiting for a rune. */
    public static boolean blankScroll(ItemStack center) {
        return center.is(ModItems.BLANK_SCROLL.get());
    }

    /** What writing a rune on a blank scroll draws: the rune's own essence, once per star the scroll will carry. */
    public static EssenceMix writingCost(ComplexRune rune, int stars) {
        EssenceMix one = InfusionRecipe.runeEssence(rune);
        EssenceMix cost = EssenceMix.EMPTY;
        for (int i = 0; i < stars; i++) {
            cost = cost.plus(one);
        }
        return cost;
    }

    /**
     * The runes that do something for this centre item, each once: first the ones it takes (in the holder's order, as
     * the book shows them), then the ones its rituals ask for (codex order). Null for an empty centre.
     */
    @Nullable
    public static List<RuneUse> usesFor(ItemStack center, boolean clientSide) {
        if (center.isEmpty()) {
            return null;
        }
        if (blankScroll(center)) {
            // Any rune in use can be written on a blank scroll (the stele offers the ones its researcher knows).
            List<RuneUse> uses = new ArrayList<>();
            for (ComplexRune rune : com.pockyl.hextale.rune.ComplexRunes.inUse()) {
                uses.add(new RuneUse(rune, null, List.of(RuneScrollItem.of(rune, 1, ModItems.RUNE_SCROLL.get())), List.of(), true));
            }
            return uses;
        }
        Map<ComplexRune, List<InfusionRecipe>> asking = new java.util.TreeMap<>(Comparator.comparingInt(ComplexRune::index));
        for (InfusionRecipe recipe : InfusionRecipes.get(clientSide).values()) {
            if (recipe.center().test(center)) {
                for (ComplexRune rune : recipe.runes()) {
                    asking.computeIfAbsent(rune, key -> new ArrayList<>()).add(recipe);
                }
            }
        }
        List<RuneUse> uses = new ArrayList<>();
        RuneHolder holder = RuneHolder.of(center);
        if (holder != null) {
            for (ComplexRune rune : holder.takes(center)) {
                List<InfusionRecipe> recipes = asking.remove(rune);
                uses.add(use(rune, holder, recipes == null ? List.of() : recipes, center));
            }
        }
        asking.forEach((rune, recipes) -> uses.add(use(rune, null, recipes, center)));
        return uses;
    }

    /**
     * The runes the item would carry with what the OTHER steles of the circle show (the item's own runes plus the new
     * ones it takes), for the slot count; empty when the item takes no runes.
     */
    public static Set<ComplexRune> carried(ItemStack center, List<ComplexRune> onOtherSteles) {
        Set<ComplexRune> carried = new java.util.LinkedHashSet<>();
        RuneHolder holder = RuneHolder.of(center);
        if (holder == null) {
            return carried;
        }
        carried.addAll(holder.runes(center).complexRunes());
        for (ComplexRune rune : onOtherSteles) {
            if (holder.takes(center, rune)) {
                carried.add(rune);
            }
        }
        return carried;
    }

    /** The runes chosen on the steles of the altar's circle other than the one at {@code stele}. */
    public static List<ComplexRune> onOtherSteles(Level level, HextechAltarBlockEntity altar, BlockPos stele) {
        List<ComplexRune> runes = new ArrayList<>();
        for (RuneSteleBlockEntity other : Circle.scan(level, altar.getBlockPos()).steles()) {
            if (!other.getBlockPos().equals(stele) && other.rune() != null) {
                runes.add(other.rune());
            }
        }
        return runes;
    }

    /**
     * Why this rune cannot be chosen on a stele whose circle's other steles show {@code onOtherSteles}, for the item
     * on the altar, or null when it can: a rune stands on one stele of a circle only; a rune holder never gets more runes
     * than its slots (raising the stars of a rune it has takes none) nor two that clash.
     */
    @Nullable
    public static Component steleRefusal(ItemStack center, List<ComplexRune> onOtherSteles, ComplexRune rune) {
        if (onOtherSteles.contains(rune)) {
            return Component.translatable("gui.hextale.stele.refuse.twice");
        }
        if (blankScroll(center) && !onOtherSteles.isEmpty()) {
            return Component.translatable("gui.hextale.stele.refuse.one_scroll");
        }
        RuneHolder holder = RuneHolder.of(center);
        if (holder == null || !holder.takes(center, rune)) {
            return null;
        }
        Set<ComplexRune> carried = carried(center, onOtherSteles);
        for (ComplexRune other : carried) {
            if (other != rune && holder.clashes(rune, other)) {
                return Component.translatable("gui.hextale.stele.refuse.clash",
                        Component.translatable(other.translationKey()));
            }
        }
        if (!carried.contains(rune) && carried.size() >= holder.slots()) {
            return Component.translatable("gui.hextale.stele.refuse.full", holder.slots());
        }
        return null;
    }

    private static RuneUse use(ComplexRune rune, @Nullable RuneHolder holder, List<InfusionRecipe> recipes, ItemStack center) {
        return new RuneUse(rune, holder, recipes.stream().map(recipe -> recipe.resultFor(center)).toList(), recipes, false);
    }

    /** What the altar's surroundings hold: pedestals, rune steles (clockwise from north) and essence tanks. */
    public record Circle(List<PedestalBlockEntity> pedestals, List<RuneSteleBlockEntity> steles, List<EssenceTankBlockEntity> tanks) {
        public static Circle scan(Level level, BlockPos altar) {
            List<PedestalBlockEntity> pedestals = new ArrayList<>();
            List<RuneSteleBlockEntity> steles = new ArrayList<>();
            List<EssenceTankBlockEntity> tanks = new ArrayList<>();
            // Walk the block entities of the nearby chunks rather than every block of the cube.
            for (int cx = (altar.getX() - TANK_RADIUS) >> 4; cx <= (altar.getX() + TANK_RADIUS) >> 4; cx++) {
                for (int cz = (altar.getZ() - TANK_RADIUS) >> 4; cz <= (altar.getZ() + TANK_RADIUS) >> 4; cz++) {
                    if (!level.hasChunk(cx, cz)) {
                        continue;
                    }
                    for (var entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                        BlockPos pos = entity.getBlockPos();
                        int dx = Math.abs(pos.getX() - altar.getX());
                        int dy = pos.getY() - altar.getY();
                        int dz = Math.abs(pos.getZ() - altar.getZ());
                        if (entity.isRemoved()) {
                            continue;
                        }
                        if (entity instanceof PedestalBlockEntity pedestal) {
                            if (dx <= PEDESTAL_RADIUS && dz <= PEDESTAL_RADIUS && Math.abs(dy) <= 1) {
                                pedestals.add(pedestal);
                            }
                        } else if (entity instanceof RuneSteleBlockEntity stele) {
                            if (dx <= STELE_RADIUS && dz <= STELE_RADIUS && dy >= -1 && dy <= 2) {
                                steles.add(stele);
                            }
                        } else if (entity instanceof EssenceTankBlockEntity tank && dx <= TANK_RADIUS && dz <= TANK_RADIUS
                                && Math.abs(dy) <= TANK_RADIUS) {
                            tanks.add(tank);
                        }
                    }
                }
            }
            steles.sort(Comparator.comparingDouble(stele -> clockwise(altar, stele.getBlockPos())));
            pedestals.sort(Comparator.comparingDouble(pedestal -> clockwise(altar, pedestal.getBlockPos())));
            return new Circle(pedestals, steles, tanks);
        }

        /** The runes chosen on the steles, in stele order, each once (with the best stars it is chosen with). */
        public List<Chosen> chosenRunes() {
            Map<ComplexRune, Chosen> chosen = new java.util.LinkedHashMap<>();
            for (RuneSteleBlockEntity stele : steles) {
                if (stele.rune() != null) {
                    chosen.merge(stele.rune(), new Chosen(stele.rune(), stele.stars(), stele.exact()),
                            (a, b) -> b.stars() > a.stars() ? b : a);
                }
            }
            return List.copyOf(chosen.values());
        }

        /** The stars a rune is chosen with on the steles, 0 when no stele shows it. */
        public int stars(ComplexRune rune) {
            return chosenRunes().stream().filter(c -> c.rune() == rune).mapToInt(Chosen::stars).findFirst().orElse(0);
        }

        public List<PedestalBlockEntity> filledPedestals() {
            return pedestals.stream().filter(pedestal -> !pedestal.item().isEmpty()).toList();
        }
    }

    /** Ticks the altar takes to draw {@code mb} mB of essence. */
    public static int drawTicks(int mb) {
        return Mth.ceil(mb * DRAW_TICKS / (float) DRAW_MB);
    }

    /** mB drawn on the {@code tick}-th tick of drawing (0-based): {@value #DRAW_MB} per {@value #DRAW_TICKS} ticks, spread evenly. */
    public static int drawnOn(int tick) {
        return (tick + 1) * DRAW_MB / DRAW_TICKS - tick * DRAW_MB / DRAW_TICKS;
    }

    /** The essence a ritual draws now: the first one still owed, in essence order; null when nothing is owed. */
    public static @Nullable Essence drawingEssence(EssenceMix left) {
        for (Essence essence : Essence.values()) {
            if (left.get(essence) > 0) {
                return essence;
            }
        }
        return null;
    }

    /** Angle of {@code pos} around {@code centre}, 0 at north, growing clockwise (seen from above). */
    static double clockwise(BlockPos centre, BlockPos pos) {
        double angle = Mth.atan2(pos.getX() - centre.getX(), centre.getZ() - pos.getZ());
        return angle < 0 ? angle + Math.PI * 2 : angle;
    }

    /**
     * A rune chosen on a stele and the stars it goes in with: the researcher's best unless they were picked by hand
     * ({@code exact}), which may also make a rune already burned in weaker.
     */
    public record Chosen(ComplexRune rune, int stars, boolean exact) {
        public Chosen(ComplexRune rune, int stars) {
            this(rune, stars, false);
        }
    }

    /**
     * What a ritual does: make something new from the centre item, burn the steles' runes into a hextech pickaxe (or
     * another rune holder), or write a stele's rune on a blank scroll.
     */
    public enum Kind {
        INFUSE, ENGRAVE, WRITE
    }

    /**
     * Burning the steles' runes into a pickaxe: what it carries afterwards and what it costs, or why it cannot be done
     * ({@code refusal}: a translation key under {@code block.hextale.hextech_altar.engrave.} with the runes it names).
     */
    public record Engraving(@Nullable PickaxeRunes after, EssenceMix cost, @Nullable String refusal, List<ComplexRune> named) {
        static Engraving refuse(String key, ComplexRune... named) {
            return new Engraving(null, EssenceMix.EMPTY, key, List.of(named));
        }

        public boolean possible() {
            return after != null;
        }
    }

    /**
     * Burns every rune chosen on the steles into the pickaxe or the pylon ({@link RuneHolder}) like an enchantment: a
     * new rune takes a free place (up to the holder's slots), a rune it already carries with fewer stars is raised to the
     * stele's stars - or set to exactly them, weaker too, when the researcher picked the stars by hand (a 5 x 5 Drill
     * may be wanted as a 3 x 3 one). Only runes the holder takes go in, and never two that clash. Runes need no items,
     * only their essence.
     */
    public static Engraving engraving(ItemStack item, Circle circle) {
        RuneHolder holder = RuneHolder.of(item);
        if (holder == null) {
            return Engraving.refuse("nothing_new");
        }
        String suffix = holder.refusalSuffix();
        PickaxeRunes runes = holder.runes(item);
        EssenceMix cost = EssenceMix.EMPTY;
        boolean changed = false;
        for (Chosen chosen : circle.chosenRunes()) {
            ComplexRune rune = chosen.rune();
            if (!holder.takes(item, rune)) {
                return Engraving.refuse(holder.notTakenRefusal(), rune);
            }
            int have = runes.stars(rune);
            if (chosen.exact() ? chosen.stars() == have : chosen.stars() <= have) {
                continue;
            }
            for (ComplexRune other : runes.complexRunes()) {
                if (holder.clashes(rune, other)) {
                    return Engraving.refuse("clash" + suffix, rune, other);
                }
            }
            if (have == 0 && runes.count() >= holder.slots()) {
                return Engraving.refuse("full" + suffix, rune);
            }
            runes = runes.with(rune, chosen.stars());
            cost = cost.plus(holder.cost(rune, chosen.stars()));
            changed = true;
        }
        return changed ? new Engraving(runes, cost, null, List.of()) : Engraving.refuse("nothing_new");
    }

    /**
     * What a ritual will do.
     *
     * @param recipe   the infusion recipe's id
     * @param inputs   the pedestals to draw from, in drawing order, with the item each must hold
     * @param essences what it draws from the tanks (the recipe's and its runes')
     * @param result   what the centre item becomes
     */
    public record Plan(Kind kind, @Nullable Identifier recipe, List<Input> inputs, EssenceMix essences, ItemStack result) {
        /** Warm-up, then the pedestal items one by one, then the essences one by one, then the finale. */
        public int duration() {
            return WARMUP_TICKS + inputs.size() * TICKS_PER_ITEM + drawTicks(essences.total()) + FINALE_TICKS;
        }
    }

    /** One pedestal the ritual draws from and the item it must still hold then. */
    public record Input(BlockPos pos, Identifier item) {
        public boolean holds(ItemStack stack) {
            return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(item);
        }
    }

    /** The recipes, those that need the most runes first (a circle set up for a rich recipe never makes a poorer one). */
    private static List<Map.Entry<Identifier, InfusionRecipe>> recipes(boolean clientSide) {
        List<Map.Entry<Identifier, InfusionRecipe>> list = new ArrayList<>(InfusionRecipes.get(clientSide).entrySet());
        list.sort(Comparator.comparingInt((Map.Entry<Identifier, InfusionRecipe> e) -> -e.getValue().runes().size()));
        return list;
    }

    /**
     * What the altar would do with this centre item and circle, or null (nothing fits). A recipe needs its runes chosen
     * on the steles. A hextech pickaxe or a Hex Smelter alone on the altar (no pedestal items) with runes on the steles takes them in.
     */
    @Nullable
    public static Plan plan(ItemStack center, Circle circle, boolean clientSide) {
        if (center.isEmpty()) {
            return null;
        }
        List<PedestalBlockEntity> filled = circle.filledPedestals();
        if (blankScroll(center)) {
            // One rune from one stele onto the scroll, with the stars it is chosen with; paid with its essence only.
            List<Chosen> chosen = circle.chosenRunes();
            if (!filled.isEmpty() || chosen.size() != 1) {
                return null;
            }
            Chosen rune = chosen.getFirst();
            return new Plan(Kind.WRITE, null, List.of(), writingCost(rune.rune(), rune.stars()),
                    RuneScrollItem.of(rune.rune(), rune.stars(), ModItems.RUNE_SCROLL.get()));
        }
        RuneHolder holder = RuneHolder.of(center);
        if (holder != null && filled.isEmpty() && !circle.chosenRunes().isEmpty()) {
            // A pickaxe or a pylon alone on the altar with runes on the steles: they are burned in, paid with essence only.
            Engraving engraving = engraving(center, circle);
            if (!engraving.possible()) {
                return null;
            }
            ItemStack result = center.copy();
            holder.set(result, engraving.after());
            return new Plan(Kind.ENGRAVE, null, List.of(), engraving.cost(), result);
        }
        for (Map.Entry<Identifier, InfusionRecipe> entry : recipes(clientSide)) {
            InfusionRecipe recipe = entry.getValue();
            if (!recipe.center().test(center) || !recipe.runes().stream().allMatch(rune -> circle.stars(rune) > 0)) {
                continue;
            }
            List<Input> inputs = match(recipe.pedestals(), filled);
            if (inputs == null) {
                continue;
            }
            return new Plan(Kind.INFUSE, entry.getKey(), inputs, recipe.totalEssences(), recipe.resultFor(center));
        }
        return null;
    }

    /**
     * What the circle still lacks for the ritual it is being set up for: the pedestal items nobody has laid out yet
     * (as stacks to show), the pedestals holding something that ritual does not take, and the runes no stele shows.
     * Candidates are every recipe the centre item fits; the one closest to complete wins. Empty when a ritual can start
     * or nothing fits the centre item.
     */
    public record Hint(List<ItemStack> missing, List<BlockPos> extra, List<ComplexRune> runes) {
        public static final Hint NONE = new Hint(List.of(), List.of(), List.of());

        public boolean isEmpty() {
            return missing.isEmpty() && extra.isEmpty() && runes.isEmpty();
        }
    }

    public static Hint hint(ItemStack center, Circle circle, boolean clientSide) {
        if (center.isEmpty() || plan(center, circle, clientSide) != null
                || RuneHolder.of(center) != null && circle.filledPedestals().isEmpty() && !circle.chosenRunes().isEmpty()) {
            return Hint.NONE;
        }
        List<PedestalBlockEntity> filled = circle.filledPedestals();
        Hint best = Hint.NONE;
        int bestScore = Integer.MAX_VALUE;
        for (Map.Entry<Identifier, InfusionRecipe> entry : recipes(clientSide)) {
            InfusionRecipe recipe = entry.getValue();
            if (!recipe.center().test(center)) {
                continue;
            }
            List<Ingredient> needed = recipe.pedestals();
            int[] owner = assign(needed, filled);
            boolean[] used = new boolean[filled.size()];
            boolean[] met = new boolean[needed.size()];
            for (int p = 0; p < owner.length; p++) {
                if (owner[p] >= 0) {
                    used[p] = true;
                    met[owner[p]] = true;
                }
            }
            List<ItemStack> missing = new ArrayList<>();
            for (int i = 0; i < needed.size(); i++) {
                if (!met[i]) {
                    missing.add(needed.get(i).items().findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY));
                }
            }
            List<BlockPos> extra = new ArrayList<>();
            for (int p = 0; p < filled.size(); p++) {
                if (!used[p]) {
                    extra.add(filled.get(p).getBlockPos());
                }
            }
            List<ComplexRune> runes = recipe.runes().stream().filter(rune -> circle.stars(rune) == 0).toList();
            int score = missing.size() + extra.size() + runes.size();
            if (score < bestScore) {
                bestScore = score;
                best = new Hint(missing, extra, runes);
            }
        }
        return best;
    }

    /**
     * Matches every ingredient to its own filled pedestal and every filled pedestal to an ingredient (nothing extra may
     * lie around), or null.
     */
    @Nullable
    static List<Input> match(List<Ingredient> needed, List<PedestalBlockEntity> filled) {
        if (needed.size() != filled.size()) {
            return null;
        }
        // Bipartite matching (augmenting paths): fast even for many pedestals holding interchangeable items.
        int n = needed.size();
        int[] owner = new int[n];
        java.util.Arrays.fill(owner, -1);
        boolean[][] fits = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int p = 0; p < n; p++) {
                fits[i][p] = needed.get(i).test(filled.get(p).item());
            }
        }
        for (int i = 0; i < n; i++) {
            if (!augment(i, fits, owner, new boolean[n])) {
                return null;
            }
        }
        // Draw in circle order, so items leave the pedestals one after another around the ring.
        List<Input> inputs = new ArrayList<>();
        for (PedestalBlockEntity pedestal : filled) {
            inputs.add(new Input(pedestal.getBlockPos(), BuiltInRegistries.ITEM.getKey(pedestal.item().getItem())));
        }
        return inputs;
    }

    /** The largest matching of ingredients to filled pedestals: for each pedestal the ingredient it serves, or -1. */
    private static int[] assign(List<Ingredient> needed, List<PedestalBlockEntity> filled) {
        int[] owner = new int[filled.size()];
        java.util.Arrays.fill(owner, -1);
        boolean[][] fits = new boolean[needed.size()][filled.size()];
        for (int i = 0; i < needed.size(); i++) {
            for (int p = 0; p < filled.size(); p++) {
                fits[i][p] = needed.get(i).test(filled.get(p).item());
            }
        }
        for (int i = 0; i < needed.size(); i++) {
            augment(i, fits, owner, new boolean[filled.size()]);
        }
        return owner;
    }

    private static boolean augment(int ingredient, boolean[][] fits, int[] owner, boolean[] seen) {
        for (int p = 0; p < owner.length; p++) {
            if (fits[ingredient][p] && !seen[p]) {
                seen[p] = true;
                if (owner[p] < 0 || augment(owner[p], fits, owner, seen)) {
                    owner[p] = ingredient;
                    return true;
                }
            }
        }
        return false;
    }
}
