package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import com.pockyl.hextale.port.ResourceHandler;
import com.pockyl.hextale.port.ItemResource;
import com.pockyl.hextale.port.Transaction;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.HexPylonBlockEntity;
import com.pockyl.hextale.block.HexTerminalBlock;
import com.pockyl.hextale.block.HexTerminalBlockEntity;
import com.pockyl.hextale.block.HexVaultBlock;
import com.pockyl.hextale.block.HexVaultBlockEntity;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.multiblock.MultiblockPattern;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.multiblock.Multiblocks;
import com.pockyl.hextale.block.ShapePartBlock;
import com.pockyl.hextale.block.ShapeParts;
import com.pockyl.hextale.block.StorageCubeBlock;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.item.RiftLinkCrystalItem;
import com.pockyl.hextale.menu.VaultMenu;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.VaultRune;
import com.pockyl.hextale.storage.VaultContents;
import com.pockyl.hextale.storage.VaultLink;
import com.pockyl.hextale.storage.VaultMemory;
import com.pockyl.hextale.storage.VaultRules;
import com.pockyl.hextale.storage.VaultStore;
import com.pockyl.hextale.storage.VaultToll;

import java.util.List;
import java.util.UUID;

/**
 * The Hex Vault, its cubes and terminals. What matters most here is that items are neither made nor lost: every way in and
 * out, a full vault, a broken one, two players at one screen.
 */
public final class VaultGameTests {
    private static final BlockPos AT = new BlockPos(9, 1, 9);
    /** Where the vault stands in a built gate: on its platform, with the ground of the test under that. */
    private static final BlockPos BUILT = new BlockPos(9, 3, 9);
    private static final int ROOM = 10;

    private VaultGameTests() {
    }

