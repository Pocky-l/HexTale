package com.pockyl.hextale.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.PedestalBlockEntity;
import com.pockyl.hextale.block.RuneSteleBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechTools;
import com.pockyl.hextale.runework.RuneSlots;
import com.pockyl.hextale.runework.ToolRunes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * The ritual circle around a Hextech Altar and what it would do now. Pure planning: nothing in the world changes here.
 */
public final class Ritual {
    public static final int PEDESTAL_RADIUS = 4;
    public static final int STELE_RADIUS = 5;
    public static final int TANK_RADIUS = 8;
    public static final int MAX_PEDESTALS = 16;
    public static final int WARMUP_TICKS = 40;
    public static final int TICKS_PER_ITEM = 20;
    public static final int FINALE_TICKS = 24;
    /** mB of each essence the altar draws per tick while infusing, and while charging a tool. */
    public static final int DRAW_PER_TICK = 4;
    public static final int CHARGE_PER_TICK = 2;
    /** A ritual short of something waits this long, then collapses. */
    public static final int STALL_LIMIT = 600;

    private Ritual() {
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

        /** The runes chosen on the steles, in stele order, each once. */
        public List<RuneSlots.Engraving> chosenRunes() {
            List<RuneSlots.Engraving> runes = new ArrayList<>();
            Set<ComplexRune> seen = new LinkedHashSet<>();
            for (RuneSteleBlockEntity stele : steles) {
                if (stele.rune() != null && seen.add(stele.rune())) {
                    runes.add(new RuneSlots.Engraving(0, stele.rune(), stele.stars()));
                }
            }
            return runes;
        }

        public List<PedestalBlockEntity> filledPedestals() {
            return pedestals.stream().filter(pedestal -> !pedestal.item().isEmpty()).toList();
        }
    }

    /** Angle of {@code pos} around {@code centre}, 0 at north, growing clockwise (seen from above). */
    static double clockwise(BlockPos centre, BlockPos pos) {
        double angle = Mth.atan2(pos.getX() - centre.getX(), centre.getZ() - pos.getZ());
        return angle < 0 ? angle + Math.PI * 2 : angle;
    }

    public enum Kind {
        INFUSE, ENGRAVE, ERASE, CHARGE
    }

    /**
     * What a ritual will do.
     *
     * @param recipe     the infusion recipe's id (infusions only)
     * @param inputs     the pedestals to draw from, in drawing order, with the item each must hold
     * @param essences   what it draws from the tanks
     * @param engravings runes engraved into the tool, slots assigned
     * @param result     what the centre item becomes
     */
    public record Plan(Kind kind, @Nullable Identifier recipe, List<Input> inputs, EssenceMix essences, List<RuneSlots.Engraving> engravings,
                       ItemStack result) {
        public int duration() {
            int draw = DRAW_PER_TICK;
            if (kind == Kind.CHARGE) {
                draw = CHARGE_PER_TICK;
            }
            int longest = 0;
            for (Essence essence : Essence.values()) {
                longest = Math.max(longest, Mth.ceil(essences.get(essence) / (float) draw));
            }
            if (kind == Kind.CHARGE) {
                return longest;
            }
            return WARMUP_TICKS + Math.max(inputs.size() * TICKS_PER_ITEM, longest) + FINALE_TICKS;
        }
    }

    /** One pedestal the ritual draws from and the item it must still hold then. */
    public record Input(BlockPos pos, Identifier item) {
        public boolean holds(ItemStack stack) {
            return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(item);
        }
    }

    /** What the altar would do with this centre item and circle, or null (nothing fits). */
    @Nullable
    public static Plan plan(ItemStack center, Circle circle, boolean clientSide) {
        if (center.isEmpty()) {
            return null;
        }
        List<PedestalBlockEntity> filled = circle.filledPedestals();
        List<ComplexRune> chosen = circle.chosenRunes().stream().map(RuneSlots.Engraving::rune).toList();
        for (Map.Entry<Identifier, InfusionRecipe> entry : InfusionRecipes.get(clientSide).entrySet()) {
            InfusionRecipe recipe = entry.getValue();
            if (recipe.kind() != InfusionRecipe.Kind.INFUSE || !recipe.center().orElseThrow().test(center)
                    || !chosen.containsAll(recipe.runes())) {
                continue;
            }
            List<Input> inputs = match(recipe.pedestals(), filled);
            if (inputs != null) {
                return new Plan(Kind.INFUSE, entry.getKey(), inputs, recipe.essences(), List.of(), recipe.resultFor(center));
            }
        }
        if (!HextechTools.isTool(center)) {
            return null;
        }
        RuneSlots runes = HextechTools.runes(center);
        Plan engraving = engravingPlan(center, runes, circle, filled, clientSide);
        if (engraving != null) {
            return engraving;
        }
        InfusionRecipe erase = InfusionRecipes.erasing(clientSide);
        if (erase != null && !runes.isEmpty() && !filled.isEmpty()) {
            List<Input> inputs = match(erase.pedestals(), filled);
            if (inputs != null) {
                ItemStack result = center.copy();
                HextechTools.setRunes(result, RuneSlots.EMPTY);
                return new Plan(Kind.ERASE, null, inputs, erase.essences(), List.of(), result);
            }
        }
        if (filled.isEmpty() && !runes.isEmpty() && HextechTools.space(center) > 0) {
            EssenceMix need = chargeNeed(center, runes);
            if (!need.isEmpty()) {
                ItemStack result = center.copy();
                HextechTools.fill(result, need);
                return new Plan(Kind.CHARGE, null, List.of(), need, List.of(), result);
            }
        }
        return null;
    }

