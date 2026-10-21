package com.pockyl.hextale.runework;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * Things that take runes like enchantments at the Hextech Altar: what each one takes ({@link HolderRune}s), how many,
 * which runes exclude each other and what burning one in costs. The runes live on the item as a {@link PickaxeRunes}
 * component; a placed device's block entity keeps them while it stands. Every device and tool of the mod is meant to be
 * one (see the mod's rune direction).
 */
public enum RuneHolder {
    PICKAXE(() -> ModDataComponents.PICKAXE_RUNES.get(), PickaxeRunes.SLOTS, PickaxeRune.values()),
    /** The hex pylons share one set of runes. */
    PYLON(() -> ModDataComponents.PYLON_RUNES.get(), PylonRune.SLOTS, PylonRune.values()),
    /** The Whirl Reaper. */
    REAPER(() -> ModDataComponents.REAPER_RUNES.get(), ReaperRune.SLOTS, ReaperRune.values()),
    /** The Hex Sawmill. */
    SAWMILL(() -> ModDataComponents.SAWMILL_RUNES.get(), SawmillRune.SLOTS, SawmillRune.values()),
    /** The Hex Furnace. */
    FURNACE(() -> ModDataComponents.FURNACE_RUNES.get(), FurnaceRune.SLOTS, FurnaceRune.values()),
    /** The Chem Burner. */
    BURNER(() -> ModDataComponents.BURNER_RUNES.get(), BurnerRune.SLOTS, BurnerRune.values()),
    /** The Axolotl Tank. */
    TANK(() -> ModDataComponents.TANK_RUNES.get(), TankRune.SLOTS, TankRune.values()),
    /** The Chem Still. */
    STILL(() -> ModDataComponents.STILL_RUNES.get(), StillRune.SLOTS, StillRune.values()),
    /** The Lab Bench. */
    LAB(() -> ModDataComponents.LAB_RUNES.get(), LabRune.SLOTS, LabRune.values()),
    /** The Pink Brazier. */
    BRAZIER(() -> ModDataComponents.BRAZIER_RUNES.get(), BrazierRune.SLOTS, BrazierRune.values()),
    /** The Hextech Skypack. */
    SKYPACK(() -> ModDataComponents.SKYPACK_RUNES.get(), SkypackRune.SLOTS, SkypackRune.values()),
    /** The hextech axe, shovel and sword keep their runes where the pickaxe does. */
    AXE(() -> ModDataComponents.PICKAXE_RUNES.get(), PickaxeRunes.SLOTS, tool(ToolRune.Tool.AXE)),
    SHOVEL(() -> ModDataComponents.PICKAXE_RUNES.get(), PickaxeRunes.SLOTS, tool(ToolRune.Tool.SHOVEL)),
    SWORD(() -> ModDataComponents.PICKAXE_RUNES.get(), PickaxeRunes.SLOTS, tool(ToolRune.Tool.SWORD));

    private final Supplier<DataComponentType<PickaxeRunes>> component;
    private final int slots;
    private final List<HolderRune> runes;

    RuneHolder(Supplier<DataComponentType<PickaxeRunes>> component, int slots, HolderRune[] runes) {
        this.component = component;
        this.slots = slots;
        this.runes = List.of(runes);
    }

    private static HolderRune[] tool(ToolRune.Tool tool) {
        return Arrays.stream(ToolRune.values()).filter(rune -> rune.on(tool)).toArray(HolderRune[]::new);
    }

    @Nullable
    public static RuneHolder of(ItemStack stack) {
        if (HextechPickaxeItem.is(stack)) {
            return PICKAXE;
        }
        Item item = stack.getItem();
        if (item == ModItems.WHIRL_REAPER.get()) {
            return REAPER;
        }
        if (item == ModItems.HEX_SAWMILL.get()) {
            return SAWMILL;
        }
        if (item == ModItems.HEX_FURNACE.get()) {
            return FURNACE;
        }
        if (item == ModItems.CHEM_BURNER.get()) {
            return BURNER;
        }
        if (item == ModItems.AXOLOTL_TANK.get()) {
            return TANK;
        }
        if (item == ModItems.CHEM_STILL.get()) {
            return STILL;
        }
        if (item == ModItems.PINK_BRAZIER.get()) {
            return BRAZIER;
        }
        if (item == ModItems.LAB_BENCH.get()) {
            return LAB;
        }
        if (item == ModItems.HEXTECH_SKYPACK.get()) {
            return SKYPACK;
        }
        if (item == ModItems.HEXTECH_AXE.get()) {
            return AXE;
        }
        if (item == ModItems.HEXTECH_SHOVEL.get()) {
            return SHOVEL;
        }
        if (item == ModItems.HEXTECH_SWORD.get()) {
            return SWORD;
        }
        return ModItems.isPylon(stack) ? PYLON : null;
    }

    public DataComponentType<PickaxeRunes> component() {
        return component.get();
    }

    /** Whether it is a tool held in the hand (it lists its runes in its own tooltip and loses them at a grindstone). */
    public boolean handTool() {
        return this == PICKAXE || this == AXE || this == SHOVEL || this == SWORD;
    }

    public PickaxeRunes runes(ItemStack stack) {
        return stack.getOrDefault(component(), PickaxeRunes.EMPTY);
    }

    public void set(ItemStack stack, PickaxeRunes runes) {
        if (runes.isEmpty()) {
            stack.remove(component());
        } else {
            stack.set(component(), runes);
        }
    }

    public int slots() {
        return slots;
    }

    /** Every rune it takes, in the order the book and JEI show them. */
    public List<HolderRune> holderRunes() {
        return runes;
    }

    public List<ComplexRune> takes() {
        return runes.stream().map(HolderRune::rune).toList();
    }

    @Nullable
    public HolderRune find(ComplexRune rune) {
        for (HolderRune value : runes) {
            if (value.rune() == rune) {
                return value;
            }
        }
        return null;
    }

    public boolean takes(ComplexRune rune) {
        return find(rune) != null;
    }

    private HolderRune get(ComplexRune rune) {
        return Objects.requireNonNull(find(rune), () -> rune.key() + " on " + this);
    }

    /** Whether the two runes cannot share one holder (only pickaxe runes clash). */
    public boolean clashes(ComplexRune a, ComplexRune b) {
        if (this != PICKAXE) {
            return false;
        }
        PickaxeRune x = PickaxeRune.of(a);
        PickaxeRune y = PickaxeRune.of(b);
        return x != null && y != null && x.clashes(y);
    }

    /** Essence for burning the rune in with these stars. */
    public EssenceMix cost(ComplexRune rune, int stars) {
        return get(rune).cost(stars);
    }

    /** What the rune does here, one short line. */
    public String descriptionKey(ComplexRune rune) {
        return get(rune).descriptionKey();
    }

    /** The line with the rune's number at a strength ({@code %s} = {@link #amount}). */
    public String valueKey(ComplexRune rune) {
        return get(rune).valueKey();
    }

    public String amount(ComplexRune rune, int strength) {
        return get(rune).amountText(strength);
    }

    /** What it looks like on the altar and in JEI: every item of the kind. */
    public List<ItemStack> items() {
        return switch (this) {
            case PICKAXE -> ModItems.PICKAXES.values().stream().map(item -> new ItemStack(item.get())).toList();
            case PYLON -> List.of(new ItemStack(ModItems.GATHERING_PYLON.get()), new ItemStack(ModItems.SENDING_PYLON.get()),
                    new ItemStack(ModItems.RELAY_PYLON.get()), new ItemStack(ModItems.SIPHON_PYLON.get()),
                    new ItemStack(ModItems.SORTING_MATRIX.get()));
            case REAPER -> List.of(new ItemStack(ModItems.WHIRL_REAPER.get()));
            case SAWMILL -> List.of(new ItemStack(ModItems.HEX_SAWMILL.get()));
            case FURNACE -> List.of(new ItemStack(ModItems.HEX_FURNACE.get()));
            case BURNER -> List.of(new ItemStack(ModItems.CHEM_BURNER.get()));
            case TANK -> List.of(new ItemStack(ModItems.AXOLOTL_TANK.get()));
            case STILL -> List.of(new ItemStack(ModItems.CHEM_STILL.get()));
            case BRAZIER -> List.of(new ItemStack(ModItems.PINK_BRAZIER.get()));
            case LAB -> List.of(new ItemStack(ModItems.LAB_BENCH.get()));
            case SKYPACK -> List.of(new ItemStack(ModItems.HEXTECH_SKYPACK.get()));
            case AXE -> List.of(new ItemStack(ModItems.HEXTECH_AXE.get()));
            case SHOVEL -> List.of(new ItemStack(ModItems.HEXTECH_SHOVEL.get()));
            case SWORD -> List.of(new ItemStack(ModItems.HEXTECH_SWORD.get()));
        };
    }

    /** Lower-case name used in translation keys and guide displays. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static RuneHolder byId(String id) {
        for (RuneHolder holder : values()) {
            if (holder.id().equals(id)) {
                return holder;
            }
        }
        return null;
    }

    /** The note under the runes in the book: "up to %s runes ...". */
    public String bookNoteKey() {
        return "book.hextale.quest." + id() + "_rune.note";
    }

    /** The note under a rune in JEI; the pickaxe keeps the old key. */
    public String jeiNoteKey() {
        return this == PICKAXE ? "jei.hextale.rune_burning.note" : "jei.hextale.rune_burning.note_" + id();
    }

    /** The altar's refusal when a stele's rune does not go on this holder. */
    public String notTakenRefusal() {
        return this == PICKAXE ? "not_pickaxe" : "not_" + id();
    }

    /** Suffix of the altar's refusal keys: the pickaxe keeps the old keys. */
    public String refusalSuffix() {
        return this == PICKAXE ? "" : "_" + id();
    }
}