    /** A working vault without the gate round it, for what only cares about the store. */
    private static HexVaultBlockEntity vault(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, formedVault());
        HexVaultBlockEntity vault = helper.<HexVaultBlockEntity>getBlockEntity(at);
        vault.onLoad();
        return vault;
    }

    private static net.minecraft.world.level.block.state.BlockState formedVault() {
        return ModBlocks.HEX_VAULT.get().defaultBlockState().setValue(HexVaultBlock.FORMED, true);
    }

    /** A gate built for real round {@code at}, the vault between its pillars, and woken with no player there. */
    private static HexVaultBlockEntity gate(GameTestHelper helper, BlockPos at) {
        return gate(helper, at, Rotation.NONE, null);
    }

    private static HexVaultBlockEntity gate(GameTestHelper helper, BlockPos at, Rotation rotation, @org.jspecify.annotations.Nullable ServerPlayer player) {
        var gate = Multiblocks.VAULT_GATE;
        var heart = gate.pattern().controller();
        BlockPos middle = helper.absolutePos(at);
        BlockPos origin = middle.subtract(new BlockPos(heart.x(), heart.y(), heart.z()).rotate(rotation));
        var placement = new MultiblockPattern.Placement(origin, rotation);
        for (MultiblockPattern.Cell cell : gate.pattern().cells()) {
            if (cell.symbol() != '.') {
                ItemStack shown = cell.key().shown().get().getFirst();
                helper.getLevel().setBlockAndUpdate(placement.at(cell.x(), cell.y(), cell.z()),
                        ((net.minecraft.world.item.BlockItem) shown.getItem()).getBlock().defaultBlockState());
            }
        }
        HexVaultBlockEntity vault = helper.<HexVaultBlockEntity>getBlockEntity(at);
        vault.onLoad();
        helper.assertTrue(gate.pattern().matches(helper.getLevel(), placement), "the gate is what the pattern asks for");
        Multiblocks.form(helper.getLevel(), gate, placement, player);
        vault.recount();
        return vault;
    }

    private static ServerPlayer player(GameTestHelper helper, BlockPos near) {
        ServerPlayer player = VoidLifeGameTests.survivor(helper);
        Vec3 at = helper.absoluteVec(Vec3.atCenterOf(near)).add(2, 0, 0);
        player.moveTo(at.x, at.y, at.z, 0, 0);
        return player;
    }

    private static VaultMenu menu(ServerPlayer player, HexVaultBlockEntity vault) {
        return new VaultMenu(1, player.getInventory(), vault.contents(), vault.id(), VaultMenu.Kind.VAULT, vault.getBlockPos(), InteractionHand.MAIN_HAND);
    }

    private static VaultMenu terminalMenu(ServerPlayer player, HexVaultBlockEntity vault, BlockPos terminal) {
        return new VaultMenu(1, player.getInventory(), vault.contents(), vault.id(), VaultMenu.Kind.TERMINAL, terminal, InteractionHand.MAIN_HAND);
    }

    private static HexTerminalBlockEntity terminal(GameTestHelper helper, BlockPos at, HexVaultBlockEntity vault) {
        helper.setBlock(at, ModBlocks.HEX_TERMINAL.get());
        HexTerminalBlockEntity terminal = helper.<HexTerminalBlockEntity>getBlockEntity(at);
        terminal.tie(vault.id());
        return terminal;
    }

    private static int carriedIn(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getInventory().countItem(item);
    }

    private static List<ItemEntity> drops(GameTestHelper helper, BlockPos at) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(at)).inflate(3));
    }

    /** An item weighs its essence points, at least 1; one capacity counts them all. */
    @HexTest(padding = ROOM)
    public static void itemsWeighWhatTheyAreMadeOf(GameTestHelper helper) {
        VaultContents contents = vault(helper, AT).contents();
        ItemResource iron = ItemResource.of(Items.IRON_INGOT);
        int weight = VaultRules.weight(iron);
        helper.assertTrue(weight > 1, "iron carries points");
        helper.assertTrue(VaultRules.weight(ItemResource.of(Items.BARRIER)) >= 1, "whatever carries nothing still weighs 1");
        ItemStack stack = new ItemStack(Items.IRON_INGOT, 10);
        helper.assertTrue(contents.store(stack) == 10 && stack.isEmpty(), "the stack went in whole");
        helper.assertTrue(contents.used() == 10L * weight, "used points are the weights added up");
        ItemStack back = contents.take(iron, 4);
        helper.assertTrue(back.getCount() == 4 && back.is(Items.IRON_INGOT), "takes what was asked");
        helper.assertTrue(contents.count(iron) == 6 && contents.used() == 6L * weight, "and exactly that is gone");
        helper.assertTrue(contents.take(iron, 100).getCount() == 6 && contents.kinds() == 0 && contents.used() == 0, "the rest comes out, nothing is left over");
        helper.succeed();
    }

    /** A full vault refuses what does not fit and keeps the rest in the hand. */
    @HexTest(padding = ROOM)
    public static void aFullVaultRefusesAndLosesNothing(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        VaultContents contents = vault.contents();
        int weight = VaultRules.weight(ItemResource.of(Items.DIAMOND));
        contents.setPerks(weight * 5L, 0, 0);
        ItemStack stack = new ItemStack(Items.DIAMOND, 8);
        helper.assertTrue(contents.store(stack) == 5, "five fit");
        helper.assertTrue(stack.getCount() == 3, "three stay in the stack");
        helper.assertTrue(contents.store(stack) == 0 && stack.getCount() == 3, "a full vault takes none");
        helper.assertTrue(contents.refusal(ItemResource.of(Items.DIAMOND)) == VaultContents.Refusal.FULL, "and says why");
        helper.assertTrue(contents.count(ItemResource.of(Items.DIAMOND)) == 5, "nothing was made or lost");
        helper.succeed();
    }

    /** Items with data are their own entries; what holds items stays out, a vault with a store too; entries have a limit. */
    @HexTest(padding = ROOM, maxTicks = 200)
    public static void itemsWithDataAndContainers(GameTestHelper helper) {
        VaultContents contents = vault(helper, AT).contents();
        ItemStack named = new ItemStack(Items.STICK, 3);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("a"));
        contents.store(new ItemStack(Items.STICK, 2));
        contents.store(named.copy());
        helper.assertTrue(contents.kinds() == 2, "a named stick is an entry of its own");
        helper.assertTrue(contents.take(ItemResource.of(named), 64).getCount() == 3 && contents.count(ItemResource.of(Items.STICK)) == 2, "and comes back as it was");

        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));
        helper.assertTrue(contents.store(box) == 0 && !box.isEmpty(), "a full shulker box stays out");
        helper.assertTrue(contents.store(new ItemStack(Items.SHULKER_BOX)) == 1, "an empty one goes in");
        ItemStack lifted = new ItemStack(ModItems.HEX_VAULT.get());
        lifted.set(ModDataComponents.VAULT_MEMORY.get(), new VaultMemory(UUID.randomUUID(), 1, 1));
        helper.assertTrue(contents.store(lifted) == 0, "a vault with a store stays out");

        contents.setPerks(Long.MAX_VALUE / 2, 0, 0);
        for (int i = contents.kinds(); i < VaultRules.MAX_ENTRIES; i++) {
            ItemStack kind = new ItemStack(Items.STICK);
            kind.set(DataComponents.CUSTOM_NAME, Component.literal("kind " + i));
            contents.store(kind);
        }
        helper.assertTrue(contents.kinds() == VaultRules.MAX_ENTRIES, "it fills up to the limit");
        ItemStack another = new ItemStack(Items.APPLE);
        helper.assertTrue(contents.store(another) == 0 && contents.refusal(ItemResource.of(another)) == VaultContents.Refusal.TOO_MANY_KINDS, "one more kind stays out");
        helper.assertTrue(contents.store(new ItemStack(Items.STICK)) == 1, "more of a kind it has still goes in");
        helper.succeed();
    }

    /** Whatever breaks the vault, it comes back as one item that remembers, and that item restores everything. */
    @HexTest(padding = ROOM, maxTicks = 60)
    public static void breakingKeepsEverything(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        ServerLevel level = helper.getLevel();
        UUID id = vault.id();
        vault.contents().store(new ItemStack(Items.GOLD_INGOT, 40));
        vault.setRunes(PickaxeRunes.EMPTY.with(VaultRune.CACHE.rune(), 1));
        level.destroyBlock(helper.absolutePos(AT), true);
        List<ItemEntity> drops = drops(helper, AT);
        helper.assertTrue(drops.size() == 1, "one item drops, the loot table adds none: " + drops.size());
        ItemStack lifted = drops.getFirst().getItem();
        VaultMemory memory = lifted.get(ModDataComponents.VAULT_MEMORY.get());
        helper.assertTrue(lifted.is(ModItems.HEX_VAULT.get()) && memory != null && memory.vault().equals(id), "it remembers its store");
        helper.assertTrue(lifted.has(ModDataComponents.VAULT_RUNES.get()), "and its runes");
        VaultContents kept = VaultStore.of(level.getServer()).find(id);
        helper.assertTrue(kept != null && kept.count(ItemResource.of(Items.GOLD_INGOT)) == 40, "the items wait");
        helper.assertTrue(kept.home() == null, "and know the vault is not standing");
        helper.assertTrue(lifted.getEntityLifespan(level) == Integer.MAX_VALUE, "the item never despawns");
        drops.getFirst().discard();
        BlockPos other = AT.east(2);
        helper.setBlock(other, formedVault());
        HexVaultBlockEntity placed = helper.<HexVaultBlockEntity>getBlockEntity(other);
        placed.applyComponentsFromItemStack(lifted);
        placed.onLoad();
        helper.assertTrue(placed.id().equals(id) && placed.contents() == kept, "placed again it is the same vault");
        helper.assertTrue(kept.home() != null && kept.home().pos().equals(placed.getBlockPos()), "at its new place");
        helper.assertTrue(kept.count(ItemResource.of(Items.GOLD_INGOT)) == 40, "with all it held");
        helper.assertTrue(placed.runes().strength(VaultRune.CACHE.rune()) == 1, "and its runes");
        helper.succeed();
    }

    /** A rolled back transaction leaves no trace, a committed one all of it. */
    @HexTest(padding = ROOM)
    public static void transactionsRollBack(GameTestHelper helper) {
        VaultContents contents = vault(helper, AT).contents();
        ItemResource coal = ItemResource.of(Items.COAL);
        ItemResource flint = ItemResource.of(Items.FLINT);
        contents.store(new ItemStack(Items.COAL, 10));
        long used = contents.used();
        try (Transaction outer = Transaction.openRoot()) {
            helper.assertTrue(contents.extract(coal, 10, outer) == 10, "taken inside");
            helper.assertTrue(contents.insert(flint, 5, outer) == 5, "put inside");
            try (Transaction inner = Transaction.open(outer)) {
                contents.insert(flint, 5, inner);
            }
            helper.assertTrue(contents.count(flint) == 5, "the inner one rolled back alone");
        }
        helper.assertTrue(contents.count(coal) == 10 && contents.count(flint) == 0 && contents.used() == used, "all back as it was");
        helper.assertTrue(contents.kinds() == 1, "no empty entry stays behind");
        helper.succeed();
    }

    /** Hoppers, pylons and pipes see a normal inventory, through the stand-ins too. */
    @HexTest(padding = ROOM, maxTicks = 120)
    public static void aHopperFillsTheVault(GameTestHelper helper) {
        HexVaultBlockEntity vault = gate(helper, BUILT);
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(BUILT);
        helper.assertTrue(com.pockyl.hextale.port.ResourceHandler.ofBlock(level, at, Direction.SOUTH) == vault.contents(), "the vault is a normal inventory");
        helper.assertTrue(com.pockyl.hextale.port.ResourceHandler.ofBlock(level, at.above(), Direction.SOUTH) == vault.contents(), "and so is its door");
        // Into the door over the vault's own cell, from the front.
        BlockPos hopper = BUILT.above().south();
        helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.NORTH));
        helper.<HopperBlockEntity>getBlockEntity(hopper).setItem(0, new ItemStack(Items.COBBLESTONE, 3));
        helper.succeedWhen(() -> helper.assertTrue(vault.contents().count(ItemResource.of(Items.COBBLESTONE)) == 3
                && helper.<HopperBlockEntity>getBlockEntity(hopper).isEmpty(), "three stones went in through the door"));
    }

    /** A comparator reads how full it is; the reading follows what is held. */
    @HexTest(padding = ROOM, maxTicks = 60)
    public static void aComparatorReadsTheVault(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        VaultContents contents = vault.contents();
        helper.assertTrue(contents.signal() == 0, "empty is 0");
        contents.setPerks(VaultRules.weight(ItemResource.of(Items.DIAMOND)) * 2L, 0, 0);
        contents.store(new ItemStack(Items.DIAMOND));
        helper.assertTrue(contents.signal() == 8, "half full: " + contents.signal());
        contents.store(new ItemStack(Items.DIAMOND));
        helper.assertTrue(contents.signal() == 15, "full is 15");
        helper.succeed();
    }

    /** Two players at one screen take from one count: the second gets what is left, never a copy. */
    @HexTest(padding = ROOM)
    public static void twoPlayersTakeFromOneCount(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        ServerPlayer first = player(helper, AT);
        ServerPlayer second = player(helper, AT);
        VaultMenu one = menu(first, vault);
        VaultMenu two = menu(second, vault);
        ItemResource gold = ItemResource.of(Items.GOLD_INGOT);
        vault.contents().store(new ItemStack(Items.GOLD_INGOT, 70));
        one.act(first, gold, VaultMenu.Action.TAKE_TO_INVENTORY);
        two.act(second, gold, VaultMenu.Action.TAKE_TO_INVENTORY);
        one.act(first, gold, VaultMenu.Action.TAKE_TO_INVENTORY);
        int held = carriedIn(first, Items.GOLD_INGOT) + carriedIn(second, Items.GOLD_INGOT);
        helper.assertTrue(held == 70, "seventy came out in all: " + held);
        helper.assertTrue(vault.contents().count(gold) == 0, "nothing is left");
        one.act(first, gold, VaultMenu.Action.TAKE_STACK);
        helper.assertTrue(one.getCarried().isEmpty(), "and nothing more comes");
        helper.succeed();
    }

    /** The grid's clicks: a stack to the cursor, half, carried items back; a full inventory takes nothing and pays nothing. */
    @HexTest(padding = ROOM)
    public static void theScreenMovesItemsExactly(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        ServerPlayer player = player(helper, AT);
        VaultMenu menu = menu(player, vault);
        ItemResource coal = ItemResource.of(Items.COAL);
        VaultContents contents = vault.contents();
        contents.store(new ItemStack(Items.COAL, 100));
        menu.act(player, coal, VaultMenu.Action.TAKE_STACK);
        helper.assertTrue(menu.getCarried().getCount() == 64 && contents.count(coal) == 36, "a stack on the cursor");
        menu.act(player, ItemResource.EMPTY, VaultMenu.Action.PUT_ONE);
        helper.assertTrue(menu.getCarried().getCount() == 63 && contents.count(coal) == 37, "one back");
        menu.act(player, ItemResource.EMPTY, VaultMenu.Action.PUT_CARRIED);
        helper.assertTrue(menu.getCarried().isEmpty() && contents.count(coal) == 100, "all back");
        menu.act(player, coal, VaultMenu.Action.TAKE_HALF);
        helper.assertTrue(menu.getCarried().getCount() == 32 && contents.count(coal) == 68, "half a stack");
        menu.act(player, ItemResource.of(Items.DIAMOND), VaultMenu.Action.TAKE_STACK);
        helper.assertTrue(menu.getCarried().getCount() == 32, "nothing of what it does not hold");
        menu.setCarried(ItemStack.EMPTY);
        contents.take(coal, 68);

        // Shift-click out of the inventory, and into a full one.
        player.getInventory().setItem(9, new ItemStack(Items.FLINT, 20));
        Integer slot = menu.slots.stream().filter(candidate -> candidate.getItem().is(Items.FLINT)).map(candidate -> candidate.index).findFirst().orElseThrow();
        menu.quickMoveStack(player, slot);
        helper.assertTrue(contents.count(ItemResource.of(Items.FLINT)) == 20 && carriedIn(player, Items.FLINT) == 0, "shift-click moves the stack");
        for (int i = 0; i < net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE; i++) {
            player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        menu.act(player, ItemResource.of(Items.FLINT), VaultMenu.Action.TAKE_TO_INVENTORY);
        helper.assertTrue(contents.count(ItemResource.of(Items.FLINT)) == 20, "a full inventory takes nothing, so the vault loses nothing");
        helper.succeed();
    }

    /** A click puts nothing in, whatever is held: on the block or its door a plain click opens the screen, sneaking does nothing. */
    @HexTest(padding = ROOM)
    public static void aClickStoresNothing(GameTestHelper helper) {
        HexVaultBlockEntity vault = gate(helper, BUILT);
        ServerPlayer player = player(helper, BUILT.south(2));
        VaultContents contents = vault.contents();
        contents.store(new ItemStack(Items.FLINT, 3));
        player.getInventory().setItem(12, new ItemStack(Items.FLINT, 5));
        ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
        for (BlockPos at : List.of(BUILT, BUILT.above())) {
            for (ItemStack held : List.of(new ItemStack(Items.FLINT, 7), new ItemStack(Items.BONE, 7), worn, new ItemStack(ModItems.UNSTABLE_HEXTECH.get(), 2))) {
                helper.assertTrue(click(helper, player, at, held, false), "a plain click opens the screen with " + held);
                helper.assertTrue(ItemStack.matches(player.getMainHandItem(), held), "and leaves the hand as it was: " + player.getMainHandItem());
                if (!held.is(ModItems.UNSTABLE_HEXTECH.get())) {
                    helper.assertTrue(!click(helper, player, at, held, true), "sneaking with " + held + " opens nothing");
                    helper.assertTrue(ItemStack.matches(player.getMainHandItem(), held), "and leaves the hand as it was: " + player.getMainHandItem());
                }
            }
        }
        helper.assertTrue(contents.count(ItemResource.of(Items.FLINT)) == 3 && carriedIn(player, Items.FLINT) == 5, "nothing it holds went in");
        helper.assertTrue(contents.used() == VaultRules.weight(ItemResource.of(Items.FLINT)) * 3L, "and nothing else did");
        helper.succeed();
    }

    /** Unstable Hextech used while sneaking becomes a crystal that remembers the vault, on the block and anywhere on its door. */
    @HexTest(padding = ROOM)
    public static void sneakingPressesAnOrbIntoACrystal(GameTestHelper helper) {
        HexVaultBlockEntity vault = gate(helper, BUILT);
        ServerPlayer player = player(helper, BUILT.south(2));
        for (BlockPos at : List.of(BUILT, BUILT.above())) {
            player.getInventory().clearContent();
            click(helper, player, at, new ItemStack(ModItems.UNSTABLE_HEXTECH.get(), 2), true);
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "one orb was pressed");
            ItemStack crystal = player.getInventory().items.stream().filter(stack -> stack.is(ModItems.RIFT_LINK_CRYSTAL.get())).findFirst().orElse(ItemStack.EMPTY);
            VaultLink link = crystal.get(ModDataComponents.VAULT_LINK.get());
            helper.assertTrue(link != null && link.vault().equals(vault.id()), "into a crystal that remembers the vault");
        }
        helper.succeed();
    }

    /** A real click on {@code at}; true when it opened a screen, which a mock player's connection refuses to carry. */
    private static boolean click(GameTestHelper helper, ServerPlayer player, BlockPos at, ItemStack held, boolean sneaking) {
        BlockPos pos = helper.absolutePos(at);
        player.setShiftKeyDown(sneaking);
        player.setItemInHand(InteractionHand.MAIN_HAND, held.copy());
        try {
            player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false));
            return false;
        } catch (UnsupportedOperationException refused) {
            helper.assertTrue(refused.getMessage().contains("open_screen"), "only the screen's packet may stop a click: " + refused.getMessage());
            return true;
        }
    }

    /** A cube counts by itself on the platform or against it, twelve at the most; a gone one leaves what is held alone. */
    @HexTest(padding = ROOM)
    public static void storageCubesCountAroundThePlatform(GameTestHelper helper) {
        HexVaultBlockEntity vault = gate(helper, BUILT);
        VaultContents contents = vault.contents();
        helper.assertTrue(contents.capacity() == VaultRules.BASE_CAPACITY, "no cubes yet");
        // Against the platform's west edge: it spans x - 3 .. x + 3 round the vault, one layer under it.
        BlockPos side = BUILT.west(4).below().north();
        helper.setBlock(side, ModBlocks.STORAGE_CUBE.get());
        helper.assertTrue(contents.capacity() == VaultRules.BASE_CAPACITY + VaultRules.CUBE_CAPACITY, "one cube counts by itself");
        helper.assertTrue(helper.getBlockState(side).getValue(StorageCubeBlock.ACTIVE), "and lights up");
        BlockPos loose = BUILT.west(6).below();
        helper.setBlock(loose, ModBlocks.STORAGE_CUBE.get());
        helper.assertTrue(contents.capacity() == VaultRules.BASE_CAPACITY + VaultRules.CUBE_CAPACITY, "one off the platform does not");
        helper.assertTrue(!helper.getBlockState(loose).getValue(StorageCubeBlock.ACTIVE), "and stays dark");
        for (int x = -3; x <= 3; x++) {
            helper.setBlock(BUILT.offset(x, 0, -1), ModBlocks.STORAGE_CUBE.get());
            helper.setBlock(BUILT.offset(x, 0, 1), ModBlocks.STORAGE_CUBE.get());
        }
        helper.assertTrue(contents.capacity() == VaultRules.BASE_CAPACITY + HexVaultBlockEntity.MOST_CUBES * VaultRules.CUBE_CAPACITY,
                "twelve at the most: " + contents.capacity());
        contents.setPerks(contents.capacity(), 0, 0);
        ItemResource dirt = ItemResource.of(Items.DIRT);
        contents.store(new ItemStack(Items.DIRT, 64));
        long held = contents.count(dirt);
        helper.setBlock(side, Blocks.AIR);
        helper.assertTrue(contents.count(dirt) == held, "what was held stays");
        helper.succeed();
    }

    /** Forming opens it, a pillar gone puts it to sleep: nothing goes in or to a terminal then, and nothing is lost or locked away. */
    @HexTest(padding = ROOM, maxTicks = 200)
    public static void aPillarGoneLeavesTheVaultAsleepWithAllItHolds(GameTestHelper helper) {
        HexVaultBlockEntity vault = gate(helper, BUILT);
        VaultContents contents = vault.contents();
        ServerPlayer player = player(helper, BUILT.south(2));
        helper.assertTrue(vault.usable() && contents.open(), "a finished gate works");
        contents.store(new ItemStack(Items.GOLD_INGOT, 40));
        helper.assertTrue(com.pockyl.hextale.port.ResourceHandler.ofBlock(helper.getLevel(), helper.absolutePos(BUILT.above()), null) != null,
                "machines reach it through the door");
        helper.setBlock(BUILT.west(3).above(), Blocks.AIR);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!vault.usable() && !contents.open(), "the gate was looked at and it fell asleep"))
                .thenExecute(() -> {
                    helper.assertTrue(contents.count(ItemResource.of(Items.GOLD_INGOT)) == 40, "with all it held");
                    helper.assertTrue(helper.getBlockState(BUILT.above()).isAir(), "the air the door hung in is empty again");
                    helper.assertTrue(contents.store(new ItemStack(Items.DIRT, 4)) == 0, "nothing more goes in");
                    helper.assertTrue(contents.refusal(ItemResource.of(Items.DIRT)) == VaultContents.Refusal.ASLEEP, "and it says why");
                    helper.assertTrue(com.pockyl.hextale.port.ResourceHandler.ofBlock(helper.getLevel(), vault.getBlockPos(), null) == null, "machines get nothing");
                    helper.assertTrue(!VaultMenu.open(player, vault.id(), VaultMenu.Kind.TERMINAL, vault.getBlockPos(), net.minecraft.world.InteractionHand.MAIN_HAND,
                            net.minecraft.network.chat.Component.empty()), "and no terminal");
                    VaultMenu screen = menu(player, vault);
                    screen.act(player, ItemResource.of(Items.GOLD_INGOT), VaultMenu.Action.TAKE_TO_INVENTORY);
                    helper.assertTrue(carriedIn(player, Items.GOLD_INGOT) == 40, "by hand it still gives everything back");
                })
                .thenSucceed();
    }

    /** Hextech Dust is all it takes: a player wakes the gate and the vault works at once, however the gate is turned. */
    @HexTest(padding = ROOM)
    public static void aWokenGateWorksAtOnce(GameTestHelper helper) {
        ServerPlayer player = player(helper, BUILT.south(3));
        HexVaultBlockEntity vault = gate(helper, BUILT, Rotation.CLOCKWISE_90, player);
        helper.assertTrue(vault.usable() && vault.contents().open(), "it works");
        helper.assertTrue(vault.contents().store(new ItemStack(Items.GOLD_INGOT, 4)) == 4, "and items go in");
        var door = helper.getBlockState(BUILT.above()).getShape(helper.getLevel(), helper.absolutePos(BUILT.above())).bounds();
        helper.assertTrue(door.getZsize() == 1 && door.getXsize() < 0.5, "the door's slabs turn with the gate: " + door);
        helper.succeed();
    }

    /** The kinds that take up the most room are what the clients draw behind the door, and they travel without their data. */
    @HexTest(padding = ROOM, maxTicks = 100)
    public static void theHeaviestKindsDriftBehindTheDoor(GameTestHelper helper) {
        HexVaultBlockEntity vault = gate(helper, BUILT);
        vault.contents().store(new ItemStack(Items.GOLD_BLOCK, 64));
        vault.contents().store(new ItemStack(Items.DIRT, 1));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!vault.shown().isEmpty(), "the picture was sent"))
                .thenExecute(() -> {
                    helper.assertTrue(vault.shown().getFirst().is(Items.GOLD_BLOCK), "the heaviest comes first: " + vault.shown());
                    helper.assertTrue(vault.shown().size() == 2, "and only what is held: " + vault.shown());
                })
                .thenSucceed();
    }

    /** Out of its gate a vault sleeps with its contents; stores saved in the days of the trial still load. */
    @HexTest(padding = ROOM)
    public static void aVaultOutOfItsGateSleeps(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.HEX_VAULT.get());
        HexVaultBlockEntity vault = helper.<HexVaultBlockEntity>getBlockEntity(AT);
        vault.onLoad();
        VaultContents fresh = vault.contents();
        helper.assertTrue(!vault.usable() && !fresh.open(), "out of a gate it sleeps");
        var encoded = (net.minecraft.nbt.CompoundTag) VaultContents.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, fresh).getOrThrow();
        encoded.putBoolean("awakened", false);
        VaultContents.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded).getOrThrow();
        helper.succeed();
    }

    @HexTest(padding = ROOM, maxTicks = 200)
    public static void aTerminalPaysForDistance(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, AT);
        BlockPos terminalAt = AT.south(4);
        terminal(helper, terminalAt, vault);
        BlockPos jarAt = terminalAt.east();
        helper.setBlock(jarAt, ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity jar = helper.<EssenceTankBlockEntity>getBlockEntity(jarAt);
        jar.tank().set(EssenceMix.pure(Essence.RIFT, 1));
        helper.assertTrue(com.pockyl.hextale.port.ResourceHandler.ofBlock(level, helper.absolutePos(terminalAt), Direction.UP) == null, "a terminal is no way in for hoppers");
        ItemResource bone = ItemResource.of(Items.BONE);
        vault.contents().store(new ItemStack(Items.BONE, 40));
        VaultMenu menu = terminalMenu(player, vault, helper.absolutePos(terminalAt));
        for (int i = 0; i < VaultToll.TAKES_PER_MB; i++) {
            ItemStack before = menu.getCarried();
            menu.act(player, bone, VaultMenu.Action.PUT_ONE);
            menu.setCarried(ItemStack.EMPTY);
            menu.act(player, bone, VaultMenu.Action.TAKE_HALF);
            helper.assertTrue(!menu.getCarried().isEmpty(), "take " + i + " went through, before: " + before);
            vault.contents().store(menu.getCarried().copy());
            menu.setCarried(ItemStack.EMPTY);
        }
        helper.assertTrue(jar.tank().contents().get(Essence.RIFT) == 0, "one mB paid for eight takes");
        menu.act(player, bone, VaultMenu.Action.TAKE_HALF);
        helper.assertTrue(!menu.getCarried().isEmpty(), "dry jars still let one through");
        vault.contents().store(menu.getCarried().copy());
        menu.setCarried(ItemStack.EMPTY);
        menu.act(player, bone, VaultMenu.Action.TAKE_HALF);
        helper.assertTrue(menu.getCarried().isEmpty(), "and then make the next one wait");
        helper.assertTrue(vault.contents().count(bone) == 40, "a refused take costs no item");
        VaultMenu reopened = terminalMenu(player, vault, helper.absolutePos(terminalAt));
        reopened.act(player, bone, VaultMenu.Action.TAKE_HALF);
        helper.assertTrue(reopened.getCarried().isEmpty(), "reopening the screen changes nothing");
        VaultMenu here = menu(player, vault);
        here.act(player, bone, VaultMenu.Action.TAKE_HALF);
        helper.assertTrue(!here.getCarried().isEmpty(), "at the vault itself takes stay free");
        vault.contents().store(here.getCarried().copy());
        here.setCarried(ItemStack.EMPTY);
        helper.runAfterDelay(VaultToll.SLOW_TICKS + 2, () -> {
            menu.act(player, bone, VaultMenu.Action.TAKE_HALF);
            helper.assertTrue(!menu.getCarried().isEmpty(), "three seconds later the next one goes");
            helper.succeed();
        });
    }

    /** A link crystal ties a terminal; the tie outlives a moved vault, and a lifted vault closes it until it stands again. */
    @HexTest(padding = ROOM)
    public static void aCrystalTiesATerminal(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        ServerPlayer player = player(helper, AT);
        ServerLevel level = helper.getLevel();
        BlockPos terminalAt = AT.south(4);
        helper.setBlock(terminalAt, ModBlocks.HEX_TERMINAL.get());
        ItemStack crystal = RiftLinkCrystalItem.rememberingVault(vault.id(), vault.getBlockPos());
        player.setItemInHand(InteractionHand.MAIN_HAND, crystal);
        BlockPos absolute = helper.absolutePos(terminalAt);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        HexTerminalBlockEntity terminal = helper.<HexTerminalBlockEntity>getBlockEntity(terminalAt);
        helper.assertTrue(vault.id().equals(terminal.vault()), "the terminal knows its vault");
        helper.assertTrue(helper.getBlockState(terminalAt).getValue(HexTerminalBlock.LINKED), "and shows it");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the crystal is spent");
        VaultMenu menu = terminalMenu(player, vault, absolute);
        helper.assertTrue(menu.stillValid(player), "its screen opens");
        level.destroyBlock(helper.absolutePos(AT), true);
        helper.assertTrue(!menu.stillValid(player), "a lifted vault closes it");
        helper.assertTrue(!VaultMenu.open(player, terminal.vault(), VaultMenu.Kind.TERMINAL, absolute, InteractionHand.MAIN_HAND, Component.empty()), "and refuses to open");
        ItemEntity item = drops(helper, AT).getFirst();
        BlockPos other = AT.west(3);
        helper.setBlock(other, formedVault());
        HexVaultBlockEntity placed = helper.<HexVaultBlockEntity>getBlockEntity(other);
        placed.applyComponentsFromItemStack(item.getItem());
        placed.onLoad();
        item.discard();
        helper.assertTrue(menu.stillValid(player), "standing somewhere else, the same tie works");
        helper.succeed();
    }

    /** A Pocket Terminal takes the crystal from the other hand, and its screen needs it in the hand. */
    @HexTest(padding = ROOM)
    public static void aPocketTerminalTiesItself(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        ServerPlayer player = player(helper, AT);
        ItemStack pocket = new ItemStack(ModItems.POCKET_TERMINAL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, pocket);
        player.setItemInHand(InteractionHand.OFF_HAND, RiftLinkCrystalItem.rememberingVault(vault.id(), vault.getBlockPos()));
        pocket.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(com.pockyl.hextale.item.PocketTerminalItem.vault(pocket) != null, "the terminal is tied");
        helper.assertTrue(player.getOffhandItem().isEmpty(), "and the crystal spent");
        VaultMenu menu = new VaultMenu(1, player.getInventory(), vault.contents(), vault.id(), VaultMenu.Kind.POCKET, player.blockPosition(), InteractionHand.MAIN_HAND);
        helper.assertTrue(menu.stillValid(player), "its screen stays open while it is held");
        ItemResource bone = ItemResource.of(Items.BONE);
        vault.contents().store(new ItemStack(Items.BONE, 4));
        menu.act(player, bone, VaultMenu.Action.TAKE_STACK);
        helper.assertTrue(menu.getCarried().getCount() == 4, "far from jars a first take still goes");
        menu.setCarried(ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(!menu.stillValid(player), "put away, the screen closes");
        helper.succeed();
    }

    /** Runes: Cache gives room, Radiance pays only for new heights, Mending mends what is inside without losing it. */
    @HexTest(padding = ROOM, maxTicks = 300)
    public static void runesOnTheVault(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        VaultContents contents = vault.contents();
        vault.setRunes(PickaxeRunes.EMPTY.with(VaultRune.CACHE.rune(), 2).with(VaultRune.RADIANCE.rune(), 1).with(VaultRune.MENDING.rune(), 3));
        helper.assertTrue(contents.capacity() == (long) (VaultRules.BASE_CAPACITY * 1.5), "Cache gives room: " + contents.capacity());

        ItemStack stone = new ItemStack(Items.GOLD_BLOCK, 64);
        contents.store(stone.copy());
        long first = contents.unpaidPoints();
        helper.assertTrue(first > 0, "a new height is worth something");
        contents.paid(first);
        ItemResource gold = ItemResource.of(Items.GOLD_BLOCK);
        contents.take(gold, 64);
        contents.store(stone.copy());
        helper.assertTrue(contents.unpaidPoints() == 0, "taking out and putting back earns nothing");

        ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
        worn.setDamageValue(10);
        contents.store(worn.copy());
        helper.assertTrue(contents.count(ItemResource.of(worn)) == 1, "the worn pick is in");
        helper.succeedWhen(() -> {
            ItemStack fresh = new ItemStack(Items.IRON_PICKAXE);
            helper.assertTrue(contents.count(ItemResource.of(fresh)) == 1, "it mended whole");
            helper.assertTrue(contents.count(ItemResource.of(worn)) == 0, "and is one pick, not two");
        });
    }

    /** What is saved comes back, weights and all; an entry of a mod that left stays in the file. */
    @HexTest(padding = ROOM)
    public static void contentsSurviveSaving(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HexVaultBlockEntity vault = vault(helper, AT);
        VaultContents contents = vault.contents();
        contents.store(new ItemStack(Items.EMERALD, 9));
        ItemStack named = new ItemStack(Items.STICK);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("kept"));
        contents.store(named.copy());
        RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag saved = VaultContents.CODEC.encodeStart(ops, contents).getOrThrow();
        net.minecraft.nbt.CompoundTag tag = (net.minecraft.nbt.CompoundTag) saved;
        net.minecraft.nbt.ListTag entries = tag.getList("entries", net.minecraft.nbt.Tag.TAG_COMPOUND);
        net.minecraft.nbt.CompoundTag gone = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.CompoundTag item = new net.minecraft.nbt.CompoundTag();
        item.putString("id", "gone_mod:thing");
        gone.put("item", item);
        gone.putLong("count", 3);
        entries.add(gone);
        VaultContents read = VaultContents.CODEC.parse(ops, tag).getOrThrow();
        helper.assertTrue(read.count(ItemResource.of(Items.EMERALD)) == 9 && read.count(ItemResource.of(named)) == 1, "what was held comes back");
        helper.assertTrue(read.used() == contents.used() && read.kinds() == 2, "and weighs the same");
        net.minecraft.nbt.CompoundTag again = (net.minecraft.nbt.CompoundTag) VaultContents.CODEC.encodeStart(ops, read).getOrThrow();
        helper.assertTrue(again.getList("entries", net.minecraft.nbt.Tag.TAG_COMPOUND).size() == 3, "the unknown entry is written back");
        helper.assertTrue(read.home() != null && read.home().equals(GlobalPos.of(level.dimension(), vault.getBlockPos())), "the vault's place too");
        helper.succeed();
    }

    /** A pylon fills the vault through a stand-in; one standing right on the vault's own cell empties it. */
    @HexTest(maxTicks = 400)
    public static void pylonsFillAndEmptyTheVault(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        BlockPos start = AT.west(3);
        BlockPos end = AT.south(3);
        helper.setBlock(start, Blocks.CHEST);
        helper.setBlock(end, Blocks.CHEST);
        net.minecraft.world.Container from = helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(start);
        net.minecraft.world.Container to = helper.<net.minecraft.world.level.block.entity.ChestBlockEntity>getBlockEntity(end);
        from.setItem(0, new ItemStack(Items.COBBLESTONE, 6));
        HexPylonBlockEntity in = PylonGameTests.place(helper, ModBlocks.SENDING_PYLON.get(), start.above());
        helper.assertTrue(in.toggleLink(PylonGameTests.link(helper, AT, Direction.WEST)), "linked to the vault");
        HexPylonBlockEntity out = PylonGameTests.place(helper, ModBlocks.SENDING_PYLON.get(), AT.above());
        helper.assertTrue(out.toggleLink(PylonGameTests.link(helper, end, Direction.UP)), "linked from the vault");
        helper.succeedWhen(() -> helper.assertTrue(PylonGameTests.count(to, Items.COBBLESTONE) == 6 && PylonGameTests.count(from, Items.COBBLESTONE) == 0
                && vault.contents().count(ItemResource.of(Items.COBBLESTONE)) == 0, "the stones went through the vault"));
    }

    /** Shift and the wheel: one item out, one item back, and the count stays whole. */
    @HexTest(padding = ROOM)
    public static void theWheelMovesOneItem(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        ServerPlayer player = player(helper, AT);
        VaultMenu menu = menu(player, vault);
        ItemResource coal = ItemResource.of(Items.COAL);
        VaultContents contents = vault.contents();
        contents.store(new ItemStack(Items.COAL, 10));
        menu.act(player, coal, VaultMenu.Action.TAKE_ONE);
        menu.act(player, coal, VaultMenu.Action.TAKE_ONE);
        helper.assertTrue(contents.count(coal) == 8 && carriedIn(player, Items.COAL) == 2, "two came out one by one");
        menu.act(player, coal, VaultMenu.Action.RETURN_ONE);
        helper.assertTrue(contents.count(coal) == 9 && carriedIn(player, Items.COAL) == 1, "one went back");
        menu.act(player, ItemResource.of(Items.DIAMOND), VaultMenu.Action.RETURN_ONE);
        helper.assertTrue(contents.count(ItemResource.of(Items.DIAMOND)) == 0, "what the pockets do not hold cannot go back");
        helper.succeed();
    }

    /** A double click takes stack after stack until the pockets are full; at a terminal each stack pays, so dry jars stop it at one. */
    @HexTest(padding = ROOM, maxTicks = 200)
    public static void aDoubleClickFillsThePockets(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        ServerPlayer player = player(helper, AT);
        VaultMenu menu = menu(player, vault);
        ItemResource coal = ItemResource.of(Items.COAL);
        VaultContents contents = vault.contents();
        contents.store(new ItemStack(Items.COAL, 64));
        contents.store(new ItemStack(Items.COAL, 64));
        contents.store(new ItemStack(Items.COAL, 64));
        for (int i = 2; i < net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE; i++) {
            player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        menu.act(player, coal, VaultMenu.Action.TAKE_ALL);
        helper.assertTrue(carriedIn(player, Items.COAL) == 128 && contents.count(coal) == 64, "two free slots took two stacks: " + carriedIn(player, Items.COAL));

        player.getInventory().clearContent();
        BlockPos terminalAt = AT.south(4);
        terminal(helper, terminalAt, vault);
        VaultMenu far = terminalMenu(player, vault, helper.absolutePos(terminalAt));
        contents.store(new ItemStack(Items.COAL, 64));
        far.act(player, coal, VaultMenu.Action.TAKE_ALL);
        helper.assertTrue(carriedIn(player, Items.COAL) == 64, "dry jars let one stack through: " + carriedIn(player, Items.COAL));
        helper.succeed();
    }

    /** One button stores the pockets: the hotbar stays, and what cannot go in stays where it is. */
    @HexTest(padding = ROOM)
    public static void storingTheInventoryLeavesTheHotbar(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        ServerPlayer player = player(helper, AT);
        VaultMenu menu = menu(player, vault);
        VaultContents contents = vault.contents();
        player.getInventory().setItem(0, new ItemStack(Items.FLINT, 5));
        player.getInventory().setItem(8, new ItemStack(Items.BONE, 3));
        player.getInventory().setItem(9, new ItemStack(Items.COAL, 10));
        player.getInventory().setItem(35, new ItemStack(Items.GOLD_INGOT, 7));
        menu.act(player, ItemResource.EMPTY, VaultMenu.Action.STORE_ALL);
        helper.assertTrue(contents.count(ItemResource.of(Items.COAL)) == 10 && contents.count(ItemResource.of(Items.GOLD_INGOT)) == 7, "the pockets went in");
        helper.assertTrue(carriedIn(player, Items.COAL) == 0 && carriedIn(player, Items.GOLD_INGOT) == 0, "and left the inventory");
        helper.assertTrue(carriedIn(player, Items.FLINT) == 5 && carriedIn(player, Items.BONE) == 3, "the hotbar stayed");
        helper.assertTrue(contents.count(ItemResource.of(Items.FLINT)) == 0, "nothing of the hotbar went in");
        helper.succeed();
    }

    /** A pin outlives its last item, comes off with a second click, is saved with the store, and the row has an end. */
    @HexTest(padding = ROOM)
    public static void pinsStayUntilTakenOff(GameTestHelper helper) {
        HexVaultBlockEntity vault = vault(helper, AT);
        ServerPlayer player = player(helper, AT);
        VaultMenu menu = menu(player, vault);
        ItemResource coal = ItemResource.of(Items.COAL);
        VaultContents contents = vault.contents();
        contents.store(new ItemStack(Items.COAL, 3));
        menu.act(player, coal, VaultMenu.Action.PIN);
        helper.assertTrue(contents.pins().equals(List.of(coal)), "the pin is on");
        contents.take(coal, 3);
        helper.assertTrue(contents.count(coal) == 0 && contents.pins().contains(coal), "it stays with nothing left");

        RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag saved = VaultContents.CODEC.encodeStart(ops, contents).getOrThrow();
        helper.assertTrue(VaultContents.CODEC.parse(ops, saved).getOrThrow().pins().equals(List.of(coal)), "it is saved with the store");

        // A pin of a mod that is gone costs the row, never the store.
        net.minecraft.nbt.CompoundTag broken = (net.minecraft.nbt.CompoundTag) saved;
        net.minecraft.nbt.CompoundTag gone = new net.minecraft.nbt.CompoundTag();
        gone.putString("id", "gone_mod:thing");
        broken.getList("pins", net.minecraft.nbt.Tag.TAG_COMPOUND).add(gone);
        helper.assertTrue(VaultContents.CODEC.parse(ops, broken).isSuccess(), "a pin that cannot be read does not break the store");

        menu.act(player, coal, VaultMenu.Action.PIN);
        helper.assertTrue(contents.pins().isEmpty(), "a second click takes it off");
        List<net.minecraft.world.item.Item> kinds = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR).limit(VaultContents.MOST_PINS + 1L).toList();
        kinds.forEach(item -> menu.act(player, ItemResource.of(item), VaultMenu.Action.PIN));
        helper.assertTrue(contents.pins().size() == VaultContents.MOST_PINS, "the row ends at its widest: " + contents.pins().size());
        helper.succeed();
    }
}