    /** Runes on the steles that the tool lacks go into its free slots, in stele order, paid by their engraving entries. */
    @Nullable
    private static Plan engravingPlan(ItemStack center, RuneSlots runes, Circle circle, List<PedestalBlockEntity> filled, boolean clientSide) {
        List<RuneSlots.Engraving> added = new ArrayList<>();
        List<Ingredient> needed = new ArrayList<>();
        EssenceMix essences = EssenceMix.EMPTY;
        RuneSlots after = runes;
        for (RuneSlots.Engraving chosen : circle.chosenRunes()) {
            ComplexRune rune = chosen.rune();
            InfusionRecipe entry = InfusionRecipes.engraving(clientSide, rune);
            if (entry == null || !ToolRunes.worksOnTools(rune) || after.has(rune)) {
                continue;
            }
            int slot = freeSlot(after);
            if (slot < 0) {
                break;
            }
            RuneSlots.Engraving engraving = new RuneSlots.Engraving(slot, rune, chosen.stars());
            after = after.with(engraving);
            added.add(engraving);
            needed.addAll(entry.pedestals());
            essences = essences.plus(entry.essences());
        }
        if (added.isEmpty()) {
            return null;
        }
        List<Input> inputs = match(needed, filled);
        if (inputs == null) {
            return null;
        }
        ItemStack result = center.copy();
        HextechTools.setRunes(result, after);
        return new Plan(Kind.ENGRAVE, null, inputs, essences, added, result);
    }

    private static int freeSlot(RuneSlots runes) {
        for (int slot = 0; slot < HextechTools.SLOTS; slot++) {
            if (runes.get(slot) == null) {
                return slot;
            }
        }
        return -1;
    }

    /** The cell's free space split evenly between the action essences of the tool's runes. */
    public static EssenceMix chargeNeed(ItemStack tool, RuneSlots runes) {
        Map<Essence, Integer> want = new EnumMap<>(Essence.class);
        List<Essence> actions = runes.engravings().stream().map(engraving -> engraving.rune().action()).distinct().toList();
        if (actions.isEmpty()) {
            return EssenceMix.EMPTY;
        }
        EssenceMix cell = HextechTools.cell(tool);
        int share = HextechTools.CELL / actions.size();
        int space = HextechTools.space(tool);
        for (Essence essence : actions) {
            int missing = Math.max(0, share - cell.get(essence));
            int take = Math.min(missing, space);
            if (take > 0) {
                want.put(essence, take);
                space -= take;
            }
        }
        return EssenceMix.of(want);
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
        int[] assigned = new int[needed.size()];
        boolean[] used = new boolean[filled.size()];
        if (!assign(needed, filled, 0, assigned, used)) {
            return null;
        }
        // Draw in circle order, so items leave the pedestals one after another around the ring.
        List<Input> inputs = new ArrayList<>();
        for (int p = 0; p < filled.size(); p++) {
            PedestalBlockEntity pedestal = filled.get(p);
            inputs.add(new Input(pedestal.getBlockPos(), BuiltInRegistries.ITEM.getKey(pedestal.item().getItem())));
        }
        return inputs;
    }

    private static boolean assign(List<Ingredient> needed, List<PedestalBlockEntity> filled, int index, int[] assigned, boolean[] used) {
        if (index == needed.size()) {
            return true;
        }
        for (int p = 0; p < filled.size(); p++) {
            if (!used[p] && needed.get(index).test(filled.get(p).item())) {
                used[p] = true;
                assigned[index] = p;
                if (assign(needed, filled, index + 1, assigned, used)) {
                    return true;
                }
                used[p] = false;
            }
        }
        return false;
    }
}
