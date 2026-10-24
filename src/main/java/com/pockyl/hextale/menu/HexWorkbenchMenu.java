package com.pockyl.hextale.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import com.pockyl.hextale.block.HexWorkbenchBlockEntity;
import com.pockyl.hextale.crafting.HexCraftingRecipe;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModMenus;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The Hex Workbench: the 3x3 grid, the crystal socket and the result. It makes the hextech recipes and everything an
 * ordinary crafting table makes (those cost no charge). The result shows only what this researcher may make
 * (researched topics) and the bench can pay for; taking it spends the grid and the crystal's charge. Data: 0 = charge
 * left, 1 = charge the shown recipe costs (0 when none), 2 = why there is no result (see {@link Why}).
 */
public class HexWorkbenchMenu extends AbstractContainerMenu {
    public static final int GRID_X = 48;
    public static final int GRID_Y = 18;
    public static final int CRYSTAL_X = 14;
    public static final int CRYSTAL_Y = 68;
    public static final int RESULT_X = 132;
    public static final int RESULT_Y = 36;
    public static final int INVENTORY_Y = MachineMenu.INVENTORY_Y;
    private static final int RESULT_SLOT = HexWorkbenchBlockEntity.SIZE;
    private static final int INVENTORY_START = RESULT_SLOT + 1;
    private static final int INVENTORY_END = INVENTORY_START + 36;

    /** Why the result slot is empty while the grid holds a recipe. */
    public enum Why {
        NONE, LOCKED, NO_CHARGE
    }

    private final Container bench;
    private final ResultContainer result = new ResultContainer();
    private final ContainerData data;
    private final BlockPos pos;
    private final Player player;
    private int seenRevision = -1;

    public HexWorkbenchMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainer(HexWorkbenchBlockEntity.SIZE), new SimpleContainerData(3), buf.readBlockPos());
    }

    public HexWorkbenchMenu(int containerId, Inventory inventory, HexWorkbenchBlockEntity bench, BlockPos pos) {
        this(containerId, inventory, bench, new SimpleContainerData(3), pos);
    }

    private HexWorkbenchMenu(int containerId, Inventory inventory, Container bench, ContainerData data, BlockPos pos) {
        super(ModMenus.HEX_WORKBENCH.get(), containerId);
        this.bench = bench;
        this.data = data;
        this.pos = pos;
        this.player = inventory.player;
        checkContainerSize(bench, HexWorkbenchBlockEntity.SIZE);
        bench.startOpen(player);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(bench, col + row * 3, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        addSlot(new Slot(bench, HexWorkbenchBlockEntity.CRYSTAL, CRYSTAL_X, CRYSTAL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return HexWorkbenchBlockEntity.chargeOf(stack) > 0;
            }
        });
        addSlot(new Slot(result, 0, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player taker, ItemStack stack) {
                craft(taker);
                super.onTake(taker, stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, MachineMenu.INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, MachineMenu.INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public int charge() {
        return data.get(0);
    }

    public int cost() {
        return data.get(1);
    }

    public Why why() {
        return Why.values()[Math.floorMod(data.get(2), Why.values().length)];
    }

    // ---- Result -------------------------------------------------------------------------------------------------------

    @Override
    public void broadcastChanges() {
        if (bench instanceof HexWorkbenchBlockEntity entity && entity.revision() != seenRevision) {
            seenRevision = entity.revision();
            refresh(entity);
        }
        super.broadcastChanges();
    }

    /** Finds the recipe in the grid and shows its result if this researcher may make it and the bench can pay. */
    private void refresh(HexWorkbenchBlockEntity entity) {
        data.set(0, entity.charge());
        Map.Entry<ResourceLocation, HexCraftingRecipe> found = HexCraftingRecipe.find(entity.input().input(), false);
        ItemStack shown = ItemStack.EMPTY;
        Why why = Why.NONE;
        int cost = 0;
        if (found != null) {
            HexCraftingRecipe recipe = found.getValue();
            cost = recipe.charge();
            ItemStack made = recipe.result().create();
            if (!ResearchTopics.mayMake(player, made, Research.of(player))) {
                why = Why.LOCKED;
            } else if (entity.chargeAvailable() < cost && !player.getAbilities().instabuild) {
                why = Why.NO_CHARGE;
            } else {
                shown = made;
            }
        } else if (player.level() instanceof ServerLevel level) {
            // Everything an ordinary crafting table makes, the bench makes too, free of charge.
            CraftingInput input = entity.input().input();
            RecipeHolder<CraftingRecipe> plain = plainRecipe(level, input);
            if (plain != null) {
                ItemStack made = plain.value().assemble(input);
                if (!ResearchTopics.mayMake(player, made, Research.of(player))) {
                    why = Why.LOCKED;
                } else {
                    shown = made;
                }
            }
        }
        data.set(1, cost);
        data.set(2, why.ordinal());
        result.setItem(0, shown);
    }

    /** The result was taken: the grid gives one of everything (remainders stay or go to the player), the crystal pays. */
    private void craft(Player taker) {
        if (!(bench instanceof HexWorkbenchBlockEntity entity) || !(taker.level() instanceof ServerLevel level)) {
            return;
        }
        CraftingInput.Positioned positioned = entity.input();
        CraftingInput input = positioned.input();
        Map.Entry<ResourceLocation, HexCraftingRecipe> found = HexCraftingRecipe.find(input, false);
        if (found == null) {
            RecipeHolder<CraftingRecipe> plain = plainRecipe(level, input);
            if (plain != null) {
                craftPlain(taker, entity, positioned, plain.value().getRemainingItems(input));
                entity.crafted(level);
                seenRevision = -1;
                broadcastChanges();
            }
            return;
        }
        if (!taker.getAbilities().instabuild) {
            entity.spend(found.getValue().charge());
        }
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                int slot = x + positioned.left() + (y + positioned.top()) * 3;
                ItemStack here = entity.getItem(slot);
                if (here.isEmpty()) {
                    continue;
                }
                ItemStackTemplate remainder = here.getCraftingRemainder();
                entity.removeItem(slot, 1);
                if (remainder != null) {
                    ItemStack rest = remainder.create();
                    if (entity.getItem(slot).isEmpty()) {
                        entity.setItem(slot, rest);
                    } else if (!taker.getInventory().add(rest)) {
                        taker.getInventory().placeItemBackInInventory(rest, net.minecraft.util.Prediction.SERVER_ONLY);
                    }
                }
            }
        }
        entity.crafted(level);
        seenRevision = -1;
        broadcastChanges();
    }

    /** The ordinary crafting recipe the grid holds (when no hextech recipe does), or null. */
    @Nullable
    private static RecipeHolder<CraftingRecipe> plainRecipe(ServerLevel level, CraftingInput input) {
        return input.isEmpty() ? null : level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
    }

    /** An ordinary craft: one of everything in the grid is used, the recipe's remainders stay in place or go to the taker. */
    private static void craftPlain(Player taker, HexWorkbenchBlockEntity entity, CraftingInput.Positioned positioned,
                                   java.util.List<ItemStack> remaining) {
        CraftingInput input = positioned.input();
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                int slot = x + positioned.left() + (y + positioned.top()) * 3;
                if (!entity.getItem(slot).isEmpty()) {
                    entity.removeItem(slot, 1);
                }
                ItemStack rest = remaining.get(x + y * input.width());
                if (rest.isEmpty()) {
                    continue;
                }
                if (entity.getItem(slot).isEmpty()) {
                    entity.setItem(slot, rest);
                } else if (!taker.getInventory().add(rest)) {
                    taker.getInventory().placeItemBackInInventory(rest, net.minecraft.util.Prediction.SERVER_ONLY);
                }
            }
        }
    }

    // ---- Moving items -------------------------------------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player taker, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == RESULT_SLOT) {
            // Whole results only: a half-moved result would be lost when the grid is spent.
            if (!fits(taker.getInventory(), stack)) {
                return ItemStack.EMPTY;
            }
            moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true);
            slot.onQuickCraft(stack, original);
        } else if (index < RESULT_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (HexWorkbenchBlockEntity.chargeOf(stack) > 0 && moveItemStackTo(stack, HexWorkbenchBlockEntity.CRYSTAL,
                HexWorkbenchBlockEntity.CRYSTAL + 1, false)) {
            // A crystal goes to the socket first.
        } else if (!moveItemStackTo(stack, 0, HexWorkbenchBlockEntity.GRID, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(taker, stack);
        return original;
    }

    private static boolean fits(Inventory inventory, ItemStack stack) {
        int room = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack here = inventory.getItem(i);
            if (here.isEmpty()) {
                room += stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(here, stack)) {
                room += here.getMaxStackSize() - here.getCount();
            }
            if (room >= stack.getCount()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(carried, slot);
    }

    @Override
    public boolean stillValid(Player taker) {
        return bench.stillValid(taker);
    }

    @Override
    public void removed(Player taker) {
        super.removed(taker);
        bench.stopOpen(taker);
    }

    /** The overlay line for a result that does not show, or null. */
    public static @Nullable Component whyText(Why why, @Nullable ResearchTopic topic) {
        return switch (why) {
            case LOCKED -> topic == null ? null : Component.translatable("research.hextale.locked",
                    Component.translatable(topic.translationKey()).withColor(topic.color()));
            case NO_CHARGE -> Component.translatable("gui.hextale.hex_workbench.no_charge");
            case NONE -> null;
        };
    }
}
